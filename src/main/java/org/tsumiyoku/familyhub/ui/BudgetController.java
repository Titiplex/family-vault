package org.tsumiyoku.familyhub.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.tsumiyoku.familyhub.budget.BudgetQueries;
import org.tsumiyoku.familyhub.budget.CurrencyService;
import org.tsumiyoku.familyhub.db.Database;
import org.tsumiyoku.familyhub.util.Dialogs;
import org.tsumiyoku.familyhub.util.SafeFXML;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class BudgetController {

    // Filtres période
    @FXML
    private DatePicker filterStart, filterEnd;

    // Saisie transaction
    @FXML
    private DatePicker txDate;
    @FXML
    private ComboBox<String> currencyBox;
    @FXML
    private TextField amountField, noteField, rateField;
    @FXML
    private ComboBox<String> categoryBox;
    @FXML
    private ComboBox<String> payerBox;
    @FXML
    private CheckBox incomeCheck;

    // Alerte limite + export
    @FXML
    private ProgressBar limitProgress;
    @FXML
    private Label limitLabel;

    // Participants
    @FXML
    private TableView<ParticipantRow> participantsTable;
    @FXML
    private TableColumn<ParticipantRow, String> colPerson, colShare;
    @FXML
    private Button btnSplitEqual;

    // Tableau des transactions
    @FXML
    private TableView<TxRow> table;
    @FXML
    private TableColumn<TxRow, String> tcolDate, tcolCat, tcolPayer, tcolCur, tcolAmount, tcolDef, tcolNote;

    // Devise par défaut affichée
    @FXML
    private Label defaultCurLabel;

    private final Map<Integer, String> users = BudgetQueries.listUsers();
    private final Map<Integer, String> categories = new LinkedHashMap<>();
    private final Map<String, Integer> catByName = new HashMap<>();
    private final List<String> currencies = BudgetQueries.listCurrencies();

    private static class TxRow {
        final int id;
        final SimpleStringProperty date, cat, payer, cur, amount, amountDef, note;

        TxRow(int id, String date, String cat, String payer, String cur, String amount, String amountDef, String note) {
            this.id = id;
            this.date = new SimpleStringProperty(date);
            this.cat = new SimpleStringProperty(cat);
            this.payer = new SimpleStringProperty(payer);
            this.cur = new SimpleStringProperty(cur);
            this.amount = new SimpleStringProperty(amount);
            this.amountDef = new SimpleStringProperty(amountDef);
            this.note = new SimpleStringProperty(note);
        }
    }

    public static class ParticipantRow {
        final int userId;
        final SimpleStringProperty name;
        double share;

        ParticipantRow(int userId, String name, double share) {
            this.userId = userId;
            this.name = new SimpleStringProperty(name);
            this.share = share;
        }
    }

    @FXML
    public void initialize() {
        // Période par défaut = mois courant
        filterStart.setValue(LocalDate.now().withDayOfMonth(1));
        filterEnd.setValue(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));

        // Catégories & users
        categories.clear();
        categories.putAll(BudgetQueries.listCategories(Database.getCurrentUserId()));
        catByName.clear();
        categories.forEach((id, name) -> catByName.put(name, id));
        categoryBox.setItems(FXCollections.observableArrayList(categories.values()));

        payerBox.setItems(FXCollections.observableArrayList(users.values()));

        currencyBox.setItems(FXCollections.observableArrayList(currencies));

        // participants
        var pr = FXCollections.<ParticipantRow>observableArrayList();
        for (var e : users.entrySet()) pr.add(new ParticipantRow(e.getKey(), e.getValue(), 0));
        participantsTable.setItems(pr);
        colPerson.setCellValueFactory(d -> d.getValue().name);
        colShare.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f%%", d.getValue().share)));

        // tableau tx
        tcolDate.setCellValueFactory(d -> d.getValue().date);
        tcolCat.setCellValueFactory(d -> d.getValue().cat);
        tcolPayer.setCellValueFactory(d -> d.getValue().payer);
        tcolCur.setCellValueFactory(d -> d.getValue().cur);
        tcolAmount.setCellValueFactory(d -> d.getValue().amount);
        tcolDef.setCellValueFactory(d -> d.getValue().amountDef);
        tcolNote.setCellValueFactory(d -> d.getValue().note);

        // labels
        defaultCurLabel.setText(CurrencyService.getDefaultCurrency(Database.getCurrentUserId()));

        reload();
    }

    @FXML
    public void onRefresh() {
        reload();
    }

    @FXML
    public void onSplitEqual() {
        int n = (int) participantsTable.getItems().stream().filter(p -> true).count();
        double each = n == 0 ? 0.0 : Math.round((100.0 / n) * 10.0) / 10.0;
        for (var p : participantsTable.getItems()) p.share = each;
        participantsTable.refresh();
    }

    @FXML
    public void onClear() {
        txDate.setValue(LocalDate.now());
        categoryBox.getSelectionModel().clearSelection();
        payerBox.getSelectionModel().clearSelection();
        currencyBox.getSelectionModel().select(CurrencyService.getDefaultCurrency(Database.getCurrentUserId()));
        amountField.clear();
        rateField.clear();
        noteField.clear();
        incomeCheck.setSelected(false);
        for (var p : participantsTable.getItems()) p.share = 0;
        participantsTable.refresh();
    }

    @FXML
    public void onSetDefaultCurrency() {
        var choice = new ChoiceDialog<>(CurrencyService.getDefaultCurrency(Database.getCurrentUserId()),
                BudgetQueries.listCurrencies());
        choice.setTitle("Devise par défaut");
        choice.setHeaderText("Choisir la devise par défaut pour les calculs");
        var res = choice.showAndWait();
        res.ifPresent(code -> {
            CurrencyService.setDefaultCurrency(Database.getCurrentUserId(), code);
            defaultCurLabel.setText(code);
            reload();
        });
    }

    @FXML
    public void onManageCategories() {
        open("budget_categories.fxml", "Catégories");
        // après fermeture, recharger
        categories.clear();
        categories.putAll(BudgetQueries.listCategories(Database.getCurrentUserId()));
        categoryBox.setItems(FXCollections.observableArrayList(categories.values()));
    }

    @FXML
    public void onManageRates() {
        open("budget_rates.fxml", "Taux de change");
    }

    @FXML
    public void onOpenStats() {
        open("budget_stats.fxml", "Budget – Analyses & Prévisions");
    }

    private void open(String fxml, String title) {
        try {
            var root = SafeFXML.load(fxml);
            var s = new Stage();
            s.setTitle(title);
            var sc = new Scene(root, 980, 640);
            var css = getClass().getResource("/app.css");
            if (css != null) sc.getStylesheets().add(css.toExternalForm());
            s.setScene(sc);
            s.showAndWait();
        } catch (Exception e) {
            Dialogs.error("Ouverture", "Impossible d'ouvrir " + fxml, e);
        }
    }

    @FXML
    public void onAdd() {
        try (Connection c = Database.get()) {
            int uid = Database.getCurrentUserId();
            String cur = currencyBox.getValue();
            String def = CurrencyService.getDefaultCurrency(uid);
            String date = BudgetQueries.iso(txDate.getValue());
            double amount = parseAmount(amountField.getText());
            double rate = computeRate(cur, def, date);
            if (rate <= 0) {
                Dialogs.error("Devise", "Aucun taux trouvé (ajoute-le dans 'Taux de change').", null);
                return;
            }

            Integer catId = categoryBox.getValue() == null ? null : catByName.get(categoryBox.getValue());
            Integer payerUserId = resolveUserId(payerBox.getValue());
            if (payerUserId == null) {
                Dialogs.error("Validation", "Choisis un payeur", null);
                return;
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO budget_tx(user_id,tx_date,category_id,payer_user_id,currency,amount,rate_to_default,amount_default,note,is_income,uuid) " +
                            "VALUES(?,?,?,?,?,?,?,?,?,?,lower(hex(randomblob(16))))",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, uid);
                ps.setString(2, date);
                if (catId == null) ps.setNull(3, Types.INTEGER);
                else ps.setInt(3, catId);
                ps.setInt(4, payerUserId);
                ps.setString(5, cur);
                ps.setDouble(6, amount);
                ps.setDouble(7, rate);
                ps.setDouble(8, amount * rate);
                ps.setString(9, noteField.getText());
                ps.setInt(10, incomeCheck.isSelected() ? 1 : 0);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int txId = keys.getInt(1);
                        saveParticipants(c, txId);
                    }
                }
            }
            reload();
            onClear();
        } catch (Exception e) {
            Dialogs.error("Erreur", "Insertion", e);
        }
    }

    @FXML
    public void onUpdate() {
        TxRow sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Dialogs.error("Validation", "Sélectionne une transaction", null);
            return;
        }
        try (Connection c = Database.get()) {
            int uid = Database.getCurrentUserId();
            String cur = currencyBox.getValue();
            String def = CurrencyService.getDefaultCurrency(uid);
            String date = BudgetQueries.iso(txDate.getValue());
            double amount = parseAmount(amountField.getText());
            double rate = computeRate(cur, def, date);
            Integer catId = categoryBox.getValue() == null ? null : catByName.get(categoryBox.getValue());
            Integer payerUserId = resolveUserId(payerBox.getValue());
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE budget_tx SET tx_date=?,category_id=?,payer_user_id=?,currency=?,amount=?,rate_to_default=?,amount_default=?,note=?,is_income=? " +
                            "WHERE id=? AND user_id=?")) {
                ps.setString(1, date);
                if (catId == null) ps.setNull(2, Types.INTEGER);
                else ps.setInt(2, catId);
                ps.setInt(3, payerUserId);
                ps.setString(4, cur);
                ps.setDouble(5, amount);
                ps.setDouble(6, rate);
                ps.setDouble(7, amount * rate);
                ps.setString(8, noteField.getText());
                ps.setInt(9, incomeCheck.isSelected() ? 1 : 0);
                ps.setInt(10, sel.id);
                ps.setInt(11, uid);
                ps.executeUpdate();
            }
            // Recrée les participants
            try (PreparedStatement del = c.prepareStatement("UPDATE budget_tx_participants SET deleted=1 WHERE tx_id=?")) {
                del.setInt(1, sel.id);
                del.executeUpdate();
            }
            saveParticipants(c, sel.id);
            reload();
        } catch (Exception e) {
            Dialogs.error("Erreur", "Mise à jour", e);
        }
    }

    @FXML
    public void onDelete() {
        TxRow sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Dialogs.error("Validation", "Sélectionne une transaction", null);
            return;
        }
        if (!Dialogs.confirm("Confirmation", "Supprimer (soft-delete) cette transaction ?")) return;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("UPDATE budget_tx SET deleted=1 WHERE id=? AND user_id=?")) {
            ps.setInt(1, sel.id);
            ps.setInt(2, Database.getCurrentUserId());
            ps.executeUpdate();
            try (PreparedStatement ps2 = c.prepareStatement("UPDATE budget_tx_participants SET deleted=1 WHERE tx_id=?")) {
                ps2.setInt(1, sel.id);
                ps2.executeUpdate();
            }
            reload();
        } catch (Exception e) {
            Dialogs.error("Erreur", "Suppression", e);
        }
    }

    private void saveParticipants(Connection c, int txId) throws SQLException {
        double total = 0;
        for (var p : participantsTable.getItems()) total += p.share;
        if (Math.abs(total - 100.0) > 0.01)
            throw new SQLException("La somme des parts doit faire 100% (actuellement " + total + "%).");
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO budget_tx_participants(tx_id,participant_user_id,share_percent,uuid) VALUES(?,?,?,lower(hex(randomblob(16))))")) {
            for (var p : participantsTable.getItems()) {
                ps.setInt(1, txId);
                ps.setInt(2, p.userId);
                ps.setDouble(3, p.share);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private Integer resolveUserId(String name) {
        for (var e : users.entrySet()) if (Objects.equals(e.getValue(), name)) return e.getKey();
        return null;
    }

    private double parseAmount(String s) {
        if (s == null || s.isBlank()) return 0;
        return Double.parseDouble(s.replace(',', '.'));
    }

    private double computeRate(String from, String to, String at) {
        if (rateField.getText() != null && !rateField.getText().isBlank())
            return Double.parseDouble(rateField.getText().replace(',', '.'));
        Double r = CurrencyService.findRate(from, to, at);
        if (r != null) rateField.setText(String.format(Locale.US, "%.6f", r));
        return r == null ? 0.0 : r;
    }

    private void reload() {
        String s = BudgetQueries.iso(filterStart.getValue());
        String e = BudgetQueries.iso(filterEnd.getValue());
        if (s == null || e == null) {
            var today = LocalDate.now();
            s = today.withDayOfMonth(1).toString();
            e = today.toString();
        }
        var txs = BudgetQueries.listTx(Database.getCurrentUserId(), s, e);
        var rows = FXCollections.<TxRow>observableArrayList();
        for (var t : txs)
            rows.add(new TxRow(
                    t.id(), t.date(), t.category(), users.getOrDefault(t.payerUserId(), "?"),
                    t.currency(), String.format(Locale.US, "%.2f", t.amount()),
                    String.format(Locale.US, "%.2f", t.amountDefault()), t.note()
            ));
        table.setItems(rows);
        updateLimitProgress(s, e);
        // (Optionnel) Alerte console si limites dépassées dans la période filtrée
        int uid = Database.getCurrentUserId();
        var limits = BudgetQueries.listLimits(uid);
        int exceeded = 0;
        for (var l : limits) {
            double used = BudgetQueries.spentFor(uid, s, e, l.categoryId());
            if ("spend".equalsIgnoreCase(l.goalType()) && used > l.limit()) exceeded++;
            if ("save".equalsIgnoreCase(l.goalType()) && (-used) < l.limit()) {/* non atteint */}
        }
        if (exceeded > 0) System.out.println("[Budget] Limites dépassées: " + exceeded);

    }

    // pour édition rapide en cliquant une transaction
    @FXML
    public void onSelectTx(MouseEvent e) {
        var sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        txDate.setValue(LocalDate.parse(sel.date.get()));
        categoryBox.getSelectionModel().select(sel.cat.get());
        payerBox.getSelectionModel().select(sel.payer.get());
        currencyBox.getSelectionModel().select(sel.cur.get());
        amountField.setText(sel.amount.get());
        rateField.setText(""); // facultatif; on laisse recomputé
        noteField.setText(sel.note.get());
        incomeCheck.setSelected(false); // pas dans le tableau, garde comme dépense par défaut
        // Charger les participants actuels
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT participant_user_id, share_percent FROM budget_tx_participants WHERE tx_id=? AND deleted=0")) {
            ps.setInt(1, sel.id);
            Map<Integer, Double> cur = new HashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) cur.put(rs.getInt(1), rs.getDouble(2));
            }
            for (var row : participantsTable.getItems()) row.share = cur.getOrDefault(row.userId, 0.0);
            participantsTable.refresh();
        } catch (Exception ignored) {
        }
    }

    @FXML
    public void onManageLimits() {
        open("budget_limits.fxml", "Limites & objectifs");
    }

    @FXML
    public void onManageShares() {
        open("budget_shares.fxml", "Parts cibles du foyer");
    }

    @FXML
    public void onExportCsv() {
        String s = BudgetQueries.iso(filterStart.getValue());
        String e = BudgetQueries.iso(filterEnd.getValue());
        var chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Exporter les transactions");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
        chooser.setInitialFileName("budget_" + s + "_" + e + ".csv");
        var file = chooser.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;

        int uid = Database.getCurrentUserId();
        var txs = BudgetQueries.listTx(uid, s, e);

        try (var w = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
            w.write("date,category,payer,currency,amount,rate_to_default,amount_default,is_income,note,participants\n");
            for (var t : txs) {
                // participants → "Alice:40|Bob:60"
                String parts = "";
                try (var c = Database.get();
                     var ps = c.prepareStatement("SELECT p.participant_user_id, p.share_percent FROM budget_tx_participants p WHERE p.tx_id=? AND p.deleted=0")) {
                    ps.setInt(1, t.id());
                    try (var rs = ps.executeQuery()) {
                        StringBuilder sb = new StringBuilder();
                        parts = getString(rs, sb);
                    }
                }
                String payer = BudgetQueries.listUsers().getOrDefault(t.payerUserId(), "U" + t.payerUserId());
                writeCsv(w, t, parts, payer);
            }
        } catch (Exception ex) {
            Dialogs.error("Export CSV", "Échec de l'export", ex);
        }
    }

    private String rateForRow(BudgetQueries.Tx t) {
        // On peut recomposer: amount_default / amount (évite un SELECT)
        if (Math.abs(t.amount()) < 1e-9) return "0";
        double r = t.amountDefault() / t.amount();
        return String.format(java.util.Locale.US, "%.6f", r);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String csv(String s) {
        String v = s == null ? "" : s;
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = v.replace("\"", "\"\"");
            return "\"" + v + "\"";
        }
        return v;
    }

    private void updateLimitProgress(String start, String end) {
        var limits = BudgetQueries.listLimits(Database.getCurrentUserId());
        double bestRatio = -1;
        String bestLabel = "Aucune limite active sur la période";
        boolean isSaveGoal = false;

        for (var l : limits) {
            String[] inter = BudgetQueries.intersectIso(start, end, l.start(), l.end());
            if (inter == null) continue;
            double used = BudgetQueries.spentFor(Database.getCurrentUserId(), inter[0], inter[1], l.categoryId());
            double ratio;
            String label;
            boolean save = "save".equalsIgnoreCase(l.goalType());
            if (save) {
                double saved = Math.max(0, -used);
                ratio = (l.limit() <= 0) ? 0 : saved / l.limit();
                label = String.format(java.util.Locale.US, "Épargne %s : %.2f / %.2f",
                        l.categoryName(), saved, l.limit());
            } else {
                ratio = (l.limit() <= 0) ? 0 : used / l.limit();
                label = String.format(java.util.Locale.US, "Limite %s : %.2f / %.2f",
                        l.categoryName(), used, l.limit());
            }
            if (ratio > bestRatio) {
                bestRatio = ratio;
                bestLabel = label;
                isSaveGoal = save;
            }
        }

        if (limitProgress == null || limitLabel == null) return;

        if (bestRatio < 0) { // aucune limite concernée
            limitProgress.setProgress(0);
            limitProgress.setStyle(""); // reset couleur
            limitLabel.setText("Aucune");
            return;
        }

        // Couleurs : vert < 60%, orange [60–100%], rouge > 100% (ou épargne atteinte)
        double r = bestRatio;
        String color;
        if (!isSaveGoal) {
            if (r > 1.0) color = "#c62828";            // rouge
            else if (r >= 0.60) color = "#ef6c00";     // orange
            else color = "#2e7d32";                    // vert
        } else {
            // objectif d'épargne: vert si >=100%, orange si [60–100), rouge <60%
            if (r >= 1.0) color = "#2e7d32";
            else if (r >= 0.60) color = "#ef6c00";
            else color = "#c62828";
        }

        limitProgress.setProgress(Math.min(1.0, Math.abs(r)));
        limitProgress.setStyle("-fx-accent: " + color + ";");
        limitLabel.setText(bestLabel + ((!isSaveGoal && r > 1.0) ? "  (DÉPASSÉE)" : (isSaveGoal && r >= 1.0 ? "  (ATTEINTE)" : "")));
    }

    @FXML
    public void onExportCsvByCategory() {
        // 1) choix catégorie
        var names = new ArrayList<String>();
        names.add("(Sans catégorie)");
        names.addAll(categories.values());
        var dlg = new ChoiceDialog<>(names.getFirst(), names);
        dlg.setTitle("Exporter CSV (catégorie)");
        dlg.setHeaderText("Choisis la catégorie à exporter sur la période visible");
        var res = dlg.showAndWait();
        if (res.isEmpty()) return;
        String chosen = res.get();
        Integer catId = null;
        if (!"(Sans catégorie)".equals(chosen)) catId = catByName.get(chosen);

        // 2) chemin
        String s = BudgetQueries.iso(filterStart.getValue());
        String e = BudgetQueries.iso(filterEnd.getValue());
        var chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Exporter les transactions – " + chosen);
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
        chooser.setInitialFileName(("budget_" + (chosen.replace(' ', '_')) + "_" + s + "_" + e + ".csv").replaceAll("[()]", ""));
        var file = chooser.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;

        // 3) données
        int uid = Database.getCurrentUserId();
        var txs = BudgetQueries.listTxForCategory(uid, s, e, catId);
        try (var w = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
            w.write("date,category,payer,currency,amount,rate_to_default,amount_default,is_income,note,participants\n");
            for (var t : txs) {
                String parts = "";
                try (var c = Database.get();
                     var ps = c.prepareStatement("SELECT participant_user_id, share_percent FROM budget_tx_participants WHERE tx_id=? AND deleted=0")) {
                    ps.setInt(1, t.id());
                    try (var rs = ps.executeQuery()) {
                        var sb = new StringBuilder();
                        parts = getString(rs, sb);
                    }
                }
                String payer = BudgetQueries.listUsers().getOrDefault(t.payerUserId(), "U" + t.payerUserId());
                writeCsv(w, t, parts, payer);
            }
        } catch (Exception ex) {
            org.tsumiyoku.familyhub.util.Dialogs.error("Export CSV (catégorie)", "Échec", ex);
        }
    }

    private String getString(ResultSet rs, StringBuilder sb) throws SQLException {
        String parts;
        while (rs.next()) {
            int pid = rs.getInt(1);
            double sh = rs.getDouble(2);
            String name = BudgetQueries.listUsers().getOrDefault(pid, "U" + pid);
            if (!sb.isEmpty()) sb.append("|");
            sb.append(name).append(":").append(String.format(Locale.US, "%.1f", sh));
        }
        parts = sb.toString();
        return parts;
    }

    private void writeCsv(BufferedWriter w, BudgetQueries.Tx t, String parts, String payer) throws IOException {
        w.write(csv(t.date()) + "," + csv(nullToEmpty(t.category())) + "," + csv(payer) + "," + csv(t.currency()) + "," +
                String.format(Locale.US, "%.2f", t.amount()) + "," +
                rateForRow(t) + "," +
                String.format(Locale.US, "%.2f", t.amountDefault()) + "," +
                (t.income() ? "1" : "0") + "," + csv(nullToEmpty(t.note())) + "," + csv(parts) + "\n");
    }
}