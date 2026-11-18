package dev.banova.bukkit.gui;

import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.model.Records.AuditEntry;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Read-only staff audit log.
 */
public final class AuditMenu extends PagedMenu<AuditEntry> {

    public AuditMenu(BanovaBukkit plugin, Player viewer, Menu parent) {
        super(plugin, viewer, parent);
    }

    @Override
    protected String title() {
        return msg("gui.audit.title");
    }

    @Override
    protected List<AuditEntry> loadEntries() {
        return core.records().audit(null, null, null, 450, 0);
    }

    @Override
    protected ItemStack icon(AuditEntry e) {
        String material = switch (e.action()) {
            case "PUNISH" -> "RED_DYE";
            case "REVOKE", "APPEAL_ACCEPT" -> "LIME_DYE";
            case "APPEAL_DENY" -> "GRAY_DYE";
            case "WEB_LOGIN", "WEB_LOGOUT", "WEB_LOGOUT_ALL" -> "COMPASS";
            case "NOTE_ADD", "NOTE_DELETE" -> "MAP";
            case "EVIDENCE_ADD", "EVIDENCE_DELETE" -> "SPYGLASS";
            default -> "BOOK";
        };
        String key = "gui.review.actions." + e.action();
        String action = core.messages().has(key) ? msg(key) : e.action().replace('_', ' ');
        return Items.item(material, msg("gui.audit.name", "action", action, "actor", e.actorName()),
                core.messages().lines("gui.audit.lore", "target", e.targetName() == null ? "-" : e.targetName(),
                        "details", e.details() == null ? "-" : e.details(), "date", core.formatter().date(e.createdAt()),
                        "source", source(e.source()), "id", e.punishmentId() > 0 ? "#" + e.punishmentId() : "-"));
    }

    @Override
    protected void onClick(AuditEntry e, ClickType type) {
        if (e.punishmentId() > 0) {
            core.async(() -> {
                var p = core.punishments().byId(e.punishmentId());
                if (p != null) {
                    new PunishmentMenu(plugin, viewer, this, p).open();
                }
            });
        }
    }
}
