package com.titiplex.familyhub.db;

import com.titiplex.familyhub.util.AppPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static int currentUserId = -1;
    private static String URL;

    /**
     * Retourne une NOUVELLE connexion à chaque appel (thread-safe par design).
     */
    public static Connection get() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement s = c.createStatement()) {
            s.execute("pragma foreign_keys=on");
            s.execute("pragma journal_mode=wal");    // meilleure concurrence lecture/écriture
            s.execute("pragma busy_timeout=5000");   // évite les 'database is locked'
            s.execute("pragma synchronous=normal");  // perfs OK pour app desktop
        }
        return c;
    }

    public static void init() {
        Path dbPath = AppPaths.dbFile();
        try {
            Files.createDirectories(dbPath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create DB directory: " + dbPath.getParent(), e);
        }
        if (!Files.isWritable(dbPath.getParent())) {
            throw new RuntimeException("Database directory is not writable: " + dbPath.getParent());
        }

        // URL SQLite correcte (Windows OK)
        URL = "jdbc:sqlite:" + dbPath.toAbsolutePath().normalize().toString().replace("\\", "/");
        System.out.println("[URL] " + URL);

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }

        try {
            // Ouvre une connexion temporaire juste pour init + migrations
            try (Connection conn = get()) {
                var migDir = Migrations.ensureExtractedMigrations();
                Migrations.SimpleMigrator.migrate(conn, migDir);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }
    }

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static void setCurrentUserId(int id) {
        currentUserId = id;
    }
}