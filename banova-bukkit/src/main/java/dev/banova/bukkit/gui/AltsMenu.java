package dev.banova.bukkit.gui;

import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.model.PlayerRecord;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Accounts that share an IP with a player.
 */
public final class AltsMenu extends PagedMenu<AltsMenu.Alt> {

    record Alt(PlayerRecord record, boolean banned, boolean muted) {
    }

    private final PlayerRecord target;

    public AltsMenu(BanovaBukkit plugin, Player viewer, Menu parent, PlayerRecord target) {
        super(plugin, viewer, parent);
        this.target = target;
    }

    @Override
    protected String title() {
        return msg("gui.alts.title", "player", target.name());
    }

    @Override
    protected List<Alt> loadEntries() {
        List<Alt> out = new ArrayList<>();
        for (PlayerRecord r : core.playerRepo().alts(target.uuid())) {
            out.add(new Alt(r, core.punishments().activeBan(r.uuid(), null) != null, core.punishments().activeMute(r.uuid(), null) != null));
        }
        return out;
    }

    @Override
    protected ItemStack icon(Alt alt) {
        boolean online = core.platform().player(alt.record().uuid()) != null;
        String status = alt.banned() ? msg("gui.alts.banned") : alt.muted() ? msg("gui.alts.muted")
                : online ? msg("gui.alts.online") : msg("gui.alts.offline");
        return Items.head(alt.record().uuid(), "&e" + alt.record().name(), core.messages().lines("gui.alts.lore",
                "status", status, "last", core.formatter().date(alt.record().lastSeen())));
    }

    @Override
    protected void onClick(Alt alt, ClickType type) {
        new PlayerMenu(plugin, viewer, this, alt.record()).open();
    }
}
