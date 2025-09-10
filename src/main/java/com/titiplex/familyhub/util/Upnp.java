package com.titiplex.familyhub.util;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Upnp {
    private Upnp() {
    }

    public static void tryMapTcpPort(int port, String desc) {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        ex.submit(() -> {
            try {
                GatewayDiscover discover = new GatewayDiscover();
                discover.discover();
                GatewayDevice d = discover.getValidGateway();
                if (d == null) return;
                InetAddress local = d.getLocalAddress();
                boolean ok = d.addPortMapping(port, port, local.getHostAddress(), "TCP", desc);
                System.out.println("[UPnP] " + (ok ? "Mapping OK" : "Mapping FAILED") + " on " + d.getFriendlyName());
            } catch (Exception ignored) {
            }
        });
        ex.shutdown();
    }
}
