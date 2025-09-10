package com.titiplex.familyhub.sync;

import com.titiplex.familyhub.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public final class PeerStore {
    public record Peer(String deviceId, String publicKey, String address, int port,
                       String pairingSecretHash, String signPublicKey) {
    }

    public static void addPeer(Peer p) throws Exception {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT OR REPLACE INTO trusted_peers(device_id, public_key, address, port, pairing_secret_hash, sign_public_key, added_at) " +
                             "VALUES(?,?,?,?,?,?,strftime('%Y-%m-%dT%H:%M:%fZ','now'))")) {
            ps.setString(1, p.deviceId());
            ps.setString(2, p.publicKey());
            ps.setString(3, p.address());
            ps.setInt(4, p.port());
            ps.setString(5, p.pairingSecretHash());
            ps.setString(6, p.signPublicKey());
            ps.executeUpdate();
        }
    }

    public static List<Peer> list() throws Exception {
        try (Connection c = Database.get();
             ResultSet rs = c.createStatement().executeQuery(
                     "SELECT device_id, public_key, address, port, pairing_secret_hash, sign_public_key FROM trusted_peers ORDER BY added_at DESC")) {
            List<Peer> out = new ArrayList<>();
            while (rs.next())
                out.add(new Peer(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getString(5), rs.getString(6)));
            return out;
        }
    }

    public static String lastSyncAt(String peerId) throws Exception {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT last_sync_at FROM sync_peer_state WHERE peer_device_id=?")) {
            ps.setString(1, peerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
            return "1970-01-01T00:00:00.000Z";
        }
    }

    public static void updateLastSync(String peerId, String ts) throws Exception {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("INSERT INTO sync_peer_state(peer_device_id,last_sync_at) VALUES(?,?) ON CONFLICT(peer_device_id) DO UPDATE SET last_sync_at=excluded.last_sync_at")) {
            ps.setString(1, peerId);
            ps.setString(2, ts);
            ps.executeUpdate();
        }
    }
}