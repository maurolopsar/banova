package dev.banova.core.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.model.Punishment;
import dev.banova.core.util.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Posts punishments, revocations and appeals to a Discord channel using a webhook (no bot needed).
 */
public final class DiscordWebhook {

    private final BanovaCore core;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public DiscordWebhook(BanovaCore core) {
        this.core = core;
    }

    private boolean enabled(String event) {
        return core.config().getBoolean("discord.enabled", false)
                && !core.config().getString("discord.webhook-url", "").isBlank()
                && core.config().getBoolean("discord.events." + event, true)
                && !core.platform().isProxy();
    }

    public void punishment(Punishment p) {
        String event = p.type.name().toLowerCase(Locale.ROOT);
        if (!enabled(event) || (p.silent && !core.config().getBoolean("discord.include-silent", true))) {
            return;
        }
        JsonObject embed = new JsonObject();
        embed.addProperty("title", core.formatter().typeName(p.type) + (p.ipBased ? " (IP)" : "") + " · " + p.targetName);
        embed.addProperty("color", color(p.type));
        JsonArray fields = new JsonArray();
        field(fields, core.messages().raw("discord.reason"), p.reason, false);
        field(fields, core.messages().raw("discord.staff"), p.actorName, true);
        if (p.type.isLasting()) {
            field(fields, core.messages().raw("discord.duration"), core.formatter().duration(p.length()), true);
        }
        field(fields, "ID", "#" + p.id, true);
        if (p.server != null && !p.server.isBlank()) {
            field(fields, core.messages().raw("discord.server"), p.server, true);
        }
        if (p.silent) {
            field(fields, core.messages().raw("discord.silent"), "✔", true);
        }
        embed.add("fields", fields);
        addCommon(embed, p);
        send(embed);
    }

    public void revoke(Punishment p) {
        if (!enabled("revoke")) {
            return;
        }
        JsonObject embed = new JsonObject();
        embed.addProperty("title", core.messages().raw("discord.revoked") + " " + core.formatter().typeName(p.type) + " · " + p.targetName);
        embed.addProperty("color", 0x57F287);
        JsonArray fields = new JsonArray();
        field(fields, core.messages().raw("discord.staff"), p.revokedByName, true);
        field(fields, "ID", "#" + p.id, true);
        field(fields, core.messages().raw("discord.reason"), p.revokeReason, false);
        embed.add("fields", fields);
        addCommon(embed, p);
        send(embed);
    }

    public void appeal(Punishment p, long appealId, String message) {
        if (!enabled("appeal")) {
            return;
        }
        JsonObject embed = new JsonObject();
        embed.addProperty("title", core.messages().raw("discord.new-appeal") + " · " + p.targetName);
        embed.addProperty("color", 0x5865F2);
        embed.addProperty("description", Text.truncate(message, 1500));
        JsonArray fields = new JsonArray();
        field(fields, core.messages().raw("discord.punishment"), core.formatter().typeName(p.type) + " #" + p.id, true);
        field(fields, core.messages().raw("discord.reason"), p.reason, true);
        embed.add("fields", fields);
        if (core.config().getBoolean("web.enabled", false)) {
            String base = core.config().getString("web.public-url", "").replaceAll("/+$", "");
            if (!base.isBlank()) {
                embed.addProperty("url", base + "/#/appeals/" + appealId);
            }
        }
        addCommon(embed, p);
        send(embed);
    }

    private void addCommon(JsonObject embed, Punishment p) {
        embed.addProperty("timestamp", Instant.now().toString());
        if (p.targetUuid != null) {
            JsonObject thumb = new JsonObject();
            thumb.addProperty("url", "https://mc-heads.net/avatar/" + p.targetUuid + "/64");
            embed.add("thumbnail", thumb);
        }
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "Banova · " + core.serverName());
        embed.add("footer", footer);
    }

    private static void field(JsonArray fields, String name, String value, boolean inline) {
        JsonObject f = new JsonObject();
        f.addProperty("name", Text.truncate(name, 250));
        f.addProperty("value", value == null || value.isBlank() ? "-" : Text.truncate(Text.strip(value), 1000));
        f.addProperty("inline", inline);
        fields.add(f);
    }

    private static int color(PunishmentType type) {
        return switch (type) {
            case BAN -> 0xED4245;
            case MUTE -> 0xFEE75C;
            case WARN -> 0xF0B232;
            case KICK -> 0x99AAB5;
        };
    }

    private void send(JsonObject embed) {
        JsonObject body = new JsonObject();
        body.addProperty("username", core.config().getString("discord.username", "Banova"));
        String avatar = core.config().getString("discord.avatar-url", "");
        if (!avatar.isBlank()) {
            body.addProperty("avatar_url", avatar);
        }
        JsonObject mentions = new JsonObject();
        mentions.add("parse", new JsonArray());
        body.add("allowed_mentions", mentions);
        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        body.add("embeds", embeds);
        String url = core.config().getString("discord.webhook-url", "");
        core.async(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json").header("User-Agent", "Banova")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 300) {
                    core.platform().logger().warn("Discord webhook answered " + response.statusCode() + ": " + Text.truncate(response.body(), 200));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                core.platform().logger().warn("Discord webhook failed: " + e.getMessage());
            }
        });
    }
}
