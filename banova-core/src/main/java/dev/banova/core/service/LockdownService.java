package dev.banova.core.service;

import dev.banova.core.BanovaCore;
import dev.banova.core.model.ActorRef;

/**
 * Network-wide lockdown: only staff with {@code banova.lockdown.bypass} can join.
 */
public final class LockdownService {

    private final BanovaCore core;
    private volatile String reason;

    public LockdownService(BanovaCore core) {
        this.core = core;
    }

    public void load() {
        try {
            reason = core.records().meta("lockdown");
        } catch (RuntimeException e) {
            core.platform().logger().warn("Could not read lockdown state: " + e.getMessage());
        }
    }

    public boolean active() {
        return reason != null;
    }

    public String reason() {
        return reason;
    }

    public void set(String newReason, ActorRef actor) {
        reason = newReason;
        core.records().setMeta("lockdown", newReason);
        core.audit().log(actor, newReason == null ? "LOCKDOWN_OFF" : "LOCKDOWN_ON", null, null, 0, newReason);
        core.sync().publish("LOCKDOWN", newReason == null ? "" : newReason);
    }

    public String screen() {
        return core.messages().get("lockdown.screen", "reason", reason == null ? "" : reason);
    }
}
