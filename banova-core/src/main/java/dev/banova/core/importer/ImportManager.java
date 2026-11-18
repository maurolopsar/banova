package dev.banova.core.importer;

import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.model.ActorRef;
import dev.banova.core.model.Punishment;
import dev.banova.core.model.Source;
import dev.banova.core.platform.CommandActor;
import dev.banova.core.util.Codes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs imports from other punishment plugins. Imports are safe to repeat: entries already
 * imported are skipped.
 */
public final class ImportManager {

    private final BanovaCore core;
    private final Map<String, Importer> importers = new LinkedHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean();

    public ImportManager(BanovaCore core) {
        this.core = core;
        register(new VanillaImporter());
        register(new EssentialsImporter());
        register(new LiteBansImporter());
        register(new AdvancedBanImporter());
    }

    private void register(Importer importer) {
        importers.put(importer.id(), importer);
    }

    public List<String> sources() {
        return new ArrayList<>(importers.keySet());
    }

    /** Called from a worker thread by /banova import. */
    public void run(String source, CommandActor actor, boolean confirm) {
        Importer importer = importers.get(source.toLowerCase(Locale.ROOT));
        if (importer == null) {
            actor.send(core.messages().get("import.unknown", "source", source, "sources", String.join(", ", importers.keySet())));
            return;
        }
        if (!running.compareAndSet(false, true)) {
            actor.send(core.messages().get("import.busy"));
            return;
        }
        try {
            actor.send(core.messages().get("import.reading", "source", importer.displayName()));
            List<Imported> entries;
            try {
                entries = importer.read(core);
            } catch (Importer.ImportException e) {
                actor.send(core.messages().get("import.failed", "error", e.getMessage()));
                if (e.getCause() != null) {
                    core.platform().logger().error("Import from " + importer.displayName() + " failed", e.getCause());
                }
                return;
            }
            long active = entries.stream().filter(e -> e.active).count();
            if (!confirm) {
                actor.send(core.messages().get("import.preview", "source", importer.displayName(), "count", entries.size(), "active", active));
                actor.sendClickable(core.messages().get("import.confirm-button"), CommandActor.ClickAction.RUN_COMMAND,
                        "/banova import " + importer.id() + " confirm", core.messages().get("import.confirm-button"));
                return;
            }
            int imported = 0;
            int skipped = 0;
            int failed = 0;
            for (Imported entry : entries) {
                try {
                    if (store(entry)) {
                        imported++;
                    } else {
                        skipped++;
                    }
                } catch (RuntimeException e) {
                    failed++;
                    if (failed <= 3) {
                        core.platform().logger().warn("Could not import " + entry.ref + ": " + e.getMessage());
                    }
                }
                int done = imported + skipped + failed;
                if (done % 1000 == 0) {
                    actor.send(core.messages().get("import.progress", "done", done, "total", entries.size()));
                }
            }
            core.audit().log(actor.ref(), "IMPORT", null, importer.displayName(), 0,
                    imported + " imported, " + skipped + " skipped, " + failed + " failed");
            actor.send(core.messages().get("import.done", "imported", imported, "skipped", skipped, "failed", failed));
        } finally {
            running.set(false);
        }
    }

    private boolean store(Imported e) {
        if (e.ref == null) {
            return false;
        }
        if (e.note) {
            // Notes have no import column; remember imported ones in the meta table.
            String marker = "imp:" + Codes.sha256(e.ref).substring(0, 40);
            if (e.uuid == null || core.records().meta(marker) != null) {
                return false;
            }
            if (e.name != null) {
                core.playerRepo().ensure(e.uuid, e.name, e.createdAt);
            }
            core.records().addNote(e.uuid, e.reason == null ? "" : truncate(e.reason, 1000),
                    new ActorRef(e.actorUuid, e.actorName == null ? "Import" : truncate(e.actorName, 32), Source.IMPORT));
            core.records().setMeta(marker, "1");
            return true;
        }
        if (core.punishmentRepo().importRefExists(e.ref)) {
            return false;
        }
        if (e.uuid != null && e.name != null) {
            core.playerRepo().ensure(e.uuid, e.name, e.createdAt);
        }
        Punishment p = base(e);
        core.punishmentRepo().insert(p);
        return true;
    }

    private Punishment base(Imported e) {
        Punishment p = new Punishment();
        p.type = e.type == null ? PunishmentType.BAN : e.type;
        p.targetUuid = e.uuid;
        p.targetName = e.name != null ? truncate(e.name, 45) : (e.ip != null ? e.ip : "?");
        p.targetIp = e.ip;
        p.ipBased = e.ipBased && e.ip != null;
        p.reason = truncate(e.reason == null || e.reason.isBlank() ? core.messages().raw("default-reason." + p.type.name().toLowerCase(Locale.ROOT)) : e.reason, 500);
        p.actorUuid = e.actorUuid;
        p.actorName = truncate(e.actorName == null || e.actorName.isBlank() ? "Console" : e.actorName, 32);
        p.createdAt = e.createdAt > 0 ? e.createdAt : System.currentTimeMillis();
        p.expiresAt = Math.max(0, e.expiresAt);
        boolean expired = p.expiresAt > 0 && p.expiresAt <= System.currentTimeMillis();
        p.active = p.type.isLasting() && e.active && !expired;
        p.revokedByName = e.revokedByName == null ? null : truncate(e.revokedByName, 32);
        p.revokedAt = e.revokedAt;
        p.revokeReason = e.revokeReason == null ? null : truncate(e.revokeReason, 500);
        if (!p.active && p.revokedAt == 0 && !expired && p.type.isLasting()) {
            // Removed in the source plugin without details.
            p.revokedAt = p.createdAt;
            p.revokedByName = "Import";
            p.revokeReason = "Removed before import";
        }
        p.server = e.server == null ? core.serverName() : truncate(e.server, 64);
        p.silent = e.silent;
        p.importRef = truncate(e.ref, 96);
        if (p.active && (p.type == PunishmentType.BAN || p.type == PunishmentType.MUTE)) {
            String code = Codes.humanCode();
            while (core.punishmentRepo().appealCodeExists(code)) {
                code = Codes.humanCode();
            }
            p.appealCode = code;
        }
        return p;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
