package dev.banova.core.platform;

import dev.banova.core.model.ActorRef;
import dev.banova.core.model.Source;

import java.util.UUID;

/**
 * Someone who runs commands: a player, the console, or a web panel user.
 */
public interface CommandActor {

    /** @return the player UUID, or {@code null} for the console */
    UUID uuid();

    String name();

    boolean hasPermission(String permission);

    /** Sends a message using {@code &} color codes; {@code \n} splits lines. */
    void send(String message);

    Source source();

    default boolean isConsole() {
        return uuid() == null;
    }

    default boolean isPlayer() {
        return uuid() != null && source() == Source.GAME;
    }

    /** Sends a line that runs a command or opens a link when clicked (falls back to plain text). */
    default void sendClickable(String text, ClickAction action, String value, String hover) {
        send(text);
    }

    default ActorRef ref() {
        return new ActorRef(uuid(), name(), source());
    }

    enum ClickAction {
        RUN_COMMAND,
        SUGGEST_COMMAND,
        OPEN_URL,
        COPY
    }
}
