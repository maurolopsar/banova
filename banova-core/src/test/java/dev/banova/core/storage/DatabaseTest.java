package dev.banova.core.storage;

import dev.banova.api.PunishmentType;
import dev.banova.core.config.YamlConfig;
import dev.banova.core.model.ActorRef;
import dev.banova.core.model.Punishment;
import dev.banova.core.platform.BanovaLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the schema and the main queries against a real H2 database.
 */
class DatabaseTest {

    private static final BanovaLogger LOG = BanovaLogger.ofJul(java.util.logging.Logger.getLogger("test"));

    private static Database open(Path dir) throws Exception {
        YamlConfig storage = YamlConfig.load(new StringReader("type: h2\ntable-prefix: test_\n"), null);
        return new Database(storage, dir, LOG);
    }

    private static Punishment ban(UUID uuid, String ip, boolean ipBased, long duration) {
        Punishment p = new Punishment();
        p.type = PunishmentType.BAN;
        p.targetUuid = uuid;
        p.targetName = "Steve";
        p.targetIp = ip;
        p.ipBased = ipBased;
        p.reason = "Griefing";
        p.actorName = "Console";
        p.createdAt = System.currentTimeMillis();
        p.expiresAt = duration == 0 ? 0 : p.createdAt + duration;
        p.active = true;
        p.server = "test";
        p.appealCode = "ABCD-" + (ipBased ? "EFGH" : "JKMN");
        return p;
    }

    @Test
    void storesAndFindsActivePunishments(@TempDir Path dir) throws Exception {
        try (Database db = open(dir)) {
            PunishmentRepository repo = new PunishmentRepository(db);
            UUID steve = UUID.randomUUID();
            long id = repo.insert(ban(steve, "10.0.0.1", false, 0));
            assertTrue(id > 0);

            assertEquals(1, repo.active(PunishmentType.BAN, steve, null).size());
            assertNotNull(repo.byAppealCode("ABCD-JKMN"));
            assertEquals(1, repo.countHistory(steve, null));

            // IP ban affects another account on the same IP
            repo.insert(ban(null, "10.0.0.9", true, 60_000));
            assertEquals(1, repo.active(PunishmentType.BAN, UUID.randomUUID(), "10.0.0.9").size());

            assertTrue(repo.revoke(id, null, "Console", "test"));
            assertFalse(repo.revoke(id, null, "Console", "again"));
            assertTrue(repo.active(PunishmentType.BAN, steve, null).isEmpty());
            assertNotNull(repo.byId(id).revokeReason);
        }
    }

    @Test
    void manualLevelsAndEvidenceFollowTheCase(@TempDir Path dir) throws Exception {
        try (Database db = open(dir)) {
            PunishmentRepository repo = new PunishmentRepository(db);
            UUID target = UUID.randomUUID();
            Punishment p = ban(target, "10.0.0.1", false, 60_000);
            p.template = "insults";
            p.templateStep = 3;
            repo.insert(p, "Witness statement");
            assertEquals("Witness statement", new RecordRepository(db).evidence(p.id).get(0).content());
            var progress = new PunishmentRepository.Progress(1, 3);
            assertEquals(progress, repo.progress(target, "insults", 0));
            assertEquals(progress, repo.progress(target, java.util.Map.of("insults", 0L)).get("insults"));
            assertTrue(repo.progress(target, java.util.Map.of("insults", p.createdAt + 1)).isEmpty());
            assertTrue(repo.revoke(p.id, null, "Console", "Appeal accepted"));
            assertEquals(-1, repo.progress(target, "insults", 0).highestIndex());
            assertTrue(repo.progress(target, java.util.Map.of("insults", 0L)).isEmpty());
            assertEquals(p.id, repo.routeHistory(target, "insults", 7, 0).get(0).id);
        }
    }

    @Test
    void schemaMigrationIsIdempotent(@TempDir Path dir) throws Exception {
        try (Database db = open(dir)) {
            new RecordRepository(db).setMeta("k", "v");
        }
        try (Database db = open(dir)) {
            RecordRepository records = new RecordRepository(db);
            assertEquals("v", records.meta("k"));
            records.setMeta("k", null);
            assertNull(records.meta("k"));
        }
    }

    @Test
    void notesAuditAndPlayers(@TempDir Path dir) throws Exception {
        try (Database db = open(dir)) {
            RecordRepository records = new RecordRepository(db);
            PlayerRepository players = new PlayerRepository(db);
            UUID a = UUID.randomUUID();
            UUID b = UUID.randomUUID();
            players.recordLogin(a, "Alpha", "1.2.3.4");
            players.recordLogin(b, "Beta", "1.2.3.4");
            players.recordLogin(a, "AlphaRenamed", "5.6.7.8");
            assertEquals("AlphaRenamed", players.byUuid(a).name());
            assertEquals(1, players.alts(a).size());
            assertEquals(2, players.ips(a).size());

            long note = records.addNote(a, "watch xray", ActorRef.CONSOLE);
            assertEquals(1, records.notes(a).size());
            assertTrue(records.deleteNote(note));

            records.addAudit(ActorRef.CONSOLE, "PUNISH", a, "Alpha", 1, "details");
            assertEquals(1, records.audit(null, null, null, 10, 0).size());
            assertEquals(1, records.audit("console", "punish", a, 10, 0).size());
        }
    }
}
