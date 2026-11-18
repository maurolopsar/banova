package dev.banova.bukkit.gui;

import dev.banova.bukkit.BanovaBukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/**
 * Yes/no confirmation with an optional "silent" toggle.
 */
public final class ConfirmMenu extends Menu {

    private final String title;
    private final ItemStack preview;
    private final boolean showSilent;
    private final Consumer<Boolean> onConfirm;
    private boolean silent;
    private boolean submitted;

    public ConfirmMenu(BanovaBukkit plugin, Player viewer, Menu parent, String title, ItemStack preview,
                       boolean showSilent, boolean silentDefault, Consumer<Boolean> onConfirm) {
        super(plugin, viewer, parent);
        this.title = title;
        this.preview = preview;
        this.showSilent = showSilent;
        this.silent = silentDefault;
        this.onConfirm = onConfirm;
    }

    @Override
    protected String title() {
        return title;
    }

    @Override
    protected int rows() {
        return 3;
    }

    @Override
    protected void render() {
        set(13, preview);
        set(11, Items.item("LIME_CONCRETE", msg("gui.confirm.yes"), List.of(msg("gui.review.commit"))), type -> {
            if (submitted) return;
            submitted = true;
            viewer.closeInventory();
            core.async(() -> onConfirm.accept(silent));
        });
        set(15, Items.item("RED_CONCRETE", msg("gui.confirm.no"), null), type -> {
            if (parent != null) {
                parent.open();
            } else {
                viewer.closeInventory();
            }
        });
        if (showSilent) {
            set(22, Items.item(silent ? "ENDER_EYE" : "ENDER_PEARL", msg(silent ? "gui.silent-on" : "gui.silent-off"),
                    List.of(msg("gui.silent-lore"))), type -> {
                silent = !silent;
                render();
            });
        }
        fillEmpty();
    }
}
