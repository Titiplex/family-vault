package org.tsumiyoku.familyhub.sync;

import org.tsumiyoku.familyhub.db.Database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Surveille la table 'oplog' et déclenche une sync P2P quand elle bouge.
 */
public final class SyncAuto {
    private static final ScheduledExecutorService SCHED = Executors.newSingleThreadScheduledExecutor();
    private static volatile long lastSeen = -1;
    private static volatile boolean started = false;

    private SyncAuto() {
    }

    public static synchronized void start() {
        if (started) return;
        started = true;
        initLastSeen();
        SCHED.scheduleWithFixedDelay(SyncAuto::tick, 800, 800, TimeUnit.MILLISECONDS);
    }

    private static void initLastSeen() {
        try (Connection c = Database.get();
             ResultSet rs = c.createStatement().executeQuery("SELECT COALESCE(MAX(id),0) FROM oplog")) {
            if (rs.next()) lastSeen = rs.getLong(1);
        } catch (Exception ignored) {
            lastSeen = 0;
        }
    }

    private static void tick() {
        try (Connection c = Database.get();
             ResultSet rs = c.createStatement().executeQuery("SELECT COALESCE(MAX(id),0) FROM oplog")) {
            long max = rs.next() ? rs.getLong(1) : 0;
            if (max > lastSeen) {
                lastSeen = max;
                // Déclenche une sync avec tous les pairs connus (asynchrone)
                SyncService.syncAllKnownPeers();
            }
        } catch (Exception ignored) { /* best-effort */ }
    }
}
