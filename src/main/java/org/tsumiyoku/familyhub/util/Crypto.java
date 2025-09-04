package org.tsumiyoku.familyhub.util;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.NamedParameterSpec;
import java.util.Base64;

public final class Crypto {
    private Crypto() {
    }

    public record KeyPairX(String publicB64, String privateB64) {
    }

    public static KeyPairX generateX25519() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("XDH");
            kpg.initialize(new NamedParameterSpec("X25519"));
            KeyPair kp = kpg.generateKeyPair();
            return new KeyPairX(
                    Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()),
                    Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded())
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PublicKey decodePublic(String b64) {
        try {
            return KeyFactory.getInstance("XDH")
                    .generatePublic(new java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(b64)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PrivateKey decodePrivate(String b64) {
        try {
            return KeyFactory.getInstance("XDH")
                    .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(Base64.getDecoder().decode(b64)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] deriveSharedKey(String myPrivB64, String theirPubB64, String saltInfo) {
        try {
            PrivateKey priv = decodePrivate(myPrivB64);
            PublicKey pub = decodePublic(theirPubB64);
            KeyAgreement ka = KeyAgreement.getInstance("XDH");
            ka.init(priv);
            ka.doPhase(pub, true);
            byte[] secret = ka.generateSecret();
            byte[] key = HKDF.hkdfSha256(secret,
                    HKDF.str("familyhub-salt"),
                    HKDF.str("familyhub-" + saltInfo),
                    32);
            java.util.Arrays.fill(secret, (byte) 0);
            return key;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encrypt(byte[] key, byte[] plaintext, byte[] aad) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            if (aad != null) c.updateAAD(aad);
            byte[] ct = c.doFinal(plaintext);
            byte[] out = new byte[12 + ct.length];
            System.arraycopy(iv, 0, out, 0, 12);
            System.arraycopy(ct, 0, out, 12, ct.length);
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decrypt(byte[] key, byte[] ivCt, byte[] aad) {
        try {
            byte[] iv = java.util.Arrays.copyOfRange(ivCt, 0, 12);
            byte[] ct = java.util.Arrays.copyOfRange(ivCt, 12, ivCt.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            if (aad != null) c.updateAAD(aad);
            return c.doFinal(ct);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}