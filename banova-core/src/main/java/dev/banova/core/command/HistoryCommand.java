package dev.banova.core.command;

import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Punishment;
import dev.banova.core.platform.CommandActor;

import java.util.Collections;
import java.util.List;

/**
 * /history &lt;player&gt; [page] [type]
 */
public final class HistoryCommand extends Command {

    private static final int PAGE_SIZE = 8;

    public HistoryCommand(BanovaCore core) {
        super(core, "history", "banova.history", "hist", "historial");
    }

    @Override
    public void execute(CommandActor actor, String label, String[] args) {
        if (args.length < 1) {
            usage(actor);
            return;
        }
        PlayerRecord target = player(actor, args[0]);
        if (target == null || target.uuid() == null) {
            return;
        }
        boolean forceText = args.length > 1;
        if (!forceText && actor.isPlayer() && core.platform().gui() != null) {
            core.platform().gui().openHistory(actor, target);
            return;
        }
        int page = args.length > 1 ? parsePage(args[1]) : 1;
        PunishmentType type = args.length > 2 ? PunishmentType.parse(args[2]) : null;
        long total = core.punishmentRepo().countHistory(target.uuid(), type);
        if (total == 0) {
            send(actor, "history.empty", "player", target.name());
            return;
        }
        int pages = pages(total, PAGE_SIZE);
        page = Math.min(page, pages);
        send(actor, "history.header", "player", target.name(), "total", total);
        List<Punishment> list = core.punishmentRepo().history(target.uuid(), type, PAGE_SIZE, (page - 1) * PAGE_SIZE);
        for (Punishment p : list) {
            sendPunishmentLine(actor, p);
        }
        sendPageFooter(actor, page, pages, "/history " + target.name());
    }

    @Override
    public List<String> complete(CommandActor actor, String[] args) {
        if (args.length == 1) {
            return onlineNames(args[0]);
        }
        if (args.length == 3) {
            return filter(List.of("ban", "mute", "warn", "kick"), args[2]);
        }
        return Collections.emptyList();
    }
}
