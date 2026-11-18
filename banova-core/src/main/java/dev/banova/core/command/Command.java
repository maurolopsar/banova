package dev.banova.core.command;

import dev.banova.core.BanovaCore;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Punishment;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.platform.OnlinePlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Base class for Banova commands. {@link #execute} runs on a worker thread, so it may use the database.
 */
public abstract class Command {

    protected final BanovaCore core;
    private final String name;
    private final List<String> aliases;
    private final String permission;

    protected Command(BanovaCore core, String name, String permission, String... aliases) {
        this.core = core;
        this.name = name;
        this.permission = permission;
        this.aliases = List.of(aliases);
    }

    public String name() {
        return name;
    }

    public List<String> aliases() {
        return aliases;
    }

    /** Base permission, checked before {@link #execute}. May be null. */
    public String permission() {
        return permission;
    }

    public abstract void execute(CommandActor actor, String label, String[] args);

    /** Tab completion; runs on the calling thread and must not touch the database. */
    public List<String> complete(CommandActor actor, String[] args) {
        if (args.length == 1) {
            return onlineNames(args[0]);
        }
        return Collections.emptyList();
    }

    // ------------------------------------------------------------------ helpers

    protected void send(CommandActor actor, String key, Object... placeholders) {
        actor.send(core.messages().get(key, placeholders));
    }

    protected void usage(CommandActor actor) {
        send(actor, "error.usage", "usage", core.messages().raw("usage." + name));
    }

    protected boolean require(CommandActor actor, String perm) {
        if (perm == null || actor.hasPermission(perm)) {
            return true;
        }
        send(actor, "error.no-permission");
        return false;
    }

    /** Resolves a player and sends an error when not found. */
    protected PlayerRecord player(CommandActor actor, String input) {
        PlayerRecord record = core.players().resolve(input);
        if (record == null) {
            send(actor, "error.player-not-found", "player", input);
        }
        return record;
    }

    /** Parses {@code #12} or {@code 12} into a punishment and sends an error when missing. */
    protected Punishment punishmentById(CommandActor actor, String input) {
        long id = parseId(input);
        Punishment p = id > 0 ? core.punishments().byId(id) : null;
        if (p == null) {
            send(actor, "error.punishment-not-found", "id", input);
        }
        return p;
    }

    protected static long parseId(String input) {
        if (input == null) {
            return -1;
        }
        String s = input.startsWith("#") ? input.substring(1) : input;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    protected static int parsePage(String input) {
        try {
            return Math.max(1, Integer.parseInt(input));
        } catch (Exception e) {
            return 1;
        }
    }

    protected List<String> onlineNames(String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (OnlinePlayer player : core.platform().onlinePlayers()) {
            if (player.name().toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(player.name());
            }
        }
        return out;
    }

    protected static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }

    /** One line summary of a punishment with a click to open details. */
    protected void sendPunishmentLine(CommandActor actor, Punishment p) {
        String line = core.messages().get("list.line", core.formatter().placeholders(p));
        actor.sendClickable(line, CommandActor.ClickAction.RUN_COMMAND, "/banova info " + p.id,
                core.messages().get("list.hover"));
    }

    protected void sendPageFooter(CommandActor actor, int page, int pages, String command) {
        if (pages <= 1) {
            return;
        }
        String text = core.messages().get("list.page", "page", page, "pages", pages);
        if (page < pages) {
            actor.sendClickable(text, CommandActor.ClickAction.RUN_COMMAND, command + " " + (page + 1), core.messages().get("list.next-page"));
        } else {
            actor.send(text);
        }
    }

    protected static int pages(long total, int size) {
        return (int) Math.max(1, (total + size - 1) / size);
    }
}
