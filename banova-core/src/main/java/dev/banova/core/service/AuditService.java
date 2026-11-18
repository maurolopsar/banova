package dev.banova.core.service;

import dev.banova.core.BanovaCore;
import dev.banova.core.model.ActorRef;

import java.util.UUID;

/**
 * Writes the staff audit log. Failures are logged but never break the action itself.
 */
public final class AuditService {

    private final BanovaCore core;

    public AuditService(BanovaCore core) {
        this.core = core;
    }

    public void log(ActorRef actor, String action, UUID targetUuid, String targetName, long punishmentId, String details) {
        try {
            core.records().addAudit(actor, action, targetUuid, targetName, punishmentId, details);
        } catch (RuntimeException e) {
            core.platform().logger().error("Could not write audit entry " + action, e);
        }
    }
}
