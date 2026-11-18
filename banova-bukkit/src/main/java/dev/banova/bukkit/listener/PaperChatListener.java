package dev.banova.bukkit.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Chat listener for Paper (Adventure based chat event). Only registered on Paper.
 */
public final class PaperChatListener implements Listener {

    private final ChatHandler handler;

    public PaperChatListener(ChatHandler handler) {
        this.handler = handler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void record(AsyncChatEvent event) {
        if (event.viewers().containsAll(org.bukkit.Bukkit.getOnlinePlayers())) {
            handler.record(event.getPlayer(), PlainTextComponentSerializer.plainText().serialize(event.message()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        ChatHandler.Decision decision = handler.handle(event.getPlayer(), text);
        if (decision.cancel()) {
            event.setCancelled(true);
        } else if (decision.message() != null) {
            event.message(Component.text(decision.message()));
        }
    }
}
