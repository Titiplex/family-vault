package org.tsumiyoku.familyhub.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.tsumiyoku.familyhub.db.Database;
import org.tsumiyoku.familyhub.util.Dialogs;
import org.tsumiyoku.familyhub.util.Password;
import org.tsumiyoku.familyhub.util.SafeFXML;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class LoginController {
    @FXML
    private ComboBox<User> userCombo;
    @FXML
    private TextField newUserName;
    @FXML
    private TextField newUserEmail;
    @FXML
    private PasswordField newUserPass;
    @FXML
    private PasswordField loginPass;

    public static class User {
        public final int id;
        public final String name;

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @FXML
    public void initialize() {
        try (Connection c = Database.get();
             ResultSet rs = c.createStatement().executeQuery("SELECT id, name FROM users ORDER BY name")) {
            var list = FXCollections.<User>observableArrayList();
            while (rs.next()) {
                list.add(new User(rs.getInt(1), rs.getString(2)));
            }
            userCombo.setItems(list);
            if (!list.isEmpty()) userCombo.getSelectionModel().selectFirst();
        } catch (Exception e) {
            Dialogs.error("Erreur", "Chargement des utilisateurs", e);
        }
    }

    @FXML
    public void onCreate() {
        String name = newUserName.getText() == null ? "" : newUserName.getText().trim();
        String email = newUserEmail.getText() == null ? "" : newUserEmail.getText().trim();
        String pass = newUserPass != null ? newUserPass.getText() : "";
        if (name.isEmpty()) {
            Dialogs.error("Validation", "Nom requis", null);
            return;
        }
        try (Connection c = Database.get()) {
            boolean hasPwdCol = hasPasswordHashColumn(c);
            String sql = hasPwdCol
                    ? "INSERT INTO users(name, email, password_hash) VALUES(?, ?, ?)"
                    : "INSERT INTO users(name, email) VALUES(?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, email.isEmpty() ? null : email);
                if (hasPwdCol) {
                    ps.setString(3, Password.hash(pass));
                }
                ps.executeUpdate();
            }
            initialize(); // reload list
            newUserName.clear();
            newUserEmail.clear();
            if (newUserPass != null) newUserPass.clear();
        } catch (Exception e) {
            Dialogs.error("Erreur", "Création utilisateur", e);
        }
    }

    @FXML
    public void onLogin() {
        User u = userCombo.getSelectionModel().getSelectedItem();
        if (u == null) {
            Dialogs.error("Validation", "Sélectionnez un utilisateur", null);
            return;
        }

        try (Connection c = Database.get()) {
            String hash = null;
            // Tolère les anciennes bases sans la colonne password_hash
            if (hasPasswordHashColumn(c)) {
                try (PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM users WHERE id=?")) {
                    ps.setInt(1, u.id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) hash = rs.getString(1);
                    }
                }
                // Si un hash existe, le vérifier ; si pas de hash => pas de mot de passe requis
                if (hash != null && !Password.verify(loginPass == null ? "" : loginPass.getText(), hash)) {
                    Dialogs.error("Connexion", "Mot de passe invalide", null);
                    return;
                }
            }
        } catch (Exception e) {
            Dialogs.error("Erreur", "Vérification du mot de passe", e);
            return;
        }

        // OK → on ouvre l'app
        Database.setCurrentUserId(u.id);
        try {
            Parent root = SafeFXML.load("main.fxml");
            Scene scene = new Scene(root, 1200, 760);
            var css = getClass().getResource("/app.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            Stage stage = (Stage) userCombo.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("FamilyHub - " + u.name);
        } catch (Exception e) {
            Dialogs.error("Erreur", "Ouverture de l'application", e);
        }
    }

    /**
     * Retourne true si la colonne users.password_hash existe (DB déjà migrée en V2).
     */
    private boolean hasPasswordHashColumn(Connection c) {
        try (PreparedStatement ps = c.prepareStatement("PRAGMA table_info(users)");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if ("password_hash".equalsIgnoreCase(rs.getString("name"))) return true;
            }
        } catch (SQLException ignored) {
        }
        return false;
    }
}
