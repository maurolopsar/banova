package dev.banova.bukkit.gui;

import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.command.AppealsCommand;
import dev.banova.core.model.Punishment;
import dev.banova.core.model.Records.Appeal;
import dev.banova.core.model.Records.AppealStatus;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * One appeal: message, punishment and accept/deny buttons.
 */
public final class AppealMenu extends Menu {

    private Appeal appeal;
    private Punishment punishment;

    public AppealMenu(BanovaBukkit plugin, Player viewer, Menu parent, Appeal appeal) {
        super(plugin, viewer, parent);
        this.appeal = appeal;
    }

    @Override
    protected String title() {
        return msg("gui.appeal.title", "id", appeal.id());
    }

    @Override
    protected int rows() {
        return 5;
    }

    @Override
    protected void load() {
        Appeal fresh = core.records().appeal(appeal.id());
        if (fresh != null) {
            appeal = fresh;
        }
        punishment = core.punishments().byId(appeal.punishmentId());
    }

    @Override
    protected void render() {
        if (punishment != null) {
            set(11, Icons.punishment(core, punishment, "gui.click-details"),
                    type -> new PunishmentMenu(plugin, viewer, this, punishment).open());
        }
        set(15, Items.item("WRITABLE_BOOK", msg("gui.appeal.message"), core.messages().lines("gui.appeal.message-lore",
                "message", appeal.message(), "contact", appeal.contact() == null ? "-" : appeal.contact(),
                "date", core.formatter().date(appeal.createdAt()), "source", source(appeal.source()))));
        if (appeal.status() == AppealStatus.PENDING) {
            set(29, Items.item("LIME_CONCRETE", msg("gui.appeal.accept"), core.messages().lines("gui.appeal.accept-lore")),
                    type -> prompt("gui.prompt.appeal-response", input -> review(msg("gui.appeal.accept"),
                            "#" + appeal.id() + " · " + (punishment == null ? "—" : punishment.targetName) + "\n" + input, () -> {
                        AppealsCommand.handle(core, actor, appeal, true, input.equals("-") ? "" : input);
                        open();
                    })));
            set(33, Items.item("RED_CONCRETE", msg("gui.appeal.deny"), core.messages().lines("gui.appeal.deny-lore")),
                    type -> prompt("gui.prompt.appeal-response", input -> review(msg("gui.appeal.deny"),
                            "#" + appeal.id() + " · " + (punishment == null ? "—" : punishment.targetName) + "\n" + input, () -> {
                        AppealsCommand.handle(core, actor, appeal, false, input.equals("-") ? "" : input);
                        open();
                    })));
        } else {
            set(31, Items.item(appeal.status() == AppealStatus.ACCEPTED ? "LIME_DYE" : "GRAY_DYE",
                    msg("gui.appeal.handled", "status", core.messages().raw("appeal.status-" + appeal.status().name().toLowerCase(Locale.ROOT))),
                    core.messages().lines("gui.appeal.handled-lore", "actor", appeal.handledByName(),
                            "date", core.formatter().date(appeal.handledAt()),
                            "response", appeal.response() == null || appeal.response().isBlank() ? "-" : appeal.response())));
        }
        backButton(40);
        fillEmpty();
    }
}
