package dev.banova.core.command;

import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.model.Punishment;
import dev.banova.core.model.Records.Appeal;
import dev.banova.core.model.Source;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.platform.OnlinePlayer;
import dev.banova.core.util.Durations;
import dev.banova.core.util.Text;

import java.util.Collections;
import java.util.List;

/**
 * /appeal [message] — players appeal their active mute or warning from inside the game.
 * Banned players use the web appeal page shown on their ban screen.
 */
public final class AppealCommand extends Command {

    public AppealCommand(BanovaCore core) {
        super(core, "appeal", "banova.appeal", "apelar");
    }

    @Override
    public void execute(CommandActor actor, String label, String[] args) {
        if (!actor.isPlayer()) {
            send(actor, "error.players-only");
            return;
        }
        if (!core.appeals().enabled()) {
            send(actor, "appeal.disabled");
            return;
        }
        OnlinePlayer online = core.platform().player(actor.uuid());
        String ip = online == null ? null : online.ip();
        Punishment target = core.punishments().activeMute(actor.uuid(), ip);
        if (target == null || !core.appeals().isAppealable(PunishmentType.MUTE)) {
            List<Punishment> warns = core.punishments().activeWarns(actor.uuid());
            target = warns.isEmpty() || !core.appeals().isAppealable(PunishmentType.WARN) ? null : warns.get(0);
        }
        if (args.length == 1 && (args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("estado"))) {
            showStatus(actor);
            return;
        }
        if (target == null) {
            send(actor, "appeal.nothing");
            return;
        }
        String block = core.appeals().blockReason(target);
        if (block != null) {
            send(actor, block, "time", Durations.format(core.appeals().cooldownLeft(target)));
            return;
        }
        if (args.length == 0) {
            send(actor, "appeal.how", "type", core.formatter().typeName(target.type), "reason", target.reason,
                    "min", core.appeals().minLength());
            return;
        }
        String message = Text.escapeColors(Text.join(args, 0));
        if (message.length() < core.appeals().minLength()) {
            send(actor, "appeal.too-short", "min", core.appeals().minLength());
            return;
        }
        long id = core.appeals().create(target, message, null, Source.GAME, ip, actor.ref());
        send(actor, "appeal.sent", "id", id);
    }

    private void showStatus(CommandActor actor) {
        boolean any = false;
        for (Punishment p : core.punishmentRepo().history(actor.uuid(), null, 10, 0)) {
            for (Appeal a : core.records().appealsFor(p.id)) {
                any = true;
                send(actor, "appeal.status-line", "id", a.id(), "type", core.formatter().typeName(p.type),
                        "status", core.messages().raw("appeal.status-" + a.status().name().toLowerCase(java.util.Locale.ROOT)),
                        "response", a.response() == null || a.response().isBlank() ? "-" : a.response());
            }
        }
        if (!any) {
            send(actor, "appeal.status-none");
        }
    }

    @Override
    public List<String> complete(CommandActor actor, String[] args) {
        if (args.length == 1) {
            return filter(List.of("status"), args[0]);
        }
        return Collections.emptyList();
    }
}
