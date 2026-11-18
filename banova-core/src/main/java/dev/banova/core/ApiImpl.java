package dev.banova.core;

import dev.banova.api.BanovaApi;
import dev.banova.api.BanovaListener;
import dev.banova.api.PunishmentView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Implementation of the public API on top of the core services.
 */
final class ApiImpl implements BanovaApi {

    private final BanovaCore core;

    ApiImpl(BanovaCore core) {
        this.core = core;
    }

    private <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, core.executor());
    }

    @Override
    public CompletableFuture<Optional<PunishmentView>> getActiveBan(UUID player) {
        return supply(() -> Optional.ofNullable(core.punishments().activeBan(player, null)));
    }

    @Override
    public CompletableFuture<Optional<PunishmentView>> getActiveMute(UUID player) {
        return supply(() -> Optional.ofNullable(core.punishments().activeMute(player, null)));
    }

    @Override
    public CompletableFuture<List<PunishmentView>> getHistory(UUID player) {
        return supply(() -> new ArrayList<>(core.punishmentRepo().history(player, null, 1000, 0)));
    }

    @Override
    public CompletableFuture<Optional<PunishmentView>> getPunishment(long id) {
        return supply(() -> Optional.ofNullable(core.punishments().byId(id)));
    }

    @Override
    public void registerListener(BanovaListener listener) {
        core.apiListeners().add(listener);
    }

    @Override
    public void unregisterListener(BanovaListener listener) {
        core.apiListeners().remove(listener);
    }
}
