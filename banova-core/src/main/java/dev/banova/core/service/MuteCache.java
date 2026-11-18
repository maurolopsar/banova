package dev.banova.core.service;

import dev.banova.core.model.Punishment;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Active mutes of online players, so chat checks never touch the database.
 */
public final class MuteCache {

    private final Map<UUID, Punishment> mutes = new ConcurrentHashMap<>();

    public void put(UUID player, Punishment mute) {
        if (player != null && mute != null) {
            mutes.put(player, mute);
        }
    }

    /** @return the active mute, or null (expired mutes are dropped on read) */
    public Punishment get(UUID player) {
        Punishment p = mutes.get(player);
        if (p == null) {
            return null;
        }
        if (!p.isInEffect()) {
            mutes.remove(player, p);
            return null;
        }
        return p;
    }

    public boolean removeIf(UUID player, long punishmentId) {
        Punishment p = mutes.get(player);
        if (p != null && p.id == punishmentId) {
            return mutes.remove(player, p);
        }
        return false;
    }

    public void remove(UUID player) {
        mutes.remove(player);
    }

    public void clear() {
        mutes.clear();
    }
}
