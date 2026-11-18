package dev.banova.bukkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Adventure based text for Paper. Only loaded when running on Paper.
 */
final class PaperText {

    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.builder().character('&').hexColors().build();

    private PaperText() {
    }

    static Component component(String text) {
        return AMP.deserialize(text == null ? "" : text);
    }

    /** Item names/lore are italic by default; a non-italic parent keeps them straight (works on old Adventure too). */
    private static Component item(String text) {
        return Component.text().decoration(TextDecoration.ITALIC, false).append(component(text)).build();
    }

    static void send(CommandSender sender, String line) {
        sender.sendMessage(component(line));
    }

    static void sendClickable(Player player, String text, String action, String value, String hover) {
        TextComponent.Builder builder = Component.text().append(component(text));
        ClickEvent click = switch (action) {
            case "RUN_COMMAND" -> ClickEvent.runCommand(value);
            case "OPEN_URL" -> ClickEvent.openUrl(value);
            case "COPY" -> ClickEvent.copyToClipboard(value);
            default -> ClickEvent.suggestCommand(value);
        };
        builder.clickEvent(click);
        if (hover != null && !hover.isEmpty()) {
            builder.hoverEvent(HoverEvent.showText(component(hover)));
        }
        player.sendMessage(builder.build());
    }

    static void kick(Player player, String message) {
        player.kick(component(message));
    }

    static void disallow(AsyncPlayerPreLoginEvent event, AsyncPlayerPreLoginEvent.Result result, String message) {
        event.disallow(result, component(message));
    }

    static void disallow(PlayerLoginEvent event, PlayerLoginEvent.Result result, String message) {
        event.disallow(result, component(message));
    }

    static void name(ItemMeta meta, String text) {
        meta.displayName(item(text));
    }

    static void lore(ItemMeta meta, List<String> lines) {
        List<Component> list = new ArrayList<>();
        for (String l : lines) {
            list.add(item(l));
        }
        meta.lore(list);
    }

    static Inventory inventory(InventoryHolder holder, int size, String title) {
        return Bukkit.createInventory(holder, size, component(title));
    }

    static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
