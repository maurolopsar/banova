package dev.banova.api;

/**
 * Receives Banova events. Methods are called asynchronously, never on the server main thread.
 */
public interface BanovaListener {

    /** A punishment was created on this server or anywhere on the network. */
    default void onPunish(PunishmentView punishment) {
    }

    /** A punishment was revoked (unban, unmute, unwarn, appeal accepted...). */
    default void onRevoke(PunishmentView punishment) {
    }
}
