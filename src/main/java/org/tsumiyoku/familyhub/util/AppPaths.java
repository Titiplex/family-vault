package org.tsumiyoku.familyhub.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppPaths {
    private AppPaths() {
    }

    /**
     * Répertoire de données (surchargable par -Dfamilyhub.dataDir).
     */
    public static Path dataDir() {
        String override = System.getProperty("familyhub.dataDir");
        if (override != null && !override.isBlank()) return ensure(Path.of(override));

        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String base = System.getenv("LOCALAPPDATA");
            if (base == null || base.isBlank()) base = System.getProperty("user.home");
            return ensure(Path.of(base, "FamilyHub"));
        } else if (os.contains("mac")) {
            return ensure(Path.of(System.getProperty("user.home"), "Library", "Application Support", "FamilyHub"));
        } else {
            return ensure(Path.of(System.getProperty("user.home"), ".local", "share", "familyhub"));
        }
    }

    public static Path dbFile() {
        return dataDir().resolve("app.db");
    }

    public static Path mediaDir() {
        return ensure(dataDir().resolve("media"));
    }

    public static Path exportDir() {
        return ensure(dataDir().resolve("exports"));
    }

    private static Path ensure(Path p) {
        try {
            Files.createDirectories(p);
        } catch (IOException ignored) {
        }
        return p;
    }
}