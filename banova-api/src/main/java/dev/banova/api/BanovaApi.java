package dev.banova.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Entry point for other plugins.
 *
 * <pre>{@code
 * BanovaApi api = BanovaApi.get();
 * api.getActiveBan(uuid).thenAccept(ban -> ...);
 * }</pre>
 */
public interface BanovaApi {

    /**
     * @return the running API instance
     * @throws IllegalStateException if Banova is not loaded yet
     */
    static BanovaApi get() {
        BanovaApi api = Holder.instance;
        if (api == null) {
            throw new IllegalStateException("Banova is not loaded");
        }
        return api;
    }

    CompletableFuture<Optional<PunishmentView>> getActiveBan(UUID player);

    CompletableFuture<Optional<PunishmentView>> getActiveMute(UUID player);

    CompletableFuture<List<PunishmentView>> getHistory(UUID player);

    CompletableFuture<Optional<PunishmentView>> getPunishment(long id);

    void registerListener(BanovaListener listener);

    void unregisterListener(BanovaListener listener);

    /**
     * Internal holder; set by Banova on enable.
     */
    final class Holder {
        private static volatile BanovaApi instance;

        private Holder() {
        }

        public static void set(BanovaApi api) {
            instance = api;
        }
    }
}
