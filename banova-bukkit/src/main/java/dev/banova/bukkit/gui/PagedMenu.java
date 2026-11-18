package dev.banova.bukkit.gui;

import dev.banova.bukkit.BanovaBukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu showing a list of entries over several pages.
 */
public abstract class PagedMenu<T> extends Menu {

    private static final int COLUMNS = 7;
    private static final int PER_PAGE = 4 * COLUMNS;
    protected List<T> entries = new ArrayList<>();
    protected int page;

    protected PagedMenu(BanovaBukkit plugin, Player viewer, Menu parent) {
        super(plugin, viewer, parent);
    }

    /** Loads every entry (worker thread). */
    protected abstract List<T> loadEntries();

    protected abstract ItemStack icon(T entry);

    protected abstract void onClick(T entry, ClickType type);

    /** Extra buttons in the bottom row (slots 46, 47, 50 and 52 are free). */
    protected void renderControls() {
    }

    protected ItemStack emptyIcon() {
        return Items.item("STRUCTURE_VOID", msg("gui.empty"), null);
    }

    @Override
    protected final void load() {
        entries = loadEntries();
        int pages = pages();
        if (page >= pages) {
            page = pages - 1;
        }
    }

    private int pages() {
        return Math.max(1, (entries.size() + PER_PAGE - 1) / PER_PAGE);
    }

    @Override
    protected final void render() {
        int from = page * PER_PAGE;
        if (entries.isEmpty()) {
            set(22, emptyIcon());
        }
        for (int i = 0; i < PER_PAGE && from + i < entries.size(); i++) {
            T entry = entries.get(from + i);
            int slot = 10 + (i / COLUMNS) * 9 + i % COLUMNS;
            set(slot, icon(entry), type -> onClick(entry, type));
        }
        backButton(49);
        if (page > 0) {
            set(48, Items.item("ARROW", msg("gui.previous-page", "page", page), null), type -> {
                page--;
                open();
            });
        }
        if (page < pages() - 1) {
            set(50, Items.item("ARROW", msg("gui.next-page", "page", page + 2), null), type -> {
                page++;
                open();
            });
        }
        if (!entries.isEmpty()) {
            set(51, Items.item("PAPER", msg("gui.dossier.page", "page", page + 1, "pages", pages()),
                    List.of(msg("gui.dossier.entries", "count", entries.size()))));
        }
        renderControls();
        fillEmpty();
    }
}
