package dev.banova.bukkit.listener;

import dev.banova.bukkit.BanovaBukkit;
import dev.banova.bukkit.BukkitActor;
import dev.banova.bukkit.BukkitText;
import dev.banova.core.filter.ChatFilter;
import dev.banova.core.model.Punishment;
import org.bukkit.entity.Player;

/**
 * Shared chat logic for the Paper and Spigot chat events: menu prompts, mutes and the chat filter.
 */
public final class ChatHandler {

    /** What to do with a chat message. {@code message} is null when unchanged. */
    public record Decision(boolean cancel, String message) {
    }

    private static final Decision ALLOW = new Decision(false, null);
    private static final Decision CANCEL = new Decision(true, null);

    private final BanovaBukkit plugin;

    public ChatHandler(BanovaBukkit plugin) {
        this.plugin = plugin;
    }

    public void record(Player player, String text) {
        if (plugin.core().config().getBoolean("chat.recent-register", true))
            plugin.recentChat().record(player.getUniqueId(), text);
    }

    public Decision handle(Player player, String message) {
        if (plugin.prompts().handle(player, message)) {
            return CANCEL;
        }
        Punishment mute = plugin.core().mutes().get(player.getUniqueId());
        if (mute != null) {
            BukkitText.send(player, plugin.core().messages().get("mute.chat-denied", plugin.core().formatter().placeholders(mute)));
            return CANCEL;
        }
        ChatFilter.Result result = plugin.core().filter().check((BukkitActor.BukkitPlayer) BukkitActor.of(plugin, player), message);
        return switch (result.action()) {
            case BLOCK -> CANCEL;
            case MODIFY -> new Decision(false, result.message());
            default -> ALLOW;
        };
    }
}
