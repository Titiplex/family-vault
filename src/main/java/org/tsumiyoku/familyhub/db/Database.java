package org.tsumiyoku.familyhub.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.tsumiyoku.familyhub.util.AppPaths;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static Connection conn;
    private static int currentUserId = -1;

    public static void init() {
        try {
            String url = "jdbc:sqlite:" + AppPaths.dbFile();

            System.out.println("[Flyway] DB URL = " + url);
            System.out.println("[Flyway] Migrations path = classpath:db/migration");

            var cfg = Flyway.configure()
                    .dataSource(url, "", "")
                    .locations("filesystem:src/main/resources/db/migration")
                    .baselineOnMigrate(false)        // on gère le baseline nous-mêmes
                    .connectRetries(2)
                    .failOnMissingLocations(true)
                    .mixed(true);

            var flyway = cfg.load();
            try (Connection raw = DriverManager.getConnection(url)) {
                boolean hasTables = hasAnyUserTable(raw);
                boolean hasHistory = hasFlywayHistory(raw);

                // Si schéma existant sans historique → baseline à V0 pour accepter V1__...
                if (hasTables && !hasHistory) {
                    System.out.println("[Flyway] Baseline (schéma existant, pas d'historique)...");
                    FluentConfiguration cfg2 =
                            Flyway.configure()
                                    .dataSource(url, "", "")
                                    .locations("classpath:db/migration")
                                    .baselineVersion("0")
                                    .baselineOnMigrate(true)
                                    .mixed(true);
                    cfg2.load().baseline();
                } else if (!hasTables) {
                    if (!createTables(url)) throw new RuntimeException("table creation failed");
                }
            }
            // Répare (checksums) si un script a été modifié
            try {
                flyway.repair();
            } catch (Exception ignore) {
            }

            // Migre
            var res = flyway.migrate();
            System.out.println("[Flyway] Migrations appliquées: " + res.migrationsExecuted);

            conn = java.sql.DriverManager.getConnection(url);
        } catch (Exception e) {
            throw new RuntimeException("DB init failed", e);
        }
    }

    private static boolean hasFlywayHistory(Connection c) {
        try (var rs = c.getMetaData().getTables(null, null, "flyway_schema_history", null)) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private static boolean hasAnyUserTable(Connection c) {
        try (var rs = c.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String t = rs.getString("TABLE_NAME");
                if (t != null && !t.equalsIgnoreCase("flyway_schema_history")) return true;
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    public static Connection get() throws SQLException {
        if (conn == null || conn.isClosed()) {
            init();
        }
        return conn;
    }

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static void setCurrentUserId(int id) {
        currentUserId = id;
    }

    private static boolean createTables(String url) throws IOException, SQLException {
        BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/db/initialize/V1__init.sql"));
        StringBuilder sqlScript = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sqlScript.append(line).append("\n");
        }
        reader.close();

        conn = DriverManager.getConnection(url);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(sqlScript.toString());
        stmt.close();

        return true;
    }
}
