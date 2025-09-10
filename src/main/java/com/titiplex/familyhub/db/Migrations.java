package com.titiplex.familyhub.db;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public final class Migrations {
    // Dossier de destination pour les scripts extraits
    public static Path ensureExtractedMigrations() throws Exception {
        Path target = Path.of(System.getProperty("user.home"),
                "AppData", "Local", "FamilyHub", "migrations");
        Files.createDirectories(target);

        // 1) Lister tous les fichiers *.sql sous /db/migration dans le runtime (IntelliJ, jar, jimage)
        List<String> files = listSqlResourceNames("/db/migration");
        if (files.isEmpty()) {
            throw new IllegalStateException(
                    "Aucune migration trouvée dans les ressources (/db/migration). " +
                            "Vérifie que tes .sql sont bien sous src/main/resources/db/migration");
        }

        // 2) Copier si absent
        for (String res : files) {
            String fileName = res.substring(res.lastIndexOf('/') + 1);
            Path out = target.resolve(fileName);
            if (!Files.exists(out)) {
                try (InputStream in = Migrations.class.getResourceAsStream(res)) {
                    if (in == null) throw new IOException("Ressource introuvable: " + res);
                    Files.copy(in, out);
                }
            }
        }

        // Log visuel + sanity check
        try (Stream<Path> s = Files.list(target)) {
            List<String> present = s.filter(p -> p.toString().endsWith(".sql"))
                    .map(p -> p.getFileName().toString()).sorted().toList();
            System.out.println("[Migrations] Extraites vers: " + target);
            System.out.println("[Migrations] Fichiers: " + present);
            if (present.isEmpty()) throw new IllegalStateException("Dossier migrations vide: " + target);
        }

        return target;
    }

    // Liste les chemins ressources absolus (ex: "/db/migration/V1__init.sql")
    private static List<String> listSqlResourceNames(String resourceDir) throws Exception {
        if (!resourceDir.startsWith("/")) resourceDir = "/" + resourceDir;

        URL url = Migrations.class.getResource(resourceDir);
        if (url == null) {
            // Cas jimage: on ne peut pas lister via URL de dir -> on tente FileSystem "jrt:/"
            return listFromJrt(resourceDir);
        }

        switch (url.getProtocol()) {
            case "file": {
                Path dir = Paths.get(url.toURI());
                try (Stream<Path> s = Files.list(dir)) {
                    String finalResourceDir = resourceDir;
                    return s.filter(p -> p.toString().endsWith(".sql"))
                            .map(p -> finalResourceDir + "/" + p.getFileName()).toList();
                }
            }
            case "jar": {
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                try (JarFile jar = conn.getJarFile()) {
                    String prefix = resourceDir.substring(1); // sans le premier '/'
                    return jar.stream()
                            .map(ZipEntry::getName)
                            .filter(n -> n.startsWith(prefix + "/") && n.endsWith(".sql"))
                            .map(n -> "/" + n)
                            .toList();
                }
            }
            case "jrt": // jlink image
            default:
                return listFromJrt(resourceDir);
        }
    }

    // Parcours des ressources dans l'image modulaire jlink ("jrt:/modules/<module>/<path>")
    private static List<String> listFromJrt(String resourceDir) throws Exception {
        String moduleName = Migrations.class.getModule().getName(); // ex: FamilyHub.main ou com.titiplex.merged.module
        if (moduleName == null) return List.of(); // cas improbable
        FileSystem fs;
        try {
            fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        } catch (FileSystemNotFoundException e) {
            fs = FileSystems.newFileSystem(URI.create("jrt:/"), Map.of());
        }
        String rel = resourceDir.startsWith("/") ? resourceDir.substring(1) : resourceDir;
        Path dir = fs.getPath("/modules", moduleName, rel);
        if (!Files.isDirectory(dir)) return List.of();

        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().endsWith(".sql"))
                    .map(p -> "/" + rel + "/" + p.getFileName().toString())
                    .toList();
        }
    }

    /* ---------- Ton SimpleMigrator inchangé, mais protégé contre null ---------- */

    public static final class SimpleMigrator {
        private static final DateTimeFormatter TS = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        public static void migrate(Connection c, Path dir) throws Exception {
            if (dir == null || !Files.isDirectory(dir)) {
                throw new IllegalArgumentException("Dossier migrations invalide: " + dir);
            }

            try (Statement st = c.createStatement()) {
                st.execute("""
                        create table if not exists schema_migrations(
                          version integer primary key,
                          name TEXT not null,
                          checksum TEXT not null,
                          applied_at TEXT not null
                        )""");
            }

            Map<Integer, String> applied = method(c);

            List<Path> files;
            try (Stream<Path> s = Files.list(dir)) {
                files = s
                        .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".sql"))
                        .sorted(Comparator.comparingInt(p -> {
                            String fname = p.getFileName().toString();
                            Matcher m = Pattern.compile("(?i)^V?(\\d+)__.+\\.sql$").matcher(fname);
                            if (!m.matches()) return Integer.MAX_VALUE;
                            return Integer.parseInt(m.group(1));
                        }))
                        .filter(p -> Pattern.compile("(?i)^V?(\\d+)__.+\\.sql$")
                                .matcher(p.getFileName().toString()).matches())
                        .toList();
            }

            boolean oldAuto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                for (Path p : files) {
                    String name = p.getFileName().toString();
                    Matcher m = Pattern.compile("(?i)^V?(\\d+)__.+\\.sql$").matcher(name);
                    if (!m.matches()) continue;

                    int ver = Integer.parseInt(m.group(1));
                    String raw = Files.readString(p);
                    String sql = stripBom(raw);
                    String sum = sha256(sql);

                    if (applied.containsKey(ver)) {
                        if (!applied.get(ver).equals(sum)) {
                            System.out.println("Script modifié après application: " + name);
//                            throw new IllegalStateException("Script modifié après application: " + name);
                        }
                        continue;
                    }

                    // Exécute le script en CONSOMMANT les éventuels ResultSet
                    runSqlScript(c, sql);

                    try (var ins = c.prepareStatement(
                            "insert into schema_migrations(version,name,checksum,applied_at) values(?,?,?,?)")) {
                        ins.setInt(1, ver);
                        ins.setString(2, name);
                        ins.setString(3, sum);
                        ins.setString(4, LocalDateTime.now().format(TS));
                        ins.executeUpdate();
                    }

                    System.out.println("[Migrations] Applied " + name);
                }
                c.commit();
            } catch (Exception e) {
                try {
                    c.rollback();
                } catch (Exception ignored) {
                }
                throw e;
            } finally {
                try {
                    c.setAutoCommit(oldAuto);
                } catch (Exception ignored) {
                }
            }
        }

        private static String stripBom(String s) {
            if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') return s.substring(1);
            assert s != null;
            return s.replace("\r\n", "\n");
        }

        private static void runSqlScript(Connection c, String script) throws SQLException {
            // On découpe proprement, on ignore BEGIN/COMMIT présents dans le fichier
            for (String stmt : splitSqlStatements(script)) {
                String s = stmt.trim();
                if (s.isEmpty()) continue;

                String low = s.toLowerCase(Locale.ROOT);
                if (low.equals("begin") || low.startsWith("begin transaction")) continue;
                if (low.equals("commit") || low.equals("end")) continue;

                try (Statement st = c.createStatement()) {
                    // IMPORTANT: consommer le ResultSet si présent
                    boolean hasRs = st.execute(s);
                    if (hasRs) {
                        try (ResultSet rs = st.getResultSet()) {
                            while (rs != null && rs.next()) { /* consume */ }
                        }
                    }
                } catch (SQLException ex) {
                    System.err.println("[Migrations] Statement failed:\n" + s);
                    throw ex;
                }
            }
        }

        private static List<String> splitSqlStatements(String script) {
            // identique à ta version, avec juste normalisation BOM/CRLF déjà faite avant
            List<String> out = new ArrayList<>();
            StringBuilder cur = new StringBuilder();

            boolean inString = false;
            char quote = 0;
            boolean inLineComment = false;
            boolean inTrigger = false;

            for (int i = 0; i < script.length(); i++) {
                char ch = script.charAt(i);
                char next = (i + 1 < script.length()) ? script.charAt(i + 1) : '\0';

                if (!inString && !inLineComment && ch == '-' && next == '-') {
                    inLineComment = true;
                    i++;
                    continue;
                }
                if (inLineComment) {
                    if (ch == '\n' || ch == '\r') inLineComment = false;
                    continue;
                }

                if (!inString && (ch == '\'' || ch == '"')) {
                    inString = true;
                    quote = ch;
                    cur.append(ch);
                    continue;
                } else if (inString) {
                    cur.append(ch);
                    if (ch == quote) {
                        if (i + 1 < script.length() && script.charAt(i + 1) == quote) {
                            cur.append(script.charAt(i + 1));
                            i++;
                        } else {
                            inString = false;
                            quote = 0;
                        }
                    }
                    continue;
                }

                if (!inTrigger) {
                    String tail = script.substring(i).toLowerCase(Locale.ROOT);
                    if (tail.startsWith("create trigger")) inTrigger = true;
                }

                cur.append(ch);
                if (inTrigger) {
                    if (cur.toString().toLowerCase(Locale.ROOT).trim().endsWith("end;")) {
                        out.add(cur.toString());
                        cur.setLength(0);
                        inTrigger = false;
                    }
                } else {
                    if (ch == ';') {
                        out.add(cur.toString());
                        cur.setLength(0);
                    }
                }
            }
            if (!cur.isEmpty()) out.add(cur.toString());
            return out;
        }

        static Map<Integer, String> method(Connection c) throws SQLException {
            Map<Integer, String> applied = new HashMap<>();
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("select version, checksum from schema_migrations")) {
                while (rs.next()) {
                    applied.put(rs.getInt(1), rs.getString(2));
                }
            }
            return applied;
        }

        private static String sha256(String s) throws Exception {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        }
    }
}