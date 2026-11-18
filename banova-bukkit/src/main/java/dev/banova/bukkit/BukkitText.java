package dev.banova.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text output facade. Uses Adventure on Paper ({@link PaperText}) and legacy strings / BungeeCord chat
 * on Spigot ({@link SpigotText}). Those classes are only loaded on the platform that has their APIs.
 */
@SuppressWarnings("deprecation")
public final class BukkitText {

    public static final boolean PAPER = classExists("io.papermc.paper.event.player.AsyncChatEvent")
            && classExists("net.kyori.adventure.text.Component");

    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");

    private BukkitText() {
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** {@code &} codes (and {@code &#rrggbb}) to a legacy {@code §} string. */
    public static String legacy(String text) {
        if (text == null) {
            return "";
        }
        Matcher m = HEX.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            StringBuilder rep = new StringBuilder("§x");
            for (char c : m.group(1).toCharArray()) {
                rep.append('§').append(c);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(rep.toString()));
        }
        m.appendTail(sb);
        char[] chars = sb.toString().toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(chars[i + 1]) > -1) {
                chars[i] = '§';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    public static void send(CommandSender sender, String message) {
        for (String line : message.split("\n", -1)) {
            if (PAPER) {
                PaperText.send(sender, line);
            } else {
                sender.sendMessage(legacy(line));
            }
        }
    }

    public static void sendClickable(CommandSender sender, String text, String action, String value, String hover) {
        if (!(sender instanceof Player player)) {
            send(sender, text + ("OPEN_URL".equals(action) ? " &7" + value : ""));
            return;
        }
        if (PAPER) {
            PaperText.sendClickable(player, text, action, value, hover);
        } else {
            SpigotText.sendClickable(player, text, action, value, hover);
        }
    }

    public static void kick(Player player, String message) {
        if (PAPER) {
            PaperText.kick(player, message);
        } else {
            player.kickPlayer(legacy(message));
        }
    }

    public static void name(ItemMeta meta, String text) {
        if (PAPER) {
            PaperText.name(meta, text);
        } else {
            meta.setDisplayName(legacy(text));
        }
    }

    public static void lore(ItemMeta meta, List<String> lines) {
        if (PAPER) {
            PaperText.lore(meta, lines);
        } else {
            List<String> list = new ArrayList<>();
            for (String l : lines) {
                list.add(legacy(l));
            }
            meta.setLore(list);
        }
    }

    public static Inventory inventory(InventoryHolder holder, int size, String title) {
        if (PAPER) {
            return PaperText.inventory(holder, size, title);
        }
        String t = legacy(title);
        return Bukkit.createInventory(holder, size, t.length() > 32 ? t.substring(0, 32) : t);
    }
}
