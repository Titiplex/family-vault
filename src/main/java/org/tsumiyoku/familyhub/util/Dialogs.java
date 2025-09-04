package org.tsumiyoku.familyhub.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class Dialogs {
    public static void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    public static void error(String title, String msg, Throwable e) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg + (e != null ? "\n" + e.getMessage() : ""));
        a.showAndWait();
        System.err.println(msg + (e != null ? e.getMessage() : ""));
    }

    public static boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        Optional<ButtonType> res = a.showAndWait();
        return res.isPresent() && res.get() == ButtonType.OK;
    }
}
