module family.hub {
    requires flyway.core;
    requires java.sql;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires com.fasterxml.jackson.databind;
    requires bcrypt;
    requires java.desktop;
    requires weupnp;

    exports org.tsumiyoku.familyhub.ui;
    exports org.tsumiyoku.familyhub.db;
    exports org.tsumiyoku.familyhub.util;
    exports org.tsumiyoku.familyhub;
    exports org.tsumiyoku.familyhub.sync;
    opens org.tsumiyoku.familyhub.ui to javafx.fxml;
    opens org.tsumiyoku.familyhub to javafx.fxml;
    opens org.tsumiyoku.familyhub.db to flyway.core;
    opens org.tsumiyoku.familyhub.util to javafx.fxml, weupnp;
    opens org.tsumiyoku.familyhub.sync to com.fasterxml.jackson.databind;
}