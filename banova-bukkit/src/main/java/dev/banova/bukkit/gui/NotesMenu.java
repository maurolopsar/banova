package dev.banova.bukkit.gui;

import dev.banova.bukkit.BanovaBukkit;
import dev.banova.core.command.NoteCommand;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Records.Note;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Private staff notes about a player.
 */
public final class NotesMenu extends PagedMenu<Note> {

    private final PlayerRecord target;

    public NotesMenu(BanovaBukkit plugin, Player viewer, Menu parent, PlayerRecord target) {
        super(plugin, viewer, parent);
        this.target = target;
    }

    @Override
    protected String title() {
        return msg("gui.notes.title", "player", target.name());
    }

    @Override
    protected List<Note> loadEntries() {
        return core.records().notes(target.uuid());
    }

    @Override
    protected ItemStack icon(Note n) {
        return Items.item("OAK_SIGN", msg("gui.notes.name", "id", n.id(), "actor", n.actorName()),
                core.messages().lines("gui.notes.lore", "text", n.content(), "date", core.formatter().date(n.createdAt())));
    }

    @Override
    protected void onClick(Note n, ClickType type) {
        if (type.isRightClick()) {
            review(msg("gui.review.delete-note", "id", n.id()), target.name() + "\n" + n.content(), () -> {
                NoteCommand.deleteNote(core, actor, n.id());
                open();
            });
        }
    }

    @Override
    protected void renderControls() {
        if (actor.hasPermission("banova.notes.add")) {
            set(53, Items.item("WRITABLE_BOOK", msg("gui.notes.add"), core.messages().lines("gui.notes.add-lore")),
                    type -> prompt("gui.prompt.note", input -> {
                        NoteCommand.addNote(core, actor, target, input);
                        open();
                    }, "player", target.name()));
        }
    }
}
