package dev.banova.bukkit.gui;

import dev.banova.api.PunishmentType;
import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.service.PunishmentService;
import dev.banova.core.util.Durations;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Custom ban/mute/warn/kick: pick a duration, type a reason.
 */
public final class CustomMenu extends Menu {

    private static final List<String> BAN = List.of("1h", "6h", "1d", "3d", "7d", "14d", "30d", "perm");
    private static final List<String> MUTE = List.of("15m", "1h", "6h", "1d", "3d", "7d", "30d", "perm");
    private static final List<String> WARN = List.of("1d", "7d", "14d", "30d", "90d", "perm");
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 22};

    private final PlayerRecord target;
    private final PunishmentType type;
    private boolean silent;
    private boolean ip;

    public CustomMenu(BanovaBukkit plugin, Player viewer, Menu parent, PlayerRecord target, PunishmentType type) {
        super(plugin, viewer, parent);
        this.target = target;
        this.type = type;
        this.silent = core.config().getBoolean("punishments.silent-by-default", false);
    }

    @Override
    protected String title() {
        return msg("gui.custom.title", "type", core.formatter().typeName(type), "player", target.name());
    }

    @Override
    protected int rows() {
        return 4;
    }

    @Override
    protected void render() {
        if (type == PunishmentType.KICK) {
            set(13, Items.item("LEATHER_BOOTS", msg("gui.custom.kick-now"), core.messages().lines("gui.custom.reason-lore")),
                    t -> askReason(0));
        } else {
            List<String> durations = type == PunishmentType.BAN ? BAN : type == PunishmentType.MUTE ? MUTE : WARN;
            for (int i = 0; i < durations.size() && i < SLOTS.length; i++) {
                String raw = durations.get(i);
                long duration = Durations.parse(raw);
                boolean perm = duration == Durations.PERMANENT;
                set(SLOTS[i], Items.item(perm ? "BEDROCK" : "CLOCK", msg("gui.custom.duration", "duration", core.formatter().duration(duration)),
                        core.messages().lines("gui.custom.reason-lore")), t -> askReason(duration));
            }
            set(21, Items.item("NAME_TAG", msg("gui.custom.duration-custom"), core.messages().lines("gui.custom.duration-custom-lore")),
                    t -> prompt("gui.prompt.duration", input -> {
                        long d = Durations.parse(input);
                        if (d < 0) {
                            actor.send(msg("error.invalid-duration", "input", input));
                            open();
                            return;
                        }
                        askReason(d);
                    }));
            if ((type == PunishmentType.BAN || type == PunishmentType.MUTE)
                    && actor.hasPermission(PunishmentService.permissionFor(type, false, true))) {
                set(28, Items.item(ip ? "REDSTONE_TORCH" : "LEVER", msg(ip ? "gui.custom.ip-on" : "gui.custom.ip-off"),
                        core.messages().lines("gui.custom.ip-lore")), t -> {
                    ip = !ip;
                    render();
                });
            }
        }
        set(32, Items.item(silent ? "ENDER_EYE" : "ENDER_PEARL", msg(silent ? "gui.silent-on" : "gui.silent-off"),
                List.of(msg("gui.silent-lore"))), t -> {
            silent = !silent;
            render();
        });
        backButton(31);
        fillEmpty();
    }

    private void askReason(long duration) {
        prompt("gui.prompt.reason", input -> {
            String detail = target.name() + " · " + core.formatter().stepDuration(type, duration)
                    + (ip ? " · IP" : "") + (silent ? " · " + msg("gui.silent-on") : "")
                    + "\n" + (input.equals("-") ? msg("default-reason." + type.name().toLowerCase(java.util.Locale.ROOT)) : input);
            review(core.formatter().typeName(type), detail, () -> apply(duration, input));
        }, "type", core.formatter().typeName(type), "player", target.name());
    }

    private void apply(long duration, String input) {
            PunishmentService.Request req = PunishmentService.Request.of(type, target, actor.ref());
            req.duration = type.isLasting() ? duration : 0;
            req.reason = input.equals("-") ? null : input;
            req.silent = silent;
            req.ipBased = ip;
            if (type == PunishmentType.KICK && core.platform().player(target.uuid()) == null) {
                actor.send(msg("error.not-online", "player", target.name()));
                return;
            }
            PunishmentService.Outcome error = core.punishments().check(actor, req, false);
            if (error != null) {
                actor.send(msg(error.errorKey(), error.errorArgs()));
                open();
                return;
            }
            PunishmentService.Outcome outcome = core.punishments().create(req);
            if (!outcome.ok()) {
                actor.send(msg(outcome.errorKey(), outcome.errorArgs()));
                open();
                return;
            }
            actor.send(msg("punish.success", core.formatter().placeholders(outcome.punishment())));
            if (parent != null) {
                parent.open();
            }
    }
}
