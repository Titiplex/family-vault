package org.tsumiyoku.familyhub.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class SafeFXML {
    private SafeFXML() {
    }

    /**
     * Charge un FXML depuis /fxml/<name>, log en cas d'échec et remonte une RuntimeException.
     */
    public static Parent load(String name) {
        String path = "/fxml/" + name;
        try {
            URL url = SafeFXML.class.getResource(path);
            if (url == null) throw new IllegalStateException("Ressource introuvable sur le classpath: " + path);
            return FXMLLoader.load(url);
        } catch (Throwable t) {
            // dump complet
            try {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                Files.writeString(AppPaths.dataDir().resolve("last_error.txt"), sw.toString(), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Echec chargement FXML: " + path + " (voir last_error.txt)", t);
        }
    }
}