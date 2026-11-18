package dev.banova.core.service;

import dev.banova.core.BanovaCore;
import dev.banova.core.model.Punishment;
import dev.banova.core.model.Records.SyncEvent;
import dev.banova.core.util.Codes;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Keeps servers sharing a MySQL/MariaDB database in sync by polling a small event table.
 * No Redis or plugin messaging needed, and it works even with no players online.
 */
public final class SyncService {

    private final BanovaCore core;
    private final String instanceId;
    private volatile long lastId;
    private ScheduledFuture<?> pollTask;
    private ScheduledFuture<?> pruneTask;

    public SyncService(BanovaCore core) {
        this.core = core;
        this.instanceId = core.serverName() + "-" + Codes.token(4);
    }

    public boolean enabled() {
        return core.database().isShared();
    }

    private volatile long proxyHeartbeat;

    /**
     * True when a Velocity proxy with Banova is running on this network. Backends then let the proxy
     * disconnect banned players, so they see the ban screen instead of being moved to another server.
     */
    public boolean proxyActive() {
        return enabled() && !core.platform().isProxy() && System.currentTimeMillis() - proxyHeartbeat < 60_000;
    }

    private void heartbeat() {
        try {
            if (core.platform().isProxy()) {
                core.records().setMeta("proxy_heartbeat", String.valueOf(System.currentTimeMillis()));
            } else {
                String value = core.records().meta("proxy_heartbeat");
                proxyHeartbeat = value == null ? 0 : Long.parseLong(value);
            }
        } catch (RuntimeException e) {
            core.platform().logger().warn("Network heartbeat failed: " + e.getMessage());
        }
    }

    public void start() {
        if (!enabled()) {
            return;
        }
        heartbeat();
        core.scheduler().scheduleWithFixedDelay(this::heartbeat, 15, 15, TimeUnit.SECONDS);
        lastId = core.records().maxSyncId();
        long interval = Math.max(250, core.config().getLong("network.sync-interval", 1000));
        pollTask = core.scheduler().scheduleWithFixedDelay(this::poll, interval, interval, TimeUnit.MILLISECONDS);
        pruneTask = core.scheduler().scheduleWithFixedDelay(() -> {
            try {
                core.records().pruneSync(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10));
            } catch (RuntimeException e) {
                core.platform().logger().warn("Could not prune sync table: " + e.getMessage());
            }
        }, 1, 5, TimeUnit.MINUTES);
    }

    public void stop() {
        if (pollTask != null) {
            pollTask.cancel(false);
        }
        if (pruneTask != null) {
            pruneTask.cancel(false);
        }
    }

    /** Tells other servers about something that happened here. Safe to call when sync is disabled. */
    public void publish(String type, String data) {
        if (!enabled()) {
            return;
        }
        try {
            core.records().addSync(instanceId, type, data);
        } catch (RuntimeException e) {
            core.platform().logger().error("Could not publish network event " + type, e);
        }
    }

    /** Sends a staff notification on this server and every other server. */
    public void notifyStaff(String permission, String message) {
        if (!core.platform().isProxy()) {
            core.platform().broadcast(message, permission);
        }
        publish("NOTIFY", permission + "\n" + message);
    }

    private void poll() {
        try {
            for (SyncEvent event : core.records().syncAfter(lastId)) {
                lastId = Math.max(lastId, event.id());
                if (instanceId.equals(event.origin())) {
                    continue;
                }
                handle(event);
            }
        } catch (Throwable t) {
            core.platform().logger().warn("Network sync failed: " + t.getMessage());
        }
    }

    private void handle(SyncEvent event) {
        switch (event.type()) {
            case "PUNISH" -> {
                Punishment p = core.punishments().byId(parseId(event.data()));
                if (p != null) {
                    core.punishments().apply(p, false);
                    for (dev.banova.api.BanovaListener l : core.apiListeners()) {
                        l.onPunish(p);
                    }
                }
            }
            case "REVOKE" -> {
                Punishment p = core.punishments().byId(parseId(event.data()));
                if (p != null) {
                    core.punishments().applyRevoke(p);
                    for (dev.banova.api.BanovaListener l : core.apiListeners()) {
                        l.onRevoke(p);
                    }
                }
            }
            case "UPDATE" -> {
                Punishment p = core.punishments().byId(parseId(event.data()));
                if (p != null) {
                    core.punishments().refreshLocal(p);
                }
            }
            case "NOTIFY" -> {
                if (!core.platform().isProxy() && event.data() != null) {
                    int split = event.data().indexOf('\n');
                    if (split > 0) {
                        core.platform().broadcast(event.data().substring(split + 1), event.data().substring(0, split));
                    }
                }
            }
            case "LOCKDOWN" -> core.lockdown().load();
            default -> {
                // unknown event from a newer version
            }
        }
    }

    private static long parseId(String data) {
        try {
            return Long.parseLong(data.trim());
        } catch (Exception e) {
            return -1;
        }
    }
}
