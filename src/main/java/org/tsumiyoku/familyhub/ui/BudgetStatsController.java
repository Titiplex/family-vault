package org.tsumiyoku.familyhub.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import org.tsumiyoku.familyhub.budget.BudgetQueries;
import org.tsumiyoku.familyhub.budget.CurrencyService;
import org.tsumiyoku.familyhub.db.Database;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

public class BudgetStatsController {
    @FXML
    private DatePicker startField, endField;
    @FXML
    private PieChart pieCategories;
    @FXML
    private BarChart<String, Number> barByPerson;
    @FXML
    private BarChart<String, Number> barVsTargets;
    @FXML
    private LineChart<String, Number> lineEvolution;
    @FXML
    private BarChart<String, Number> barCatForecast;
    @FXML
    private Label forecastLabel, defaultCur;
    @FXML
    private ComboBox<String> displayCurrencyBox;

    @FXML
    public void initialize() {
        var now = LocalDate.now();
        startField.setValue(now.withDayOfMonth(1));
        endField.setValue(now);
        String def = CurrencyService.getDefaultCurrency(Database.getCurrentUserId());
        defaultCur.setText(def);
        displayCurrencyBox.setItems(FXCollections.observableArrayList(BudgetQueries.listCurrencies()));
        displayCurrencyBox.getSelectionModel().select(def);
        refreshAll();
    }

    @FXML
    public void onRefresh() {
        refreshAll();
    }

    private void refreshAll() {
        int uid = Database.getCurrentUserId();
        String s = BudgetQueries.iso(startField.getValue());
        String e = BudgetQueries.iso(endField.getValue());
        String def = CurrencyService.getDefaultCurrency(uid);
        String disp = displayCurrencyBox.getValue();
        double conv = 1.0;
        if (!def.equals(disp)) {
            Double r = CurrencyService.findRate(def, disp, LocalDate.now().toString());
            conv = (r == null ? 1.0 : r);
        }

        // 1) Catégories (Pie)
        var totals = BudgetQueries.totalsByCategory(uid, s, e);
        var pieData = FXCollections.<PieChart.Data>observableArrayList();
        for (var en : totals.entrySet()) pieData.add(new PieChart.Data(en.getKey(), Math.max(en.getValue() * conv, 0)));
        pieCategories.setData(pieData);

        // 2) Bilans par personne (réel)
        var bal = BudgetQueries.balanceByPerson(uid, s, e);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Payé - Dû (réel)");
        for (var en : bal.entrySet()) {
            String name = BudgetQueries.listUsers().getOrDefault(en.getKey(), "U" + en.getKey());
            series.getData().add(new XYChart.Data<>(name, en.getValue() * conv));
        }
        barByPerson.getData().setAll(series);

        // 3) Écarts vs parts cibles
        var vs = BudgetQueries.diffAgainstTargets(uid, s, e);
        XYChart.Series<String, Number> s2 = new XYChart.Series<>();
        s2.setName("Payé - Part cible");
        for (var en : vs.entrySet()) {
            String name = BudgetQueries.listUsers().getOrDefault(en.getKey(), "U" + en.getKey());
            s2.getData().add(new XYChart.Data<>(name, en.getValue() * conv));
        }
        barVsTargets.getData().setAll(s2);

        // 4) Evolution + Forecast global
        var monthly = BudgetQueries.monthlySeries(uid, 18);
        XYChart.Series<String, Number> s1 = new XYChart.Series<>();
        s1.setName("Net / mois");
        for (var en : monthly.entrySet()) s1.getData().add(new XYChart.Data<>(en.getKey(), en.getValue() * conv));
        lineEvolution.getData().setAll(s1);
        double forecast = BudgetQueries.forecastNextMonth(monthly) * conv;
        forecastLabel.setText(String.format(java.util.Locale.US, "Prévision mois prochain ≈ %.2f %s", forecast, disp));

        // 5) Prévision par catégorie (top 6 par dépenses sur la période)
        var catNameById = BudgetQueries.listCategories(uid); // id->name
        var totalsById = BudgetQueries.totalsByCategoryId(uid, s, e);
        // tri par valeur desc, garder 6
        var top = new ArrayList<Map.Entry<Integer, Double>>(totalsById.entrySet());
        top.sort((a, b) -> Double.compare(Math.abs(b.getValue()), Math.abs(a.getValue())));
        if (top.size() > 6) top = new ArrayList<>(top.subList(0, 6));

        XYChart.Series<String, Number> sForecast = new XYChart.Series<>();
        sForecast.setName("Prévision (mois prochain)");
        for (var en : top) {
            Integer cid = (en.getKey() == 0 ? null : en.getKey());
            var seriesCat = BudgetQueries.monthlySeriesForCategory(uid, cid, 12);
            double f = BudgetQueries.forecastNextMonth(seriesCat) * conv;
            String name = (cid == null) ? "(Sans catégorie)" : catNameById.getOrDefault(cid, "Cat." + cid);
            sForecast.getData().add(new XYChart.Data<>(name, f));
        }
        barCatForecast.getData().setAll(sForecast);
    }
}