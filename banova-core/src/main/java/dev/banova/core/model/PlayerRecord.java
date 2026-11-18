package dev.banova.core.model;

import java.util.UUID;

/**
 * A known player (stored when they join) or an IP-only target.
 */
public final class PlayerRecord {

    private final UUID uuid;
    private final String name;
    private final String lastIp;
    private final long firstSeen;
    private final long lastSeen;

    public PlayerRecord(UUID uuid, String name, String lastIp, long firstSeen, long lastSeen) {
        this.uuid = uuid;
        this.name = name;
        this.lastIp = lastIp;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
    }

    /** Target that is only an IP address (for /ipban 1.2.3.4). */
    public static PlayerRecord ipOnly(String ip) {
        return new PlayerRecord(null, ip, ip, 0, 0);
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public String lastIp() {
        return lastIp;
    }

    public long firstSeen() {
        return firstSeen;
    }

    public long lastSeen() {
        return lastSeen;
    }

    public boolean isIpOnly() {
        return uuid == null;
    }
}
