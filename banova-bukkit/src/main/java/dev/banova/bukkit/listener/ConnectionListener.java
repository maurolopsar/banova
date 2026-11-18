package dev.banova.bukkit.listener;

import dev.banova.bukkit.BanovaBukkit;
import dev.banova.bukkit.BukkitActor;
import dev.banova.bukkit.BukkitText;
import dev.banova.core.BanovaCore;
import dev.banova.core.model.Records.AppealStatus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Ban checks on login, lockdown, alt alerts and cleanup on quit.
 */
public final class ConnectionListener implements Listener {

    private final BanovaBukkit plugin;

    public ConnectionListener(BanovaBukkit plugin) {
        this.plugin = plugin;
    }

    private BanovaCore core() {
        return plugin.core();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        String ip = event.getAddress() == null ? null : event.getAddress().getHostAddress();
        try {
            String screen = core().players().handleLogin(event.getUniqueId(), event.getName(), ip);
            if (screen != null) {
                plugin.disallow(event, AsyncPlayerPreLoginEvent.Result.KICK_BANNED, screen);
            }
        } catch (RuntimeException e) {
            // Fail open: a database outage must not lock everyone out.
            core().platform().logger().error("Ban check failed for " + event.getName() + ", letting them join", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED || !core().lockdown().active()) {
            return;
        }
        if (!event.getPlayer().hasPermission("banova.lockdown.bypass")) {
            plugin.disallow(event, PlayerLoginEvent.Result.KICK_OTHER, core().lockdown().screen());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        BukkitActor.BukkitPlayer actor = (BukkitActor.BukkitPlayer) BukkitActor.of(plugin, player);
        boolean appealsStaff = player.hasPermission("banova.appeals") && player.hasPermission("banova.notify.appeals");
        boolean web = player.hasPermission("banova.web");
        core().async(() -> {
            core().players().afterJoin(uuid, name);
            if (web || core().web().running()) {
                core().web().refreshPermissions(actor);
            }
            if (appealsStaff) {
                long pending = core().records().countAppeals(AppealStatus.PENDING);
                if (pending > 0) {
                    actor.sendClickable(core().messages().get("gui.join-pending-appeals", "count", pending),
                            dev.banova.core.platform.CommandActor.ClickAction.RUN_COMMAND, "/appeals", core().messages().get("list.hover"));
                }
            }
        });
        if (core().lockdown().active() && player.hasPermission("banova.lockdown")) {
            BukkitText.send(player, core().messages().get("lockdown.status-on", "reason", core().lockdown().reason()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        core().players().handleQuit(uuid);
        core().filter().forget(uuid);
        plugin.prompts().cancel(uuid);
    }
}
