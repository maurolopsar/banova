package dev.banova.core.importer;

import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.config.YamlConfig;
import dev.banova.core.util.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * EssentialsX: bans live in the vanilla files, mutes in plugins/Essentials/userdata/&lt;uuid&gt;.yml.
 */
public final class EssentialsImporter implements Importer {

    @Override
    public String id() {
        return "essentials";
    }

    @Override
    public String displayName() {
        return "EssentialsX";
    }

    @Override
    public List<Imported> read(BanovaCore core) throws ImportException {
        if (core.platform().isProxy()) {
            throw new ImportException(core.messages().raw("import.backend-only"));
        }
        List<Imported> out = new ArrayList<>();
        try {
            out.addAll(new VanillaImporter().read(core));
        } catch (ImportException ignored) {
            // No vanilla ban files: only mutes.
        }
        Path userdata = core.platform().serverRoot().resolve("plugins").resolve("Essentials").resolve("userdata");
        if (!Files.isDirectory(userdata)) {
            if (out.isEmpty()) {
                throw new ImportException("plugins/Essentials/userdata not found");
            }
            return out;
        }
        try (Stream<Path> files = Files.list(userdata)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String fileName = file.getFileName().toString();
                if (!fileName.endsWith(".yml")) {
                    continue;
                }
                UUID uuid = Text.parseUuid(fileName.substring(0, fileName.length() - 4));
                if (uuid == null) {
                    continue;
                }
                YamlConfig data;
                try {
                    data = YamlConfig.load(file, null);
                } catch (IOException | RuntimeException e) {
                    continue;
                }
                boolean muted = data.getBoolean("muted", data.getBoolean("mute", false));
                if (!muted) {
                    continue;
                }
                long until = data.getLong("timestamps.mute", 0);
                if (until > 0 && until < System.currentTimeMillis()) {
                    continue;
                }
                Imported e = new Imported();
                e.type = PunishmentType.MUTE;
                e.uuid = uuid;
                e.name = data.getString("last-account-name", data.getString("lastAccountName", null));
                e.reason = data.getString("mute-reason", data.getString("muteReason", null));
                e.actorName = "Console";
                e.createdAt = System.currentTimeMillis();
                e.expiresAt = Math.max(0, until);
                e.active = true;
                e.ref = "essentials:mute:" + uuid + ":" + until;
                out.add(e);
            }
        } catch (IOException e) {
            throw new ImportException("Cannot read Essentials userdata: " + e.getMessage(), e);
        }
        return out;
    }
}
