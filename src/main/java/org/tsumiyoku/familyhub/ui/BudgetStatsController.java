package org.tsumiyoku.familyhub.ui;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import org.tsumiyoku.familyhub.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BudgetStatsController {
    @FXML
    private BarChart<String, Number> chart;

    @FXML
    public void initialize() {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT category, SUM(amount) FROM budget WHERE user_id=? GROUP BY category ORDER BY 2 DESC")) {
            ps.setInt(1, Database.getCurrentUserId());
            ResultSet rs = ps.executeQuery();
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName("Total");
            while (rs.next()) {
                s.getData().add(new XYChart.Data<>(rs.getString(1), rs.getDouble(2)));
            }
            chart.getData().add(s);
        } catch (Exception e) {
            org.tsumiyoku.familyhub.util.Dialogs.error("Stats budget", "Chargement", e);
        }
    }
}