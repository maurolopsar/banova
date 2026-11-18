package dev.banova.core.service;

import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.config.Messages;
import dev.banova.core.model.Punishment;
import dev.banova.core.util.Durations;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns punishments into the placeholder values used in messages, ban screens and webhooks.
 */
public final class Formatter {

    private final BanovaCore core;
    private DateTimeFormatter dateFormat;

    public Formatter(BanovaCore core) {
        this.core = core;
        reload();
    }

    public void reload() {
        String pattern = core.config().getString("date-format", "dd/MM/yyyy HH:mm");
        String zone = core.config().getString("timezone", "");
        ZoneId zoneId;
        try {
            zoneId = zone == null || zone.isBlank() ? ZoneId.systemDefault() : ZoneId.of(zone);
        } catch (Exception e) {
            zoneId = ZoneId.systemDefault();
        }
        try {
            dateFormat = DateTimeFormatter.ofPattern(pattern, Locale.ROOT).withZone(zoneId);
        } catch (IllegalArgumentException e) {
            dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT).withZone(zoneId);
        }
    }

    private Messages msg() {
        return core.messages();
    }

    public String date(long millis) {
        if (millis <= 0) {
            return "-";
        }
        return dateFormat.format(Instant.ofEpochMilli(millis));
    }

    public String duration(long millis) {
        return millis <= 0 ? msg().raw("format.permanent") : Durations.format(millis);
    }

    /** Duration of a template step; kicks have none. */
    public String stepDuration(PunishmentType type, long millis) {
        return type == PunishmentType.KICK ? "" : duration(millis);
    }

    public String typeName(PunishmentType type) {
        return msg().raw("type." + type.name().toLowerCase(Locale.ROOT));
    }

    public String statusName(Punishment p) {
        return msg().raw("status." + p.status());
    }

    public String expires(Punishment p) {
        if (!p.type.isLasting()) {
            return "-";
        }
        return p.isPermanent() ? msg().raw("format.never") : date(p.expiresAt);
    }

    public String remaining(Punishment p) {
        if (p.isPermanent()) {
            return msg().raw("format.permanent");
        }
        long left = p.remaining();
        return left <= 0 ? msg().raw("status.expired") : Durations.format(left);
    }

    /** Appeal instructions for ban screens; empty when appeals are not available. */
    public String appealLine(Punishment p) {
        if (p.appealCode != null && core.appeals().webAppealsAvailable() && core.appeals().isAppealable(p.type)) {
            return msg().get("screen.appeal-web", "url", core.appeals().appealUrl(), "code", p.appealCode);
        }
        String custom = core.config().getString("ban-screen.appeal-url", "");
        if (!custom.isBlank()) {
            return msg().get("screen.appeal-link", "url", custom);
        }
        return "";
    }

    /** Placeholder pairs for {@link Messages#get(String, Object...)}. */
    public Object[] placeholders(Punishment p) {
        List<Object> list = new ArrayList<>();
        list.add("id");
        list.add(p.id);
        list.add("player");
        list.add(p.targetName == null ? "?" : p.targetName);
        list.add("reason");
        list.add(p.reason);
        list.add("actor");
        list.add(p.actorName);
        list.add("type");
        list.add(typeName(p.type));
        list.add("date");
        list.add(date(p.createdAt));
        list.add("expires");
        list.add(expires(p));
        list.add("remaining");
        list.add(remaining(p));
        list.add("duration");
        list.add(p.type.isLasting() ? duration(p.length()) : "-");
        list.add("server");
        list.add(p.server == null ? "" : p.server);
        list.add("status");
        list.add(statusName(p));
        list.add("appeal_code");
        list.add(p.appealCode == null ? "" : p.appealCode);
        list.add("appeal");
        list.add(appealLine(p));
        list.add("ip");
        list.add(p.ipBased ? msg().raw("format.ip-tag") : "");
        list.add("silent");
        list.add(p.silent ? msg().raw("format.silent-tag") : "");
        list.add("revoked_by");
        list.add(p.revokedByName == null ? "" : p.revokedByName);
        list.add("revoke_reason");
        list.add(p.revokeReason == null ? "" : p.revokeReason);
        return list.toArray();
    }

    /** Disconnect screen for a ban. */
    public String banScreen(Punishment p) {
        String key = p.isPermanent() ? "screen.ban-permanent" : "screen.ban-temporary";
        return cleanEmptyLines(msg().get(key, placeholders(p)));
    }

    public String kickScreen(Punishment p) {
        return cleanEmptyLines(msg().get("screen.kick", placeholders(p)));
    }

    public String muteMessage(Punishment p) {
        String key = p.isPermanent() ? "mute.notify-permanent" : "mute.notify-temporary";
        return cleanEmptyLines(msg().get(key, placeholders(p)));
    }

    /** Removes lines left empty because an optional placeholder (like {appeal}) was blank. */
    static String cleanEmptyLines(String text) {
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean placeholderOnly = line.replaceAll("(?i)[&§][0-9a-fk-or]", "").isBlank();
            if (placeholderOnly && i > 0 && i < lines.length - 1 && lines[i - 1].replaceAll("(?i)[&§][0-9a-fk-or]", "").isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(line);
        }
        return sb.toString().replaceAll("\n+$", "");
    }
}
