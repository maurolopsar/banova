package dev.banova.core.command;

import dev.banova.core.BanovaCore;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Punishment;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.util.Text;

/**
 * /check &lt;player&gt; — quick summary: ban, mute, warnings, notes, alts.
 */
public final class CheckCommand extends Command {

    public CheckCommand(BanovaCore core) {
        super(core, "check", "banova.check", "checkban", "checkmute", "lookup");
    }

    @Override
    public void execute(CommandActor actor, String label, String[] args) {
        if (args.length < 1) {
            usage(actor);
            return;
        }
        PlayerRecord target = player(actor, args[0]);
        if (target == null) {
            return;
        }
        if (target.uuid() == null) {
            send(actor, "error.player-not-found", "player", args[0]);
            return;
        }
        PlayerRecord known = core.players().byUuid(target.uuid());
        PlayerRecord p = known != null ? known : target;
        send(actor, "check.header", "player", p.name(), "uuid", p.uuid());
        Punishment ban = core.punishments().activeBan(p.uuid(), p.lastIp());
        Punishment mute = core.punishments().activeMute(p.uuid(), p.lastIp());
        if (ban != null) {
            sendPunishmentLine(actor, ban);
        } else {
            send(actor, "check.not-banned");
        }
        if (mute != null) {
            sendPunishmentLine(actor, mute);
        } else {
            send(actor, "check.not-muted");
        }
        int warns = core.punishments().activeWarns(p.uuid()).size();
        long total = core.punishmentRepo().countHistory(p.uuid(), null);
        long notes = actor.hasPermission("banova.notes") ? core.records().countNotes(p.uuid()) : -1;
        int alts = actor.hasPermission("banova.alts") ? core.playerRepo().alts(p.uuid()).size() : -1;
        send(actor, "check.stats", "warns", warns, "total", total, "notes", notes < 0 ? "-" : notes, "alts", alts < 0 ? "-" : alts);
        if (p.lastSeen() > 0) {
            boolean online = core.platform().player(p.uuid()) != null;
            send(actor, "check.seen", "first", core.formatter().date(p.firstSeen()),
                    "last", online ? core.messages().raw("check.online-now") : core.formatter().date(p.lastSeen()));
        }
        if (p.lastIp() != null) {
            send(actor, "check.ip", "ip", actor.hasPermission("banova.ips") ? p.lastIp() : Text.maskIp(p.lastIp()));
        }
        actor.sendClickable(core.messages().get("check.history-button"), CommandActor.ClickAction.RUN_COMMAND,
                "/history " + p.name(), core.messages().get("list.hover"));
    }
}
