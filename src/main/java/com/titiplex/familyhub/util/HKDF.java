package com.titiplex.familyhub.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public final class HKDF {
    private HKDF() {
    }

    public static byte[] hkdfSha256(byte[] ikm, byte[] salt, byte[] info, int outLen) {
        try {
            if (salt == null) salt = new byte[32];
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(salt, "HmacSHA256"));
            byte[] prk = mac.doFinal(ikm);

            byte[] t = new byte[0];
            byte[] okm = new byte[outLen];
            int pos = 0;
            int i = 1;
            while (pos < outLen) {
                mac.init(new SecretKeySpec(prk, "HmacSHA256"));
                mac.update(t);
                if (info != null) mac.update(info);
                mac.update((byte) i);
                t = mac.doFinal();
                int n = Math.min(t.length, outLen - pos);
                System.arraycopy(t, 0, okm, pos, n);
                pos += n;
                i++;
            }
            return okm;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] str(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
