package dev.banova.core.command;

import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.platform.OnlinePlayer;
import dev.banova.core.service.PunishmentService;
import dev.banova.core.service.PunishmentService.Outcome;
import dev.banova.core.service.PunishmentService.Request;
import dev.banova.core.util.Durations;
import dev.banova.core.util.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /ban, /tempban, /ipban, /mute, /tempmute, /ipmute, /warn and /kick.
 * Syntax: {@code /<cmd> <player|ip> [duration] [reason...] [-s|-p]}
 */
public final class PunishCommand extends Command {

    public enum Mode {
        NORMAL,
        TEMP,
        IP
    }

    private static final List<String> DURATIONS = List.of("30m", "1h", "6h", "1d", "3d", "7d", "30d", "perm");

    private final PunishmentType type;
    private final Mode mode;

    public PunishCommand(BanovaCore core, String name, PunishmentType type, Mode mode, String... aliases) {
        super(core, name, null, aliases);
        this.type = type;
        this.mode = mode;
    }

    /** Parsed arguments shared with menus and the web panel. */
    public record Parsed(String target, long duration, boolean hasDuration, String reason, Boolean silent) {
    }

    /** Splits flags (-s silent, -p public) from the rest and reads an optional duration as 2nd argument. */
    public static Parsed parse(String[] args, boolean allowDuration) {
        Boolean silent = null;
        List<String> rest = new ArrayList<>();
        for (String a : args) {
            if (a.equalsIgnoreCase("-s")) {
                silent = Boolean.TRUE;
            } else if (a.equalsIgnoreCase("-p")) {
                silent = Boolean.FALSE;
            } else {
                rest.add(a);
            }
        }
        if (rest.isEmpty()) {
            return null;
        }
        String target = rest.get(0);
        long duration = Durations.PERMANENT;
        boolean hasDuration = false;
        int reasonStart = 1;
        if (allowDuration && rest.size() > 1) {
            long parsed = Durations.parse(rest.get(1));
            if (parsed >= 0) {
                duration = parsed;
                hasDuration = true;
                reasonStart = 2;
            }
        }
        String reason = Text.join(rest.toArray(new String[0]), reasonStart);
        return new Parsed(target, duration, hasDuration, reason, silent);
    }

    @Override
    public void execute(CommandActor actor, String label, String[] args) {
        boolean allowDuration = type.isLasting();
        Parsed parsed = parse(args, allowDuration);
        if (parsed == null || (mode == Mode.TEMP && (!parsed.hasDuration() || parsed.duration() == 0))) {
            usage(actor);
            return;
        }
        boolean ip = mode == Mode.IP;
        PlayerRecord target;
        if (ip && Text.isIp(parsed.target())) {
            target = PlayerRecord.ipOnly(parsed.target());
        } else {
            target = player(actor, parsed.target());
            if (target == null) {
                return;
            }
        }
        if (type == PunishmentType.KICK) {
            OnlinePlayer online = target.uuid() == null ? null : core.platform().player(target.uuid());
            if (online == null) {
                send(actor, "error.not-online", "player", target.name());
                return;
            }
        }
        Request req = Request.of(type, target, actor.ref());
        req.ipBased = ip;
        req.duration = parsed.duration();
        req.reason = parsed.reason();
        req.silent = parsed.silent() != null ? parsed.silent() : core.config().getBoolean("punishments.silent-by-default", false);

        Outcome error = core.punishments().check(actor, req, false);
        if (error != null) {
            send(actor, error.errorKey(), error.errorArgs());
            return;
        }
        Outcome outcome = core.punishments().create(req);
        if (!outcome.ok()) {
            send(actor, outcome.errorKey(), outcome.errorArgs());
            return;
        }
        send(actor, "punish.success", core.formatter().placeholders(outcome.punishment()));
        if (!outcome.replaced().isEmpty()) {
            send(actor, "punish.replaced", "id", outcome.replaced().get(0).id);
        }
    }

    @Override
    public List<String> complete(CommandActor actor, String[] args) {
        if (!PunishmentService.canCreate(actor, type, mode == Mode.TEMP, mode == Mode.IP) && !actor.hasPermission("banova." + name())) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return onlineNames(args[0]);
        }
        if (args.length == 2 && type.isLasting()) {
            return filter(mode == Mode.TEMP ? DURATIONS.subList(0, DURATIONS.size() - 1) : DURATIONS, args[1]);
        }
        if (args.length >= 2) {
            return filter(List.of("-s", "-p"), args[args.length - 1].startsWith("-") ? args[args.length - 1] : "~");
        }
        return Collections.emptyList();
    }
}
