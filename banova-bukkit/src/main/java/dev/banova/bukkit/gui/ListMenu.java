package dev.banova.bukkit.gui;

import dev.banova.api.PunishmentType;
import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.model.Punishment;
import dev.banova.core.storage.PunishmentRepository;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Active punishments of one type, or the latest punishments when type is null.
 */
public final class ListMenu extends PagedMenu<Punishment> {

    private final PunishmentType type;

    public ListMenu(BanovaBukkit plugin, Player viewer, Menu parent, PunishmentType type) {
        super(plugin, viewer, parent);
        this.type = type;
    }

    @Override
    protected String title() {
        return type == null ? msg("gui.list.recent-title") : msg("gui.list.title", "type", core.formatter().typeName(type));
    }

    @Override
    protected List<Punishment> loadEntries() {
        PunishmentRepository.Filter f = new PunishmentRepository.Filter();
        f.type = type;
        f.activeOnly = type != null;
        return core.punishmentRepo().search(f, type == null ? 135 : 450, 0);
    }

    @Override
    protected ItemStack icon(Punishment p) {
        return Icons.punishment(core, p, "gui.click-details");
    }

    @Override
    protected void onClick(Punishment p, ClickType click) {
        new PunishmentMenu(plugin, viewer, this, p).open();
    }
}
