package com.titiplex.familyhub.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public final class CsvJson {
    public record TableDef(String table, List<String> cols) {
    }

    private CsvJson() {
    }

    public static void exportCsv(Connection c, TableDef def, Path file, int userId) throws Exception {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
             PreparedStatement ps = c.prepareStatement("SELECT " + String.join(",", def.cols())
                     + " FROM " + def.table() + " WHERE user_id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            // header
            w.write(String.join(",", def.cols()));
            w.write("\n");
            while (rs.next()) {
                List<String> line = new ArrayList<>();
                for (int i = 0; i < def.cols().size(); i++) line.add(escapeCsv(rs.getString(i + 1)));
                w.write(String.join(",", line));
                w.write("\n");
            }
        }
    }

    public static void exportJson(Connection c, TableDef def, Path file, int userId) throws Exception {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
             PreparedStatement ps = c.prepareStatement("SELECT " + String.join(",", def.cols())
                     + " FROM " + def.table() + " WHERE user_id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            w.write("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) w.write(",");
                first = false;
                w.write("{");
                for (int i = 0; i < def.cols().size(); i++) {
                    if (i > 0) w.write(",");
                    String k = def.cols().get(i);
                    String v = rs.getString(i + 1);
                    w.write("\"" + escJson(k) + "\":\"" + escJson(v) + "\"");
                }
                w.write("}");
            }
            w.write("]");
        }
    }

    public static void importCsv(Connection c, TableDef def, Path file, int userId) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = r.readLine();
            String line;
            while ((line = r.readLine()) != null) rows.add(parseCsv(line));
        }
        String placeholders = String.join(",", Collections.nCopies(def.cols().size(), "?"));
        String sql = "INSERT INTO " + def.table() + "(user_id," + String.join(",", def.cols()) + ") VALUES(?, " + placeholders + ")";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (String[] row : rows) {
                ps.setInt(1, userId);
                for (int i = 0; i < def.cols().size(); i++) ps.setString(i + 2, i < row.length ? row[i] : null);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public static void importJson(Connection c, TableDef def, Path file, int userId) throws Exception {
        String txt = Files.readString(file, StandardCharsets.UTF_8).trim();
        // Parsing très simple: attend un tableau d'objets {col:"val",...}
        txt = txt.replaceAll("^[\\s\\uFEFF]*\\[", "").replaceAll("]$", "");
        String[] objects = txt.split("\\},\\s*\\{");
        String placeholders = String.join(",", Collections.nCopies(def.cols().size(), "?"));
        String sql = "INSERT INTO " + def.table() + "(user_id," + String.join(",", def.cols()) + ") VALUES(?, " + placeholders + ")";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (String obj : objects) {
                String o = obj.replaceAll("^\\{", "").replaceAll("}$", "");
                Map<String, String> map = new HashMap<>();
                for (String kv : o.split("\",\\s*\"")) {
                    String[] p = kv.split("\":\"");
                    if (p.length == 2)
                        map.put(unescJson(p[0].replaceAll("^\"", "")), unescJson(p[1].replaceAll("\"$", "")));
                }
                ps.setInt(1, userId);
                for (int i = 0; i < def.cols().size(); i++) ps.setString(i + 2, map.get(def.cols().get(i)));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // --- helpers ---
    private static String escapeCsv(String s) {
        if (s == null) return "";
        boolean need = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String out = s.replace("\"", "\"\"");
        return need ? "\"" + out + "\"" : out;
    }

    private static String[] parseCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (q) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else q = false;
                } else cur.append(ch);
            } else {
                if (ch == '"') q = true;
                else if (ch == ',') {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private static String escJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String unescJson(String s) {
        return s == null ? null : s.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}