package dev.banova.bukkit.gui;

import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.model.Punishment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

final class RevokeMenu extends Menu {
    private final Punishment punishment;

    RevokeMenu(BanovaBukkit plugin, Player viewer, Menu parent, Punishment punishment) {
        super(plugin, viewer, parent);
        this.punishment = punishment;
    }

    static boolean allowed(Menu menu, Punishment p) {
        String permission = switch (p.type) {
            case BAN -> "banova.unban";
            case MUTE -> "banova.unmute";
            default -> "banova.unwarn";
        };
        return p.type.isLasting() && !p.isRevoked() && menu.actor.hasPermission(permission);
    }

    static void openEntry(BanovaBukkit plugin, Player viewer, Menu parent, Punishment p, ClickType click) {
        if (click.isRightClick() && allowed(parent, p)) new RevokeMenu(plugin, viewer, parent, p).open();
        else if (click.isLeftClick()) new PunishmentMenu(plugin, viewer, parent, p).open();
    }

    @Override protected String title() { return msg("gui.route.revoke", "id", punishment.id); }
    @Override protected int rows() { return 4; }

    @Override protected void render() {
        set(4, Icons.punishment(core, punishment, null));
        int slot = 10;
        for (String key : List.of("mistake", "appeal", "duplicate")) {
            String reason = msg("gui.route." + key);
            set(slot, Items.item("PAPER", "&f" + reason, null), click -> confirmReason(reason));
            slot += 2;
        }
        set(16, Items.item("NAME_TAG", msg("gui.route.other"), null), click ->
                prompt("gui.prompt.revoke-reason", input -> confirmReason(input.equals("-") ? "" : input)));
        backButton(31);
        fillEmpty();
    }

    private void confirmReason(String reason) {
        review(title(), punishment.targetName + "\n" + reason, () -> {
            if (allowed(this, punishment) && core.punishments().revoke(punishment, actor.ref(), reason))
                actor.send(msg("revoke.success", core.formatter().placeholders(punishment)));
            parent.open();
        });
    }
}
