package org.tsumiyoku.familyhub.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import org.tsumiyoku.familyhub.budget.BudgetQueries;
import org.tsumiyoku.familyhub.db.Database;
import org.tsumiyoku.familyhub.util.Dialogs;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class BudgetSharesController {
    @FXML
    private TableView<Row> table;
    @FXML
    private TableColumn<Row, String> colPerson;
    @FXML
    private TableColumn<Row, String> colShare;
    @FXML
    private Button btnEqual;

    public static class Row {
        final int userId;
        String name;
        double share;

        Row(int id, String n, double s) {
            userId = id;
            name = n;
            share = s;
        }
    }

    @FXML
    public void initialize() {
        table.setEditable(true);
        colPerson.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().name));
        colShare.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(String.format(java.util.Locale.US, "%.1f", d.getValue().share)));
        colShare.setCellFactory(TextFieldTableCell.forTableColumn());
        colShare.setOnEditCommit(ev -> {
            try {
                ev.getRowValue().share = Double.parseDouble(ev.getNewValue().replace(',', '.'));
                table.refresh();
            } catch (Exception ignored) {
            }
        });
        reload();
    }

    private void reload() {
        var users = BudgetQueries.listUsers();
        var targets = BudgetQueries.shareTargets(Database.getCurrentUserId());
        var list = FXCollections.<Row>observableArrayList();
        for (var e : users.entrySet())
            list.add(new Row(e.getKey(), e.getValue(), targets.getOrDefault(e.getKey(), 0.0)));
        table.setItems(list);
    }

    @FXML
    public void onEqual() {
        int n = table.getItems().size();
        double each = n == 0 ? 0.0 : Math.round((100.0 / n) * 10.0) / 10.0;
        for (var r : table.getItems()) r.share = each;
        table.refresh();
    }

    @FXML
    public void onSave() {
        double sum = 0;
        for (var r : table.getItems()) sum += r.share;
        if (Math.abs(sum - 100.0) > 0.01) {
            Dialogs.error("Validation", "La somme doit faire 100% (actuellement " + sum + "%).", null);
            return;
        }
        try (Connection c = Database.get()) {
            for (var r : table.getItems()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO budget_share_targets(user_id,participant_user_id,share_percent,uuid) VALUES(?,?,?,lower(hex(randomblob(16)))) " +
                                "ON CONFLICT(user_id,participant_user_id) DO UPDATE SET share_percent=excluded.share_percent")) {
                    ps.setInt(1, Database.getCurrentUserId());
                    ps.setInt(2, r.userId);
                    ps.setDouble(3, r.share);
                    ps.executeUpdate();
                }
            }
            Dialogs.error("Enregistré", "Parts cibles mises à jour.", null);
        } catch (Exception e) {
            Dialogs.error("Erreur", "Enregistrement des parts", e);
        }
    }
}