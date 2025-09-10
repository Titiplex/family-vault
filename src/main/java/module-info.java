module FamilyHub.main {
    requires bcrypt;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires flyway.core;
    requires java.datatransfer;
    requires java.sql;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires weupnp;
    requires java.desktop;
    requires org.apache.logging.log4j;

    // Donne accès à JavaFX pour instancier Application(Main)
    exports com.titiplex.familyhub;
    opens com.titiplex.familyhub to javafx.graphics;

    // Donne accès au chargement FXML + controllers
    exports com.titiplex.familyhub.ui;
    opens com.titiplex.familyhub.ui to javafx.fxml;
}