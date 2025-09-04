package org.tsumiyoku.familyhub.sync;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public final class Protocol {
    public static final ObjectMapper M = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static void sendFrame(OutputStream out, byte[] bytes) throws Exception {
        DataOutputStream d = new DataOutputStream(out);
        d.writeInt(bytes.length);
        d.write(bytes);
        d.flush();
    }

    public static byte[] recvFrame(InputStream in) throws Exception {
        DataInputStream d = new DataInputStream(in);
        int len = d.readInt();
        byte[] b = new byte[len];
        d.readFully(b);
        return b;
    }

    // Non chiffré (échange public keys + id)
    public record Hello(String type, String deviceId, String publicKey) {
    }

    // Chiffré
    public record SyncRequest(String type, String since) {
    }

    public record TableRows(String type, String table, List<Map<String, Object>> rows, String now) {
    }

    public record Done(String type, String now) {
    }

    public static byte[] toJson(Object o) throws Exception {
        return M.writeValueAsBytes(o);
    }

    public static <T> T fromJson(byte[] b, Class<T> cls) throws Exception {
        return M.readValue(b, cls);
    }
}