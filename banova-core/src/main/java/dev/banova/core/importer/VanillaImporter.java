package dev.banova.core.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.util.Text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Minecraft's own banned-players.json and banned-ips.json (also used by EssentialsX and /ban from vanilla).
 */
public class VanillaImporter implements Importer {

    @Override
    public String id() {
        return "vanilla";
    }

    @Override
    public String displayName() {
        return "Vanilla (banned-players.json)";
    }

    @Override
    public List<Imported> read(BanovaCore core) throws ImportException {
        if (core.platform().isProxy()) {
            throw new ImportException(core.messages().raw("import.backend-only"));
        }
        Path root = core.platform().serverRoot();
        Path players = root.resolve("banned-players.json");
        Path ips = root.resolve("banned-ips.json");
        if (!Files.exists(players) && !Files.exists(ips)) {
            throw new ImportException("banned-players.json not found in " + root.toAbsolutePath());
        }
        List<Imported> out = new ArrayList<>();
        for (JsonObject o : readArray(players)) {
            UUID uuid = Text.parseUuid(str(o, "uuid"));
            if (uuid == null) {
                continue;
            }
            Imported e = base(o, "vanilla:player:" + uuid);
            e.uuid = uuid;
            e.name = str(o, "name");
            out.add(e);
        }
        for (JsonObject o : readArray(ips)) {
            String ip = str(o, "ip");
            if (ip == null) {
                continue;
            }
            Imported e = base(o, "vanilla:ip:" + ip);
            e.ip = ip;
            e.ipBased = true;
            e.name = ip;
            out.add(e);
        }
        return out;
    }

    private static Imported base(JsonObject o, String ref) {
        Imported e = new Imported();
        e.type = PunishmentType.BAN;
        e.reason = str(o, "reason");
        String source = str(o, "source");
        e.actorName = source == null || source.equals("(Unknown)") || source.equalsIgnoreCase("Server") ? "Console" : source;
        e.createdAt = parseDate(str(o, "created"));
        String expires = str(o, "expires");
        e.expiresAt = expires == null || expires.equalsIgnoreCase("forever") ? 0 : parseDate(expires);
        e.active = true;
        e.ref = ref;
        return e;
    }

    private static List<JsonObject> readArray(Path file) throws ImportException {
        List<JsonObject> out = new ArrayList<>();
        if (!Files.exists(file)) {
            return out;
        }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (root.isJsonArray()) {
                JsonArray array = root.getAsJsonArray();
                for (JsonElement el : array) {
                    if (el.isJsonObject()) {
                        out.add(el.getAsJsonObject());
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            throw new ImportException("Cannot read " + file.getFileName() + ": " + e.getMessage(), e);
        }
        return out;
    }

    static String str(JsonObject o, String key) {
        JsonElement el = o.get(key);
        return el == null || el.isJsonNull() ? null : el.getAsString();
    }

    static long parseDate(String text) {
        if (text == null) {
            return 0;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT).parse(text).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }
}
