package dev.banova.bukkit.gui;

import dev.banova.bukkit.BanovaBukkit;
import dev.banova.bukkit.BukkitActor;
import dev.banova.bukkit.BukkitText;
import dev.banova.core.BanovaCore;
import dev.banova.core.platform.CommandActor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Base inventory menu. Data is loaded on a worker thread ({@link #load()}), items are placed
 * on the main thread ({@link #render()}).
 */
public abstract class Menu implements InventoryHolder {

    @FunctionalInterface
    public interface Click {
        void run(ClickType type);
    }

    protected final BanovaBukkit plugin;
    protected final BanovaCore core;
    protected final Player viewer;
    protected final CommandActor actor;
    protected Menu parent;
    private Inventory inventory;
    private final Map<Integer, Click> clicks = new HashMap<>();

    protected Menu(BanovaBukkit plugin, Player viewer, Menu parent) {
        this.plugin = plugin;
        this.core = plugin.core();
        this.viewer = viewer;
        this.actor = BukkitActor.of(plugin, viewer);
        this.parent = parent;
    }

    protected abstract String title();

    protected int rows() {
        return 6;
    }

    /** Runs on a worker thread before rendering; may use the database. */
    protected void load() {
    }

    /** Runs on the main thread; place items with {@link #set}. */
    protected abstract void render();

    /** Loads data and opens (or refreshes) the menu. */
    public void open() {
        core.async(() -> {
            try {
                load();
            } catch (RuntimeException e) {
                core.platform().logger().error("Could not load menu " + getClass().getSimpleName(), e);
                actor.send(core.messages().get("error.database"));
                return;
            }
            plugin.runFor(viewer, this::show);
        });
    }

    private void show() {
        if (!viewer.isOnline()) {
            return;
        }
        inventory = BukkitText.inventory(this, rows() * 9, title());
        clicks.clear();
        render();
        viewer.openInventory(inventory);
    }

    protected void set(int slot, ItemStack item, Click click) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, item);
        if (click != null) {
            clicks.put(slot, click);
        } else {
            clicks.remove(slot);
        }
    }

    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    /** The blue work area and dark bottom toolbar are shared by all inventories. */
    protected void fillEmpty() {
        int footer = inventory.getSize() - 9;
        ItemStack body = Items.item("LIGHT_BLUE_STAINED_GLASS_PANE", " ", null);
        ItemStack bar = Items.item("BLACK_STAINED_GLASS_PANE", " ", null);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, slot < footer ? body : bar);
            }
        }
    }

    /** Back button (or close when there is no parent). */
    protected void backButton(int slot) {
        if (parent != null) {
            set(slot, Items.item("ARROW", msg("gui.back"), java.util.List.of("&7" + parent.title())), type -> parent.open());
        } else {
            set(slot, Items.item("BARRIER", msg("gui.close"), null), type -> viewer.closeInventory());
        }
        if (actor.hasPermission("banova.menu")) {
            set(slot - 1, Items.item("COMPASS", msg("gui.review.home"), null),
                    type -> new MainMenu(plugin, viewer, null).open());
        }
    }

    void click(int slot, ClickType type) {
        Click click = clicks.get(slot);
        if (click != null) {
            try {
                click.run(type);
            } catch (RuntimeException e) {
                core.platform().logger().error("Menu click failed in " + getClass().getSimpleName(), e);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // ------------------------------------------------------------------ helpers

    protected String source(dev.banova.core.model.Source source) {
        return msg("gui.review.sources." + source.name());
    }

    protected String msg(String key, Object... placeholders) {
        return core.messages().get(key, placeholders);
    }

    protected void review(String action, String detail, Runnable commit) {
        plugin.runFor(viewer, () -> {
            var preview = Items.item("WRITABLE_BOOK", action, java.util.List.of("&f" + detail));
            new ConfirmMenu(plugin, viewer, this, msg("gui.review.title"), preview, false, false,
                    ignored -> commit.run()).open();
        });
    }

    /** Runs a task on a worker thread, then reopens this menu with fresh data. */
    protected void runAndRefresh(Runnable task) {
        core.async(() -> {
            task.run();
            open();
        });
    }

    /** Asks for text in chat; null-safe "-" means empty. */
    protected void prompt(String messageKey, java.util.function.Consumer<String> handler, Object... placeholders) {
        plugin.prompts().ask(viewer, msg(messageKey, placeholders), handler, this);
    }
}
