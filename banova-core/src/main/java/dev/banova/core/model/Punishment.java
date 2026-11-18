package dev.banova.core.model;

import dev.banova.api.PunishmentType;
import dev.banova.api.PunishmentView;

import java.util.UUID;

/**
 * A stored punishment. Mutable only inside the storage/service layer.
 */
public final class Punishment implements PunishmentView {

    public long id;
    public PunishmentType type;
    public UUID targetUuid;
    public String targetName;
    public String targetIp;
    public boolean ipBased;
    public String reason;
    public UUID actorUuid;
    public String actorName;
    public long createdAt;
    /** 0 = permanent. */
    public long expiresAt;
    public boolean active;
    public UUID revokedByUuid;
    public String revokedByName;
    public long revokedAt;
    public String revokeReason;
    public String server;
    public boolean silent;
    public String template;
    public int templateStep = -1;
    public String appealCode;
    public String importRef;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public PunishmentType getType() {
        return type;
    }

    @Override
    public UUID getTargetUuid() {
        return targetUuid;
    }

    @Override
    public String getTargetName() {
        return targetName;
    }

    @Override
    public boolean isIpBased() {
        return ipBased;
    }

    @Override
    public String getReason() {
        return reason;
    }

    @Override
    public UUID getActorUuid() {
        return actorUuid;
    }

    @Override
    public String getActorName() {
        return actorName;
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public long getExpiresAt() {
        return expiresAt;
    }

    @Override
    public boolean isPermanent() {
        return expiresAt <= 0;
    }

    public boolean isExpired() {
        return expiresAt > 0 && System.currentTimeMillis() >= expiresAt;
    }

    public boolean isRevoked() {
        return revokedAt > 0;
    }

    @Override
    public boolean isInEffect() {
        return type.isLasting() && active && !isExpired();
    }

    @Override
    public boolean isSilent() {
        return silent;
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public String getServer() {
        return server;
    }

    /** Remaining time in ms, 0 when permanent or already expired. */
    public long remaining() {
        if (expiresAt <= 0) {
            return 0;
        }
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    /** Original length in ms, 0 when permanent. */
    public long length() {
        return expiresAt <= 0 ? 0 : Math.max(0, expiresAt - createdAt);
    }

    /** Human friendly status key used for messages: active, expired, revoked, done. */
    public String status() {
        if (!type.isLasting()) {
            return "done";
        }
        if (isRevoked()) {
            return "revoked";
        }
        if (isExpired() || !active) {
            return "expired";
        }
        return "active";
    }
}
