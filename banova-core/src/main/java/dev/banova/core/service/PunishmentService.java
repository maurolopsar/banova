package dev.banova.core.service;

import dev.banova.api.BanovaListener;
import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.config.YamlConfig;
import dev.banova.core.model.ActorRef;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Punishment;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.platform.OnlinePlayer;
import dev.banova.core.storage.PunishmentRepository;
import dev.banova.core.util.Codes;
import dev.banova.core.util.Durations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Creates, revokes, edits and enforces punishments. Every method blocks on the database,
 * so call it from an async thread.
 */
public final class PunishmentService {

    private final BanovaCore core;

    public PunishmentService(BanovaCore core) {
        this.core = core;
    }

    private PunishmentRepository repo() {
        return core.punishmentRepo();
    }

    // ------------------------------------------------------------------ requests

    /** Everything needed to create a punishment. */
    public static final class Request {
        public PunishmentType type;
        public PlayerRecord target;
        public boolean ipBased;
        public String reason;
        /** 0 = permanent. */
        public long duration;
        public boolean silent;
        public ActorRef actor;
        public String context;
        public String template;
        public int templateStep = -1;
        public String importRef;

        public static Request of(PunishmentType type, PlayerRecord target, ActorRef actor) {
            Request r = new Request();
            r.type = type;
            r.target = target;
            r.actor = actor;
            return r;
        }
    }

    public record Outcome(Punishment punishment, List<Punishment> replaced, String errorKey, Object[] errorArgs) {

        static Outcome error(String key, Object... args) {
            return new Outcome(null, List.of(), key, args);
        }

        public boolean ok() {
            return punishment != null;
        }
    }

    // ------------------------------------------------------------------ permission & limit checks

    /** Permission needed for a custom (non-template) punishment. */
    public static String permissionFor(PunishmentType type, boolean temporary, boolean ip) {
        String base = type.name().toLowerCase(Locale.ROOT);
        if (ip && (type == PunishmentType.BAN || type == PunishmentType.MUTE)) {
            return "banova.ip" + base;
        }
        if (temporary && (type == PunishmentType.BAN || type == PunishmentType.MUTE)) {
            return "banova.temp" + base;
        }
        return "banova." + base;
    }

    public static boolean canCreate(CommandActor actor, PunishmentType type, boolean temporary, boolean ip) {
        if (ip && !actor.hasPermission(permissionFor(type, temporary, true))) {
            return false;
        }
        if (temporary && (type == PunishmentType.BAN || type == PunishmentType.MUTE)) {
            return actor.hasPermission(permissionFor(type, true, false)) || actor.hasPermission(permissionFor(type, false, false));
        }
        return actor.hasPermission(permissionFor(type, false, false));
    }

    /**
     * Longest duration the actor may use for a type, from {@code limits} in config.yml.
     *
     * @return -1 for unlimited, otherwise the maximum in ms
     */
    public long maxDuration(CommandActor actor, PunishmentType type) {
        if (actor.isConsole() || actor.hasPermission("banova.limit.bypass")) {
            return -1;
        }
        YamlConfig limits = core.config().section("punishments.limits");
        long best = -2;
        for (String group : limits.keys("")) {
            if (!actor.hasPermission("banova.limit." + group.toLowerCase(Locale.ROOT))) {
                continue;
            }
            String raw = limits.getString(group + "." + type.name().toLowerCase(Locale.ROOT), "");
            if (raw.isBlank()) {
                return -1; // this group does not limit the type
            }
            long max = Durations.parse(raw);
            if (max == Durations.PERMANENT) {
                return -1;
            }
            if (max > best) {
                best = max;
            }
        }
        return best == -2 ? -1 : best;
    }

    /**
     * Validates a staff-made punishment. Returns null when allowed, otherwise an error message key + args.
     */
    public Outcome check(CommandActor actor, Request req, boolean fromTemplate) {
        if (req.target == null) {
            return Outcome.error("error.player-not-found", "player", "?");
        }
        if (!fromTemplate && !canCreate(actor, req.type, req.duration > 0, req.ipBased)) {
            return Outcome.error("error.no-permission");
        }
        if (req.target.uuid() != null && req.target.uuid().equals(actor.uuid())) {
            return Outcome.error("error.self");
        }
        if (!fromTemplate && req.type.isLasting()) {
            long max = maxDuration(actor, req.type);
            if (max >= 0 && (req.duration == 0 || req.duration > max)) {
                return Outcome.error("error.limit", "max", Durations.format(max));
            }
        }
        if (req.target.uuid() != null && !actor.hasPermission("banova.exempt.bypass")) {
            OnlinePlayer online = core.platform().player(req.target.uuid());
            if (online != null && online.hasPermission("banova.exempt")) {
                return Outcome.error("error.exempt", "player", req.target.name());
            }
        }
        if (req.ipBased && (req.target.lastIp() == null || req.target.lastIp().isEmpty())) {
            return Outcome.error("error.no-ip", "player", req.target.name());
        }
        return null;
    }

    // ------------------------------------------------------------------ create

    public Outcome create(Request req) {
        PunishmentType type = req.type;
        PlayerRecord target = req.target;
        String ip = target.lastIp();
        List<Punishment> replaced = new ArrayList<>();
        if (type == PunishmentType.BAN || type == PunishmentType.MUTE) {
            List<Punishment> existing = req.ipBased ? repo().activeByIp(type, ip) : repo().active(type, target.uuid(), null);
            if (!req.ipBased) {
                existing.removeIf(p -> p.ipBased);
            }
            if (!existing.isEmpty()) {
                if (!core.config().getBoolean("punishments.replace-existing", true) && req.importRef == null) {
                    return Outcome.error("error.already-" + type.name().toLowerCase(Locale.ROOT), "player", target.name(), "id", existing.get(0).id);
                }
                replaced.addAll(existing);
            }
        }

        long now = System.currentTimeMillis();
        long duration = req.duration;
        if (type == PunishmentType.WARN && duration == 0) {
            long def = Durations.parse(core.config().getString("warnings.default-duration", "perm"));
            duration = Math.max(0, def);
        }
        Punishment p = new Punishment();
        p.type = type;
        p.targetUuid = target.uuid();
        p.targetName = target.name();
        p.targetIp = ip;
        p.ipBased = req.ipBased;
        p.reason = cleanReason(req.reason, type);
        p.actorUuid = req.actor.uuid();
        p.actorName = req.actor.name();
        p.createdAt = now;
        p.expiresAt = type.isLasting() && duration > 0 ? now + duration : 0;
        p.active = type.isLasting();
        p.server = core.serverName();
        p.silent = req.silent;
        p.template = req.template;
        p.templateStep = req.templateStep;
        p.importRef = req.importRef;
        if (type == PunishmentType.BAN || type == PunishmentType.MUTE || type == PunishmentType.WARN) {
            p.appealCode = uniqueAppealCode();
        }
        repo().insert(p, req.context);
        core.audit().log(req.actor, "PUNISH", p.targetUuid, p.targetName, p.id,
                core.formatter().typeName(type) + (p.ipBased ? " (IP)" : "") + " · "
                        + (type.isLasting() ? core.formatter().duration(p.length()) : "-") + " · " + p.reason
                        + (p.template != null ? " · template " + p.template + " #" + (p.templateStep + 1) : ""));

        for (Punishment old : replaced) {
            repo().revoke(old.id, req.actor.uuid(), req.actor.name(), "Replaced by #" + p.id);
            old.revokedAt = now;
            old.active = false;
        }

        if (req.importRef == null) {
            apply(p, true);
            core.sync().publish("PUNISH", String.valueOf(p.id));
            core.discord().punishment(p);
            fire(listener -> listener.onPunish(p));
            if (type == PunishmentType.WARN) {
                checkWarnThresholds(p);
            }
        }
        return new Outcome(p, replaced, null, null);
    }

    private String cleanReason(String reason, PunishmentType type) {
        String r = reason == null ? "" : reason.trim();
        if (r.isEmpty()) {
            r = core.messages().raw("default-reason." + type.name().toLowerCase(Locale.ROOT));
        }
        return r.length() > 500 ? r.substring(0, 500) : r;
    }

    private String uniqueAppealCode() {
        for (int i = 0; i < 10; i++) {
            String code = Codes.humanCode();
            if (!repo().appealCodeExists(code)) {
                return code;
            }
        }
        return Codes.humanCode() + Codes.humanCode().charAt(0);
    }

    private void checkWarnThresholds(Punishment warn) {
        if (warn.targetUuid == null) {
            return;
        }
        int count = repo().active(PunishmentType.WARN, warn.targetUuid, null).size();
        for (YamlConfig threshold : core.config().getSectionList("warnings.thresholds")) {
            if (threshold.getInt("count", -1) != count) {
                continue;
            }
            PunishmentType type = PunishmentType.parse(threshold.getString("type", "mute"));
            long duration = Durations.parse(threshold.getString("duration", "1h"));
            if (type == null || duration < 0) {
                core.platform().logger().warn("Invalid warnings.thresholds entry for count " + count);
                continue;
            }
            PlayerRecord target = core.players().byUuid(warn.targetUuid);
            if (target == null) {
                target = new PlayerRecord(warn.targetUuid, warn.targetName, warn.targetIp, 0, 0);
            }
            Request req = Request.of(type, target, ActorRef.SYSTEM);
            req.duration = duration;
            req.reason = threshold.getString("reason", core.messages().raw("default-reason.warn-threshold")).replace("{count}", String.valueOf(count));
            create(req);
        }
    }

    // ------------------------------------------------------------------ enforcement

    /** Online players affected by a punishment on this server/proxy. */
    public List<OnlinePlayer> affected(Punishment p) {
        List<OnlinePlayer> out = new ArrayList<>();
        for (OnlinePlayer online : core.platform().onlinePlayers()) {
            if (p.targetUuid != null && p.targetUuid.equals(online.uuid())) {
                out.add(online);
            } else if (p.ipBased && p.targetIp != null && p.targetIp.equals(online.ip())) {
                out.add(online);
            }
        }
        return out;
    }

    /**
     * Enforces a punishment on this instance: kicks, mute cache, messages and broadcasts.
     * Called on the server that created it and, through network sync, on every other server.
     */
    public void apply(Punishment p, boolean origin) {
        if (p == null) {
            return;
        }
        boolean proxy = core.platform().isProxy();
        List<OnlinePlayer> targets = affected(p);
        switch (p.type) {
            case BAN -> {
                if (p.isInEffect()) {
                    disconnect(targets, core.formatter().banScreen(p));
                }
            }
            case KICK -> disconnect(targets, core.formatter().kickScreen(p));
            case MUTE -> {
                if (!proxy && p.isInEffect()) {
                    String message = core.formatter().muteMessage(p);
                    for (OnlinePlayer player : targets) {
                        core.mutes().put(player.uuid(), p);
                        player.send(message);
                    }
                }
            }
            case WARN -> {
                if (!proxy) {
                    String message = core.messages().get("warn.notify", core.formatter().placeholders(p));
                    targets.forEach(player -> player.send(message));
                }
            }
        }
        if (!proxy) {
            broadcastPunishment(p);
        }
    }

    /**
     * Disconnects players. Behind a Banova proxy the proxy does it (the player sees the screen instead of
     * being sent to a fallback server); the backend only acts as a backup a few seconds later.
     */
    private void disconnect(List<OnlinePlayer> targets, String screen) {
        if (targets.isEmpty()) {
            return;
        }
        if (core.sync().proxyActive()) {
            List<java.util.UUID> ids = targets.stream().map(OnlinePlayer::uuid).toList();
            core.scheduler().schedule(() -> {
                for (java.util.UUID id : ids) {
                    OnlinePlayer still = core.platform().player(id);
                    if (still != null) {
                        still.kick(screen);
                    }
                }
            }, 3, java.util.concurrent.TimeUnit.SECONDS);
            return;
        }
        targets.forEach(player -> player.kick(screen));
    }

    private void broadcastPunishment(Punishment p) {
        String key = "broadcast." + p.type.name().toLowerCase(Locale.ROOT) + (p.type.isLasting() && !p.isPermanent() ? "-temp" : "");
        Object[] placeholders = core.formatter().placeholders(p);
        String mode = core.config().getString("punishments.broadcast", "all").toLowerCase(Locale.ROOT);
        if (p.silent || mode.equals("staff")) {
            core.platform().broadcast(core.messages().get("broadcast.silent-prefix") + core.messages().get(key, placeholders), "banova.notify.silent");
        } else if (mode.equals("all")) {
            core.platform().broadcast(core.messages().get(key, placeholders), null);
        } else {
            core.platform().broadcast(core.messages().get(key, placeholders), "banova.notify");
        }
    }

    // ------------------------------------------------------------------ revoke

    public boolean revoke(Punishment p, ActorRef actor, String reason) {
        if (p == null || !p.type.isLasting() || p.isRevoked()) {
            return false;
        }
        String clean = reason == null || reason.isBlank() ? core.messages().raw("default-reason.revoke") : reason.trim();
        if (clean.length() > 500) {
            clean = clean.substring(0, 500);
        }
        if (!repo().revoke(p.id, actor.uuid(), actor.name(), clean)) {
            return false;
        }
        p.active = false;
        p.revokedAt = System.currentTimeMillis();
        p.revokedByUuid = actor.uuid();
        p.revokedByName = actor.name();
        p.revokeReason = clean;
        core.audit().log(actor, "REVOKE", p.targetUuid, p.targetName, p.id, core.formatter().typeName(p.type) + " · " + clean);
        applyRevoke(p);
        core.sync().publish("REVOKE", String.valueOf(p.id));
        core.discord().revoke(p);
        fire(listener -> listener.onRevoke(p));
        return true;
    }

    public void applyRevoke(Punishment p) {
        if (p.type == PunishmentType.MUTE) {
            for (OnlinePlayer player : affected(p)) {
                if (core.mutes().removeIf(player.uuid(), p.id) && !core.platform().isProxy()) {
                    player.send(core.messages().get("mute.unmuted", core.formatter().placeholders(p)));
                }
            }
        }
        if (!core.platform().isProxy()) {
            String key = "broadcast.revoke-" + p.type.name().toLowerCase(Locale.ROOT);
            core.platform().broadcast(core.messages().get(key, core.formatter().placeholders(p)), "banova.notify");
        }
    }

    // ------------------------------------------------------------------ edit

    public void editReason(Punishment p, String reason, ActorRef actor) {
        String old = p.reason;
        p.reason = reason.length() > 500 ? reason.substring(0, 500) : reason;
        repo().updateReason(p.id, p.reason);
        core.audit().log(actor, "EDIT", p.targetUuid, p.targetName, p.id, "reason: " + old + " -> " + p.reason);
        core.sync().publish("UPDATE", String.valueOf(p.id));
        refreshLocal(p);
    }

    /** Changes the duration, counted from the original creation time. 0 = permanent. */
    public void editDuration(Punishment p, long duration, ActorRef actor) {
        long old = p.length();
        p.expiresAt = duration <= 0 ? 0 : p.createdAt + duration;
        repo().updateExpiry(p.id, p.expiresAt);
        p.active = p.expiresAt == 0 || p.expiresAt > System.currentTimeMillis();
        core.audit().log(actor, "EDIT", p.targetUuid, p.targetName, p.id,
                "duration: " + core.formatter().duration(old) + " -> " + core.formatter().duration(p.length()));
        core.sync().publish("UPDATE", String.valueOf(p.id));
        refreshLocal(p);
    }

    /** Re-reads an edited punishment into local caches. */
    public void refreshLocal(Punishment p) {
        if (p.type != PunishmentType.MUTE || core.platform().isProxy()) {
            return;
        }
        for (OnlinePlayer player : affected(p)) {
            if (p.isInEffect()) {
                core.mutes().put(player.uuid(), p);
            } else {
                core.mutes().removeIf(player.uuid(), p.id);
            }
        }
    }

    // ------------------------------------------------------------------ queries

    public Punishment byId(long id) {
        return repo().byId(id);
    }

    public Punishment activeBan(UUID uuid, String ip) {
        List<Punishment> list = repo().active(PunishmentType.BAN, uuid, ip);
        return list.isEmpty() ? null : list.get(0);
    }

    public Punishment activeMute(UUID uuid, String ip) {
        List<Punishment> list = repo().active(PunishmentType.MUTE, uuid, ip);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Punishment> activeWarns(UUID uuid) {
        return repo().active(PunishmentType.WARN, uuid, null);
    }

    /** Revokes every punishment a staff member made in the time window. */
    public int rollback(UUID staff, String staffName, long since, ActorRef actor) {
        int count = 0;
        List<Punishment> list = repo().byActor(staff, staffName, since, 5000, 0);
        for (Punishment p : list) {
            if (p.type.isLasting() && !p.isRevoked() && revoke(p, actor, "Staff rollback")) {
                count++;
            }
        }
        core.audit().log(actor, "ROLLBACK", staff, staffName, 0, count + " punishments since " + core.formatter().date(since));
        return count;
    }

    private void fire(java.util.function.Consumer<BanovaListener> action) {
        for (BanovaListener listener : core.apiListeners()) {
            try {
                action.accept(listener);
            } catch (Throwable t) {
                core.platform().logger().error("A Banova API listener failed", t);
            }
        }
    }
}
