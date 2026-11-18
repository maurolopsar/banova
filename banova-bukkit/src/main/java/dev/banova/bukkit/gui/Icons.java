package dev.banova.bukkit.gui;

import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.model.Punishment;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared icons for punishments.
 */
final class Icons {

    private Icons() {
    }

    static Material material(Punishment p) {
        if (!p.isInEffect() && p.type.isLasting()) {
            return p.isRevoked() ? Material.LIGHT_GRAY_CONCRETE : Material.GRAY_CONCRETE;
        }
        return material(p.type);
    }

    static Material material(PunishmentType type) {
        return switch (type) {
            case BAN -> Material.BARRIER;
            case MUTE -> Material.WRITABLE_BOOK;
            case WARN -> Material.BELL;
            case KICK -> Material.LEATHER_BOOTS;
        };
    }

    /** Icon with full details; {@code hintKey} adds a last line such as "click for details". */
    static ItemStack punishment(BanovaCore core, Punishment p, String hintKey) {
        Object[] ph = core.formatter().placeholders(p);
        List<String> lore = new ArrayList<>(core.messages().lines("gui.punishment-lore", ph));
        if (p.isRevoked()) {
            lore.addAll(core.messages().lines("gui.punishment-revoked-lore", ph));
        }
        if (hintKey != null) {
            lore.add("");
            lore.add(core.messages().get(hintKey));
        }
        return Items.item(material(p), core.messages().get("gui.punishment-name", ph), lore);
    }
}
