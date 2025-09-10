package com.titiplex.familyhub.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import com.titiplex.familyhub.sync.DeviceIdentity;
import com.titiplex.familyhub.sync.PeerStore;
import com.titiplex.familyhub.sync.SyncService;

import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

public class SyncController {
    @FXML
    private TextArea myInvite, peerInvite, log;
    @FXML
    private TextField pairingSecret, peerSecret, portField;
    @FXML
    private TableView<Row> peerTable;
    @FXML
    private TableColumn<Row, String> colId, colAddr, colPort;

    public static class Row {
        public final String id, addr, port;

        Row(String id, String addr, int port) {
            this.id = id;
            this.addr = addr;
            this.port = Integer.toString(port);
        }

        public String getId() {
            return id;
        }

        public String getAddr() {
            return addr;
        }

        public String getPort() {
            return port;
        }
    }

    private static final ObjectMapper M = new ObjectMapper();

    @FXML
    public void initialize() {
        DeviceIdentity me = DeviceIdentity.get();
        portField.setText(Integer.toString(SyncService.getListenPort()));
        colId.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().id));
        colAddr.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().addr));
        colPort.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().port));
        reloadPeers();

        // Pré-affiche bloc d'invite à compléter (PIN à ajouter)
        try {
            String addr = InetAddress.getLocalHost().getHostAddress();
            String invite = M.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                    "deviceId", me.deviceId, "publicKey", me.publicKey,
                    "signPublicKey", me.signPublicKey,
                    "address", addr, "port", Integer.parseInt(portField.getText())
            ));
            myInvite.setText(invite);
        } catch (Exception ignored) {
        }
    }

    private void reloadPeers() {
        try {
            peerTable.getItems().clear();
            for (PeerStore.Peer p : PeerStore.list()) {
                peerTable.getItems().add(new Row(p.deviceId(), p.address(), p.port()));
            }
        } catch (Exception e) {
            append("ERR peers: " + e.getMessage());
        }
    }

    @FXML
    public void onStart() {
        SyncService.setListenPort(Integer.parseInt(portField.getText()));
        SyncService.startServer();
        append("Serveur sync démarré sur " + portField.getText());
    }

    @FXML
    public void onStop() {
        SyncService.stopServer();
        append("Serveur sync arrêté");
    }

    @FXML
    public void onGenerateInvite() {
        try {
            DeviceIdentity me = DeviceIdentity.get();
            String json = myInvite.getText();
            var map = M.readValue(json, Map.class);
            String pin = pairingSecret.getText();
            if (pin == null || pin.isBlank()) {
                append("Entrez un PIN d’appairage.");
                return;
            }
            String pinHash = sha256Hex(pin);
            map.put("pairingSecretHash", pinHash);
            String out = M.writerWithDefaultPrettyPrinter().writeValueAsString(map);
            myInvite.setText(out);
            append("Invitation générée. Partage: JSON + le PIN par canal séparé.");
        } catch (Exception e) {
            append("ERR invite: " + e.getMessage());
        }
    }

    @FXML
    public void onAddPeer() {
        try {
            String json = peerInvite.getText();
            var m = M.readValue(json, Map.class);
            String pin = peerSecret.getText();
            if (pin == null || pin.isBlank()) {
                append("Entrez le PIN reçu.");
                return;
            }
            String pinHash = sha256Hex(pin);
            if (!pinHash.equals(m.get("pairingSecretHash"))) {
                append("PIN incorrect (hash ne correspond pas).");
                return;
            }
            PeerStore.addPeer(new PeerStore.Peer(
                    (String) m.get("deviceId"),
                    (String) m.get("publicKey"),
                    (String) m.get("address"),
                    (Integer) m.get("port"),
                    pinHash,
                    (String) m.get("signPublicKey")
            ));
            reloadPeers();
            append("Pair ajouté.");
        } catch (Exception e) {
            append("ERR add: " + e.getMessage());
        }
    }

    @FXML
    public void onSyncNow() {
        try {
            for (PeerStore.Peer p : PeerStore.list()) {
                SyncService.connectToPeer(p.address(), p.port());
            }
            append("Sync lancée avec tous les pairs connus.");
        } catch (Exception e) {
            append("ERR sync: " + e.getMessage());
        }
    }

    private String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(s.getBytes()));
    }

    private void append(String s) {
        log.appendText(s + "\n");
    }
}