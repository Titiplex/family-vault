package org.tsumiyoku.familyhub.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.tsumiyoku.familyhub.db.Database;
import org.tsumiyoku.familyhub.util.AppPaths;
import org.tsumiyoku.familyhub.util.Dialogs;

import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class GalleryController {
    @FXML
    private TableView<BaseCrud.Row> table;
    @FXML
    private TableColumn<BaseCrud.Row, String> col1, col2, col3, col4;
    @FXML
    private TextField pathField, captionField;
    @FXML
    private ImageView preview;

    @FXML
    public void initialize() {
        col1.setText("Chemin");
        col2.setText("Légende");
        col3.setText("");
        col4.setText("");
        col1.setCellValueFactory(d -> d.getValue().c1);
        col2.setCellValueFactory(d -> d.getValue().c2);
        col3.setCellValueFactory(d -> d.getValue().c3);
        col4.setCellValueFactory(d -> d.getValue().c4);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                pathField.setText(b.getC1());
                captionField.setText(b.getC2());
                loadPreview(b.getC1());
            }
        });
        reload();

        // recharge l’aperçu quand on édite le chemin
        pathField.textProperty().addListener((obs, oldV, newV) -> loadPreview(newV));
    }

    private void loadPreview(String p) {
        try {
            if (p == null || p.isBlank()) {
                preview.setImage(null);
                return;
            }
            File f = new File(p);
            if (!f.isAbsolute()) f = AppPaths.mediaDir().resolve(p).toFile();
            if (f.exists()) preview.setImage(new Image(f.toURI().toString(), 360, 240, true, true));
            else preview.setImage(null);
        } catch (Exception ignored) {
            preview.setImage(null);
        }
    }

    private void reload() {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT id, path, caption FROM photos WHERE user_id=? and deleted=0 ORDER BY id DESC")) {
            ps.setInt(1, Database.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            var list = FXCollections.<BaseCrud.Row>observableArrayList();
            while (rs.next()) list.add(new BaseCrud.Row(rs.getInt(1), rs.getString(2), rs.getString(3), "", ""));
            table.setItems(list);
        } catch (Exception e) {
            Dialogs.error("Erreur", "Chargement", e);
        }
    }

    @FXML
    public void onClear() {
        pathField.clear();
        captionField.clear();
        preview.setImage(null);
    }

    @FXML
    public void onAdd() {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("INSERT INTO photos(user_id, path, caption) VALUES(?, ?, ?)")) {
            ps.setInt(1, Database.getCurrentUserId());
            ps.setString(2, pathField.getText());
            ps.setString(3, captionField.getText());
            ps.executeUpdate();
            reload();
            onClear();
        } catch (Exception e) {
            Dialogs.error("Erreur", "Insertion", e);
        }
    }

    @FXML
    public void onUpdate() {
        BaseCrud.Row r = table.getSelectionModel().getSelectedItem();
        if (r == null) {
            Dialogs.error("Validation", "Sélectionnez une ligne", null);
            return;
        }
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("UPDATE photos SET path=?, caption=? WHERE id=? AND user_id=?")) {
            ps.setString(1, pathField.getText());
            ps.setString(2, captionField.getText());
            ps.setInt(3, r.getId());
            ps.setInt(4, Database.getCurrentUserId());
            ps.executeUpdate();
            reload();
        } catch (Exception e) {
            Dialogs.error("Erreur", "Mise à jour", e);
        }
    }

    @FXML
    public void onDelete() {
        BaseCrud.Row r = table.getSelectionModel().getSelectedItem();
        if (r == null) {
            Dialogs.error("Validation", "Sélectionnez une ligne", null);
            return;
        }
        if (!Dialogs.confirm("Confirmation", "Supprimer la photo ?")) return;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("UPDATE photos SET deleted=1 WHERE id=? AND user_id=?")) {
            ps.setInt(1, r.getId());
            ps.setInt(2, Database.getCurrentUserId());
            ps.executeUpdate();
            reload();
        } catch (Exception e) {
            Dialogs.error("Erreur", "Suppression", e);
        }
    }

    // --- Nouveau : Parcourir / Ouvrir ---
    @FXML
    public void onBrowse() {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(AppPaths.mediaDir().toFile());
        File f = fc.showOpenDialog(pathField.getScene().getWindow());
        if (f != null) {
            // On stocke soit le chemin relatif au dossier media, sinon absolu
            String rel = AppPaths.mediaDir().relativize(f.toPath()).toString();
            if (rel.contains("..")) pathField.setText(f.getAbsolutePath());
            else pathField.setText(rel);
        }
    }

    @FXML
    public void onOpenFile() {
        try {
            File f = new File(pathField.getText());
            if (!f.isAbsolute()) f = AppPaths.mediaDir().resolve(pathField.getText()).toFile();
            if (f.exists() && Desktop.isDesktopSupported()) Desktop.getDesktop().open(f);
        } catch (Exception e) {
            Dialogs.error("Ouvrir", "Impossible d'ouvrir le fichier", e);
        }
    }
}