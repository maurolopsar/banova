package dev.banova.api;

import java.util.UUID;

/**
 * Read-only view of a punishment, safe to expose to other plugins.
 */
public interface PunishmentView {

    long getId();

    PunishmentType getType();

    /** Target player, or {@code null} for pure IP punishments. */
    UUID getTargetUuid();

    String getTargetName();

    boolean isIpBased();

    String getReason();

    /** Staff member UUID, or {@code null} for the console / automatic actions. */
    UUID getActorUuid();

    String getActorName();

    /** Creation time in epoch milliseconds. */
    long getCreatedAt();

    /** Expiry in epoch milliseconds, or {@code 0} when permanent. */
    long getExpiresAt();

    boolean isPermanent();

    /** True when the punishment is still active (not revoked and not expired). */
    boolean isInEffect();

    boolean isSilent();

    /** Escalation template id that produced this punishment, or {@code null}. */
    String getTemplate();

    /** Server that created the punishment. */
    String getServer();
}
