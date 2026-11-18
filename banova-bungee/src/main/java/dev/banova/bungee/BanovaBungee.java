package dev.banova.bungee;

import dev.banova.core.BanovaCore;
import dev.banova.core.model.Source;
import dev.banova.core.platform.*;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.*;
import net.md_5.bungee.event.EventHandler;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;

public final class BanovaBungee extends Plugin implements Platform, Listener {
    private BanovaCore core;

    @Override public void onEnable() {
        core = new BanovaCore(this);
        try {
            core.enable();
        } catch (Exception failure) {
            core.disable();
            core = null;
            throw new IllegalStateException("Could not start Banova", failure);
        }
        var plugins = getProxy().getPluginManager();
        plugins.registerListener(this, this);
        plugins.registerCommand(this, new Command("banovaproxy", null, "bproxy") {
            @Override public void execute(CommandSender sender, String[] args) {
                core.commands().dispatch(new Actor(sender), core.commands().get("banova"), "banovaproxy", args);
            }
        });
    }

    @Override public void onDisable() {
        if (core != null) core.disable();
    }

    @EventHandler public void login(LoginEvent event) {
        if (event.isCancelled()) return;
        var connection = event.getConnection();
        event.registerIntent(this);
        getProxy().getScheduler().runAsync(this, () -> {
            try {
                String ip = address(connection.getSocketAddress());
                String reason = core.players().handleLogin(connection.getUniqueId(), connection.getName(), ip);
                if (reason != null) {
                    event.setCancelReason(components(reason));
                    event.setCancelled(true);
                }
            } finally {
                event.completeIntent(this);
            }
        });
    }

    @EventHandler public void serverConnect(ServerConnectEvent event) {
        var player = event.getPlayer();
        if (core.lockdown().active() && !player.hasPermission("banova.lockdown.bypass")) {
            event.setCancelled(true);
            player.disconnect(components(core.lockdown().screen()));
        }
    }

    @EventHandler public void quit(PlayerDisconnectEvent event) {
        core.players().handleQuit(event.getPlayer().getUniqueId());
    }

    private static String address(java.net.SocketAddress socket) {
        return socket instanceof InetSocketAddress a && a.getAddress() != null ? a.getAddress().getHostAddress() : null;
    }

    private static net.md_5.bungee.api.chat.BaseComponent[] components(String text) {
        return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', text));
    }

    private record Actor(CommandSender sender) implements OnlinePlayer {
        public UUID uuid() { return sender instanceof ProxiedPlayer p ? p.getUniqueId() : null; }
        public String name() { return sender.getName(); }
        public boolean hasPermission(String permission) { return isConsole() || sender.hasPermission(permission); }
        public void send(String message) { sender.sendMessage(components(message)); }
        public Source source() { return isConsole() ? Source.CONSOLE : Source.GAME; }
        public String ip() { return sender instanceof ProxiedPlayer p ? address(p.getSocketAddress()) : null; }
        public void kick(String reason) { if (sender instanceof ProxiedPlayer p) p.disconnect(components(reason)); }
    }

    public String platformName() { return "BungeeCord"; }
    public String version() { return getDescription().getVersion(); }
    public Path dataFolder() { return getDataFolder().toPath(); }
    public Path serverRoot() { return dataFolder().toAbsolutePath().getParent().getParent(); }
    public BanovaLogger logger() { return BanovaLogger.ofJul(getLogger()); }
    public boolean onlineMode() { return getProxy().getConfig().isOnlineMode(); }
    public boolean isProxy() { return true; }
    public void runSync(Runnable task) { task.run(); }
    public Collection<? extends OnlinePlayer> onlinePlayers() { return getProxy().getPlayers().stream().map(Actor::new).toList(); }
    public OnlinePlayer player(UUID id) { return id == null ? null : wrap(getProxy().getPlayer(id)); }
    public OnlinePlayer player(String name) { return wrap(getProxy().getPlayer(name)); }
    private OnlinePlayer wrap(ProxiedPlayer player) { return player == null ? null : new Actor(player); }
    public CommandActor console() { return new Actor(getProxy().getConsole()); }
    public void broadcast(String message, String permission) {
        for (OnlinePlayer p : onlinePlayers()) if (permission == null || p.hasPermission(permission)) p.send(message);
        console().send(message);
    }
}
