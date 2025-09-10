package com.titiplex.familyhub.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.titiplex.familyhub.db.Database;
import com.titiplex.familyhub.util.Dialogs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TimetableController {
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
    private TextArea ingredientsArea, stepsArea, contentArea, descArea;
    @FXML
    private DatePicker dateField, dueDate;
    @FXML
    private CheckBox doneCheck;

    @FXML
    public void initialize() {
        col1.setText("Jour");
        col2.setText("Début");
        col3.setText("Fin");
        col4.setText("Libellé");
        col1.setCellValueFactory(data -> data.getValue().c1);
        col2.setCellValueFactory(data -> data.getValue().c2);
        col3.setCellValueFactory(data -> data.getValue().c3);
        col4.setCellValueFactory(data -> data.getValue().c4);
        table.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> fillForm(b));
        reload();
    }

    private void reload() {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("select id, day_of_week, start_time, end_time, label from timetable where user_id = ? AND deleted = 0 order by id desc")) {
            ps.setInt(1, Database.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            var list = FXCollections.<BaseCrud.Row>observableArrayList();
            while (rs.next()) {
                list.add(new BaseCrud.Row(
                        rs.getInt(1),
                        Integer.toString(rs.getInt(2)),
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
             PreparedStatement ps = c.prepareStatement("insert into timetable(user_id, day_of_week, start_time, end_time, label) values(?, ?,?,?,?)")) {
            ps.setInt(1, Database.getCurrentUserId());
            ps.setInt(2, Integer.parseInt(dowField.getText().isBlank() ? "0" : dowField.getText()));
            ps.setString(3, startField.getText());
            ps.setString(4, endField.getText());
            ps.setString(5, labelField.getText());
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
             PreparedStatement ps = c.prepareStatement("update timetable set day_of_week=?,start_time=?,end_time=?,label=? where id = ? and user_id = ?")) {
            ps.setInt(2, Integer.parseInt(dowField.getText().isBlank() ? "0" : dowField.getText()));
            ps.setString(3, startField.getText());
            ps.setString(4, endField.getText());
            ps.setString(5, labelField.getText());
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
             PreparedStatement ps = c.prepareStatement("UPDATE timetable SET deleted=1 WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, sel.getId());
            ps.setInt(2, Database.getCurrentUserId());
            ps.executeUpdate();
            reload();
        } catch (Exception e) {
            Dialogs.error("Erreur", "Suppression", e);
        }
    }
}
