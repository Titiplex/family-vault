package org.tsumiyoku.familyhub.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.tsumiyoku.familyhub.util.AppPaths;
import org.tsumiyoku.familyhub.util.Crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DeviceIdentity {
    private static final ObjectMapper M = new ObjectMapper();
    private static DeviceIdentity INSTANCE;

    public final String deviceId;
    public final String publicKey;      // X25519
    public final String privateKey;     // X25519
    public final String signPublicKey;  // Ed25519
    public final String signPrivateKey; // Ed25519

    private DeviceIdentity(String deviceId, String pub, String priv, String spub, String spriv) {
        this.deviceId = deviceId;
        this.publicKey = pub;
        this.privateKey = priv;
        this.signPublicKey = spub;
        this.signPrivateKey = spriv;
    }

    public static synchronized DeviceIdentity get() {
        if (INSTANCE != null) return INSTANCE;
        Path f = AppPaths.dataDir().resolve("device.json");
        try {
            if (Files.exists(f)) {
                Map<?, ?> m = M.readValue(Files.readString(f), Map.class);
                String id = (String) m.get("deviceId");
                String pub = (String) m.get("publicKey");
                String priv = (String) m.get("privateKey");
                String spub = (String) m.get("signPublicKey");
                String spriv = (String) m.get("signPrivateKey");
                // rétro-compat: si absents, crée la paire Ed25519 et sauvegarde
                if (spub == null || spriv == null) {
                    Crypto.SigPair sig = Crypto.generateEd25519();
                    spub = sig.publicB64();
                    spriv = sig.privateB64();
                    Map<String, Object> out = new HashMap<>();
                    out.put("deviceId", id);
                    out.put("publicKey", pub);
                    out.put("privateKey", priv);
                    out.put("signPublicKey", spub);
                    out.put("signPrivateKey", spriv);
                    Files.writeString(f, M.writerWithDefaultPrettyPrinter().writeValueAsString(out));
                }
                INSTANCE = new DeviceIdentity(id, pub, priv, spub, spriv);
            } else {
                var kp = Crypto.generateX25519();
                var sig = Crypto.generateEd25519();
                String id = UUID.randomUUID().toString();
                INSTANCE = new DeviceIdentity(id, kp.publicB64(), kp.privateB64(), sig.publicB64(), sig.privateB64());
                Map<String, Object> out = new HashMap<>();
                out.put("deviceId", id);
                out.put("publicKey", kp.publicB64());
                out.put("privateKey", kp.privateB64());
                out.put("signPublicKey", sig.publicB64());
                out.put("signPrivateKey", sig.privateB64());
                Files.writeString(f, M.writerWithDefaultPrettyPrinter().writeValueAsString(out));
            }
            return INSTANCE;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
