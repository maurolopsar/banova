package dev.banova.bukkit;

import dev.banova.core.platform.BanovaLogger;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.platform.GuiBridge;
import dev.banova.core.platform.OnlinePlayer;
import dev.banova.core.platform.Platform;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Paper / Spigot implementation of the core platform.
 */
final class BukkitPlatform implements Platform {

    private final BanovaBukkit plugin;
    private final BanovaLogger logger;
    private final Boolean onlineMode;

    BukkitPlatform(BanovaBukkit plugin) {
        this.plugin = plugin;
        this.logger = BanovaLogger.ofJul(plugin.getLogger());
        this.onlineMode = detectOnlineMode();
    }

    @Override
    public String platformName() {
        return (BukkitText.PAPER ? "Paper " : "Spigot ") + Bukkit.getBukkitVersion();
    }

    @Override
    @SuppressWarnings("deprecation")
    public String version() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public Path dataFolder() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public Path serverRoot() {
        return Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
    }

    @Override
    public BanovaLogger logger() {
        return logger;
    }

    @Override
    public void runSync(Runnable task) {
        plugin.runSync(task);
    }

    @Override
    public Collection<? extends OnlinePlayer> onlinePlayers() {
        List<OnlinePlayer> list = new ArrayList<>();
        for (Player p : snapshotPlayers()) {
            list.add(new BukkitActor.BukkitPlayer(plugin, p));
        }
        return list;
    }

    /** Copy of the online player list that is safe to take from async threads. */
    static List<Player> snapshotPlayers() {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                return new ArrayList<>(Bukkit.getOnlinePlayers());
            } catch (java.util.ConcurrentModificationException ignored) {
                // player list changed while copying, try again
            }
        }
        return new ArrayList<>();
    }

    @Override
    public OnlinePlayer player(UUID uuid) {
        Player p = uuid == null ? null : Bukkit.getPlayer(uuid);
        return p == null ? null : new BukkitActor.BukkitPlayer(plugin, p);
    }

    @Override
    public OnlinePlayer player(String name) {
        Player p = Bukkit.getPlayerExact(name);
        return p == null ? null : new BukkitActor.BukkitPlayer(plugin, p);
    }

    @Override
    public void broadcast(String message, String permission) {
        runSync(() -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (permission == null || p.hasPermission(permission)) {
                    BukkitText.send(p, message);
                }
            }
            BukkitText.send(Bukkit.getConsoleSender(), message);
        });
    }

    @Override
    public boolean onlineMode() {
        return onlineMode;
    }

    @Override
    public GuiBridge gui() {
        return plugin.gui();
    }

    @Override
    public CommandActor console() {
        return BukkitActor.of(plugin, Bukkit.getConsoleSender());
    }

    /** Online mode, or behind a BungeeCord/Velocity proxy that is in online mode. */
    private static boolean detectOnlineMode() {
        if (Bukkit.getOnlineMode()) {
            return true;
        }
        try {
            File paperGlobal = new File("config", "paper-global.yml");
            if (paperGlobal.exists()) {
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(paperGlobal);
                if (yml.getBoolean("proxies.velocity.enabled", false)) {
                    return yml.getBoolean("proxies.velocity.online-mode", true);
                }
                if (yml.getBoolean("proxies.bungee-cord.online-mode", true) && Bukkit.spigot().getConfig().getBoolean("settings.bungeecord", false)) {
                    return true;
                }
            }
            return Bukkit.spigot().getConfig().getBoolean("settings.bungeecord", false);
        } catch (Throwable t) {
            return false;
        }
    }
}
