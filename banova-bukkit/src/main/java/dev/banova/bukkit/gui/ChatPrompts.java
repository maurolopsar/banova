package dev.banova.bukkit.gui;

import dev.banova.bukkit.BanovaBukkit;
import dev.banova.bukkit.BukkitText;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Asks a player to type something in chat (reason, duration, note...) while using a menu.
 */
public final class ChatPrompts {

    private static final long TIMEOUT = 120_000;

    private record Prompt(Consumer<String> handler, Menu returnTo, long createdAt) {
    }

    private final BanovaBukkit plugin;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public ChatPrompts(BanovaBukkit plugin) {
        this.plugin = plugin;
    }

    public void ask(Player player, String message, Consumer<String> handler, Menu returnTo) {
        prompts.put(player.getUniqueId(), new Prompt(handler, returnTo, System.currentTimeMillis()));
        plugin.runFor(player, () -> {
            player.closeInventory();
            BukkitText.send(player, message);
            BukkitText.send(player, plugin.core().messages().get("gui.prompt-cancel-hint"));
        });
    }

    /**
     * Called from the chat thread. Returns true when the message answered a prompt.
     */
    public boolean handle(Player player, String message) {
        Prompt prompt = prompts.remove(player.getUniqueId());
        if (prompt == null) {
            return false;
        }
        if (System.currentTimeMillis() - prompt.createdAt() > TIMEOUT) {
            return false;
        }
        String text = message.trim();
        if (text.equalsIgnoreCase("cancel") || text.equalsIgnoreCase("cancelar")) {
            BukkitText.send(player, plugin.core().messages().get("gui.prompt-cancelled"));
            if (prompt.returnTo() != null) {
                prompt.returnTo().open();
            }
            return true;
        }
        plugin.core().async(() -> {
            try {
                prompt.handler().accept(text);
            } catch (RuntimeException e) {
                plugin.core().platform().logger().error("Menu input failed", e);
            }
        });
        return true;
    }

    public void cancel(UUID player) {
        prompts.remove(player);
    }
}
