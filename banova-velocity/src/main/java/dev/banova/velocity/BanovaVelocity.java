package dev.banova.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.banova.core.BanovaCore;
import dev.banova.core.command.Command;
import dev.banova.core.platform.CommandActor;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Velocity entry point. On a proxy Banova blocks banned players before they reach any server,
 * disconnects them network-wide when banned, enforces lockdowns and can host the web panel.
 * Mutes, menus and staff commands run on the backend servers (install Banova there too).
 */
public final class BanovaVelocity {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private BanovaCore core;

    @Inject
    public BanovaVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        String version = proxy.getPluginManager().fromInstance(this)
                .map(PluginContainer::getDescription)
                .flatMap(d -> d.getVersion())
                .orElse("unknown");
        core = new BanovaCore(new VelocityPlatform(proxy, dataDirectory, logger, version));
        try {
            core.enable();
        } catch (Exception e) {
            logger.error("==============================================================");
            logger.error(" Banova could not start: {}", e.getMessage());
            logger.error(" Check the 'storage' section of plugins/banova/config.yml");
            logger.error("==============================================================", e);
            core.disable();
            core = null;
            return;
        }
        if (!core.database().isShared()) {
            logger.warn("Banova on Velocity is using local H2 storage. Set storage.type: mysql (the same database as your"
                    + " backend servers) so bans are shared across the network.");
        }
        registerCommand();
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (core != null) {
            core.disable();
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public EventTask onLogin(LoginEvent event) {
        if (core == null || !event.getResult().isAllowed()) {
            return null;
        }
        Player player = event.getPlayer();
        return EventTask.async(() -> {
            String ip = player.getRemoteAddress() == null || player.getRemoteAddress().getAddress() == null
                    ? null : player.getRemoteAddress().getAddress().getHostAddress();
            try {
                String screen = core.players().handleLogin(player.getUniqueId(), player.getUsername(), ip);
                if (screen != null) {
                    event.setResult(ResultedEvent.ComponentResult.denied(VelocityText.of(screen)));
                    return;
                }
            } catch (RuntimeException e) {
                logger.error("Ban check failed for {}, letting them join", player.getUsername(), e);
            }
            if (core.lockdown().active() && !player.hasPermission("banova.lockdown.bypass")) {
                event.setResult(ResultedEvent.ComponentResult.denied(VelocityText.of(core.lockdown().screen())));
            }
        });
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (core != null) {
            core.players().handleQuit(event.getPlayer().getUniqueId());
        }
    }

    private void registerCommand() {
        Command admin = core.commands().get("banova");
        CommandMeta meta = proxy.getCommandManager().metaBuilder("banovaproxy").aliases("bproxy").plugin(this).build();
        proxy.getCommandManager().register(meta, new SimpleCommand() {
            @Override
            public void execute(Invocation invocation) {
                CommandActor actor = VelocityActor.of(invocation.source());
                core.commands().dispatch(actor, admin, invocation.alias(), invocation.arguments());
            }

            @Override
            public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
                CommandActor actor = VelocityActor.of(invocation.source());
                String[] args = invocation.arguments().length == 0 ? new String[]{""} : invocation.arguments();
                return CompletableFuture.completedFuture(core.commands().complete(actor, admin, args));
            }

            @Override
            public boolean hasPermission(Invocation invocation) {
                CommandActor actor = VelocityActor.of(invocation.source());
                return actor.isConsole() || actor.hasPermission("banova.reload") || actor.hasPermission("banova.web")
                        || actor.hasPermission("banova.import") || actor.hasPermission("banova.audit");
            }
        });
    }
}
