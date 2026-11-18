package dev.banova.core.importer;

import dev.banova.api.PunishmentType;

import java.util.UUID;

/**
 * A punishment (or note) read from another plugin, before it is stored in Banova.
 */
public final class Imported {

    public PunishmentType type;
    /** True when this entry is a staff note instead of a punishment (AdvancedBan notes). */
    public boolean note;
    public UUID uuid;
    public String name;
    public String ip;
    public boolean ipBased;
    public String reason;
    public UUID actorUuid;
    public String actorName;
    public long createdAt;
    /** 0 = permanent. */
    public long expiresAt;
    public boolean active;
    public String revokedByName;
    public long revokedAt;
    public String revokeReason;
    public boolean silent;
    public String server;
    /** Unique reference so running an import twice does not duplicate data. */
    public String ref;
}
