package org.tsumiyoku.familyhub.ui;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.tsumiyoku.familyhub.util.Dialogs;
import org.tsumiyoku.familyhub.util.SafeFXML;

public class MainController {

    private void open(String fxml, String title) {
        try {
            Parent root = SafeFXML.load(fxml);
            Stage s = new Stage();
            s.setTitle(title);
            s.initModality(Modality.NONE);
            s.setScene(new Scene(root, 1000, 640));
            var css = getClass().getResource("/app.css");
            if (css != null) s.getScene().getStylesheets().add(css.toExternalForm());
            s.show();
        } catch (Exception e) {
            Dialogs.error("Erreur", "Ouverture " + title, e);
        }
    }

    @FXML
    public void openSync() {
        open("sync.fxml", "Synchronisation P2P");
    }

    @FXML
    public void openLists() {
        open("lists.fxml", "Lists");
    }

    @FXML
    public void openCalendar() {
        open("calendar.fxml", "Calendar");
    }

    @FXML
    public void openRecipes() {
        open("recipes.fxml", "Recipes");
    }

    @FXML
    public void openMessages() {
        open("messages.fxml", "Messages");
    }

    @FXML
    public void openGallery() {
        open("gallery.fxml", "Gallery");
    }

    @FXML
    public void openContacts() {
        open("contacts.fxml", "Contacts");
    }

    @FXML
    public void openActivity() {
        open("activity.fxml", "Activity");
    }

    @FXML
    public void openDocuments() {
        open("documents.fxml", "Documents");
    }

    @FXML
    public void openBudget() {
        open("budget.fxml", "Budget");
    }

    @FXML
    public void openMeals() {
        open("meals.fxml", "Meals");
    }

    @FXML
    public void openTimetable() {
        open("timetable.fxml", "Timetable");
    }

    @FXML
    public void openMap() {
        open("map.fxml", "Map");
    }

    @FXML
    public void openExport() {
        open("export.fxml", "Export / Import");
    }
}
