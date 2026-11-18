package dev.banova.bukkit.gui;

import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.model.Punishment;
import dev.banova.core.model.Records.Appeal;
import dev.banova.core.model.Records.AppealStatus;
import dev.banova.core.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Appeals list with a status filter.
 */
public final class AppealsMenu extends PagedMenu<AppealsMenu.Entry> {

    record Entry(Appeal appeal, Punishment punishment) {
    }

    private static final AppealStatus[] FILTERS = {AppealStatus.PENDING, AppealStatus.ACCEPTED, AppealStatus.DENIED, null};
    private int filter;
    private final java.util.UUID target;

    public AppealsMenu(BanovaBukkit plugin, Player viewer, Menu parent) {
        this(plugin, viewer, parent, null);
    }

    public AppealsMenu(BanovaBukkit plugin, Player viewer, Menu parent, java.util.UUID target) {
        super(plugin, viewer, parent);
        this.target = target;
    }

    @Override
    protected String title() {
        return msg("gui.appeals.title");
    }

    @Override
    protected List<Entry> loadEntries() {
        List<Entry> out = new ArrayList<>();
        var entries = target == null ? core.records().appeals(FILTERS[filter], 450, 0)
                : core.records().playerAppeals(target, FILTERS[filter], 450);
        for (Appeal a : entries) {
            out.add(new Entry(a, core.punishments().byId(a.punishmentId())));
        }
        return out;
    }

    @Override
    protected ItemStack icon(Entry e) {
        Appeal a = e.appeal();
        Punishment p = e.punishment();
        String material = switch (a.status()) {
            case PENDING -> "WRITABLE_BOOK";
            case ACCEPTED -> "LIME_DYE";
            case DENIED -> "GRAY_DYE";
        };
        return Items.item(material, msg("gui.appeals.name", "id", a.id(), "player", p == null ? "?" : p.targetName),
                core.messages().lines("gui.appeals.lore",
                        "type", p == null ? "?" : core.formatter().typeName(p.type),
                        "reason", p == null ? "?" : p.reason,
                        "status", core.messages().raw("appeal.status-" + a.status().name().toLowerCase(Locale.ROOT)),
                        "date", core.formatter().date(a.createdAt()),
                        "message", Text.truncate(a.message(), 120)));
    }

    @Override
    protected void onClick(Entry e, ClickType type) {
        new AppealMenu(plugin, viewer, this, e.appeal()).open();
    }

    @Override
    protected void renderControls() {
        AppealStatus current = FILTERS[filter];
        set(47, Items.item("HOPPER", msg("gui.appeals.filter", "filter", current == null ? msg("gui.filter-all")
                        : core.messages().raw("appeal.status-" + current.name().toLowerCase(Locale.ROOT))),
                core.messages().lines("gui.history.filter-lore")), type -> {
            filter = (filter + 1) % FILTERS.length;
            page = 0;
            open();
        });
    }
}
