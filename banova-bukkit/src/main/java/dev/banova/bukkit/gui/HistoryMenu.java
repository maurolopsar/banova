package dev.banova.bukkit.gui;

import dev.banova.api.PunishmentType;
import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Punishment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Punishment history of a player with a type filter.
 */
public final class HistoryMenu extends PagedMenu<Punishment> {

    private static final PunishmentType[] FILTERS = {null, PunishmentType.BAN, PunishmentType.MUTE, PunishmentType.WARN, PunishmentType.KICK};

    private final PlayerRecord target;
    private int filter;

    public HistoryMenu(BanovaBukkit plugin, Player viewer, Menu parent, PlayerRecord target) {
        super(plugin, viewer, parent);
        this.target = target;
    }

    @Override
    protected String title() {
        return msg("gui.history.title", "player", target.name());
    }

    @Override
    protected List<Punishment> loadEntries() {
        return core.punishmentRepo().history(target.uuid(), FILTERS[filter], 450, 0);
    }

    @Override
    protected ItemStack icon(Punishment p) {
        return Icons.punishment(core, p, "gui.click-details");
    }

    @Override
    protected void onClick(Punishment p, ClickType type) {
        RevokeMenu.openEntry(plugin, viewer, this, p, type);
    }

    @Override
    protected void renderControls() {
        PunishmentType current = FILTERS[filter];
        set(47, Items.item("HOPPER", msg("gui.history.filter", "filter",
                current == null ? msg("gui.filter-all") : core.formatter().typeName(current)),
                core.messages().lines("gui.history.filter-lore")), type -> {
            filter = (filter + 1) % FILTERS.length;
            page = 0;
            open();
        });
    }
}
