package dev.banova.core.command;

import dev.banova.core.BanovaCore;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.util.Text;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /lockdown [on [reason] | off] — only staff with banova.lockdown.bypass can join.
 */
public final class LockdownCommand extends Command {

    public LockdownCommand(BanovaCore core) {
        super(core, "lockdown", "banova.lockdown", "cierre");
    }

    @Override
    public void execute(CommandActor actor, String label, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "on", "start", "activar" -> {
                String reason = Text.join(args, 1);
                if (reason.isBlank()) {
                    reason = core.messages().raw("lockdown.default-reason");
                }
                core.lockdown().set(reason, actor.ref());
                core.platform().broadcast(core.messages().get("lockdown.enabled", "actor", actor.name(), "reason", reason), "banova.notify");
                if (needsDirectReply(actor)) {
                    send(actor, "lockdown.enabled", "actor", actor.name(), "reason", reason);
                }
            }
            case "off", "end", "stop", "desactivar" -> {
                if (!core.lockdown().active()) {
                    send(actor, "lockdown.not-active");
                    return;
                }
                core.lockdown().set(null, actor.ref());
                core.platform().broadcast(core.messages().get("lockdown.disabled", "actor", actor.name()), "banova.notify");
                if (needsDirectReply(actor)) {
                    send(actor, "lockdown.disabled", "actor", actor.name());
                }
            }
            default -> {
                if (core.lockdown().active()) {
                    send(actor, "lockdown.status-on", "reason", core.lockdown().reason());
                } else {
                    send(actor, "lockdown.status-off");
                }
            }
        }
    }

    private static boolean needsDirectReply(CommandActor actor) {
        return actor.source() == dev.banova.core.model.Source.WEB || (actor.isPlayer() && !actor.hasPermission("banova.notify"));
    }

    @Override
    public List<String> complete(CommandActor actor, String[] args) {
        if (args.length == 1) {
            return filter(List.of("on", "off"), args[0]);
        }
        return Collections.emptyList();
    }
}
