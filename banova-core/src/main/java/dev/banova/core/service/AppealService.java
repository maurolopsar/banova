package dev.banova.core.service;

import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.model.ActorRef;
import dev.banova.core.model.Punishment;
import dev.banova.core.model.Records.Appeal;
import dev.banova.core.model.Records.AppealStatus;
import dev.banova.core.model.Source;
import dev.banova.core.util.Durations;

import java.util.List;
import java.util.Locale;

/**
 * Appeals: players ask for a punishment to be removed, staff accept or deny.
 */
public final class AppealService {

    private final BanovaCore core;

    public AppealService(BanovaCore core) {
        this.core = core;
    }

    public boolean enabled() {
        return core.config().getBoolean("appeals.enabled", true);
    }

    public boolean webAppealsAvailable() {
        return enabled() && core.config().getBoolean("web.enabled", false) && core.config().getBoolean("web.appeals", true);
    }

    public String appealUrl() {
        String base = core.config().getString("web.public-url", "http://localhost:8090").trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/appeal";
    }

    public boolean isAppealable(PunishmentType type) {
        for (String t : core.config().getStringList("appeals.types")) {
            if (PunishmentType.parse(t) == type) {
                return true;
            }
        }
        return false;
    }

    /** Why the punishment cannot be appealed right now, as a message key, or null when it can. */
    public String blockReason(Punishment p) {
        if (!enabled()) {
            return "appeal.disabled";
        }
        if (p == null) {
            return "appeal.not-found";
        }
        if (!isAppealable(p.type)) {
            return "appeal.not-appealable";
        }
        if (!p.isInEffect()) {
            return "appeal.not-active";
        }
        List<Appeal> previous = core.records().appealsFor(p.id);
        for (Appeal a : previous) {
            if (a.status() == AppealStatus.PENDING) {
                return "appeal.already-pending";
            }
        }
        int max = core.config().getInt("appeals.max-per-punishment", 3);
        if (max > 0 && previous.size() >= max) {
            return "appeal.limit-reached";
        }
        long cooldown = Math.max(0, Durations.parse(core.config().getString("appeals.cooldown-after-deny", "3d")));
        for (Appeal a : previous) {
            if (a.status() == AppealStatus.DENIED && a.handledAt() + cooldown > System.currentTimeMillis()) {
                return "appeal.cooldown";
            }
        }
        return null;
    }

    public long cooldownLeft(Punishment p) {
        long cooldown = Math.max(0, Durations.parse(core.config().getString("appeals.cooldown-after-deny", "3d")));
        long left = 0;
        for (Appeal a : core.records().appealsFor(p.id)) {
            if (a.status() == AppealStatus.DENIED) {
                left = Math.max(left, a.handledAt() + cooldown - System.currentTimeMillis());
            }
        }
        return Math.max(0, left);
    }

    public int minLength() {
        return Math.max(1, core.config().getInt("appeals.min-length", 20));
    }

    /** Creates an appeal after validation by the caller. */
    public long create(Punishment p, String message, String contact, Source source, String ip, ActorRef creator) {
        String text = message.trim();
        if (text.length() > 2000) {
            text = text.substring(0, 2000);
        }
        String cleanContact = contact == null ? null : contact.trim();
        if (cleanContact != null && cleanContact.length() > 128) {
            cleanContact = cleanContact.substring(0, 128);
        }
        long id = core.records().addAppeal(p.id, text, cleanContact, source, ip);
        core.audit().log(creator, "APPEAL_CREATE", p.targetUuid, p.targetName, p.id, "appeal #" + id);
        core.sync().notifyStaff("banova.notify.appeals", core.messages().get("appeal.staff-alert",
                "player", p.targetName, "id", id, "type", core.formatter().typeName(p.type)));
        core.discord().appeal(p, id, text);
        return id;
    }

    public boolean accept(Appeal appeal, ActorRef actor, String response) {
        String clean = response == null ? "" : response.trim();
        if (!core.records().handleAppeal(appeal.id(), AppealStatus.ACCEPTED, actor, clean)) {
            return false;
        }
        Punishment p = core.punishments().byId(appeal.punishmentId());
        core.audit().log(actor, "APPEAL_ACCEPT", p == null ? null : p.targetUuid, p == null ? null : p.targetName,
                appeal.punishmentId(), "appeal #" + appeal.id() + (clean.isEmpty() ? "" : " · " + clean));
        if (p != null && p.isInEffect()) {
            core.punishments().revoke(p, actor, core.messages().raw("default-reason.appeal-accepted")
                    + (clean.isEmpty() ? "" : ": " + clean));
        }
        return true;
    }

    public boolean deny(Appeal appeal, ActorRef actor, String response) {
        String clean = response == null ? "" : response.trim();
        if (!core.records().handleAppeal(appeal.id(), AppealStatus.DENIED, actor, clean)) {
            return false;
        }
        Punishment p = core.punishments().byId(appeal.punishmentId());
        core.audit().log(actor, "APPEAL_DENY", p == null ? null : p.targetUuid, p == null ? null : p.targetName,
                appeal.punishmentId(), "appeal #" + appeal.id() + (clean.isEmpty() ? "" : " · " + clean));
        return true;
    }

    public static AppealStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("all")) {
            return null;
        }
        try {
            return AppealStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return AppealStatus.PENDING;
        }
    }
}
