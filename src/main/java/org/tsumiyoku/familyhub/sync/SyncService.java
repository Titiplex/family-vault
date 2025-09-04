package org.tsumiyoku.familyhub.sync;

import org.tsumiyoku.familyhub.db.Database;
import org.tsumiyoku.familyhub.util.Crypto;
import org.tsumiyoku.familyhub.util.Dialogs;
import org.tsumiyoku.familyhub.util.Upnp;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SyncService {
    private static final int DEFAULT_PORT = 56565;
    private static final ExecutorService EXEC = Executors.newCachedThreadPool();
    private static ServerSocket server;
    private static int listenPort = DEFAULT_PORT;
    private static volatile boolean running = false;

    public static int getListenPort() {
        return listenPort;
    }

    public static void setListenPort(int p) {
        listenPort = p;
    }

    public static void syncAllKnownPeers() {
        try {
            for (PeerStore.Peer p : PeerStore.list()) {
                connectToPeer(p.address(), p.port());
            }
        } catch (Exception e) {
            Dialogs.error("Sync", "syncAllKnownPeers", e);
        }
    }

    // 3) au démarrage serveur, tenter un mapping UPnP (best-effort)
    public static void startServer() {
        if (running) return;
        running = true;
        // Tentative UPnP (non bloquant) pour l'Internet sans serveur tiers
        Upnp.tryMapTcpPort(listenPort, "FamilyHub P2P");
        EXEC.submit(() -> {
            try (ServerSocket ss = new ServerSocket(listenPort)) {
                server = ss;
                while (running) {
                    Socket s = ss.accept();
                    EXEC.submit(() -> handleConnection(s, /*isServer*/true));
                }
            } catch (Exception e) {
                running = false;
                Dialogs.error("Sync", "Serveur arrêté", e);
            }
        });
    }

    public static void stopServer() {
        running = false;
        try {
            if (server != null) server.close();
        } catch (Exception ignored) {
        }
    }

    public static void connectToPeer(String host, int port) {
        EXEC.submit(() -> {
            try (Socket s = new Socket(host, port)) {
                handleConnection(s, true);
            } catch (Exception e) {
                Dialogs.error("Sync", "Connexion pair " + host + ":" + port, e);
            }
        });
    }

    private static void handleConnection(Socket s, boolean b) {
        try (Socket autoClose = s;
             InputStream in = s.getInputStream();
             OutputStream out = s.getOutputStream()) {

            DeviceIdentity me = DeviceIdentity.get();

// 1) Hello clair signé
            byte[] toSign = Protocol.helloToBeSigned(me.deviceId, me.publicKey, me.signPublicKey);
            String sig = Crypto.signEd25519(toSign, me.signPrivateKey);
            Protocol.sendFrame(out, Protocol.toJson(new Protocol.Hello("hello", me.deviceId, me.publicKey, me.signPublicKey, sig)));

            Protocol.Hello peerHello = Protocol.fromJson(Protocol.recvFrame(in), Protocol.Hello.class);

// 2) Vérif pair + vérif signature Ed25519
            PeerStore.Peer known = null;
            String peerId = "";
            for (PeerStore.Peer p : PeerStore.list()) {
                if (p.deviceId().equals(peerHello.deviceId()) && p.publicKey().equals(peerHello.publicKey())) {
                    known = p;
                    peerId = p.deviceId();
                    break;
                }
            }
            if (known == null) throw new RuntimeException("Pair inconnu: " + peerHello.deviceId());

            byte[] peerToSign = Protocol.helloToBeSigned(peerHello.deviceId(), peerHello.publicKey(), peerHello.signPublicKey());
            boolean okSig = Crypto.verifyEd25519(
                    peerToSign, peerHello.signature(), known.signPublicKey());
            if (!okSig) throw new RuntimeException("Signature HELLO invalide pour " + peerHello.deviceId());

            // 3) ECDH + HKDF (comme avant)
            String info = (me.deviceId.compareTo(peerHello.deviceId()) < 0 ?
                    me.deviceId + "|" + peerHello.deviceId() : peerHello.deviceId() + "|" + me.deviceId) + "|" + known.pairingSecretHash();
            byte[] sessionKey = Crypto.deriveSharedKey(me.privateKey, peerHello.publicKey(), info);


            // 4) Echange des curseurs (chiffré)
            String sinceLocal = PeerStore.lastSyncAt(peerId);
            Protocol.sendFrame(out, Crypto.encrypt(sessionKey, Protocol.toJson(new Protocol.SyncRequest("sync_request", sinceLocal)), null));
            Protocol.SyncRequest theirReq = Protocol.fromJson(Crypto.decrypt(sessionKey, Protocol.recvFrame(in), null), Protocol.SyncRequest.class);
            String sinceRemote = theirReq.since();

            // 5) Envoi de mon delta depuis sinceRemote, puis réception du leur depuis sinceLocal
            sendAllTablesDelta(sessionKey, out, sinceRemote);
            receiveApplyTables(sessionKey, in);

            PeerStore.updateLastSync(peerId, Instant.now().toString());
        } catch (Exception e) {
            Dialogs.error("Sync", "Erreur connexion pair", e);
        }
    }

    private static final List<String> TABLES = List.of(
            "users", "lists", "events", "recipes", "messages", "photos", "contacts", "activities", "documents", "budget", "meals", "timetable", "places"
    );

    private static void sendAllTablesDelta(byte[] key, OutputStream out, String since) throws Exception {
        try (Connection c = Database.get()) {
            for (String t : TABLES) {
                // users : ne pas exposer password_hash
                String cols = t.equals("users") ? "id,uuid,name,email,updated_at,deleted" : "*";
                String userCol = t.equals("users") ? "id" : "user_id";
                String sql = "SELECT " + cols + ", (SELECT uuid FROM users u WHERE u.id=" + userCol + ") AS user_uuid " +
                        "FROM " + t + " WHERE updated_at > ?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, since);
                    ResultSet rs = ps.executeQuery();
                    var rows = new ArrayList<Map<String, Object>>();
                    var md = rs.getMetaData();
                    int n = md.getColumnCount();
                    while (rs.next()) {
                        var m = new LinkedHashMap<String, Object>();
                        for (int i = 1; i <= n; i++) {
                            String col = md.getColumnLabel(i);
                            if ("password_hash".equalsIgnoreCase(col)) continue;
                            m.put(col, rs.getObject(i));
                        }
                        rows.add(m);
                    }
                    Protocol.TableRows payload = new Protocol.TableRows("rows", t, rows, Instant.now().toString());
                    Protocol.sendFrame(out, Crypto.encrypt(key, Protocol.toJson(payload), null));
                }
            }
            Protocol.sendFrame(out, Crypto.encrypt(key, Protocol.toJson(new Protocol.Done("done", Instant.now().toString())), null));
        }
    }

    private static void receiveApplyTables(byte[] key, InputStream in) throws Exception {
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            while (true) {
                byte[] enc = Protocol.recvFrame(in);
                byte[] plain = Crypto.decrypt(key, enc, null);
                String s = new String(plain, 0, Math.min(20, plain.length));
                if (s.contains("\"done\"")) break;
                Protocol.TableRows rows = Protocol.fromJson(plain, Protocol.TableRows.class);
                applyRows(c, rows);
            }
            c.commit();
        }
    }

    private static void applyRows(Connection c, Protocol.TableRows rows) throws Exception {
        String t = rows.table();
        for (Map<String, Object> in : rows.rows()) {
            String uuid = (String) in.get("uuid");
            if (uuid == null) continue;

            // Résoudre user_id à partir de user_uuid si fourni
            Integer userId = null;
            if (in.containsKey("user_uuid")) {
                userId = resolveUserId(c, (String) in.get("user_uuid"),
                        (String) in.getOrDefault("name", null),
                        (String) in.getOrDefault("email", null));
            }

            String remoteTs = (String) in.get("updated_at");
            if (remoteTs == null) remoteTs = "1970-01-01T00:00:00.000Z";

            // Local ?
            try (PreparedStatement ps = c.prepareStatement("SELECT updated_at FROM " + t + " WHERE uuid=?")) {
                ps.setString(1, uuid);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String localTs = rs.getString(1);
                    if (localTs != null && localTs.compareTo(remoteTs) >= 0) {
                        continue; // on garde local (LWW)
                    }
                }
            }
            upsert(c, t, in, userId);
        }
    }

    private static int resolveUserId(Connection c, String userUuid, String name, String email) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE uuid=?")) {
            ps.setString(1, userUuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        // créer l'utilisateur fantôme (sans password_hash)
        try (PreparedStatement ins = c.prepareStatement("INSERT INTO users(uuid,name,email,deleted) VALUES(?,?,?,0)", Statement.RETURN_GENERATED_KEYS)) {
            ins.setString(1, userUuid);
            ins.setString(2, name);
            ins.setString(3, email);
            ins.executeUpdate();
            ResultSet gk = ins.getGeneratedKeys();
            if (gk.next()) return gk.getInt(1);
        }
        // fallback
        throw new RuntimeException("Impossible de résoudre user_id pour " + userUuid);
    }

    private static void upsert(Connection c, String table, Map<String, Object> in, Integer resolvedUserId) throws Exception {
        // Prépare une UPSERT sur 'uuid' (ignore 'id' et 'user_uuid' ; jamais 'password_hash')
        LinkedHashMap<String, Object> m = new LinkedHashMap<>(in);
        m.remove("id");
        m.remove("user_uuid");
        if ("users".equals(table)) m.remove("password_hash");
        if (!"users".equals(table) && resolvedUserId != null) m.put("user_id", resolvedUserId);

        // Liste colonnes
        List<String> cols = new ArrayList<>(m.keySet());
        // Construction SQL
        String colList = String.join(",", cols);
        String placeholders = String.join(",", Collections.nCopies(cols.size(), "?"));

        StringBuilder up = new StringBuilder();
        up.append("INSERT INTO ").append(table).append("(").append(colList).append(") VALUES(").append(placeholders).append(") ");
        up.append("ON CONFLICT(uuid) DO UPDATE SET ");
        boolean first = true;
        for (String col : cols) {
            if ("uuid".equalsIgnoreCase(col)) continue;
            if (!first) up.append(",");
            up.append(col).append("=excluded.").append(col);
            first = false;
        }

        try (PreparedStatement ps = c.prepareStatement(up.toString())) {
            for (int i = 0; i < cols.size(); i++) {
                ps.setObject(i + 1, m.get(cols.get(i)));
            }
            ps.executeUpdate();
        }
    }
}