package com.titiplex.familyhub;

import com.titiplex.familyhub.db.Database;
import com.titiplex.familyhub.sync.DeviceIdentity;
import com.titiplex.familyhub.sync.SyncAuto;
import com.titiplex.familyhub.sync.SyncService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // IMPORTANT : Database.init() doit être appelé ici UNE SEULE FOIS.
        // (Pas de bloc static { init(); } dans Database)
        Database.init();
        DeviceIdentity.get();      // génère/charge l'identité de l'appareil
        SyncService.startServer(); // threads daemon
        SyncAuto.start();          // threads daemon

        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/login.fxml")));
        Scene scene = new Scene(root, 1000, 640);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/app.css")).toExternalForm());
        stage.setTitle("FamilyHub");
        stage.setScene(scene);

        // Si l'utilisateur ferme la fenêtre → fermer proprement puis quitter la JVM
        stage.setOnCloseRequest(_ -> Platform.exit());

        stage.show();
    }

    @Override
    public void stop() {
        // Arrêt *propre* de tout ce qui tourne en arrière-plan
        try {
            SyncAuto.stop();
            SyncService.stopServer();
        } finally {
            // Ne pas appeler Database.get().close() ici (ouvrirait
            // une nouvelle connexion juste pour la fermer).
            // On force la sortie de la JVM quoi qu'il arrive.
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}