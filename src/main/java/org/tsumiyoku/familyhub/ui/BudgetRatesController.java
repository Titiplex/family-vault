package org.tsumiyoku.familyhub.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.tsumiyoku.familyhub.budget.BudgetQueries;
import org.tsumiyoku.familyhub.budget.CurrencyService;
import org.tsumiyoku.familyhub.db.Database;
import org.tsumiyoku.familyhub.util.Dialogs;

import java.time.LocalDate;
import java.util.Locale;

public class BudgetRatesController {
    @FXML
    private ComboBox<String> baseBox, quoteBox;
    @FXML
    private DatePicker dateField;
    @FXML
    private TextField rateField;
    @FXML
    private TableView<Row> table;
    @FXML
    private TableColumn<Row, String> colPair, colDate, colRate;

    public static class Row {
        final String pair, date, rate;

        Row(String p, String d, String r) {
            pair = p;
            date = d;
            rate = r;
        }
    }

    @FXML
    public void initialize() {
        var cur = FXCollections.observableArrayList(BudgetQueries.listCurrencies());
        baseBox.setItems(cur);
        quoteBox.setItems(cur);
        baseBox.getSelectionModel().select(CurrencyService.getDefaultCurrency(Database.getCurrentUserId()));
        dateField.setValue(LocalDate.now());
        reload();
    }

    @FXML
    public void onAdd() {
        String b = baseBox.getValue(), q = quoteBox.getValue();
        if (b == null || q == null || b.equals(q)) {
            Dialogs.error("Taux", "Choisir 2 devises différentes", null);
            return;
        }
        double r = Double.parseDouble(rateField.getText().replace(',', '.'));
        CurrencyService.addRate(b, q, dateField.getValue().toString(), r);
        rateField.clear();
        reload();
    }

    private void reload() {
        var items = FXCollections.<Row>observableArrayList();
        // on liste juste les N derniers par paire en interrogeant directement SQL simple via service
        for (String b : BudgetQueries.listCurrencies()) {
            for (String q : BudgetQueries.listCurrencies()) {
                if (b.equals(q)) continue;
                var latest = CurrencyService.findRate(b, q, LocalDate.now().toString());
                if (latest != null)
                    items.add(new Row(b + "/" + q, "≤ Aujourd'hui", String.format(Locale.US, "%.6f", latest)));
            }
        }
        table.setItems(items);
    }
}