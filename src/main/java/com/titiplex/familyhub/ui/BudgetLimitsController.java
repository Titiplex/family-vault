package com.titiplex.familyhub.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.titiplex.familyhub.budget.BudgetQueries;
import com.titiplex.familyhub.db.Database;
import com.titiplex.familyhub.util.Dialogs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

public class BudgetLimitsController {
    @FXML
    private ComboBox<String> categoryBox;
    @FXML
    private DatePicker startField, endField;
    @FXML
    private TextField amountField;
    @FXML
    private ComboBox<String> goalTypeBox;

    @FXML
    private TableView<Row> table;
    @FXML
    private TableColumn<Row, String> colPeriod, colCat, colLimit, colUsed, colStatus;

    private final Map<Integer, String> categories = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, Integer> catByName = new java.util.HashMap<>();

    public static class Row {
        final int id;
        final String period;
        final String cat;
        final double limit;
        final double used;
        final String status;

        Row(int id, String p, String c, double l, double u, String s) {
            this.id = id;
            this.period = p;
            this.cat = c;
            this.limit = l;
            this.used = u;
            this.status = s;
        }
    }

    @FXML
    public void initialize() {
        categories.clear();
        categories.putAll(BudgetQueries.listCategories(Database.getCurrentUserId()));
        catByName.clear();
        categories.forEach((id, name) -> catByName.put(name, id));
        var items = FXCollections.observableArrayList(categories.values());
        items.addFirst("(Global)");
        categoryBox.setItems(items);
        categoryBox.getSelectionModel().select(0);

        startField.setValue(LocalDate.now().withDayOfMonth(1));
        endField.setValue(LocalDate.now());
        goalTypeBox.setItems(FXCollections.observableArrayList("spend", "save"));
        goalTypeBox.getSelectionModel().select("spend");

        reload();
    }

    @FXML
    public void onAdd() {
        Integer catId = categoryBox.getValue() == null || "(Global)".equals(categoryBox.getValue()) ? null : catByName.get(categoryBox.getValue());
        String start = startField.getValue().toString();
        String end = endField.getValue().toString();
        double limit = Double.parseDouble(amountField.getText().replace(',', '.'));
        String goal = goalTypeBox.getValue();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO budget_limits(user_id,category_id,period_start,period_end,amount_limit,goal_type,uuid) " +
                             "VALUES(?,?,?,?,?,?,lower(hex(randomblob(16))))")) {
            ps.setInt(1, Database.getCurrentUserId());
            if (catId == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, catId);
            ps.setString(3, start);
            ps.setString(4, end);
            ps.setDouble(5, limit);
            ps.setString(6, goal);
            ps.executeUpdate();
            amountField.clear();
            reload();
        } catch (Exception e) {
            Dialogs.error("Limites", "Ajout", e);
        }
    }

    @FXML
    public void onDelete() {
        var sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        if (!Dialogs.confirm("Confirmation", "Supprimer cette limite ?")) return;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("UPDATE budget_limits SET deleted=1 WHERE id=? AND user_id=?")) {
            ps.setInt(1, sel.id);
            ps.setInt(2, Database.getCurrentUserId());
            ps.executeUpdate();
            reload();
        } catch (Exception e) {
            Dialogs.error("Limites", "Suppression", e);
        }
    }

    @FXML
    public void onRefresh() {
        reload();
    }

    private void reload() {
        int uid = Database.getCurrentUserId();
        var limits = BudgetQueries.listLimits(uid);
        var rows = FXCollections.<Row>observableArrayList();
        for (var l : limits) {
            double used = BudgetQueries.spentFor(uid, l.start(), l.end(), l.categoryId());
            String status;
            if ("save".equalsIgnoreCase(l.goalType())) {
                double saved = Math.max(0, -used);
                status = String.format(Locale.US, "épargné %.2f / %.2f", saved, l.limit());
                rows.add(new Row(l.id(), l.start() + " → " + l.end(), l.categoryName(), l.limit(), used, status));
            } else {
                status = String.format(Locale.US, "dépensé %.2f / %.2f", used, l.limit());
                rows.add(new Row(l.id(), l.start() + " → " + l.end(), l.categoryName(), l.limit(), used, status));
            }
        }
        colPeriod.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().period));
        colCat.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().cat));
        colLimit.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(String.format(Locale.US, "%.2f", d.getValue().limit)));
        colUsed.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(String.format(Locale.US, "%.2f", d.getValue().used)));
        colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().status));
        table.setItems(rows);
    }
}