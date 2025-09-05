package org.tsumiyoku.familyhub.budget;

import org.tsumiyoku.familyhub.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.*;

public final class BudgetQueries {
    private BudgetQueries() {
    }

    public record Tx(int id, String date, String category, String currency, double amount, double amountDefault,
                     String note, boolean income, int payerUserId) {
    }

    public static List<String> listCurrencies() {
        // Liste courte, offline (tu peux en ajouter)
        return List.of("EUR", "USD", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY", "MAD", "XOF");
    }

    public static Map<Integer, String> listUsers() {
        Map<Integer, String> m = new LinkedHashMap<>();
        try (Connection c = Database.get();
             ResultSet rs = c.createStatement().executeQuery("SELECT id,name FROM users ORDER BY name")) {
            while (rs.next()) m.put(rs.getInt(1), rs.getString(2));
        } catch (Exception ignored) {
        }
        return m;
    }

    public static Map<Integer, String> listCategories(int userId) {
        Map<Integer, String> m = new LinkedHashMap<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id,name FROM budget_categories WHERE user_id=? AND deleted=0 ORDER BY name")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) m.put(rs.getInt(1), rs.getString(2));
            }
        } catch (Exception ignored) {
        }
        return m;
    }

    /**
     * Sommes par catégorie, en devise par défaut (dépenses positives, recettes négatives si tu veux afficher net).
     */
    public static Map<String, Double> totalsByCategory(int userId, String start, String end) {
        String sql = """
                  SELECT COALESCE(bc.name,'(Sans catégorie)'), SUM(CASE WHEN bt.is_income=1 THEN -bt.amount_default ELSE bt.amount_default END)
                  FROM budget_tx bt LEFT JOIN budget_categories bc ON bc.id=bt.category_id
                  WHERE bt.user_id=? AND bt.deleted=0 AND bt.tx_date BETWEEN ? AND ?
                  GROUP BY 1 ORDER BY 2 DESC
                """;
        LinkedHashMap<String, Double> out = new LinkedHashMap<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, start);
            ps.setString(3, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), rs.getDouble(2));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /**
     * Balance par personne = payé - dû (en devise par défaut), sur période.
     */
    public static Map<Integer, Double> balanceByPerson(int userId, String start, String end) {
        String sqlPaid = """
                    SELECT payer_user_id, SUM(CASE WHEN is_income=1 THEN -amount_default ELSE amount_default END)
                    FROM budget_tx WHERE user_id=? AND deleted=0 AND tx_date BETWEEN ? AND ?
                    GROUP BY payer_user_id
                """;
        String sqlDue = """
                    SELECT p.participant_user_id, SUM((CASE WHEN t.is_income=1 THEN -t.amount_default ELSE t.amount_default END)*p.share_percent/100.0)
                    FROM budget_tx t JOIN budget_tx_participants p ON p.tx_id=t.id AND p.deleted=0
                    WHERE t.user_id=? AND t.deleted=0 AND t.tx_date BETWEEN ? AND ?
                    GROUP BY p.participant_user_id
                """;
        Map<Integer, Double> paid = new HashMap<>(), due = new HashMap<>();
        try (Connection c = Database.get();
             PreparedStatement ps1 = c.prepareStatement(sqlPaid);
             PreparedStatement ps2 = c.prepareStatement(sqlDue)) {
            ps1.setInt(1, userId);
            ps1.setString(2, start);
            ps1.setString(3, end);
            try (ResultSet rs = ps1.executeQuery()) {
                while (rs.next()) paid.put(rs.getInt(1), rs.getDouble(2));
            }
            ps2.setInt(1, userId);
            ps2.setString(2, start);
            ps2.setString(3, end);
            try (ResultSet rs = ps2.executeQuery()) {
                while (rs.next()) due.put(rs.getInt(1), rs.getDouble(2));
            }
        } catch (Exception ignored) {
        }
        // paid - due
        Set<Integer> people = new HashSet<>();
        people.addAll(paid.keySet());
        people.addAll(due.keySet());
        Map<Integer, Double> bal = new LinkedHashMap<>();
        for (Integer pid : people) bal.put(pid, paid.getOrDefault(pid, 0.0) - due.getOrDefault(pid, 0.0));
        return bal;
    }

    /**
     * Série mensuelle (YYYY-MM) des dépenses nettes (dépense positive, recette négative) pour prévisions.
     */
    public static LinkedHashMap<String, Double> monthlySeries(int userId, int monthsBack) {
        String sql = """
                  SELECT substr(tx_date,1,7) AS ym,
                         SUM(CASE WHEN is_income=1 THEN -amount_default ELSE amount_default END)
                  FROM budget_tx
                  WHERE user_id=? AND deleted=0 AND tx_date >= date('now','start of month','-%d months')
                  GROUP BY ym ORDER BY ym
                """.formatted(monthsBack);
        LinkedHashMap<String, Double> out = new LinkedHashMap<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), rs.getDouble(2));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /**
     * Prévision simple : Exponential Smoothing (alpha=0.35) sur la série mensuelle.
     */
    public static double forecastNextMonth(Map<String, Double> monthly) {
        double alpha = 0.35;
        Double s = null;
        for (double v : monthly.values()) s = (s == null) ? v : (alpha * v + (1 - alpha) * s);
        return s == null ? 0.0 : s;
    }

    /**
     * Ligne brute des transactions pour affichage.
     */
    public static List<Tx> listTx(int userId, String start, String end) {
        String sql = """
                    SELECT t.id, t.tx_date, COALESCE(c.name,''), t.currency, t.amount, t.amount_default, t.note, t.is_income, t.payer_user_id
                    FROM budget_tx t LEFT JOIN budget_categories c ON c.id=t.category_id
                    WHERE t.user_id=? AND t.deleted=0 AND t.tx_date BETWEEN ? AND ?
                    ORDER BY t.tx_date DESC, t.id DESC
                """;
        ArrayList<Tx> out = new ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, start);
            ps.setString(3, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(new Tx(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getDouble(5), rs.getDouble(6), rs.getString(7), rs.getInt(8) == 1, rs.getInt(9)));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static String iso(LocalDate d) {
        return d == null ? null : d.toString();
    }

    /* === Parts cibles globales === */
    public static Map<Integer, Double> shareTargets(int userId) {
        LinkedHashMap<Integer, Double> m = new LinkedHashMap<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT participant_user_id, share_percent FROM budget_share_targets WHERE user_id=? AND deleted=0 ORDER BY participant_user_id")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) m.put(rs.getInt(1), rs.getDouble(2));
            }
        } catch (Exception ignored) {
        }
        return m;
    }

    /**
     * Écarts vs parts cibles : paid - total*target% (en devise par défaut)
     */
    public static Map<Integer, Double> diffAgainstTargets(int userId, String start, String end) {
        // Total net de la période
        double total = 0;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT SUM(CASE WHEN is_income=1 THEN -amount_default ELSE amount_default END) " +
                             "FROM budget_tx WHERE user_id=? AND deleted=0 AND tx_date BETWEEN ? AND ?")) {
            ps.setInt(1, userId);
            ps.setString(2, start);
            ps.setString(3, end);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) total = rs.getDouble(1);
            }
        } catch (Exception ignored) {
        }

        // payé par personne
        Map<Integer, Double> paid = new LinkedHashMap<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT payer_user_id, SUM(CASE WHEN is_income=1 THEN -amount_default ELSE amount_default END) " +
                             "FROM budget_tx WHERE user_id=? AND deleted=0 AND tx_date BETWEEN ? AND ? GROUP BY 1")) {
            ps.setInt(1, userId);
            ps.setString(2, start);
            ps.setString(3, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) paid.put(rs.getInt(1), rs.getDouble(2));
            }
        } catch (Exception ignored) {
        }

        // attentes selon parts cibles
        Map<Integer, Double> targets = shareTargets(userId);
        Map<Integer, Double> diff = new LinkedHashMap<>();
        for (var pid : targets.keySet()) {
            double should = total * targets.get(pid) / 100.0;
            double did = paid.getOrDefault(pid, 0.0);
            diff.put(pid, did - should);
        }
        // si des personnes ont payé sans être dans les targets, on les ajoute
        for (var pid : paid.keySet()) diff.putIfAbsent(pid, paid.get(pid));
        return diff;
    }

    /* === Limites / objectifs === */
    public record LimitRow(int id, Integer categoryId, String categoryName, String start, String end,
                           double limit, String goalType) {
    }

    public static java.util.List<LimitRow> listLimits(int userId) {
        String sql = """
                   SELECT bl.id, bl.category_id, COALESCE(bc.name,'(Global)'), bl.period_start, bl.period_end, bl.amount_limit, bl.goal_type
                   FROM budget_limits bl LEFT JOIN budget_categories bc ON bc.id=bl.category_id
                   WHERE bl.user_id=? AND bl.deleted=0 ORDER BY bl.period_start DESC, bl.id DESC
                """;
        var out = new java.util.ArrayList<LimitRow>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new LimitRow(rs.getInt(1),
                        (rs.getObject(2) == null ? null : rs.getInt(2)), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getDouble(6), rs.getString(7)));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /**
     * Montant net dépensé (dépense- recette) dans la période et la catégorie (NULL=global).
     */
    public static double spentFor(int userId, String start, String end, Integer categoryId) {
        String base = "FROM budget_tx WHERE user_id=? AND deleted=0 AND tx_date BETWEEN ? AND ?";
        String filter = (categoryId == null) ? "" : " AND category_id=?";
        String sql = "SELECT COALESCE(SUM(CASE WHEN is_income=1 THEN -amount_default ELSE amount_default END),0) "
                + base + filter;
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, start);
            ps.setString(3, end);
            if (categoryId != null) ps.setInt(4, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * Totaux par category_id (NULL=0) pour choisir les "top" catégories.
     */
    public static Map<Integer, Double> totalsByCategoryId(int userId, String start, String end) {
        String sql = """
                  SELECT COALESCE(category_id, 0) AS cid,
                         SUM(CASE WHEN is_income=1 THEN -amount_default ELSE amount_default END)
                  FROM budget_tx
                  WHERE user_id=? AND deleted=0 AND tx_date BETWEEN ? AND ?
                  GROUP BY cid ORDER BY 2 DESC
                """;
        LinkedHashMap<Integer, Double> out = new LinkedHashMap<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, start);
            ps.setString(3, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getInt(1), rs.getDouble(2));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /**
     * Série mensuelle (YYYY-MM) d'une catégorie (categoryId==null => sans catégorie). Dépense positive, recette négative.
     */
    public static LinkedHashMap<String, Double> monthlySeriesForCategory(int userId, Integer categoryId, int monthsBack) {
        String base = """
                  SELECT substr(tx_date,1,7) AS ym,
                         SUM(CASE WHEN is_income=1 THEN -amount_default ELSE amount_default END)
                  FROM budget_tx WHERE user_id=? AND deleted=0 AND tx_date >= date('now','start of month','-%d months')
                """.formatted(monthsBack);
        String filter = (categoryId == null) ? " AND category_id IS NULL " : " AND category_id=? ";
        String sql = base + filter + " GROUP BY ym ORDER BY ym";
        LinkedHashMap<String, Double> out = new LinkedHashMap<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            if (categoryId != null) ps.setInt(2, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), rs.getDouble(2));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /**
     * Petit utilitaire d'intersection de périodes ISO (yyyy-MM-dd). Retourne null si sans recouvrement.
     */
    public static String[] intersectIso(String aStart, String aEnd, String bStart, String bEnd) {
        java.time.LocalDate as = java.time.LocalDate.parse(aStart);
        java.time.LocalDate ae = java.time.LocalDate.parse(aEnd);
        java.time.LocalDate bs = java.time.LocalDate.parse(bStart);
        java.time.LocalDate be = java.time.LocalDate.parse(bEnd);
        var s = as.isAfter(bs) ? as : bs;
        var e = ae.isBefore(be) ? ae : be;
        if (s.isAfter(e)) return null;
        return new String[]{s.toString(), e.toString()};
    }

    /**
     * Transactions filtrées par catégorie (categoryId==null => sans catégorie).
     */
    public static java.util.List<Tx> listTxForCategory(int userId, String start, String end, Integer categoryId) {
        String sql = """
                    SELECT t.id, t.tx_date, COALESCE(c.name,''), t.currency, t.amount, t.amount_default, t.note, t.is_income, t.payer_user_id
                    FROM budget_tx t LEFT JOIN budget_categories c ON c.id=t.category_id
                    WHERE t.user_id=? AND t.deleted=0 AND t.tx_date BETWEEN ? AND ?
                """ + (categoryId == null ? " AND t.category_id IS NULL " : " AND t.category_id=? ") +
                " ORDER BY t.tx_date DESC, t.id DESC";
        var out = new java.util.ArrayList<Tx>();
        try (var c = org.tsumiyoku.familyhub.db.Database.get();
             var ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, start);
            ps.setString(3, end);
            if (categoryId != null) ps.setInt(4, categoryId);
            try (var rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(new Tx(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getDouble(5), rs.getDouble(6), rs.getString(7), rs.getInt(8) == 1, rs.getInt(9)));
            }
        } catch (Exception ignored) {
        }
        return out;
    }
}