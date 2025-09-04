package org.tsumiyoku.familyhub.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

public final class Password {
    private Password() {
    }

    public static String hash(String raw) {
        if (raw == null || raw.isBlank()) return null; // mot de passe optionnel
        return BCrypt.withDefaults().hashToString(12, raw.toCharArray());
    }

    public static boolean verify(String raw, String hash) {
        if (hash == null || hash.isBlank()) return true; // comptes existants sans mdp
        return BCrypt.verifyer().verify(raw == null ? new char[0] : raw.toCharArray(), hash).verified;
    }
}
