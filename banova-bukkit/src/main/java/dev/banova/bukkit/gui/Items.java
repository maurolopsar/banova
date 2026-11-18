package dev.banova.bukkit.gui;

import dev.banova.bukkit.BukkitText;
import dev.banova.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Item builders for menus.
 */
public final class Items {

    private Items() {
    }

    public static Material material(String name, Material fallback) {
        if (name == null) {
            return fallback;
        }
        Material m = Material.matchMaterial(name.trim());
        return m == null || !m.isItem() || m.isAir() ? fallback : m;
    }

    public static ItemStack item(String material, String name, List<String> lore) {
        return item(material(material, Material.PAPER), name, lore);
    }

    public static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            BukkitText.name(meta, name);
            if (lore != null && !lore.isEmpty()) {
                BukkitText.lore(meta, wrapLore(lore));
            }
            try {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            } catch (RuntimeException ignored) {
                // flags changed in this server version
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack head(UUID uuid, String name, List<String> lore) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof SkullMeta skull && uuid != null) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
        }
        if (meta != null) {
            BukkitText.name(meta, name);
            if (lore != null && !lore.isEmpty()) {
                BukkitText.lore(meta, wrapLore(lore));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** Splits long lore lines, keeping the color of the line. */
    static List<String> wrapLore(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            for (String part : line.split("\n", -1)) {
                if (Text.strip(part).length() <= 42) {
                    out.add(part);
                    continue;
                }
                String color = leadingColor(part);
                out.addAll(Text.wrap(part, 48, "").stream().map(l -> l.startsWith("&") ? l : color + l).toList());
            }
        }
        return out;
    }

    private static String leadingColor(String line) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i + 1 < line.length() && line.charAt(i) == '&') {
            sb.append(line, i, i + 2);
            i += 2;
        }
        return sb.length() == 0 ? "&7" : sb.toString();
    }
}
