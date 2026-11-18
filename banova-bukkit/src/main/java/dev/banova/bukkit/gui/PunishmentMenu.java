package dev.banova.bukkit.gui;

import dev.banova.bukkit.BanovaBukkit;
import dev.banova.bukkit.BukkitText;
import dev.banova.core.command.EvidenceCommand;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Punishment;
import dev.banova.core.model.Records.Appeal;
import dev.banova.core.model.Records.Evidence;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.util.Durations;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

/**
 * One punishment: details, remove, edit, evidence and appeals.
 */
public final class PunishmentMenu extends Menu {

    private static final int[] EVIDENCE_SLOTS = {37, 38, 39, 40, 41, 42, 43};

    private Punishment p;
    private List<Evidence> evidence = new ArrayList<>();
    private List<Appeal> appeals = new ArrayList<>();

    public PunishmentMenu(BanovaBukkit plugin, Player viewer, Menu parent, Punishment punishment) {
        super(plugin, viewer, parent);
        this.p = punishment;
    }

    @Override
    protected String title() {
        return msg("gui.punishment.title", "id", p.id, "type", core.formatter().typeName(p.type));
    }

    @Override
    protected void load() {
        Punishment fresh = core.punishments().byId(p.id);
        if (fresh != null) {
            p = fresh;
        }
        evidence = actor.hasPermission("banova.evidence") ? core.records().evidence(p.id) : List.of();
        appeals = actor.hasPermission("banova.appeals") ? core.records().appealsFor(p.id) : List.of();
    }

    @Override
    protected void render() {
        set(4, Icons.punishment(core, p, p.targetUuid != null ? "gui.punishment.open-player" : null), type -> {
            if (p.targetUuid != null) {
                core.async(() -> {
                    PlayerRecord record = core.players().byUuid(p.targetUuid);
                    if (record != null) {
                        new PlayerMenu(plugin, viewer, this, record).open();
                    }
                });
            }
        });
        if (RevokeMenu.allowed(this, p)) {
            set(19, Items.item("LIME_DYE", msg("gui.punishment.revoke"), null),
                    click -> new RevokeMenu(plugin, viewer, this, p).open());
        }
        if (actor.hasPermission("banova.edit")) {
            set(21, Items.item("WRITABLE_BOOK", msg("gui.punishment.edit-reason"), core.messages().lines("gui.punishment.edit-reason-lore")),
                    type -> prompt("gui.prompt.edit-reason", input -> {
                        if (input.equals("-")) { open(); return; }
                        review(msg("gui.punishment.edit-reason"), p.targetName + " · #" + p.id + "\n" + input, () -> {
                            core.commands().get("banova").execute(actor, "banova", new String[]{"edit", String.valueOf(p.id), "reason", input});
                            open();
                        });
                    }));
            if (p.type.isLasting() && !p.isRevoked()) {
                set(23, Items.item("CLOCK", msg("gui.punishment.edit-duration"), core.messages().lines("gui.punishment.edit-duration-lore")),
                        type -> prompt("gui.prompt.edit-duration", input -> review(msg("gui.punishment.edit-duration"),
                                p.targetName + " · #" + p.id + " · " + input, () -> {
                            core.commands().get("banova").execute(actor, "banova", new String[]{"edit", String.valueOf(p.id), "duration", input});
                            open();
                        })));
            }
        }
        if (actor.hasPermission("banova.evidence")) {
            set(25, Items.item("SPYGLASS", msg("gui.punishment.add-evidence"), core.messages().lines("gui.punishment.add-evidence-lore")),
                    type -> prompt("gui.prompt.evidence", input -> {
                        EvidenceCommand.add(core, actor, p, input);
                        open();
                    }));
        }
        if (!appeals.isEmpty()) {
            Appeal latest = appeals.get(0);
            set(31, Items.item("WRITABLE_BOOK", msg("gui.punishment.appeals", "count", appeals.size()),
                    core.messages().lines("gui.punishment.appeals-lore", "status",
                            core.messages().raw("appeal.status-" + latest.status().name().toLowerCase(java.util.Locale.ROOT)))),
                    type -> new AppealMenu(plugin, viewer, this, latest).open());
        }
        if (evidence.isEmpty()) {
            set(40, Items.item("GLASS_BOTTLE", msg("gui.punishment.no-evidence"), null));
        }
        for (int i = 0; i < evidence.size() && i < EVIDENCE_SLOTS.length; i++) {
            Evidence e = evidence.get(i);
            boolean link = e.content().startsWith("http://") || e.content().startsWith("https://");
            set(EVIDENCE_SLOTS[i], Items.item(link ? "PAINTING" : "PAPER", msg("gui.punishment.evidence-name", "eid", e.id()),
                    core.messages().lines("gui.punishment.evidence-lore", "text", e.content(), "actor", e.actorName(),
                            "date", core.formatter().date(e.createdAt()))), type -> evidenceClick(e, link, type));
        }
        backButton(49);
        fillEmpty();
    }

    private void evidenceClick(Evidence e, boolean link, ClickType type) {
        if (type.isShiftClick() && type.isRightClick()) {
            review(msg("gui.review.delete-evidence", "id", e.id()), p.targetName + " · #" + p.id + "\n" + e.content(), () -> {
                EvidenceCommand.remove(core, actor, e.id());
                open();
            });
            return;
        }
        viewer.closeInventory();
        if (link) {
            BukkitText.sendClickable(viewer, msg("gui.punishment.evidence-open", "text", e.content()),
                    CommandActor.ClickAction.OPEN_URL.name(), e.content(), msg("evidence.hover-open"));
        } else {
            BukkitText.send(viewer, msg("evidence.line", "eid", e.id(), "actor", e.actorName(),
                    "date", core.formatter().date(e.createdAt()), "text", e.content()));
        }
    }

    static long parse(String s) {
        return Durations.parse(s);
    }
}
