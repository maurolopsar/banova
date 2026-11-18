package dev.banova.bukkit;

import dev.banova.bukkit.gui.BukkitGui;
import dev.banova.bukkit.gui.ChatPrompts;
import dev.banova.bukkit.gui.MenuListener;
import dev.banova.bukkit.listener.ChatHandler;
import dev.banova.bukkit.listener.CommandListener;
import dev.banova.bukkit.listener.ConnectionListener;
import dev.banova.bukkit.listener.PaperChatListener;
import dev.banova.bukkit.listener.SpigotChatListener;
import dev.banova.core.BanovaCore;
import dev.banova.core.command.Command;
import dev.banova.core.platform.CommandActor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

/**
 * Paper / Spigot entry point.
 */
public final class BanovaBukkit extends JavaPlugin {

    private BanovaCore core;
    private BukkitGui gui;
    private ChatPrompts prompts;
    private final dev.banova.bukkit.listener.RecentChat recentChat = new dev.banova.bukkit.listener.RecentChat();

    public dev.banova.bukkit.listener.RecentChat recentChat() { return recentChat; }

    @Override
    public void onEnable() {
        gui = new BukkitGui(this);
        prompts = new ChatPrompts(this);
        core = new BanovaCore(new BukkitPlatform(this));
        try {
            core.enable();
        } catch (Exception e) {
            getLogger().severe("==============================================================");
            getLogger().severe(" Banova could not start: " + e.getMessage());
            getLogger().severe(" Check the 'storage' section of plugins/Banova/config.yml");
            getLogger().severe("==============================================================");
            getLogger().log(java.util.logging.Level.WARNING, "Startup error details", e);
            core.disable();
            core = null;
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        registerCommands();
        ChatHandler chat = new ChatHandler(this);
        Bukkit.getPluginManager().registerEvents(new ConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CommandListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);
        if (BukkitText.PAPER) {
            Bukkit.getPluginManager().registerEvents(new PaperChatListener(chat), this);
        } else {
            Bukkit.getPluginManager().registerEvents(new SpigotChatListener(chat), this);
        }
        // Players already online after /reload: load their mutes.
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            String ip = p.getAddress() == null ? null : p.getAddress().getAddress().getHostAddress();
            java.util.UUID uuid = p.getUniqueId();
            String name = p.getName();
            core.async(() -> core.players().handleLogin(uuid, name, ip));
        }
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.disable();
        }
    }

    private void registerCommands() {
        for (Command command : core.commands().all()) {
            PluginCommand pc = getCommand(command.name());
            if (pc == null) {
                getLogger().warning("Command /" + command.name() + " is missing from plugin.yml");
                continue;
            }
            pc.setExecutor((sender, cmd, label, args) -> {
                // RCON only returns output written while the command runs, so run it right away there.
                boolean rcon = sender instanceof org.bukkit.command.RemoteConsoleCommandSender;
                core.commands().dispatch(actor(sender), command, label, args, rcon);
                return true;
            });
            pc.setTabCompleter((sender, cmd, label, args) -> {
                List<String> out = core.commands().complete(actor(sender), command, args);
                return out == null ? Collections.emptyList() : out;
            });
        }
    }

    private CommandActor actor(CommandSender sender) {
        return BukkitActor.of(this, sender);
    }

    // ------------------------------------------------------------------ helpers used by other classes

    public BanovaCore core() {
        return core;
    }

    public BukkitGui gui() {
        return gui;
    }

    public ChatPrompts prompts() {
        return prompts;
    }

    private static final boolean FOLIA = detectFolia();

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public void runFor(org.bukkit.entity.Player player, Runnable task) {
        if (!isEnabled()) return;
        if (FOLIA) player.getScheduler().run(this, scheduled -> task.run(), null);
        else runSync(task);
    }

    public void runSync(Runnable task) {
        if (!isEnabled()) return;
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().execute(this, task);
        } else if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(this, task);
        }
    }

    @SuppressWarnings("deprecation")
    public void disallow(AsyncPlayerPreLoginEvent event, AsyncPlayerPreLoginEvent.Result result, String message) {
        if (BukkitText.PAPER) {
            PaperText.disallow(event, result, message);
        } else {
            event.disallow(result, BukkitText.legacy(message));
        }
    }

    @SuppressWarnings("deprecation")
    public void disallow(PlayerLoginEvent event, PlayerLoginEvent.Result result, String message) {
        if (BukkitText.PAPER) {
            PaperText.disallow(event, result, message);
        } else {
            event.disallow(result, BukkitText.legacy(message));
        }
    }
}
