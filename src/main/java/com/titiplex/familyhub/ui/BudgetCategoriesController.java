package com.titiplex.familyhub.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.titiplex.familyhub.db.Database;
import com.titiplex.familyhub.util.Dialogs;

import java.sql.*;

public class BudgetCategoriesController {
    @FXML private TableView<Row> table;
    @FXML private TableColumn<Row,String> colName;
    @FXML private TextField nameField;

    public static class Row { final int id; final String name; Row(int id,String n){this.id=id;this.name=n;} }

    @FXML public void initialize() { reload(); }

    private void reload() {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT id,name FROM budget_categories WHERE user_id=? AND deleted=0 ORDER BY name")) {
            ps.setInt(1, Database.getCurrentUserId());
            var list = FXCollections.<Row>observableArrayList();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Row(rs.getInt(1), rs.getString(2)));
            }
            table.setItems(list);
        } catch (Exception e) { Dialogs.error("Catégories","Chargement", e); }
    }

    @FXML public void onAdd() {
        String n = nameField.getText(); if (n==null || n.isBlank()) return;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO budget_categories(user_id,name,uuid) VALUES(?,?,lower(hex(randomblob(16))))")) {
            ps.setInt(1, Database.getCurrentUserId()); ps.setString(2, n.trim()); ps.executeUpdate();
            nameField.clear(); reload();
        } catch (Exception e) { Dialogs.error("Catégories","Ajout", e); }
    }

    @FXML public void onDelete() {
        var sel = table.getSelectionModel().getSelectedItem(); if (sel==null) return;
        if (!Dialogs.confirm("Confirmer","Supprimer la catégorie '"+sel.name+"' ?")) return;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("UPDATE budget_categories SET deleted=1 WHERE id=? AND user_id=?")) {
            ps.setInt(1, sel.id); ps.setInt(2, Database.getCurrentUserId()); ps.executeUpdate();
            reload();
        } catch (Exception e) { Dialogs.error("Catégories","Suppression", e); }
    }
}