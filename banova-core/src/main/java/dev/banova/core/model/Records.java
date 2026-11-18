package dev.banova.core.model;

import java.util.Set;
import java.util.UUID;

/**
 * Small immutable data types stored by Banova.
 */
public final class Records {

    private Records() {
    }

    public record Note(long id, UUID targetUuid, String content, UUID actorUuid, String actorName, long createdAt) {
    }

    public record Evidence(long id, long punishmentId, String content, UUID actorUuid, String actorName, long createdAt) {
    }

    public enum AppealStatus {
        PENDING,
        ACCEPTED,
        DENIED
    }

    public record Appeal(long id, long punishmentId, AppealStatus status, String message, String contact, Source source,
                         long createdAt, UUID handledByUuid, String handledByName, long handledAt, String response) {
    }

    public record AuditEntry(long id, long createdAt, UUID actorUuid, String actorName, Source source, String action,
                             UUID targetUuid, String targetName, long punishmentId, String details) {
    }

    public record WebSession(String id, String tokenHash, UUID uuid, String name, Set<String> permissions,
                             long createdAt, long lastUsed, long expiresAt, boolean remember, String ip, String userAgent) {

        public boolean has(String permission) {
            return permissions.contains("*") || permissions.contains(permission);
        }
    }

    public record SyncEvent(long id, long createdAt, String origin, String type, String data) {
    }
}
