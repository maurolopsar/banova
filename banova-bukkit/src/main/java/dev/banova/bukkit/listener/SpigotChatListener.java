package dev.banova.bukkit.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Chat listener for Spigot (and old Paper versions).
 */
@SuppressWarnings("deprecation")
public final class SpigotChatListener implements Listener {

    private final ChatHandler handler;

    public SpigotChatListener(ChatHandler handler) {
        this.handler = handler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void record(AsyncPlayerChatEvent event) {
        if (event.getRecipients().containsAll(org.bukkit.Bukkit.getOnlinePlayers())) {
            handler.record(event.getPlayer(), event.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        ChatHandler.Decision decision = handler.handle(event.getPlayer(), event.getMessage());
        if (decision.cancel()) {
            event.setCancelled(true);
        } else if (decision.message() != null) {
            event.setMessage(decision.message());
        }
    }
}
