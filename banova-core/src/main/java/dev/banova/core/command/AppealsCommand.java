package dev.banova.core.command;

import dev.banova.core.BanovaCore;
import dev.banova.core.model.Punishment;
import dev.banova.core.model.Records.Appeal;
import dev.banova.core.model.Records.AppealStatus;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.util.Text;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /appeals — staff review: list, view, accept and deny appeals.
 */
public final class AppealsCommand extends Command {

    private static final int PAGE_SIZE = 8;

    public AppealsCommand(BanovaCore core) {
        super(core, "appeals", "banova.appeals", "apelaciones");
    }

    @Override
    public void execute(CommandActor actor, String label, String[] args) {
        if (args.length == 0 && actor.isPlayer() && core.platform().gui() != null) {
            core.platform().gui().openAppeals(actor);
            return;
        }
        String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "view", "ver" -> {
                Appeal a = appeal(actor, args);
                if (a != null) {
                    view(actor, a);
                }
            }
            case "accept", "aceptar" -> {
                Appeal a = appeal(actor, args);
                if (a != null) {
                    handle(core, actor, a, true, Text.join(args, 2));
                }
            }
            case "deny", "denegar", "rechazar" -> {
                Appeal a = appeal(actor, args);
                if (a != null) {
                    handle(core, actor, a, false, Text.join(args, 2));
                }
            }
            default -> list(actor, args.length > 1 ? parsePage(args[1]) : parsePage(sub));
        }
    }

    /** Accepts or denies an appeal; shared with menus and web. */
    public static boolean handle(BanovaCore core, CommandActor actor, Appeal appeal, boolean accept, String response) {
        if (appeal.status() != AppealStatus.PENDING) {
            actor.send(core.messages().get("appeals.already-handled", "id", appeal.id()));
            return false;
        }
        boolean ok = accept ? core.appeals().accept(appeal, actor.ref(), response) : core.appeals().deny(appeal, actor.ref(), response);
        if (ok) {
            actor.send(core.messages().get(accept ? "appeals.accepted" : "appeals.denied", "id", appeal.id()));
        } else {
            actor.send(core.messages().get("appeals.already-handled", "id", appeal.id()));
        }
        return ok;
    }

    private Appeal appeal(CommandActor actor, String[] args) {
        if (args.length < 2) {
            usage(actor);
            return null;
        }
        Appeal a = core.records().appeal(parseId(args[1]));
        if (a == null) {
            send(actor, "appeals.not-found", "id", args[1]);
        }
        return a;
    }

    private void list(CommandActor actor, int page) {
        long total = core.records().countAppeals(AppealStatus.PENDING);
        if (total == 0) {
            send(actor, "appeals.none-pending");
            return;
        }
        int pages = pages(total, PAGE_SIZE);
        page = Math.min(page, pages);
        send(actor, "appeals.header", "total", total);
        for (Appeal a : core.records().appeals(AppealStatus.PENDING, PAGE_SIZE, (page - 1) * PAGE_SIZE)) {
            Punishment p = core.punishments().byId(a.punishmentId());
            String line = core.messages().get("appeals.line", "id", a.id(), "player", p == null ? "?" : p.targetName,
                    "type", p == null ? "?" : core.formatter().typeName(p.type), "date", core.formatter().date(a.createdAt()),
                    "message", Text.truncate(a.message(), 40));
            actor.sendClickable(line, CommandActor.ClickAction.RUN_COMMAND, "/appeals view " + a.id(), core.messages().get("list.hover"));
        }
        sendPageFooter(actor, page, pages, "/appeals list");
    }

    private void view(CommandActor actor, Appeal a) {
        Punishment p = core.punishments().byId(a.punishmentId());
        send(actor, "appeals.view-header", "id", a.id(), "status",
                core.messages().raw("appeal.status-" + a.status().name().toLowerCase(Locale.ROOT)));
        if (p != null) {
            sendPunishmentLine(actor, p);
        }
        send(actor, "appeals.view-message", "message", a.message(), "contact", a.contact() == null ? "-" : a.contact(),
                "date", core.formatter().date(a.createdAt()));
        if (a.status() == AppealStatus.PENDING) {
            actor.sendClickable(core.messages().get("appeals.button-accept"), CommandActor.ClickAction.SUGGEST_COMMAND,
                    "/appeals accept " + a.id() + " ", core.messages().get("appeals.button-accept"));
            actor.sendClickable(core.messages().get("appeals.button-deny"), CommandActor.ClickAction.SUGGEST_COMMAND,
                    "/appeals deny " + a.id() + " ", core.messages().get("appeals.button-deny"));
        } else {
            send(actor, "appeals.view-handled", "actor", a.handledByName(), "date", core.formatter().date(a.handledAt()),
                    "response", a.response() == null || a.response().isBlank() ? "-" : a.response());
        }
    }

    @Override
    public List<String> complete(CommandActor actor, String[] args) {
        if (args.length == 1) {
            return filter(List.of("list", "view", "accept", "deny"), args[0]);
        }
        return Collections.emptyList();
    }
}
