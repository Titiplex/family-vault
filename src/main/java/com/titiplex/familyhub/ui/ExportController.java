package com.titiplex.familyhub.ui;

import com.titiplex.familyhub.util.AppPaths;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import com.titiplex.familyhub.db.Database;
import com.titiplex.familyhub.util.CsvJson;
import com.titiplex.familyhub.util.Dialogs;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExportController {
    @FXML private ComboBox<String> dataset;
    @FXML private RadioButton csvBtn, jsonBtn;
    @FXML private TextArea log;

    private final Map<String, CsvJson.TableDef> defs = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        // nom lisible -> table + colonnes (sans id/user_id)
        defs.put("Lists",      new CsvJson.TableDef("lists",     List.of("title","done","due_date")));
        defs.put("Calendar",   new CsvJson.TableDef("events",    List.of("title","event_date","location","notes")));
        defs.put("Recipes",    new CsvJson.TableDef("recipes",   List.of("title","ingredients","steps")));
        defs.put("Messages",   new CsvJson.TableDef("messages",  List.of("recipient","subject","content","timestamp")));
        defs.put("Gallery",    new CsvJson.TableDef("photos",    List.of("path","caption")));
        defs.put("Contacts",   new CsvJson.TableDef("contacts",  List.of("name","phone","email")));
        defs.put("Activity",   new CsvJson.TableDef("activities",List.of("date","description","duration_minutes")));
        defs.put("Documents",  new CsvJson.TableDef("documents", List.of("path","title","tags")));
        defs.put("Budget",     new CsvJson.TableDef("budget",    List.of("date","category","amount","note")));
        defs.put("Meals",      new CsvJson.TableDef("meals",     List.of("date","meal","calories")));
        defs.put("Timetable",  new CsvJson.TableDef("timetable", List.of("day_of_week","start_time","end_time","label")));
        defs.put("Map",        new CsvJson.TableDef("places",    List.of("name","latitude","longitude","note")));

        dataset.getItems().addAll(defs.keySet());
        dataset.getSelectionModel().selectFirst();
        append("Prêt.");
    }

    @FXML
    public void onExport() {
        var def = defs.get(dataset.getValue());
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(AppPaths.exportDir().toFile());
        fc.setInitialFileName(def.table() + (csvBtn.isSelected() ? ".csv" : ".json"));
        File f = fc.showSaveDialog(dataset.getScene().getWindow());
        if (f == null) return;
        try (Connection c = Database.get()) {
            if (csvBtn.isSelected())
                CsvJson.exportCsv(c, def, Path.of(f.toURI()), Database.getCurrentUserId());
            else
                CsvJson.exportJson(c, def, Path.of(f.toURI()), Database.getCurrentUserId());
            append("Export OK → " + f.getAbsolutePath());
        } catch (Exception e) {
            Dialogs.error("Export", "Échec export", e);
        }
    }

    @FXML
    public void onImport() {
        var def = defs.get(dataset.getValue());
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(AppPaths.exportDir().toFile());
        File f = fc.showOpenDialog(dataset.getScene().getWindow());
        if (f == null) return;
        try (Connection c = Database.get()) {
            if (f.getName().toLowerCase().endsWith(".json"))
                CsvJson.importJson(c, def, Path.of(f.toURI()), Database.getCurrentUserId());
            else
                CsvJson.importCsv(c, def, Path.of(f.toURI()), Database.getCurrentUserId());
            append("Import OK ← " + f.getAbsolutePath());
        } catch (Exception e) {
            Dialogs.error("Import", "Échec import", e);
        }
    }

    private void append(String s) { log.appendText(s + "\n"); }
}