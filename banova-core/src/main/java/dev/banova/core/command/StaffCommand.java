package dev.banova.core.command;

import dev.banova.core.BanovaCore;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Punishment;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.util.Durations;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * /staffhistory &lt;staff&gt; [page] and /staffrollback &lt;staff&gt; &lt;time&gt; [confirm].
 */
public final class StaffCommand extends Command {

    private static final int PAGE_SIZE = 8;
    private final boolean rollback;

    public StaffCommand(BanovaCore core, boolean rollback) {
        super(core, rollback ? "staffrollback" : "staffhistory",
                rollback ? "banova.staffrollback" : "banova.staffhistory",
                rollback ? new String[]{"rollbackstaff"} : new String[]{"shistory"});
        this.rollback = rollback;
    }

    @Override
    public void execute(CommandActor actor, String label, String[] args) {
        if (args.length < (rollback ? 2 : 1)) {
            usage(actor);
            return;
        }
        UUID staffUuid;
        String staffName;
        if (args[0].equalsIgnoreCase("console") || args[0].equalsIgnoreCase("consola")) {
            staffUuid = null;
            staffName = "Console";
        } else {
            PlayerRecord staff = player(actor, args[0]);
            if (staff == null) {
                return;
            }
            staffUuid = staff.uuid();
            staffName = staff.name();
        }
        if (rollback) {
            long window = Durations.parse(args[1]);
            if (window <= 0) {
                send(actor, "error.invalid-duration", "input", args[1]);
                return;
            }
            long since = System.currentTimeMillis() - window;
            List<Punishment> list = core.punishmentRepo().byActor(staffUuid, staffName, since, 5000, 0);
            long revocable = list.stream().filter(p -> p.isInEffect()).count();
            boolean confirmed = args.length > 2 && (args[2].equalsIgnoreCase("confirm") || args[2].equalsIgnoreCase("confirmar"));
            if (!confirmed) {
                send(actor, "staff.rollback-confirm", "staff", staffName, "count", revocable, "time", Durations.format(window));
                actor.sendClickable(core.messages().get("staff.rollback-button"), CommandActor.ClickAction.RUN_COMMAND,
                        "/staffrollback " + args[0] + " " + args[1] + " confirm", core.messages().get("staff.rollback-button"));
                return;
            }
            int done = core.punishments().rollback(staffUuid, staffName, since, actor.ref());
            send(actor, "staff.rollback-done", "staff", staffName, "count", done);
            return;
        }
        int page = args.length > 1 ? parsePage(args[1]) : 1;
        long total = core.punishmentRepo().countByActor(staffUuid, staffName);
        if (total == 0) {
            send(actor, "staff.history-empty", "staff", staffName);
            return;
        }
        int pages = pages(total, PAGE_SIZE);
        page = Math.min(page, pages);
        send(actor, "staff.history-header", "staff", staffName, "total", total);
        for (Punishment p : core.punishmentRepo().byActor(staffUuid, staffName, 0, PAGE_SIZE, (page - 1) * PAGE_SIZE)) {
            sendPunishmentLine(actor, p);
        }
        sendPageFooter(actor, page, pages, "/staffhistory " + args[0]);
    }

    @Override
    public List<String> complete(CommandActor actor, String[] args) {
        if (args.length == 1) {
            return onlineNames(args[0]);
        }
        if (rollback && args.length == 2) {
            return filter(List.of("1h", "6h", "1d", "7d"), args[1]);
        }
        return Collections.emptyList();
    }
}
