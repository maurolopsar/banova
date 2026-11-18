package dev.banova.bukkit.gui;

import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.platform.OnlinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Online players; click one to open their menu.
 */
public final class OnlinePlayersMenu extends PagedMenu<OnlinePlayer> {

    public OnlinePlayersMenu(BanovaBukkit plugin, Player viewer, Menu parent) {
        super(plugin, viewer, parent);
    }

    @Override
    protected String title() {
        return msg("gui.players.title");
    }

    @Override
    protected List<OnlinePlayer> loadEntries() {
        List<OnlinePlayer> list = new ArrayList<>(core.platform().onlinePlayers());
        list.sort(Comparator.comparing(p -> p.name().toLowerCase(java.util.Locale.ROOT)));
        return list;
    }

    @Override
    protected ItemStack icon(OnlinePlayer p) {
        boolean muted = core.mutes().get(p.uuid()) != null;
        var lore = new ArrayList<>(core.messages().lines(muted ? "gui.players.lore-muted" : "gui.players.lore"));
        if (actor.hasPermission("banova.history")) lore.add(msg("gui.players.history-shortcut"));
        return Items.head(p.uuid(), "&e" + p.name(), lore);
    }

    @Override
    protected void onClick(OnlinePlayer p, ClickType type) {
        core.async(() -> {
            PlayerRecord record = core.players().byUuid(p.uuid());
            if (record == null) {
                record = new PlayerRecord(p.uuid(), p.name(), p.ip(), System.currentTimeMillis(), System.currentTimeMillis());
            }
            if (type.isRightClick() && actor.hasPermission("banova.history")) {
                new HistoryMenu(plugin, viewer, this, record).open();
            } else {
                new PlayerMenu(plugin, viewer, this, record).open();
            }
        });
    }
}
