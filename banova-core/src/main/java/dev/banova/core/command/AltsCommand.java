package dev.banova.core.command;

import dev.banova.core.BanovaCore;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.platform.CommandActor;

import java.util.ArrayList;
import java.util.List;

/**
 * /alts &lt;player&gt; — accounts sharing an IP, with their ban/mute state.
 */
public final class AltsCommand extends Command {

    public AltsCommand(BanovaCore core) {
        super(core, "alts", "banova.alts", "dupeip", "cuentas");
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
        if (actor.isPlayer() && core.platform().gui() != null) {
            core.platform().gui().openAlts(actor, target);
            return;
        }
        List<PlayerRecord> alts = core.playerRepo().alts(target.uuid());
        if (alts.isEmpty()) {
            send(actor, "alts.none", "player", target.name());
            return;
        }
        List<String> names = new ArrayList<>();
        for (PlayerRecord alt : alts) {
            String color;
            if (core.punishments().activeBan(alt.uuid(), null) != null) {
                color = core.messages().raw("alts.color-banned");
            } else if (core.punishments().activeMute(alt.uuid(), null) != null) {
                color = core.messages().raw("alts.color-muted");
            } else if (core.platform().player(alt.uuid()) != null) {
                color = core.messages().raw("alts.color-online");
            } else {
                color = core.messages().raw("alts.color-offline");
            }
            names.add(color + alt.name());
        }
        send(actor, "alts.header", "player", target.name(), "count", alts.size());
        actor.send(String.join("&7, ", names));
        send(actor, "alts.legend");
    }
}
