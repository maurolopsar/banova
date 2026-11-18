package dev.banova.core.platform;

import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;

/**
 * Everything the core needs from Paper/Spigot or Velocity.
 */
public interface Platform {

    /** Human readable platform name, e.g. "Paper" or "Velocity". */
    String platformName();

    /** Plugin version. */
    String version();

    boolean isProxy();

    Path dataFolder();

    /** Server root folder (used by importers to find other plugins' data). */
    Path serverRoot();

    BanovaLogger logger();

    /** Runs a task on the main thread (or immediately on platforms without one). */
    void runSync(Runnable task);

    Collection<? extends OnlinePlayer> onlinePlayers();

    OnlinePlayer player(UUID uuid);

    OnlinePlayer player(String name);

    /** Sends a message to every online player with the permission (or everyone when null) and to the console. */
    void broadcast(String message, String permission);

    /** Whether player UUIDs come from Mojang (online mode or a forwarding proxy in online mode). */
    boolean onlineMode();

    /** Optional menu support; {@code null} on platforms without inventories. */
    default GuiBridge gui() {
        return null;
    }

    CommandActor console();
}
