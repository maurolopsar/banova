package dev.banova.bukkit.gui;

import dev.banova.bukkit.BanovaBukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class InspectInventory extends Menu {
    private final ItemStack[] contents;
    private final String target;

    InspectInventory(BanovaBukkit plugin, Player viewer, Menu parent, String target, ItemStack[] contents) {
        super(plugin, viewer, parent);
        this.target = target;
        this.contents = contents;
    }

    @Override protected String title() { return msg("gui.route.inventory-title", "player", target); }
    @Override protected void render() {
        if (actor.hasPermission("banova.inspect")) {
            for (int i = 0; i < Math.min(41, contents.length); i++) {
                if (contents[i] != null) set(i, contents[i]);
            }
        }
        backButton(49);
    }
}
