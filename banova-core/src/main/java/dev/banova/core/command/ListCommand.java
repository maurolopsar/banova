package dev.banova.core.command;

import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.model.Punishment;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.storage.PunishmentRepository;

import java.util.Collections;
import java.util.List;

/**
 * /banlist, /mutelist, /warnings — active punishments of one type.
 */
public final class ListCommand extends Command {

    private static final int PAGE_SIZE = 8;
    private final PunishmentType type;

    public ListCommand(BanovaCore core, String name, PunishmentType type, String... aliases) {
        super(core, name, "banova.list", aliases);
        this.type = type;
    }

    @Override
    public void execute(CommandActor actor, String label, String[] args) {
        if (args.length == 0 && actor.isPlayer() && core.platform().gui() != null) {
            core.platform().gui().openList(actor, type);
            return;
        }
        int page = args.length > 0 ? parsePage(args[0]) : 1;
        PunishmentRepository.Filter filter = new PunishmentRepository.Filter();
        filter.type = type;
        filter.activeOnly = true;
        long total = core.punishmentRepo().count(filter);
        if (total == 0) {
            send(actor, "list.empty");
            return;
        }
        int pages = pages(total, PAGE_SIZE);
        page = Math.min(page, pages);
        send(actor, "list.header", "type", core.formatter().typeName(type), "total", total);
        for (Punishment p : core.punishmentRepo().search(filter, PAGE_SIZE, (page - 1) * PAGE_SIZE)) {
            sendPunishmentLine(actor, p);
        }
        sendPageFooter(actor, page, pages, "/" + name());
    }

    @Override
    public List<String> complete(CommandActor actor, String[] args) {
        return Collections.emptyList();
    }
}
