package org.tsumiyoku.familyhub;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.tsumiyoku.familyhub.db.Database;
import org.tsumiyoku.familyhub.sync.DeviceIdentity;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Database.init(); // make sure DB + migrations are ready
        DeviceIdentity.get(); // génère/charge l'identité de l'appareil
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/login.fxml")));
        Scene scene = new Scene(root, 1000, 640);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/app.css")).toExternalForm());
        stage.setTitle("FamilyHub");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
