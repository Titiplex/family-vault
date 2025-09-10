package com.titiplex.familyhub.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.titiplex.familyhub.db.Database;
import com.titiplex.familyhub.util.Dialogs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CalendarController {
    @FXML
    private TableView<BaseCrud.Row> table;
    @FXML
    private TableColumn<BaseCrud.Row, String> col1;
    @FXML
    private TableColumn<BaseCrud.Row, String> col2;
    @FXML
    private TableColumn<BaseCrud.Row, String> col3;
    @FXML
    private TableColumn<BaseCrud.Row, String> col4;

    // Fields (defined in FXML)

    @FXML
    private TextField titleField, locationField, subjectField, recipientField, pathField, captionField, nameField, phoneField, emailField, durationField, tagsField, categoryField, amountField, noteField, mealField, calField, dowField, startField, endField, labelField, timestampField, latField, lngField, labelField2;
    @FXML
    private TextArea ingredientsArea, stepsArea, contentArea, descArea, notesArea;
    @FXML
    private DatePicker dateField, dueDate;
    @FXML
    private CheckBox doneCheck;

    @FXML
    public void initialize() {
        col1.setText("Titre");
        col2.setText("Date");
        col3.setText("Lieu");
        col4.setText("Notes");
        col1.setCellValueFactory(data -> data.getValue().c1);
        col2.setCellValueFactory(data -> data.getValue().c2);
        col3.setCellValueFactory(data -> data.getValue().c3);
        col4.setCellValueFactory(data -> data.getValue().c4);
        table.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> fillForm(b));
        reload();
    }

    private void reload() {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("select id, title, event_date, location, notes from events where user_id = ? AND deleted = 0 order by id desc")) {
            ps.setInt(1, Database.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            var list = FXCollections.<BaseCrud.Row>observableArrayList();
            while (rs.next()) {
                list.add(new BaseCrud.Row(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5)
                ));
            }
            table.setItems(list);
        } catch (Exception e) {
            Dialogs.error("Erreur", "Chargement", e);
        }
    }

    private void fillForm(BaseCrud.Row r) {
        if (r == null) return;
        // No-op; modules will read fields directly when updating. Selection doesn't auto-populate all widgets generically.
    }

    @FXML
    public void onClear() {
        // Clear a subset of known fields
        if (titleField != null) titleField.clear();
        if (locationField != null) locationField.clear();
        if (subjectField != null) subjectField.clear();
        if (recipientField != null) recipientField.clear();
        if (pathField != null) pathField.clear();
        if (captionField != null) captionField.clear();
        if (nameField != null) nameField.clear();
        if (phoneField != null) phoneField.clear();
        if (emailField != null) emailField.clear();
        if (durationField != null) durationField.clear();
        if (tagsField != null) tagsField.clear();
        if (categoryField != null) categoryField.clear();
        if (amountField != null) amountField.clear();
        if (noteField != null) noteField.clear();
        if (mealField != null) mealField.clear();
        if (calField != null) calField.clear();
        if (dowField != null) dowField.clear();
        if (startField != null) startField.clear();
        if (endField != null) endField.clear();
        if (latField != null) latField.clear();
        if (lngField != null) lngField.clear();
        if (ingredientsArea != null) ingredientsArea.clear();
        if (stepsArea != null) stepsArea.clear();
        if (contentArea != null) contentArea.clear();
        if (descArea != null) descArea.clear();
        if (dateField != null) dateField.setValue(null);
        if (dueDate != null) dueDate.setValue(null);
        if (doneCheck != null) doneCheck.setSelected(false);
    }

    @FXML
    public void onAdd() {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("insert into events(user_id, title, event_date, location, notes) values(?, ?,?,?,?)")) {
            ps.setInt(1, Database.getCurrentUserId());
            ps.setString(2, titleField.getText());
            ps.setString(3, dateField.getValue() == null ? null : dateField.getValue().toString());
            ps.setString(4, locationField.getText());
            ps.setString(5, notesArea.getText());
            ps.executeUpdate();
            reload();
            onClear();
        } catch (Exception e) {
            Dialogs.error("Erreur", "Insertion", e);
        }
    }

    @FXML
    public void onUpdate() {
        BaseCrud.Row sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Dialogs.error("Validation", "Sélectionnez une ligne à mettre à jour", null);
            return;
        }
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("update events set title=?,event_date=?,location=?,notes=? where id = ? and user_id = ?")) {
            ps.setString(2, titleField.getText());
            ps.setString(3, dateField.getValue() == null ? null : dateField.getValue().toString());
            ps.setString(4, locationField.getText());
            ps.setString(5, notesArea.getText());
            ps.setInt(5, sel.getId());
            ps.setInt(6, Database.getCurrentUserId());
            ps.executeUpdate();
            reload();
        } catch (Exception e) {
            Dialogs.error("Erreur", "Mise à jour", e);
        }
    }

    @FXML
    public void onDelete() {
        BaseCrud.Row sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Dialogs.error("Validation", "Sélectionnez une ligne à supprimer", null);
            return;
        }
        if (!Dialogs.confirm("Confirmation", "Supprimer l'élément sélectionné ?")) return;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("UPDATE events SET deleted=1 WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, sel.getId());
            ps.setInt(2, Database.getCurrentUserId());
            ps.executeUpdate();
            reload();
        } catch (Exception e) {
            Dialogs.error("Erreur", "Suppression", e);
        }
    }
}
