package com.titiplex.familyhub.budget;

import com.titiplex.familyhub.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class CurrencyService {
    private CurrencyService() {
    }

    public static String getDefaultCurrency(int userId) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT default_currency FROM user_settings WHERE user_id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception ignored) {
        }
        return "EUR"; // fallback
    }

    public static void setDefaultCurrency(int userId, String code) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO user_settings(user_id,default_currency) VALUES(?,?) " +
                             "ON CONFLICT(user_id) DO UPDATE SET default_currency=excluded.default_currency")) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retourne un taux base->quote valable à la date 'at' (<=at, le plus récent).
     */
    public static Double findRate(String base, String quote, String at) {
        if (base.equalsIgnoreCase(quote)) return 1.0;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT rate FROM fx_rates WHERE base=? AND quote=? AND at<=? AND deleted=0 " +
                             "ORDER BY at DESC LIMIT 1")) {
            ps.setString(1, base);
            ps.setString(2, quote);
            ps.setString(3, at);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (Exception ignored) {
        }
        // Essaye l'inverse si pas trouvé
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT rate FROM fx_rates WHERE base=? AND quote=? AND at<=? AND deleted=0 " +
                             "ORDER BY at DESC LIMIT 1")) {
            ps.setString(1, quote);
            ps.setString(2, base);
            ps.setString(3, at);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return 1.0 / rs.getDouble(1);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static void addRate(String base, String quote, String atIso, double rate) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO fx_rates(base,quote,rate,at,uuid) VALUES(?,?,?,?,lower(hex(randomblob(16))))")) {
            ps.setString(1, base);
            ps.setString(2, quote);
            ps.setDouble(3, rate);
            ps.setString(4, atIso);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
