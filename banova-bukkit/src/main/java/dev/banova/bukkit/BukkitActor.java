package dev.banova.bukkit;

import dev.banova.core.model.Source;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.platform.OnlinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Wraps a Bukkit command sender (player or console).
 */
public class BukkitActor implements CommandActor {

    protected final CommandSender sender;
    protected final BanovaBukkit plugin;

    BukkitActor(BanovaBukkit plugin, CommandSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    public static CommandActor of(BanovaBukkit plugin, CommandSender sender) {
        if (sender instanceof Player p) {
            return new BukkitPlayer(plugin, p);
        }
        return new BukkitActor(plugin, sender);
    }

    public CommandSender sender() {
        return sender;
    }

    @Override
    public UUID uuid() {
        return sender instanceof Player p ? p.getUniqueId() : null;
    }

    @Override
    public String name() {
        return sender instanceof Player p ? p.getName() : "Console";
    }

    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    public void send(String message) {
        BukkitText.send(sender, message);
    }

    @Override
    public void sendClickable(String text, ClickAction action, String value, String hover) {
        BukkitText.sendClickable(sender, text, action.name(), value, hover);
    }

    @Override
    public Source source() {
        return sender instanceof Player ? Source.GAME : Source.CONSOLE;
    }

    /** Online player wrapper. */
    public static final class BukkitPlayer extends BukkitActor implements OnlinePlayer {

        private final Player player;

        BukkitPlayer(BanovaBukkit plugin, Player player) {
            super(plugin, player);
            this.player = player;
        }

        public Player player() {
            return player;
        }

        @Override
        public String ip() {
            InetSocketAddress address = player.getAddress();
            return address == null || address.getAddress() == null ? null : address.getAddress().getHostAddress();
        }

        @Override
        public void kick(String message) {
            plugin.runFor(player, () -> {
                if (player.isOnline()) {
                    BukkitText.kick(player, message);
                }
            });
        }
    }
}
