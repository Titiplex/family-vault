package org.tsumiyoku.familyhub.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.tsumiyoku.familyhub.util.AppPaths;
import org.tsumiyoku.familyhub.util.Crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public final class DeviceIdentity {
    private static final ObjectMapper M = new ObjectMapper();
    private static DeviceIdentity INSTANCE;

    public final String deviceId;
    public final String publicKey;
    public final String privateKey;

    private DeviceIdentity(String deviceId, String pub, String priv) {
        this.deviceId = deviceId; this.publicKey = pub; this.privateKey = priv;
    }

    public static synchronized DeviceIdentity get() {
        if (INSTANCE != null) return INSTANCE;
        Path f = AppPaths.dataDir().resolve("device.json");
        try {
            if (Files.exists(f)) {
                Map<?,?> m = M.readValue(Files.readString(f), Map.class);
                INSTANCE = new DeviceIdentity((String)m.get("deviceId"), (String)m.get("publicKey"), (String)m.get("privateKey"));
            } else {
                Crypto.KeyPairX kp = Crypto.generateX25519();
                String id = UUID.randomUUID().toString();
                INSTANCE = new DeviceIdentity(id, kp.publicB64(), kp.privateB64());
                String json = M.writerWithDefaultPrettyPrinter().writeValueAsString(
                        Map.of("deviceId", id, "publicKey", kp.publicB64(), "privateKey", kp.privateB64())
                );
                Files.writeString(f, json);
            }
            return INSTANCE;
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}