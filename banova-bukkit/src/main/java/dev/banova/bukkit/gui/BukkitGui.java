package dev.banova.bukkit.gui;

import dev.banova.api.PunishmentType;
import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Punishment;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.platform.GuiBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Opens menus for commands run by players.
 */
public final class BukkitGui implements GuiBridge {

    private final BanovaBukkit plugin;

    public BukkitGui(BanovaBukkit plugin) {
        this.plugin = plugin;
    }

    private Player player(CommandActor actor) {
        return actor.uuid() == null ? null : Bukkit.getPlayer(actor.uuid());
    }

    @Override
    public void openMain(CommandActor viewer) {
        Player p = player(viewer);
        if (p != null) {
            new MainMenu(plugin, p, null).open();
        }
    }

    @Override
    public void openPlayer(CommandActor viewer, PlayerRecord target) {
        Player p = player(viewer);
        if (p != null) {
            new PlayerMenu(plugin, p, new MainMenu(plugin, p, null), target).open();
        }
    }

    @Override
    public void openHistory(CommandActor viewer, PlayerRecord target) {
        Player p = player(viewer);
        if (p != null) {
            new HistoryMenu(plugin, p, new PlayerMenu(plugin, p, null, target), target).open();
        }
    }

    @Override
    public void openNotes(CommandActor viewer, PlayerRecord target) {
        Player p = player(viewer);
        if (p != null) {
            new NotesMenu(plugin, p, new PlayerMenu(plugin, p, null, target), target).open();
        }
    }

    @Override
    public void openPunishment(CommandActor viewer, Punishment punishment) {
        Player p = player(viewer);
        if (p != null) {
            new PunishmentMenu(plugin, p, null, punishment).open();
        }
    }

    @Override
    public void openAppeals(CommandActor viewer) {
        Player p = player(viewer);
        if (p != null) {
            new AppealsMenu(plugin, p, new MainMenu(plugin, p, null)).open();
        }
    }

    @Override
    public void openList(CommandActor viewer, PunishmentType type) {
        Player p = player(viewer);
        if (p != null) {
            new ListMenu(plugin, p, new MainMenu(plugin, p, null), type).open();
        }
    }

    @Override
    public void openAudit(CommandActor viewer) {
        Player p = player(viewer);
        if (p != null) {
            new AuditMenu(plugin, p, new MainMenu(plugin, p, null)).open();
        }
    }

    @Override
    public void openAlts(CommandActor viewer, PlayerRecord target) {
        Player p = player(viewer);
        if (p != null) {
            new AltsMenu(plugin, p, new PlayerMenu(plugin, p, null, target), target).open();
        }
    }
}
