package dev.banova.core.command;

import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.storage.StorageException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Holds every command and runs them off the main thread.
 */
public final class CommandManager {

    private final BanovaCore core;
    private final Map<String, Command> commands = new LinkedHashMap<>();
    private final Map<String, Command> byLabel = new LinkedHashMap<>();

    public CommandManager(BanovaCore core) {
        this.core = core;
        add(new PunishCommand(core, "ban", PunishmentType.BAN, PunishCommand.Mode.NORMAL));
        add(new PunishCommand(core, "tempban", PunishmentType.BAN, PunishCommand.Mode.TEMP));
        add(new PunishCommand(core, "ipban", PunishmentType.BAN, PunishCommand.Mode.IP, "banip"));
        add(new PunishCommand(core, "mute", PunishmentType.MUTE, PunishCommand.Mode.NORMAL));
        add(new PunishCommand(core, "tempmute", PunishmentType.MUTE, PunishCommand.Mode.TEMP));
        add(new PunishCommand(core, "ipmute", PunishmentType.MUTE, PunishCommand.Mode.IP, "muteip"));
        add(new PunishCommand(core, "warn", PunishmentType.WARN, PunishCommand.Mode.NORMAL));
        add(new PunishCommand(core, "kick", PunishmentType.KICK, PunishCommand.Mode.NORMAL));
        add(new RevokeCommand(core, "unban", PunishmentType.BAN, "pardon", "unbanip"));
        add(new RevokeCommand(core, "unmute", PunishmentType.MUTE));
        add(new RevokeCommand(core, "unwarn", PunishmentType.WARN, "delwarn"));
        add(new TemplateCommand(core));
        add(new HistoryCommand(core));
        add(new CheckCommand(core));
        add(new ListCommand(core, "banlist", PunishmentType.BAN));
        add(new ListCommand(core, "mutelist", PunishmentType.MUTE));
        add(new ListCommand(core, "warnings", PunishmentType.WARN, "warns"));
        add(new AltsCommand(core));
        add(new StaffCommand(core, false));
        add(new StaffCommand(core, true));
        add(new NoteCommand(core, false));
        add(new NoteCommand(core, true));
        add(new EvidenceCommand(core));
        add(new AppealCommand(core));
        add(new AppealsCommand(core));
        add(new LockdownCommand(core));
        add(new AdminCommand(core));
    }

    private void add(Command command) {
        commands.put(command.name(), command);
        byLabel.put(command.name(), command);
        for (String alias : command.aliases()) {
            byLabel.putIfAbsent(alias, command);
        }
    }

    public Collection<Command> all() {
        return Collections.unmodifiableCollection(commands.values());
    }

    public Command get(String label) {
        return byLabel.get(label.toLowerCase(Locale.ROOT));
    }

    /** Runs a command asynchronously. */
    public void dispatch(CommandActor actor, Command command, String label, String[] args) {
        dispatch(actor, command, label, args, false);
    }

    /**
     * Runs a command. {@code blocking} runs it on the calling thread, used for senders that only read
     * the output produced while the command runs (RCON).
     */
    public void dispatch(CommandActor actor, Command command, String label, String[] args, boolean blocking) {
        if (command.permission() != null && !actor.hasPermission(command.permission())) {
            actor.send(core.messages().get("error.no-permission"));
            return;
        }
        Runnable task = () -> {
            try {
                command.execute(actor, label, args);
            } catch (StorageException e) {
                core.platform().logger().error("Database error running /" + label, e);
                actor.send(core.messages().get("error.database"));
            } catch (Throwable t) {
                core.platform().logger().error("Error running /" + label + " " + String.join(" ", args), t);
                actor.send(core.messages().get("error.internal"));
            }
        };
        if (blocking) {
            task.run();
        } else {
            core.async(task);
        }
    }

    public List<String> complete(CommandActor actor, Command command, String[] args) {
        if (command.permission() != null && !actor.hasPermission(command.permission())) {
            return Collections.emptyList();
        }
        try {
            return new ArrayList<>(command.complete(actor, args));
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }
}
