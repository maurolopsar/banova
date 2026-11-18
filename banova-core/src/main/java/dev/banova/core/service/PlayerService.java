package dev.banova.core.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.banova.core.BanovaCore;
import dev.banova.core.model.PlayerRecord;
import dev.banova.core.model.Punishment;
import dev.banova.core.platform.OnlinePlayer;
import dev.banova.core.util.Text;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Player records, logins, name resolution and alt detection.
 */
public final class PlayerService {

    private final BanovaCore core;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public PlayerService(BanovaCore core) {
        this.core = core;
    }

    /**
     * Called before a player joins (async). Records the login and returns the disconnect screen
     * when the player is banned, otherwise null.
     */
    public String handleLogin(UUID uuid, String name, String ip) {
        try {
            core.playerRepo().recordLogin(uuid, name, ip);
        } catch (RuntimeException e) {
            core.platform().logger().error("Could not record login of " + name, e);
        }
        Punishment ban = core.punishments().activeBan(uuid, ip);
        if (ban != null) {
            return core.formatter().banScreen(ban);
        }
        if (!core.platform().isProxy()) {
            Punishment mute = core.punishments().activeMute(uuid, ip);
            if (mute != null) {
                core.mutes().put(uuid, mute);
            } else {
                core.mutes().remove(uuid);
            }
        }
        return null;
    }

    private final java.util.Map<UUID, Long> lastAlert = new java.util.concurrent.ConcurrentHashMap<>();

    /** Runs after a successful join: ban evasion alerts (at most once every 30 minutes per player, network-wide). */
    public void afterJoin(UUID uuid, String name) {
        if (!core.config().getBoolean("punishments.alert-ban-evasion", true)) {
            return;
        }
        long now = System.currentTimeMillis();
        long window = 30 * 60_000L;
        Long local = lastAlert.get(uuid);
        if (local != null && now - local < window) {
            return;
        }
        if (core.database().isShared()) {
            String stored = core.records().meta("evasion:" + uuid);
            try {
                if (stored != null && now - Long.parseLong(stored) < window) {
                    lastAlert.put(uuid, Long.parseLong(stored));
                    return;
                }
            } catch (NumberFormatException ignored) {
                // overwrite below
            }
        }
        List<String> banned = new ArrayList<>();
        for (PlayerRecord alt : core.playerRepo().alts(uuid)) {
            if (core.punishments().activeBan(alt.uuid(), null) != null) {
                banned.add(alt.name());
            }
            if (banned.size() >= 5) {
                break;
            }
        }
        if (!banned.isEmpty()) {
            lastAlert.put(uuid, now);
            if (core.database().isShared()) {
                core.records().setMeta("evasion:" + uuid, String.valueOf(now));
            }
            core.sync().notifyStaff("banova.notify.alts",
                    core.messages().get("alts.evasion-alert", "player", name, "alts", String.join(", ", banned)));
        }
    }

    public void handleQuit(UUID uuid) {
        core.mutes().remove(uuid);
    }

    public PlayerRecord byUuid(UUID uuid) {
        return core.playerRepo().byUuid(uuid);
    }

    /**
     * Resolves what staff typed (name, UUID) to a player. Online players win, then known players,
     * then a Mojang/offline lookup depending on {@code punishments.unknown-players}.
     *
     * @return the player or null when it cannot be resolved
     */
    public PlayerRecord resolve(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String text = input.trim();
        UUID asUuid = Text.parseUuid(text);
        if (asUuid != null) {
            PlayerRecord known = core.playerRepo().byUuid(asUuid);
            return known != null ? known : new PlayerRecord(asUuid, text, null, 0, 0);
        }
        if (!Text.isValidName(text)) {
            return null;
        }
        OnlinePlayer online = core.platform().player(text);
        if (online != null) {
            PlayerRecord known = core.playerRepo().byUuid(online.uuid());
            return new PlayerRecord(online.uuid(), online.name(), online.ip(),
                    known == null ? System.currentTimeMillis() : known.firstSeen(), System.currentTimeMillis());
        }
        PlayerRecord known = core.playerRepo().byName(text);
        if (known != null) {
            return known;
        }
        String mode = core.config().getString("punishments.unknown-players", "auto").toLowerCase(Locale.ROOT);
        if (mode.equals("deny")) {
            return null;
        }
        boolean mojang = mode.equals("mojang") || (mode.equals("auto") && core.platform().onlineMode());
        if (mojang) {
            return lookupMojang(text);
        }
        UUID offline = UUID.nameUUIDFromBytes(("OfflinePlayer:" + text).getBytes(StandardCharsets.UTF_8));
        return new PlayerRecord(offline, text, null, 0, 0);
    }

    private PlayerRecord lookupMojang(String name) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/"
                            + URLEncoder.encode(name, StandardCharsets.UTF_8)))
                    .timeout(Duration.ofSeconds(5)).header("User-Agent", "Banova").GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            UUID uuid = Text.parseUuid(json.get("id").getAsString());
            String realName = json.get("name").getAsString();
            if (uuid == null) {
                return null;
            }
            core.playerRepo().ensure(uuid, realName, 0);
            return new PlayerRecord(uuid, realName, null, 0, 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            core.platform().logger().warn("Mojang lookup failed for " + name + ": " + e.getMessage());
            return null;
        }
    }
}
