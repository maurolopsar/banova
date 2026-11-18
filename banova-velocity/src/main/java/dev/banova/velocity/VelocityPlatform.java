package dev.banova.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.banova.core.platform.BanovaLogger;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.platform.OnlinePlayer;
import dev.banova.core.platform.Platform;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Velocity implementation of the core platform. There is no main thread, so tasks run directly.
 */
final class VelocityPlatform implements Platform {

    private final ProxyServer proxy;
    private final Path dataFolder;
    private final String version;
    private final BanovaLogger logger;

    VelocityPlatform(ProxyServer proxy, Path dataFolder, Logger slf4j, String version) {
        this.proxy = proxy;
        this.dataFolder = dataFolder;
        this.version = version;
        this.logger = new BanovaLogger() {
            @Override
            public void info(String message) {
                slf4j.info(message);
            }

            @Override
            public void warn(String message) {
                slf4j.warn(message);
            }

            @Override
            public void error(String message, Throwable error) {
                slf4j.error(message, error);
            }
        };
    }

    @Override
    public String platformName() {
        return "Velocity " + proxy.getVersion().getVersion();
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public boolean isProxy() {
        return true;
    }

    @Override
    public Path dataFolder() {
        return dataFolder;
    }

    @Override
    public Path serverRoot() {
        Path abs = dataFolder.toAbsolutePath();
        return abs.getParent() != null && abs.getParent().getParent() != null ? abs.getParent().getParent() : abs;
    }

    @Override
    public BanovaLogger logger() {
        return logger;
    }

    @Override
    public void runSync(Runnable task) {
        task.run();
    }

    @Override
    public Collection<? extends OnlinePlayer> onlinePlayers() {
        List<OnlinePlayer> list = new ArrayList<>();
        for (Player p : proxy.getAllPlayers()) {
            list.add(new VelocityActor.VelocityPlayer(p));
        }
        return list;
    }

    @Override
    public OnlinePlayer player(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        Optional<Player> p = proxy.getPlayer(uuid);
        return p.map(VelocityActor.VelocityPlayer::new).orElse(null);
    }

    @Override
    public OnlinePlayer player(String name) {
        return proxy.getPlayer(name).map(VelocityActor.VelocityPlayer::new).orElse(null);
    }

    @Override
    public void broadcast(String message, String permission) {
        for (Player p : proxy.getAllPlayers()) {
            if (permission == null || p.hasPermission(permission)) {
                new VelocityActor(p).send(message);
            }
        }
        new VelocityActor(proxy.getConsoleCommandSource()).send(message);
    }

    @Override
    public boolean onlineMode() {
        return proxy.getConfiguration().isOnlineMode();
    }

    @Override
    public CommandActor console() {
        return VelocityActor.of(proxy.getConsoleCommandSource());
    }
}
