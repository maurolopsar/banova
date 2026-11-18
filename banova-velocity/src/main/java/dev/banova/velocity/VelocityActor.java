package dev.banova.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.banova.core.model.Source;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.platform.OnlinePlayer;

import java.util.UUID;

/**
 * Velocity command source / player wrapper.
 */
class VelocityActor implements CommandActor {

    protected final CommandSource source;

    VelocityActor(CommandSource source) {
        this.source = source;
    }

    static CommandActor of(CommandSource source) {
        return source instanceof Player p ? new VelocityPlayer(p) : new VelocityActor(source);
    }

    @Override
    public UUID uuid() {
        return source instanceof Player p ? p.getUniqueId() : null;
    }

    @Override
    public String name() {
        return source instanceof Player p ? p.getUsername() : "Console";
    }

    @Override
    public boolean hasPermission(String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void send(String message) {
        for (String line : message.split("\n", -1)) {
            source.sendMessage(VelocityText.of(line));
        }
    }

    @Override
    public void sendClickable(String text, ClickAction action, String value, String hover) {
        if (source instanceof Player) {
            source.sendMessage(VelocityText.clickable(text, action.name(), value, hover));
        } else {
            send(text + (action == ClickAction.OPEN_URL ? " &7" + value : ""));
        }
    }

    @Override
    public Source source() {
        return source instanceof Player ? Source.GAME : Source.CONSOLE;
    }

    static final class VelocityPlayer extends VelocityActor implements OnlinePlayer {

        private final Player player;

        VelocityPlayer(Player player) {
            super(player);
            this.player = player;
        }

        @Override
        public String ip() {
            return player.getRemoteAddress() == null || player.getRemoteAddress().getAddress() == null
                    ? null : player.getRemoteAddress().getAddress().getHostAddress();
        }

        @Override
        public void kick(String message) {
            player.disconnect(VelocityText.of(message));
        }
    }
}
