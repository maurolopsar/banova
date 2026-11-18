package dev.banova.bukkit;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

/**
 * Clickable messages on Spigot through the BungeeCord chat API. Only loaded on Spigot.
 */
final class SpigotText {

    private SpigotText() {
    }

    static void sendClickable(Player player, String text, String action, String value, String hover) {
        BaseComponent[] parts = TextComponent.fromLegacyText(BukkitText.legacy(text));
        ClickEvent.Action a = switch (action) {
            case "RUN_COMMAND" -> ClickEvent.Action.RUN_COMMAND;
            case "OPEN_URL" -> ClickEvent.Action.OPEN_URL;
            default -> ClickEvent.Action.SUGGEST_COMMAND;
        };
        for (BaseComponent part : parts) {
            part.setClickEvent(new ClickEvent(a, value));
            if (hover != null && !hover.isEmpty()) {
                part.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(BukkitText.legacy(hover))));
            }
        }
        player.spigot().sendMessage(parts);
    }
}
