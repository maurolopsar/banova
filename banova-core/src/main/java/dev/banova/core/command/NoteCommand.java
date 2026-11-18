package dev.banova.core.command;

import dev.banova.core.BanovaCore;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Records.Note;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.util.Text;

import java.util.Collections;
import java.util.List;

/**
 * /note &lt;player&gt; &lt;text&gt; adds a private staff note.
 * /notes &lt;player&gt; lists them; /notes delete &lt;id&gt; removes one.
 */
public final class NoteCommand extends Command {

    private final boolean list;

    public NoteCommand(BanovaCore core, boolean list) {
        super(core, list ? "notes" : "note", list ? "banova.notes" : "banova.notes.add", list ? new String[]{"notas"} : new String[]{"nota"});
        this.list = list;
    }

    @Override
    public void execute(CommandActor actor, String label, String[] args) {
        if (list) {
            executeList(actor, args);
            return;
        }
        if (args.length < 2) {
            usage(actor);
            return;
        }
        PlayerRecord target = player(actor, args[0]);
        if (target == null || target.uuid() == null) {
            return;
        }
        String text = Text.join(args, 1).trim();
        if (text.length() > 1000) {
            text = text.substring(0, 1000);
        }
        addNote(core, actor, target, text);
    }

    /** Shared with menus and the web panel. */
    public static long addNote(BanovaCore core, CommandActor actor, PlayerRecord target, String text) {
        long id = core.records().addNote(target.uuid(), text, actor.ref());
        core.audit().log(actor.ref(), "NOTE_ADD", target.uuid(), target.name(), 0, "#" + id + " " + text);
        actor.send(core.messages().get("notes.added", "player", target.name(), "id", id));
        return id;
    }

    public static boolean deleteNote(BanovaCore core, CommandActor actor, long id) {
        Note note = core.records().note(id);
        if (note == null) {
            actor.send(core.messages().get("notes.not-found", "id", id));
            return false;
        }
        boolean own = actor.uuid() != null && actor.uuid().equals(note.actorUuid());
        if (!own && !actor.hasPermission("banova.notes.delete")) {
            actor.send(core.messages().get("error.no-permission"));
            return false;
        }
        core.records().deleteNote(id);
        PlayerRecord target = core.players().byUuid(note.targetUuid());
        core.audit().log(actor.ref(), "NOTE_DELETE", note.targetUuid(), target == null ? null : target.name(), 0, "#" + id + " " + note.content());
        actor.send(core.messages().get("notes.deleted", "id", id));
        return true;
    }

    private void executeList(CommandActor actor, String[] args) {
        if (args.length < 1) {
            usage(actor);
            return;
        }
        if ((args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("borrar")) && args.length > 1) {
            deleteNote(core, actor, parseId(args[1]));
            return;
        }
        PlayerRecord target = player(actor, args[0]);
        if (target == null || target.uuid() == null) {
            return;
        }
        if (actor.isPlayer() && core.platform().gui() != null) {
            core.platform().gui().openNotes(actor, target);
            return;
        }
        List<Note> notes = core.records().notes(target.uuid());
        if (notes.isEmpty()) {
            send(actor, "notes.empty", "player", target.name());
            return;
        }
        send(actor, "notes.header", "player", target.name(), "count", notes.size());
        for (Note n : notes) {
            actor.sendClickable(core.messages().get("notes.line", "id", n.id(), "date", core.formatter().date(n.createdAt()),
                            "actor", n.actorName(), "text", n.content()),
                    CommandActor.ClickAction.SUGGEST_COMMAND, "/notes delete " + n.id(), core.messages().get("notes.hover-delete"));
        }
    }

    @Override
    public List<String> complete(CommandActor actor, String[] args) {
        if (args.length == 1) {
            List<String> names = onlineNames(args[0]);
            if (list) {
                names.addAll(filter(List.of("delete"), args[0]));
            }
            return names;
        }
        return Collections.emptyList();
    }
}
