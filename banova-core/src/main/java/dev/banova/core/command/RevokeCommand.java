package dev.banova.core.command;

import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Punishment;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.util.Text;

import java.util.List;
import java.util.Locale;

/**
 * /unban, /unmute and /unwarn. Accepts a player name, an IP or a punishment id (#12).
 */
public final class RevokeCommand extends Command {

    private final PunishmentType type;

    public RevokeCommand(BanovaCore core, String name, PunishmentType type, String... aliases) {
        super(core, name, "banova." + name, aliases);
        this.type = type;
    }

    @Override
    public void execute(CommandActor actor, String label, String[] args) {
        if (args.length < 1) {
            usage(actor);
            return;
        }
        String reason = Text.join(args, 1);
        String input = args[0];
        List<Punishment> targets;
        if (input.startsWith("#")) {
            Punishment p = punishmentById(actor, input);
            if (p == null) {
                return;
            }
            if (p.type != type) {
                send(actor, "error.wrong-type", "id", p.id, "type", core.formatter().typeName(p.type));
                return;
            }
            if (!p.isInEffect()) {
                send(actor, "revoke.not-active", "id", p.id);
                return;
            }
            targets = List.of(p);
        } else if (Text.isIp(input)) {
            targets = core.punishmentRepo().activeByIp(type, input);
        } else {
            PlayerRecord player = player(actor, input);
            if (player == null) {
                return;
            }
            targets = core.punishmentRepo().active(type, player.uuid(), type == PunishmentType.WARN ? null : player.lastIp());
            if (type == PunishmentType.WARN && !targets.isEmpty()) {
                targets = List.of(targets.get(0)); // newest warning only
            }
        }
        if (targets.isEmpty()) {
            send(actor, "revoke.none-" + type.name().toLowerCase(Locale.ROOT), "player", input);
            return;
        }
        int count = 0;
        for (Punishment p : targets) {
            if (core.punishments().revoke(p, actor.ref(), reason)) {
                count++;
                send(actor, "revoke.success", core.formatter().placeholders(p));
            }
        }
        if (count == 0) {
            send(actor, "revoke.none-" + type.name().toLowerCase(Locale.ROOT), "player", input);
        }
    }
}
