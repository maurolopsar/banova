package dev.banova.core.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.command.AppealsCommand;
import dev.banova.core.command.EvidenceCommand;
import dev.banova.core.command.NoteCommand;
import dev.banova.core.command.TemplateCommand;
import dev.banova.core.model.ActorRef;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Punishment;
import dev.banova.core.model.Records.Appeal;
import dev.banova.core.model.Records.AppealStatus;
import dev.banova.core.model.Records.AuditEntry;
import dev.banova.core.model.Records.Evidence;
import dev.banova.core.model.Records.Note;
import dev.banova.core.model.Records.WebSession;
import dev.banova.core.model.Source;
import dev.banova.core.service.AppealService;
import dev.banova.core.service.PunishmentService;
import dev.banova.core.service.TemplateService;
import dev.banova.core.storage.PlayerRepository;
import dev.banova.core.storage.PunishmentRepository;
import dev.banova.core.util.Codes;
import dev.banova.core.util.Durations;
import dev.banova.core.util.Text;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * JSON API used by the panel (staff) and the public appeal page.
 */
final class ApiRoutes {

    private static final int PAGE_SIZE = 25;

    private final BanovaCore core;
    private final WebServer web;

    ApiRoutes(BanovaCore core, WebServer web) {
        this.core = core;
        this.web = web;
    }

    void handle(WebExchange ex) throws IOException {
        String path = ex.path().substring("/api/".length());
        String[] seg = path.split("/");
        String method = ex.method();

        if (!method.equals("GET") && !method.equals("HEAD")) {
            // CSRF protection: browsers cannot add this header cross-site without our (never granted) CORS approval.
            if (!"1".equals(ex.header("X-Banova"))) {
                ex.error(403, "csrf", null);
                return;
            }
        }
        if (!web.limiter().allow("api:" + ex.ip(), 240, 60_000)) {
            ex.error(429, "rate_limited", null);
            return;
        }

        if (seg[0].equals("public")) {
            publicRoutes(ex, seg, method);
            return;
        }
        if (seg[0].equals("auth")) {
            authRoutes(ex, seg, method);
            return;
        }

        WebSession session = web.authenticate(ex);
        if (session == null) {
            ex.error(401, "unauthorized", null);
            return;
        }
        WebActor actor = new WebActor(session);
        if (!actor.hasPermission("banova.web")) {
            ex.error(403, "forbidden", null);
            return;
        }
        switch (seg[0]) {
            case "meta" -> meta(ex, actor);
            case "configuration" -> {
                if (need(ex, actor, "banova.config.edit")) {
                    synchronized (core) {
                        new ConfigurationEditor(core).handle(ex, actor, method);
                    }
                }
            }
            case "dashboard" -> dashboard(ex, actor);
            case "punishments" -> punishments(ex, actor, seg, method);
            case "evidence" -> evidence(ex, actor, seg, method);
            case "players" -> players(ex, actor, seg, method);
            case "notes" -> notes(ex, actor, seg, method);
            case "appeals" -> appeals(ex, actor, seg, method);
            case "audit" -> audit(ex, actor);
            case "sessions" -> sessions(ex, actor, seg, method);
            case "lockdown" -> lockdown(ex, actor, method);
            default -> ex.error(404, "not_found", null);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static boolean need(WebExchange ex, WebActor actor, String permission) throws IOException {
        if (actor.hasPermission(permission)) {
            return true;
        }
        ex.error(403, "forbidden", permission);
        return false;
    }

    private static long id(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return -1;
        }
    }

    private static JsonObject ok(WebActor actor) {
        JsonObject o = new JsonObject();
        o.addProperty("ok", true);
        o.add("messages", messages(actor));
        return o;
    }

    private static JsonObject fail(WebActor actor) {
        JsonObject o = new JsonObject();
        o.addProperty("ok", false);
        o.add("messages", messages(actor));
        return o;
    }

    private static JsonArray messages(WebActor actor) {
        JsonArray arr = new JsonArray();
        actor.messages().forEach(arr::add);
        return arr;
    }

    JsonObject pun(Punishment p, WebActor actor) {
        JsonObject o = new JsonObject();
        o.addProperty("id", p.id);
        o.addProperty("type", p.type.name());
        o.addProperty("typeName", core.formatter().typeName(p.type));
        o.addProperty("targetUuid", p.targetUuid == null ? null : p.targetUuid.toString());
        o.addProperty("targetName", p.targetName);
        o.addProperty("ipBased", p.ipBased);
        if (p.targetIp != null) {
            o.addProperty("ip", actor != null && actor.hasPermission("banova.ips") ? p.targetIp : Text.maskIp(p.targetIp));
        }
        o.addProperty("reason", p.reason);
        o.addProperty("actorName", p.actorName);
        o.addProperty("actorUuid", p.actorUuid == null ? null : p.actorUuid.toString());
        o.addProperty("createdAt", p.createdAt);
        o.addProperty("expiresAt", p.expiresAt);
        o.addProperty("permanent", p.isPermanent());
        o.addProperty("inEffect", p.isInEffect());
        o.addProperty("status", p.status());
        o.addProperty("statusName", core.formatter().statusName(p));
        o.addProperty("durationText", p.type.isLasting() ? core.formatter().duration(p.length()) : "-");
        o.addProperty("remainingText", p.isInEffect() ? core.formatter().remaining(p) : "-");
        o.addProperty("revokedByName", p.revokedByName);
        o.addProperty("revokedAt", p.revokedAt);
        o.addProperty("revokeReason", p.revokeReason);
        o.addProperty("server", p.server);
        o.addProperty("silent", p.silent);
        o.addProperty("template", p.template);
        o.addProperty("templateStep", p.templateStep);
        if (actor != null) {
            o.addProperty("appealCode", p.appealCode);
        }
        return o;
    }

    private JsonObject appealJson(Appeal a) {
        JsonObject o = new JsonObject();
        o.addProperty("id", a.id());
        o.addProperty("punishmentId", a.punishmentId());
        o.addProperty("status", a.status().name());
        o.addProperty("statusName", core.messages().raw("appeal.status-" + a.status().name().toLowerCase(Locale.ROOT)));
        o.addProperty("message", a.message());
        o.addProperty("contact", a.contact());
        o.addProperty("source", a.source().name());
        o.addProperty("createdAt", a.createdAt());
        o.addProperty("handledByName", a.handledByName());
        o.addProperty("handledAt", a.handledAt());
        o.addProperty("response", a.response());
        return o;
    }

    private JsonObject playerJson(PlayerRecord p) {
        JsonObject o = new JsonObject();
        o.addProperty("uuid", p.uuid() == null ? null : p.uuid().toString());
        o.addProperty("name", p.name());
        o.addProperty("firstSeen", p.firstSeen());
        o.addProperty("lastSeen", p.lastSeen());
        o.addProperty("online", p.uuid() != null && core.platform().player(p.uuid()) != null);
        return o;
    }

    private JsonArray punList(List<Punishment> list, WebActor actor) {
        JsonArray arr = new JsonArray();
        for (Punishment p : list) {
            arr.add(pun(p, actor));
        }
        return arr;
    }

    private static JsonObject page(JsonArray items, long total, int page, int size) {
        JsonObject o = new JsonObject();
        o.add("items", items);
        o.addProperty("total", total);
        o.addProperty("page", page);
        o.addProperty("pages", Math.max(1, (total + size - 1) / size));
        return o;
    }

    // ------------------------------------------------------------------ auth

    private void authRoutes(WebExchange ex, String[] seg, String method) throws IOException {
        String action = seg.length > 1 ? seg[1] : "";
        switch (action) {
            case "start" -> {
                if (!method.equals("POST")) {
                    ex.error(405, "method", null);
                    return;
                }
                if (!web.limiter().allow("login:" + ex.ip(), 10, 60_000)) {
                    ex.error(429, "rate_limited", null);
                    return;
                }
                WebServer.PendingLogin login = web.startLogin(ex.ip(), ex.userAgent(), ex.bodyBool("remember"));
                JsonObject o = new JsonObject();
                o.addProperty("code", login.code());
                o.addProperty("poll", login.poll());
                o.addProperty("command", "/banova web login " + login.code());
                ex.json(200, o);
            }
            case "poll" -> {
                if (!method.equals("POST")) {
                    ex.error(405, "method", null);
                    return;
                }
                String poll = ex.bodyString("poll");
                JsonObject o = new JsonObject();
                if (poll == null || poll.length() > 100) {
                    o.addProperty("status", "invalid");
                    ex.json(200, o);
                    return;
                }
                WebSession session = web.sessionForToken(poll);
                if (session == null) {
                    o.addProperty("status", "pending");
                    ex.json(200, o);
                    return;
                }
                ex.responseHeaders().add("Set-Cookie", web.cookieFor(poll, session.remember()));
                o.addProperty("status", "approved");
                o.addProperty("name", session.name());
                ex.json(200, o);
            }
            case "link" -> {
                if (!method.equals("POST")) {
                    ex.error(405, "method", null);
                    return;
                }
                if (!web.limiter().allow("link:" + ex.ip(), 10, 60_000)) {
                    ex.error(429, "rate_limited", null);
                    return;
                }
                String token = web.consumeLink(ex.bodyString("token"), ex.bodyBool("remember"), ex.ip(), ex.userAgent());
                if (token == null) {
                    ex.error(400, "invalid_link", null);
                    return;
                }
                ex.responseHeaders().add("Set-Cookie", web.cookieFor(token, ex.bodyBool("remember")));
                JsonObject o = new JsonObject();
                o.addProperty("ok", true);
                ex.json(200, o);
            }
            case "logout" -> {
                WebSession session = web.authenticate(ex);
                if (session != null) {
                    core.records().deleteSession(session.id());
                }
                ex.responseHeaders().add("Set-Cookie", WebServer.clearCookie());
                JsonObject o = new JsonObject();
                o.addProperty("ok", true);
                ex.json(200, o);
            }
            default -> ex.error(404, "not_found", null);
        }
    }

    // ------------------------------------------------------------------ meta & dashboard

    private void meta(WebExchange ex, WebActor actor) throws IOException {
        JsonObject o = new JsonObject();
        o.addProperty("server", core.serverName());
        o.addProperty("version", core.platform().version());
        o.addProperty("platform", core.platform().platformName());
        o.addProperty("language", core.messages().code());
        o.addProperty("avatars", core.config().getBoolean("web.avatars", true));
        o.addProperty("appealsEnabled", core.appeals().enabled());
        o.addProperty("appealUrl", core.appeals().appealUrl());
        JsonObject user = new JsonObject();
        user.addProperty("name", actor.name());
        user.addProperty("uuid", actor.uuid() == null ? null : actor.uuid().toString());
        user.addProperty("session", actor.session().id());
        JsonArray perms = new JsonArray();
        if (actor.session().permissions().contains("*")) {
            web.relevantPermissions().forEach(perms::add);
        } else {
            actor.session().permissions().forEach(perms::add);
        }
        user.add("permissions", perms);
        o.add("user", user);
        JsonArray templates = new JsonArray();
        for (TemplateService.Template t : core.templates().all()) {
            if (!core.templates().canUse(actor, t)) {
                continue;
            }
            JsonObject to = new JsonObject();
            to.addProperty("id", t.id());
            to.addProperty("name", Text.strip(t.name()));
            to.addProperty("icon", t.icon());
            to.addProperty("reason", t.reason());
            JsonArray steps = new JsonArray();
            for (TemplateService.Step s : t.steps()) {
                JsonObject so = new JsonObject();
                so.addProperty("type", s.type().name());
                so.addProperty("typeName", core.formatter().typeName(s.type()));
                so.addProperty("durationText", core.formatter().stepDuration(s.type(), s.duration()));
                so.addProperty("ip", s.ip());
                steps.add(so);
            }
            to.add("steps", steps);
            templates.add(to);
        }
        o.add("templates", templates);
        ex.json(200, o);
    }

    private void dashboard(WebExchange ex, WebActor actor) throws IOException {
        if (!need(ex, actor, "banova.history")) {
            return;
        }
        JsonObject o = new JsonObject();
        o.addProperty("activeBans", countActive(PunishmentType.BAN));
        o.addProperty("activeMutes", countActive(PunishmentType.MUTE));
        o.addProperty("activeWarns", countActive(PunishmentType.WARN));
        long midnight = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        o.addProperty("today", core.punishmentRepo().countSince(midnight));
        o.addProperty("week", core.punishmentRepo().countSince(System.currentTimeMillis() - 7L * 86_400_000L));
        o.addProperty("pendingAppeals", core.records().countAppeals(AppealStatus.PENDING));
        o.addProperty("players", core.playerRepo().count());
        o.addProperty("online", core.platform().onlinePlayers().size());
        o.add("recent", punList(core.punishmentRepo().search(new PunishmentRepository.Filter(), 10, 0), actor));
        JsonArray appeals = new JsonArray();
        if (actor.hasPermission("banova.appeals")) {
            for (Appeal a : core.records().appeals(AppealStatus.PENDING, 5, 0)) {
                JsonObject ao = appealJson(a);
                Punishment p = core.punishments().byId(a.punishmentId());
                if (p != null) {
                    ao.add("punishment", pun(p, actor));
                }
                appeals.add(ao);
            }
        }
        o.add("appeals", appeals);
        JsonObject lock = new JsonObject();
        lock.addProperty("active", core.lockdown().active());
        lock.addProperty("reason", core.lockdown().reason());
        o.add("lockdown", lock);
        ex.json(200, o);
    }

    private long countActive(PunishmentType type) {
        PunishmentRepository.Filter f = new PunishmentRepository.Filter();
        f.type = type;
        f.activeOnly = true;
        return core.punishmentRepo().count(f);
    }

    // ------------------------------------------------------------------ punishments

    private void punishments(WebExchange ex, WebActor actor, String[] seg, String method) throws IOException {
        if (seg.length == 1) {
            if (method.equals("POST")) {
                create(ex, actor);
                return;
            }
            if (!need(ex, actor, "banova.history")) {
                return;
            }
            PunishmentRepository.Filter f = new PunishmentRepository.Filter();
            f.type = PunishmentType.parse(ex.query("type"));
            f.activeOnly = "true".equals(ex.query("active"));
            f.nameQuery = ex.query("q");
            f.actorName = ex.query("actor");
            String target = ex.query("target");
            if (target != null) {
                f.targetUuid = Text.parseUuid(target);
            }
            int page = Math.max(1, ex.queryInt("page", 1));
            long total = core.punishmentRepo().count(f);
            ex.json(200, page(punList(core.punishmentRepo().search(f, PAGE_SIZE, (page - 1) * PAGE_SIZE), actor), total, page, PAGE_SIZE));
            return;
        }
        Punishment p = core.punishments().byId(id(seg[1]));
        if (p == null) {
            ex.error(404, "not_found", null);
            return;
        }
        String action = seg.length > 2 ? seg[2] : "";
        switch (action) {
            case "" -> {
                if (!need(ex, actor, "banova.history")) {
                    return;
                }
                JsonObject o = pun(p, actor);
                JsonArray ev = new JsonArray();
                for (Evidence e : core.records().evidence(p.id)) {
                    JsonObject eo = new JsonObject();
                    eo.addProperty("id", e.id());
                    eo.addProperty("content", e.content());
                    eo.addProperty("actorName", e.actorName());
                    eo.addProperty("createdAt", e.createdAt());
                    eo.addProperty("own", actor.uuid() != null && actor.uuid().equals(e.actorUuid()));
                    ev.add(eo);
                }
                o.add("evidence", ev);
                JsonArray ap = new JsonArray();
                if (actor.hasPermission("banova.appeals")) {
                    for (Appeal a : core.records().appealsFor(p.id)) {
                        ap.add(appealJson(a));
                    }
                }
                o.add("appeals", ap);
                ex.json(200, o);
            }
            case "revoke" -> {
                if (!method.equals("POST")) {
                    ex.error(405, "method", null);
                    return;
                }
                String perm = switch (p.type) {
                    case BAN -> "banova.unban";
                    case MUTE -> "banova.unmute";
                    default -> "banova.unwarn";
                };
                if (!need(ex, actor, perm)) {
                    return;
                }
                boolean done = core.punishments().revoke(p, actor.ref(), ex.bodyString("reason"));
                if (done) {
                    actor.send(core.messages().get("revoke.success", core.formatter().placeholders(p)));
                    ex.json(200, ok(actor));
                } else {
                    actor.send(core.messages().get("revoke.not-active", "id", p.id));
                    ex.json(200, fail(actor));
                }
            }
            case "edit" -> {
                if (!method.equals("POST")) {
                    ex.error(405, "method", null);
                    return;
                }
                if (!need(ex, actor, "banova.edit")) {
                    return;
                }
                String reason = ex.bodyString("reason");
                String duration = ex.bodyString("duration");
                if (reason != null && !reason.isBlank() && !reason.equals(p.reason)) {
                    core.punishments().editReason(p, reason.trim(), actor.ref());
                }
                if (duration != null && !duration.isBlank() && p.type.isLasting()) {
                    long d = Durations.parse(duration);
                    if (d < 0) {
                        actor.send(core.messages().get("error.invalid-duration", "input", duration));
                        ex.json(200, fail(actor));
                        return;
                    }
                    long max = core.punishments().maxDuration(actor, p.type);
                    if (max >= 0 && (d == 0 || d > max)) {
                        actor.send(core.messages().get("error.limit", "max", Durations.format(max)));
                        ex.json(200, fail(actor));
                        return;
                    }
                    if (d != p.length()) {
                        core.punishments().editDuration(p, d, actor.ref());
                    }
                }
                actor.send(core.messages().get("edit.done", "id", p.id));
                ex.json(200, ok(actor));
            }
            case "evidence" -> {
                if (!method.equals("POST")) {
                    ex.error(405, "method", null);
                    return;
                }
                if (!need(ex, actor, "banova.evidence")) {
                    return;
                }
                String content = ex.bodyString("content");
                if (content == null || content.isBlank()) {
                    ex.error(400, "empty", null);
                    return;
                }
                EvidenceCommand.add(core, actor, p, content);
                ex.json(200, ok(actor));
            }
            default -> ex.error(404, "not_found", null);
        }
    }

    private void create(WebExchange ex, WebActor actor) throws IOException {
        String targetInput = ex.bodyString("target");
        if (targetInput == null || targetInput.isBlank()) {
            ex.error(400, "missing_target", null);
            return;
        }
        boolean silent = ex.bodyBool("silent");
        String templateId = ex.bodyString("template");
        boolean ipFlag = ex.bodyBool("ip");
        PlayerRecord target;
        if (ipFlag && Text.isIp(targetInput.trim())) {
            target = PlayerRecord.ipOnly(targetInput.trim());
        } else {
            target = core.players().resolve(targetInput.trim());
        }
        if (target == null) {
            actor.send(core.messages().get("error.player-not-found", "player", targetInput));
            ex.json(200, fail(actor));
            return;
        }
        if (templateId != null && !templateId.isBlank()) {
            if (!need(ex, actor, "banova.punish")) {
                return;
            }
            TemplateService.Template template = core.templates().get(templateId);
            if (template == null) {
                actor.send(core.messages().get("template.unknown", "template", templateId));
                ex.json(200, fail(actor));
                return;
            }
            PunishmentService.Outcome outcome = TemplateCommand.apply(core, actor, target, template, silent);
            JsonObject res = outcome != null && outcome.ok() ? ok(actor) : fail(actor);
            if (outcome != null && outcome.ok()) {
                res.addProperty("id", outcome.punishment().id);
            }
            ex.json(200, res);
            return;
        }
        PunishmentType type = PunishmentType.parse(ex.bodyString("type"));
        if (type == null) {
            ex.error(400, "invalid_type", null);
            return;
        }
        String durationText = ex.bodyString("duration");
        long duration = durationText == null || durationText.isBlank() ? 0 : Durations.parse(durationText);
        if (duration < 0) {
            actor.send(core.messages().get("error.invalid-duration", "input", durationText));
            ex.json(200, fail(actor));
            return;
        }
        if (type == PunishmentType.KICK && (target.uuid() == null || core.platform().player(target.uuid()) == null)) {
            actor.send(core.messages().get("error.not-online", "player", target.name()));
            ex.json(200, fail(actor));
            return;
        }
        PunishmentService.Request req = PunishmentService.Request.of(type, target, actor.ref());
        req.duration = type.isLasting() ? duration : 0;
        req.reason = ex.bodyString("reason");
        req.silent = silent;
        req.ipBased = ipFlag && (type == PunishmentType.BAN || type == PunishmentType.MUTE);
        PunishmentService.Outcome error = core.punishments().check(actor, req, false);
        if (error != null) {
            actor.send(core.messages().get(error.errorKey(), error.errorArgs()));
            ex.json(200, fail(actor));
            return;
        }
        PunishmentService.Outcome outcome = core.punishments().create(req);
        if (!outcome.ok()) {
            actor.send(core.messages().get(outcome.errorKey(), outcome.errorArgs()));
            ex.json(200, fail(actor));
            return;
        }
        actor.send(core.messages().get("punish.success", core.formatter().placeholders(outcome.punishment())));
        JsonObject res = ok(actor);
        res.addProperty("id", outcome.punishment().id);
        ex.json(200, res);
    }

    private void evidence(WebExchange ex, WebActor actor, String[] seg, String method) throws IOException {
        if (seg.length < 2 || !method.equals("DELETE")) {
            ex.error(404, "not_found", null);
            return;
        }
        boolean done = EvidenceCommand.remove(core, actor, id(seg[1]));
        ex.json(200, done ? ok(actor) : fail(actor));
    }

    // ------------------------------------------------------------------ players & notes

    private void players(WebExchange ex, WebActor actor, String[] seg, String method) throws IOException {
        if (!need(ex, actor, "banova.history")) {
            return;
        }
        if (seg.length == 1) {
            String q = ex.query("q");
            List<PlayerRecord> list = q == null || q.isBlank() ? core.playerRepo().recent(25)
                    : core.playerRepo().searchByPrefix(q.trim(), 25);
            JsonArray arr = new JsonArray();
            for (PlayerRecord p : list) {
                arr.add(playerJson(p));
            }
            JsonObject o = new JsonObject();
            o.add("items", arr);
            ex.json(200, o);
            return;
        }
        UUID uuid = Text.parseUuid(seg[1]);
        PlayerRecord player = uuid == null ? core.players().resolve(seg[1]) : core.players().byUuid(uuid);
        if (player == null && uuid != null) {
            player = new PlayerRecord(uuid, uuid.toString().substring(0, 8), null, 0, 0);
        }
        if (player == null || player.uuid() == null) {
            ex.error(404, "not_found", null);
            return;
        }
        if (seg.length > 2 && seg[2].equals("notes") && method.equals("POST")) {
            if (!need(ex, actor, "banova.notes.add")) {
                return;
            }
            String content = ex.bodyString("content");
            if (content == null || content.isBlank()) {
                ex.error(400, "empty", null);
                return;
            }
            NoteCommand.addNote(core, actor, player, Text.truncate(content.trim(), 1000));
            ex.json(200, ok(actor));
            return;
        }
        JsonObject o = new JsonObject();
        JsonObject info = playerJson(player);
        if (player.lastIp() != null) {
            info.addProperty("ip", actor.hasPermission("banova.ips") ? player.lastIp() : Text.maskIp(player.lastIp()));
        }
        o.add("player", info);
        Punishment ban = core.punishments().activeBan(player.uuid(), player.lastIp());
        Punishment mute = core.punishments().activeMute(player.uuid(), player.lastIp());
        o.add("ban", ban == null ? null : pun(ban, actor));
        o.add("mute", mute == null ? null : pun(mute, actor));
        o.addProperty("warns", core.punishments().activeWarns(player.uuid()).size());
        o.addProperty("historyTotal", core.punishmentRepo().countHistory(player.uuid(), null));
        o.add("history", punList(core.punishmentRepo().history(player.uuid(), null, 100, 0), actor));
        if (actor.hasPermission("banova.notes")) {
            JsonArray notes = new JsonArray();
            for (Note n : core.records().notes(player.uuid())) {
                JsonObject no = new JsonObject();
                no.addProperty("id", n.id());
                no.addProperty("content", n.content());
                no.addProperty("actorName", n.actorName());
                no.addProperty("createdAt", n.createdAt());
                no.addProperty("own", actor.uuid() != null && actor.uuid().equals(n.actorUuid()));
                notes.add(no);
            }
            o.add("notes", notes);
        }
        if (actor.hasPermission("banova.alts")) {
            JsonArray alts = new JsonArray();
            for (PlayerRecord alt : core.playerRepo().alts(player.uuid())) {
                JsonObject ao = playerJson(alt);
                ao.addProperty("banned", core.punishments().activeBan(alt.uuid(), null) != null);
                ao.addProperty("muted", core.punishments().activeMute(alt.uuid(), null) != null);
                alts.add(ao);
            }
            o.add("alts", alts);
        }
        if (actor.hasPermission("banova.ips")) {
            JsonArray ips = new JsonArray();
            for (PlayerRepository.IpEntry e : core.playerRepo().ips(player.uuid())) {
                JsonObject io = new JsonObject();
                io.addProperty("ip", e.ip());
                io.addProperty("firstSeen", e.firstSeen());
                io.addProperty("lastSeen", e.lastSeen());
                ips.add(io);
            }
            o.add("ips", ips);
        }
        if (actor.hasPermission("banova.punish")) {
            JsonArray templates = new JsonArray();
            for (TemplateService.Template t : core.templates().all()) {
                if (!core.templates().canUse(actor, t)) {
                    continue;
                }
                TemplateService.NextStep next = core.templates().next(t, player.uuid());
                JsonObject to = new JsonObject();
                to.addProperty("id", t.id());
                to.addProperty("name", Text.strip(t.name()));
                to.addProperty("offenses", next.offenses());
                to.addProperty("step", next.index() + 1);
                to.addProperty("steps", t.steps().size());
                to.addProperty("type", next.step().type().name());
                to.addProperty("typeName", core.formatter().typeName(next.step().type()));
                to.addProperty("durationText", core.formatter().stepDuration(next.step().type(), next.step().duration()));
                templates.add(to);
            }
            o.add("templates", templates);
        }
        ex.json(200, o);
    }

    private void notes(WebExchange ex, WebActor actor, String[] seg, String method) throws IOException {
        if (seg.length < 2 || !method.equals("DELETE")) {
            ex.error(404, "not_found", null);
            return;
        }
        boolean done = NoteCommand.deleteNote(core, actor, id(seg[1]));
        ex.json(200, done ? ok(actor) : fail(actor));
    }

    // ------------------------------------------------------------------ appeals

    private void appeals(WebExchange ex, WebActor actor, String[] seg, String method) throws IOException {
        if (!need(ex, actor, "banova.appeals")) {
            return;
        }
        if (seg.length == 1) {
            AppealStatus status = AppealService.parseStatus(ex.query("status") == null ? "pending" : ex.query("status"));
            int page = Math.max(1, ex.queryInt("page", 1));
            long total = core.records().countAppeals(status);
            JsonArray items = new JsonArray();
            for (Appeal a : core.records().appeals(status, PAGE_SIZE, (page - 1) * PAGE_SIZE)) {
                JsonObject ao = appealJson(a);
                Punishment p = core.punishments().byId(a.punishmentId());
                if (p != null) {
                    ao.add("punishment", pun(p, actor));
                }
                items.add(ao);
            }
            ex.json(200, page(items, total, page, PAGE_SIZE));
            return;
        }
        Appeal appeal = core.records().appeal(id(seg[1]));
        if (appeal == null) {
            ex.error(404, "not_found", null);
            return;
        }
        String action = seg.length > 2 ? seg[2] : "";
        if (action.isEmpty()) {
            JsonObject o = appealJson(appeal);
            Punishment p = core.punishments().byId(appeal.punishmentId());
            if (p != null) {
                JsonObject po = pun(p, actor);
                JsonArray ev = new JsonArray();
                for (Evidence e : core.records().evidence(p.id)) {
                    JsonObject eo = new JsonObject();
                    eo.addProperty("id", e.id());
                    eo.addProperty("content", e.content());
                    eo.addProperty("actorName", e.actorName());
                    eo.addProperty("createdAt", e.createdAt());
                    ev.add(eo);
                }
                po.add("evidence", ev);
                o.add("punishment", po);
                JsonArray previous = new JsonArray();
                for (Appeal other : core.records().appealsFor(p.id)) {
                    if (other.id() != appeal.id()) {
                        previous.add(appealJson(other));
                    }
                }
                o.add("previous", previous);
            }
            ex.json(200, o);
            return;
        }
        if (!method.equals("POST") || !(action.equals("accept") || action.equals("deny"))) {
            ex.error(404, "not_found", null);
            return;
        }
        boolean done = AppealsCommand.handle(core, actor, appeal, action.equals("accept"), ex.bodyString("response"));
        ex.json(200, done ? ok(actor) : fail(actor));
    }

    // ------------------------------------------------------------------ audit, sessions, lockdown

    private void audit(WebExchange ex, WebActor actor) throws IOException {
        if (!need(ex, actor, "banova.audit")) {
            return;
        }
        int page = Math.max(1, ex.queryInt("page", 1));
        String target = ex.query("target");
        List<AuditEntry> list = core.records().audit(ex.query("actor"), ex.query("action"),
                target == null ? null : Text.parseUuid(target), PAGE_SIZE + 1, (page - 1) * PAGE_SIZE);
        boolean more = list.size() > PAGE_SIZE;
        JsonArray items = new JsonArray();
        for (AuditEntry e : list.subList(0, Math.min(PAGE_SIZE, list.size()))) {
            JsonObject o = new JsonObject();
            o.addProperty("id", e.id());
            o.addProperty("createdAt", e.createdAt());
            o.addProperty("actorName", e.actorName());
            o.addProperty("actorUuid", e.actorUuid() == null ? null : e.actorUuid().toString());
            o.addProperty("source", e.source().name());
            o.addProperty("action", e.action());
            o.addProperty("targetName", e.targetName());
            o.addProperty("targetUuid", e.targetUuid() == null ? null : e.targetUuid().toString());
            o.addProperty("punishmentId", e.punishmentId());
            o.addProperty("details", e.details());
            items.add(o);
        }
        JsonObject o = new JsonObject();
        o.add("items", items);
        o.addProperty("page", page);
        o.addProperty("more", more);
        ex.json(200, o);
    }

    private void sessions(WebExchange ex, WebActor actor, String[] seg, String method) throws IOException {
        boolean admin = actor.hasPermission("banova.web.admin");
        if (seg.length > 1 && method.equals("DELETE")) {
            String target = seg[1];
            boolean allowed = admin || target.equals(actor.session().id());
            if (!allowed) {
                for (WebSession s : core.records().sessions(actor.uuid())) {
                    if (s.id().equals(target)) {
                        allowed = actor.uuid() != null;
                    }
                }
            }
            if (!allowed) {
                ex.error(403, "forbidden", null);
                return;
            }
            core.records().deleteSession(target);
            core.audit().log(actor.ref(), "WEB_LOGOUT", actor.uuid(), actor.name(), 0, "session " + target);
            ex.json(200, ok(actor));
            return;
        }
        List<WebSession> list = admin ? core.records().sessions(null)
                : actor.uuid() == null ? List.of(actor.session()) : core.records().sessions(actor.uuid());
        JsonArray items = new JsonArray();
        for (WebSession s : list) {
            JsonObject o = new JsonObject();
            o.addProperty("id", s.id());
            o.addProperty("name", s.name());
            o.addProperty("createdAt", s.createdAt());
            o.addProperty("lastUsed", s.lastUsed());
            o.addProperty("expiresAt", s.expiresAt());
            o.addProperty("remember", s.remember());
            o.addProperty("ip", s.ip());
            o.addProperty("browser", WebServer.browserName(s.userAgent()));
            o.addProperty("current", s.id().equals(actor.session().id()));
            items.add(o);
        }
        JsonObject o = new JsonObject();
        o.add("items", items);
        ex.json(200, o);
    }

    private void lockdown(WebExchange ex, WebActor actor, String method) throws IOException {
        if (method.equals("POST")) {
            if (!need(ex, actor, "banova.lockdown")) {
                return;
            }
            boolean enable = ex.bodyBool("enabled");
            if (enable) {
                String reason = ex.bodyString("reason");
                if (reason == null || reason.isBlank()) {
                    reason = core.messages().raw("lockdown.default-reason");
                }
                core.lockdown().set(reason, actor.ref());
                core.platform().broadcast(core.messages().get("lockdown.enabled", "actor", actor.name(), "reason", reason), "banova.notify");
            } else if (core.lockdown().active()) {
                core.lockdown().set(null, actor.ref());
                core.platform().broadcast(core.messages().get("lockdown.disabled", "actor", actor.name()), "banova.notify");
            }
        }
        JsonObject o = new JsonObject();
        o.addProperty("active", core.lockdown().active());
        o.addProperty("reason", core.lockdown().reason());
        ex.json(200, o);
    }

    // ------------------------------------------------------------------ public appeal page

    private void publicRoutes(WebExchange ex, String[] seg, String method) throws IOException {
        String action = seg.length > 1 ? seg[1] : "";
        switch (action) {
            case "info" -> {
                JsonObject o = new JsonObject();
                o.addProperty("server", core.serverName());
                o.addProperty("language", core.messages().code());
                o.addProperty("enabled", core.appeals().webAppealsAvailable());
                o.addProperty("minLength", core.appeals().minLength());
                ex.json(200, o);
            }
            case "lookup" -> {
                if (!method.equals("POST")) {
                    ex.error(405, "method", null);
                    return;
                }
                if (!web.limiter().allow("lookup:" + ex.ip(), 20, 60_000)) {
                    ex.error(429, "rate_limited", null);
                    return;
                }
                Punishment p = findByCode(ex.bodyString("code"));
                if (p == null || !core.appeals().webAppealsAvailable()) {
                    ex.error(404, "not_found", null);
                    return;
                }
                ex.json(200, publicView(p));
            }
            case "submit" -> {
                if (!method.equals("POST")) {
                    ex.error(405, "method", null);
                    return;
                }
                if (!core.appeals().webAppealsAvailable()) {
                    ex.error(404, "not_found", null);
                    return;
                }
                if (!web.limiter().allow("submit:" + ex.ip(), 5, 3_600_000)
                        || core.records().countAppealsFromIp(ex.ip(), System.currentTimeMillis() - 86_400_000L) >= 10) {
                    ex.error(429, "rate_limited", null);
                    return;
                }
                Punishment p = findByCode(ex.bodyString("code"));
                if (p == null) {
                    ex.error(404, "not_found", null);
                    return;
                }
                String block = core.appeals().blockReason(p);
                if (block != null) {
                    JsonObject o = new JsonObject();
                    o.addProperty("ok", false);
                    o.addProperty("message", Text.strip(core.messages().get(block, "time",
                            Durations.format(core.appeals().cooldownLeft(p)))));
                    ex.json(200, o);
                    return;
                }
                String message = ex.bodyString("message");
                String contact = ex.bodyString("contact");
                if (message == null || message.trim().length() < core.appeals().minLength()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("ok", false);
                    o.addProperty("message", Text.strip(core.messages().get("appeal.too-short", "min", core.appeals().minLength())));
                    ex.json(200, o);
                    return;
                }
                ActorRef creator = new ActorRef(p.targetUuid, p.targetName == null ? "?" : p.targetName, Source.WEB);
                core.appeals().create(p, Text.escapeColors(message), contact == null ? null : Text.escapeColors(contact), Source.WEB, ex.ip(), creator);
                JsonObject o = publicView(p);
                o.addProperty("ok", true);
                ex.json(200, o);
            }
            default -> ex.error(404, "not_found", null);
        }
    }

    private Punishment findByCode(String raw) {
        if (raw == null) {
            return null;
        }
        String code = Codes.normalizeHumanCode(raw);
        if (code.length() != 9) {
            return null;
        }
        return core.punishmentRepo().byAppealCode(code);
    }

    private JsonObject publicView(Punishment p) {
        JsonObject o = pun(p, null);
        o.remove("ip");
        o.remove("actorUuid");
        o.remove("server");
        o.remove("template");
        o.remove("templateStep");
        o.remove("revokedByName");
        String block = core.appeals().blockReason(p);
        o.addProperty("canAppeal", block == null);
        if (block != null) {
            o.addProperty("blockMessage", Text.strip(core.messages().get(block, "time", Durations.format(core.appeals().cooldownLeft(p)))));
        }
        JsonArray appeals = new JsonArray();
        for (Appeal a : core.records().appealsFor(p.id)) {
            JsonObject ao = new JsonObject();
            ao.addProperty("status", a.status().name());
            ao.addProperty("statusName", core.messages().raw("appeal.status-" + a.status().name().toLowerCase(Locale.ROOT)));
            ao.addProperty("createdAt", a.createdAt());
            ao.addProperty("handledAt", a.handledAt());
            ao.addProperty("response", a.response());
            appeals.add(ao);
        }
        o.add("appeals", appeals);
        return o;
    }
}
