package dev.banova.core.platform;

/**
 * A player currently connected to this server or proxy.
 */
public interface OnlinePlayer extends CommandActor {

    /** Player IP address without port. */
    String ip();

    /** Disconnects the player. Must be safe to call from any thread. */
    void kick(String message);
}
