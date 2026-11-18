package dev.banova.core.command;

import dev.banova.core.BanovaCore;
import dev.banova.core.model.Punishment;
import dev.banova.core.model.Records.AuditEntry;
import dev.banova.core.model.Records.Evidence;
import dev.banova.core.model.Records.WebSession;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.util.Durations;
import dev.banova.core.util.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /banova — help, menu, info, edit, audit, import, web panel login and reload.
 */
public final class AdminCommand extends Command {

    private static final int PAGE_SIZE = 10;

    public AdminCommand(BanovaCore core) {
        super(core, "banova", null, "bnv");
    }

    @Override
    public void execute(CommandActor actor, String label, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "" -> {
                if (actor.isPlayer() && core.platform().gui() != null && actor.hasPermission("banova.menu")) {
                    core.platform().gui().openMain(actor);
                } else {
                    help(actor);
                }
            }
            case "menu", "gui" -> {
                if (!require(actor, "banova.menu")) {
                    return;
                }
                if (actor.isPlayer() && core.platform().gui() != null) {
                    core.platform().gui().openMain(actor);
                } else {
                    send(actor, "error.players-only");
                }
            }
            case "reload" -> {
                if (!require(actor, "banova.reload")) {
                    return;
                }
                try {
                    core.reload();
                    core.audit().log(actor.ref(), "RELOAD", null, null, 0, null);
                    send(actor, "admin.reloaded");
                } catch (Exception e) {
                    core.platform().logger().error("Reload failed", e);
                    send(actor, "admin.reload-failed", "error", e.getMessage());
                }
            }
            case "info" -> info(actor, args);
            case "edit" -> edit(actor, args);
            case "audit", "log" -> audit(actor, args);
            case "import", "importar" -> {
                if (!require(actor, "banova.import")) {
                    return;
                }
                if (args.length < 2) {
                    send(actor, "import.usage", "sources", String.join(", ", core.importer().sources()));
                    return;
                }
                boolean confirm = args.length > 2 && (args[2].equalsIgnoreCase("confirm") || args[2].equalsIgnoreCase("confirmar"));
                core.importer().run(args[1], actor, confirm);
            }
            case "web" -> web(actor, args);
            case "version", "ver" -> send(actor, "admin.version", "version", core.platform().version(),
                    "platform", core.platform().platformName(), "storage", core.database().kind(),
                    "web", core.web().running() ? core.web().publicUrl() : "off");
            default -> help(actor);
        }
    }

    private void help(CommandActor actor) {
        send(actor, "help.header", "version", core.platform().version());
        for (String line : core.messages().raw("help.lines").split("\n")) {
            int space = line.indexOf(' ');
            String cmd = space > 0 ? line.substring(0, space) : line;
            String clean = Text.strip(cmd);
            actor.sendClickable(line, CommandActor.ClickAction.SUGGEST_COMMAND, clean + " ", core.messages().get("help.hover"));
        }
    }

    // ------------------------------------------------------------------ info / edit

    private void info(CommandActor actor, String[] args) {
        if (!require(actor, "banova.history")) {
            return;
        }
        if (args.length < 2) {
            send(actor, "error.usage", "usage", "/banova info <id>");
            return;
        }
        Punishment p = punishmentById(actor, args[1]);
        if (p == null) {
            return;
        }
        if (actor.isPlayer() && core.platform().gui() != null && args.length < 3) {
            core.platform().gui().openPunishment(actor, p);
            return;
        }
        send(actor, "info.details", core.formatter().placeholders(p));
        if (p.isRevoked()) {
            send(actor, "info.revoked", core.formatter().placeholders(p));
        }
        List<Evidence> evidence = core.records().evidence(p.id);
        if (!evidence.isEmpty()) {
            send(actor, "info.evidence", "count", evidence.size());
            for (Evidence e : evidence) {
                actor.send(core.messages().get("evidence.line", "eid", e.id(), "actor", e.actorName(),
                        "date", core.formatter().date(e.createdAt()), "text", e.content()));
            }
        }
        if (p.isInEffect()) {
            String unCommand = switch (p.type) {
                case BAN -> "/unban #";
                case MUTE -> "/unmute #";
                default -> "/unwarn #";
            };
            actor.sendClickable(core.messages().get("info.button-revoke"), CommandActor.ClickAction.SUGGEST_COMMAND,
                    unCommand + p.id + " ", core.messages().get("info.button-revoke"));
        }
        actor.sendClickable(core.messages().get("info.button-evidence"), CommandActor.ClickAction.SUGGEST_COMMAND,
                "/evidence #" + p.id + " ", core.messages().get("info.button-evidence"));
    }

    private void edit(CommandActor actor, String[] args) {
        if (!require(actor, "banova.edit")) {
            return;
        }
        if (args.length < 4) {
            send(actor, "error.usage", "usage", "/banova edit <id> <reason|duration> <value>");
            return;
        }
        Punishment p = punishmentById(actor, args[1]);
        if (p == null) {
            return;
        }
        String field = args[2].toLowerCase(Locale.ROOT);
        if (field.startsWith("reason") || field.startsWith("motivo") || field.startsWith("razon")) {
            core.punishments().editReason(p, Text.join(args, 3), actor.ref());
            send(actor, "edit.done", "id", p.id);
        } else if (field.startsWith("dur") || field.startsWith("time") || field.startsWith("tiempo")) {
            if (!p.type.isLasting()) {
                send(actor, "edit.not-lasting");
                return;
            }
            long duration = Durations.parse(args[3]);
            if (duration < 0) {
                send(actor, "error.invalid-duration", "input", args[3]);
                return;
            }
            long max = core.punishments().maxDuration(actor, p.type);
            if (max >= 0 && (duration == 0 || duration > max)) {
                send(actor, "error.limit", "max", Durations.format(max));
                return;
            }
            core.punishments().editDuration(p, duration, actor.ref());
            send(actor, "edit.done", "id", p.id);
        } else {
            send(actor, "error.usage", "usage", "/banova edit <id> <reason|duration> <value>");
        }
    }

    // ------------------------------------------------------------------ audit

    private void audit(CommandActor actor, String[] args) {
        if (!require(actor, "banova.audit")) {
            return;
        }
        if (args.length == 1 && actor.isPlayer() && core.platform().gui() != null) {
            core.platform().gui().openAudit(actor);
            return;
        }
        int page = 1;
        String who = null;
        for (int i = 1; i < args.length; i++) {
            if (args[i].matches("\\d+")) {
                page = parsePage(args[i]);
            } else {
                who = args[i];
            }
        }
        List<AuditEntry> list = core.records().audit(who, null, null, PAGE_SIZE, (page - 1) * PAGE_SIZE);
        if (list.isEmpty()) {
            send(actor, "audit.empty");
            return;
        }
        send(actor, "audit.header", "page", page);
        for (AuditEntry e : list) {
            send(actor, "audit.line", "date", core.formatter().date(e.createdAt()), "actor", e.actorName(),
                    "action", e.action(), "target", e.targetName() == null ? "" : e.targetName(),
                    "details", e.details() == null ? "" : Text.truncate(e.details(), 80), "source", e.source().name());
        }
        actor.sendClickable(core.messages().get("list.page", "page", page, "pages", "?"), CommandActor.ClickAction.RUN_COMMAND,
                "/banova audit " + (who == null ? "" : who + " ") + (page + 1), core.messages().get("list.next-page"));
    }

    // ------------------------------------------------------------------ web

    private void web(CommandActor actor, String[] args) {
        if (!require(actor, "banova.web")) {
            return;
        }
        if (!core.web().running()) {
            send(actor, "web.disabled");
            return;
        }
        String sub = args.length < 2 ? "" : args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "login", "code", "codigo" -> {
                if (args.length < 3) {
                    send(actor, "error.usage", "usage", "/banova web login <code>");
                    return;
                }
                if (core.web().approveCode(args[2], actor)) {
                    send(actor, "web.code-approved");
                } else {
                    send(actor, "web.code-invalid");
                }
            }
            case "sessions", "sesiones" -> {
                boolean all = actor.hasPermission("banova.web.admin");
                List<WebSession> sessions = core.records().sessions(all ? null : actor.uuid());
                if (!all && actor.uuid() == null) {
                    sessions = core.records().sessions(null);
                }
                if (sessions.isEmpty()) {
                    send(actor, "web.no-sessions");
                    return;
                }
                send(actor, "web.sessions-header", "count", sessions.size());
                for (WebSession s : sessions) {
                    actor.sendClickable(core.messages().get("web.session-line", "id", s.id(), "name", s.name(),
                                    "last", core.formatter().date(s.lastUsed()), "ip", s.ip() == null ? "-" : s.ip()),
                            CommandActor.ClickAction.SUGGEST_COMMAND, "/banova web logout " + s.id(), core.messages().get("web.hover-logout"));
                }
            }
            case "logout", "revoke", "cerrar" -> {
                if (args.length < 3) {
                    send(actor, "error.usage", "usage", "/banova web logout <id|me|all>");
                    return;
                }
                String target = args[2];
                if (target.equalsIgnoreCase("all") || target.equalsIgnoreCase("todas")) {
                    if (!require(actor, "banova.web.admin")) {
                        return;
                    }
                    int n = core.records().deleteAllSessions();
                    core.audit().log(actor.ref(), "WEB_LOGOUT_ALL", null, null, 0, n + " sessions");
                    send(actor, "web.logged-out", "count", n);
                } else if (target.equalsIgnoreCase("me") || target.equalsIgnoreCase("yo")) {
                    int n = core.records().deleteSessionsOf(actor.uuid());
                    send(actor, "web.logged-out", "count", n);
                } else {
                    boolean allowed = actor.hasPermission("banova.web.admin");
                    if (!allowed) {
                        for (WebSession s : core.records().sessions(actor.uuid())) {
                            if (s.id().equals(target)) {
                                allowed = true;
                                break;
                            }
                        }
                    }
                    if (!allowed) {
                        send(actor, "error.no-permission");
                        return;
                    }
                    send(actor, "web.logged-out", "count", core.records().deleteSession(target) ? 1 : 0);
                }
            }
            default -> {
                String link = core.web().createLoginLink(actor);
                send(actor, "web.info", "url", core.web().publicUrl());
                if (link != null) {
                    actor.sendClickable(core.messages().get("web.login-link"), CommandActor.ClickAction.OPEN_URL, link,
                            core.messages().get("web.login-link-hover"));
                }
            }
        }
    }

    @Override
    public List<String> complete(CommandActor actor, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("help", "info", "version"));
            if (actor.hasPermission("banova.menu")) subs.add("menu");
            if (actor.hasPermission("banova.edit")) subs.add("edit");
            if (actor.hasPermission("banova.audit")) subs.add("audit");
            if (actor.hasPermission("banova.import")) subs.add("import");
            if (actor.hasPermission("banova.web")) subs.add("web");
            if (actor.hasPermission("banova.reload")) subs.add("reload");
            return filter(subs, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && sub.equals("import") && actor.hasPermission("banova.import")) {
            return filter(core.importer().sources(), args[1]);
        }
        if (args.length == 2 && sub.equals("web") && actor.hasPermission("banova.web")) {
            return filter(List.of("login", "sessions", "logout"), args[1]);
        }
        if (args.length == 3 && sub.equals("edit")) {
            return filter(List.of("reason", "duration"), args[2]);
        }
        return Collections.emptyList();
    }
}
