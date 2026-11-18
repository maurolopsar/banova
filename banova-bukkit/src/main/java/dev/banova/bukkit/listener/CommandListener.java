package dev.banova.bukkit.listener;

import dev.banova.bukkit.BanovaBukkit;
import dev.banova.bukkit.BukkitText;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

/**
 * Blocks private messages and similar commands for muted players.
 */
public final class CommandListener implements Listener {

    private final BanovaBukkit plugin;

    public CommandListener(BanovaBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (plugin.core().mutes().get(event.getPlayer().getUniqueId()) == null) {
            return;
        }
        String label = event.getMessage().substring(1).split(" ", 2)[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1);
        }
        for (String blocked : plugin.core().config().getStringList("punishments.muted-blocked-commands")) {
            if (blocked.toLowerCase(Locale.ROOT).replace("/", "").trim().equals(label)) {
                event.setCancelled(true);
                BukkitText.send(event.getPlayer(), plugin.core().messages().get("mute.command-denied"));
                return;
            }
        }
    }
}
