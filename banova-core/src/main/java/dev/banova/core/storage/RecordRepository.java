package dev.banova.core.storage;

import dev.banova.core.model.ActorRef;
import dev.banova.core.model.Records.Appeal;
import dev.banova.core.model.Records.AppealStatus;
import dev.banova.core.model.Records.AuditEntry;
import dev.banova.core.model.Records.Evidence;
import dev.banova.core.model.Records.Note;
import dev.banova.core.model.Records.SyncEvent;
import dev.banova.core.model.Records.WebSession;
import dev.banova.core.model.Source;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Storage for notes, evidence, appeals, audit log, web sessions, network sync events and key/value meta.
 */
public final class RecordRepository {

    private final Database db;

    public RecordRepository(Database db) {
        this.db = db;
    }

    // ------------------------------------------------------------------ notes

    private static Note note(ResultSet rs) throws SQLException {
        return new Note(rs.getLong("id"), Database.uuid(rs, "target_uuid"), rs.getString("content"),
                Database.uuid(rs, "actor_uuid"), rs.getString("actor_name"), rs.getLong("created_at"));
    }

    public long addNote(UUID target, String content, ActorRef actor) {
        return db.insert("INSERT INTO " + db.table("notes") + " (target_uuid, content, actor_uuid, actor_name, created_at) VALUES (?,?,?,?,?)",
                target, content, actor.uuid(), actor.name(), System.currentTimeMillis());
    }

    public List<Note> notes(UUID target) {
        return db.query("SELECT * FROM " + db.table("notes") + " WHERE target_uuid = ? ORDER BY created_at DESC", RecordRepository::note, target);
    }

    public Note note(long id) {
        return db.queryOne("SELECT * FROM " + db.table("notes") + " WHERE id = ?", RecordRepository::note, id);
    }

    public boolean deleteNote(long id) {
        return db.update("DELETE FROM " + db.table("notes") + " WHERE id = ?", id) > 0;
    }

    public long countNotes(UUID target) {
        return db.queryLong("SELECT COUNT(*) FROM " + db.table("notes") + " WHERE target_uuid = ?", target);
    }

    // ------------------------------------------------------------------ evidence

    private static Evidence evidence(ResultSet rs) throws SQLException {
        return new Evidence(rs.getLong("id"), rs.getLong("punishment_id"), rs.getString("content"),
                Database.uuid(rs, "actor_uuid"), rs.getString("actor_name"), rs.getLong("created_at"));
    }

    public long addEvidence(long punishmentId, String content, ActorRef actor) {
        return db.insert("INSERT INTO " + db.table("evidence") + " (punishment_id, content, actor_uuid, actor_name, created_at) VALUES (?,?,?,?,?)",
                punishmentId, content, actor.uuid(), actor.name(), System.currentTimeMillis());
    }

    public List<Evidence> evidence(long punishmentId) {
        return db.query("SELECT * FROM " + db.table("evidence") + " WHERE punishment_id = ? ORDER BY created_at ASC",
                RecordRepository::evidence, punishmentId);
    }

    public Evidence evidenceById(long id) {
        return db.queryOne("SELECT * FROM " + db.table("evidence") + " WHERE id = ?", RecordRepository::evidence, id);
    }

    public boolean deleteEvidence(long id) {
        return db.update("DELETE FROM " + db.table("evidence") + " WHERE id = ?", id) > 0;
    }

    // ------------------------------------------------------------------ appeals

    private static Appeal appeal(ResultSet rs) throws SQLException {
        return new Appeal(rs.getLong("id"), rs.getLong("punishment_id"), AppealStatus.valueOf(rs.getString("status")),
                rs.getString("message"), rs.getString("contact"), Source.valueOf(rs.getString("source")), rs.getLong("created_at"),
                Database.uuid(rs, "handled_by_uuid"), rs.getString("handled_by_name"), rs.getLong("handled_at"), rs.getString("response"));
    }

    public long addAppeal(long punishmentId, String message, String contact, Source source, String ip) {
        return db.insert("INSERT INTO " + db.table("appeals") + " (punishment_id, status, message, contact, source, created_at, creator_ip)"
                        + " VALUES (?,?,?,?,?,?,?)",
                punishmentId, AppealStatus.PENDING, message, contact, source, System.currentTimeMillis(), ip);
    }

    public Appeal appeal(long id) {
        return db.queryOne("SELECT * FROM " + db.table("appeals") + " WHERE id = ?", RecordRepository::appeal, id);
    }

    public List<Appeal> appealsFor(long punishmentId) {
        return db.query("SELECT * FROM " + db.table("appeals") + " WHERE punishment_id = ? ORDER BY created_at DESC",
                RecordRepository::appeal, punishmentId);
    }

    public List<Appeal> appeals(AppealStatus status, int limit, int offset) {
        if (status == null) {
            return db.query("SELECT * FROM " + db.table("appeals") + " ORDER BY created_at DESC LIMIT ? OFFSET ?",
                    RecordRepository::appeal, limit, offset);
        }
        String order = status == AppealStatus.PENDING ? "ASC" : "DESC";
        return db.query("SELECT * FROM " + db.table("appeals") + " WHERE status = ? ORDER BY created_at " + order + " LIMIT ? OFFSET ?",
                RecordRepository::appeal, status, limit, offset);
    }

    public List<Appeal> playerAppeals(java.util.UUID target, AppealStatus status, int limit) {
        String sql = "SELECT a.* FROM " + db.table("appeals") + " a JOIN " + db.table("punishments")
                + " p ON a.punishment_id = p.id WHERE p.target_uuid = ?";
        if (status == null) return db.query(sql + " ORDER BY a.created_at DESC LIMIT ?", RecordRepository::appeal, target, limit);
        return db.query(sql + " AND a.status = ? ORDER BY a.created_at DESC LIMIT ?", RecordRepository::appeal, target, status, limit);
    }

    public long countAppeals(AppealStatus status) {
        if (status == null) {
            return db.queryLong("SELECT COUNT(*) FROM " + db.table("appeals"));
        }
        return db.queryLong("SELECT COUNT(*) FROM " + db.table("appeals") + " WHERE status = ?", status);
    }

    public long countAppealsFromIp(String ip, long since) {
        return db.queryLong("SELECT COUNT(*) FROM " + db.table("appeals") + " WHERE creator_ip = ? AND created_at >= ?", ip, since);
    }

    public boolean handleAppeal(long id, AppealStatus status, ActorRef actor, String response) {
        return db.update("UPDATE " + db.table("appeals") + " SET status = ?, handled_by_uuid = ?, handled_by_name = ?, handled_at = ?, response = ?"
                        + " WHERE id = ? AND status = ?",
                status, actor.uuid(), actor.name(), System.currentTimeMillis(), response, id, AppealStatus.PENDING) > 0;
    }

    // ------------------------------------------------------------------ audit

    private static AuditEntry audit(ResultSet rs) throws SQLException {
        Source source;
        try {
            source = Source.valueOf(rs.getString("source"));
        } catch (IllegalArgumentException e) {
            source = Source.SYSTEM;
        }
        return new AuditEntry(rs.getLong("id"), rs.getLong("created_at"), Database.uuid(rs, "actor_uuid"), rs.getString("actor_name"),
                source, rs.getString("action"), Database.uuid(rs, "target_uuid"), rs.getString("target_name"),
                rs.getLong("punishment_id"), rs.getString("details"));
    }

    public void addAudit(ActorRef actor, String action, UUID targetUuid, String targetName, long punishmentId, String details) {
        db.insert("INSERT INTO " + db.table("audit") + " (created_at, actor_uuid, actor_name, source, action, target_uuid, target_name,"
                        + " punishment_id, details) VALUES (?,?,?,?,?,?,?,?,?)",
                System.currentTimeMillis(), actor.uuid(), actor.name(), actor.source(), action, targetUuid,
                targetName == null ? null : truncate(targetName, 45), punishmentId, details == null ? null : truncate(details, 1024));
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    public List<AuditEntry> audit(String actorName, String action, UUID target, int limit, int offset) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();
        if (actorName != null && !actorName.isBlank()) {
            where.append(" WHERE LOWER(actor_name) = ?");
            params.add(actorName.toLowerCase(java.util.Locale.ROOT));
        }
        if (action != null && !action.isBlank()) {
            where.append(where.length() == 0 ? " WHERE " : " AND ").append("action = ?");
            params.add(action.toUpperCase(java.util.Locale.ROOT));
        }
        if (target != null) {
            where.append(where.length() == 0 ? " WHERE " : " AND ").append("target_uuid = ?");
            params.add(target);
        }
        params.add(limit);
        params.add(offset);
        return db.query("SELECT * FROM " + db.table("audit") + where + " ORDER BY id DESC LIMIT ? OFFSET ?",
                RecordRepository::audit, params.toArray());
    }

    // ------------------------------------------------------------------ web sessions

    private static WebSession session(ResultSet rs) throws SQLException {
        Set<String> perms = new LinkedHashSet<>(Arrays.asList(rs.getString("permissions").split(",")));
        perms.remove("");
        return new WebSession(rs.getString("id"), rs.getString("token_hash"), Database.uuid(rs, "uuid"), rs.getString("name"), perms,
                rs.getLong("created_at"), rs.getLong("last_used"), rs.getLong("expires_at"), rs.getBoolean("remember"),
                rs.getString("ip"), rs.getString("user_agent"));
    }

    public void addSession(WebSession s) {
        db.update("INSERT INTO " + db.table("web_sessions") + " (id, token_hash, uuid, name, permissions, created_at, last_used, expires_at,"
                        + " remember, ip, user_agent) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                s.id(), s.tokenHash(), s.uuid(), s.name(), String.join(",", s.permissions()), s.createdAt(), s.lastUsed(), s.expiresAt(),
                s.remember(), s.ip(), s.userAgent() == null ? null : truncate(s.userAgent(), 256));
    }

    public WebSession sessionByHash(String hash) {
        return db.queryOne("SELECT * FROM " + db.table("web_sessions") + " WHERE token_hash = ?", RecordRepository::session, hash);
    }

    public List<WebSession> sessions(UUID owner) {
        if (owner == null) {
            return db.query("SELECT * FROM " + db.table("web_sessions") + " ORDER BY last_used DESC", RecordRepository::session);
        }
        return db.query("SELECT * FROM " + db.table("web_sessions") + " WHERE uuid = ? ORDER BY last_used DESC", RecordRepository::session, owner);
    }

    public void touchSession(String id, long lastUsed, long expiresAt) {
        db.update("UPDATE " + db.table("web_sessions") + " SET last_used = ?, expires_at = ? WHERE id = ?", lastUsed, expiresAt, id);
    }

    public void updateSessionPermissions(UUID owner, Set<String> permissions) {
        db.update("UPDATE " + db.table("web_sessions") + " SET permissions = ? WHERE uuid = ?", String.join(",", permissions), owner);
    }

    public boolean deleteSession(String id) {
        return db.update("DELETE FROM " + db.table("web_sessions") + " WHERE id = ?", id) > 0;
    }

    public int deleteSessionsOf(UUID owner) {
        if (owner == null) {
            return db.update("DELETE FROM " + db.table("web_sessions") + " WHERE uuid IS NULL");
        }
        return db.update("DELETE FROM " + db.table("web_sessions") + " WHERE uuid = ?", owner);
    }

    public int deleteAllSessions() {
        return db.update("DELETE FROM " + db.table("web_sessions"));
    }

    public int deleteExpiredSessions() {
        return db.update("DELETE FROM " + db.table("web_sessions") + " WHERE expires_at <= ?", System.currentTimeMillis());
    }

    // ------------------------------------------------------------------ sync

    public long addSync(String origin, String type, String data) {
        return db.insert("INSERT INTO " + db.table("sync") + " (created_at, origin, type, data) VALUES (?,?,?,?)",
                System.currentTimeMillis(), origin, type, data);
    }

    public List<SyncEvent> syncAfter(long id) {
        return db.query("SELECT * FROM " + db.table("sync") + " WHERE id > ? ORDER BY id ASC LIMIT 500",
                rs -> new SyncEvent(rs.getLong("id"), rs.getLong("created_at"), rs.getString("origin"), rs.getString("type"), rs.getString("data")), id);
    }

    public long maxSyncId() {
        return db.queryLong("SELECT COALESCE(MAX(id), 0) FROM " + db.table("sync"));
    }

    public int pruneSync(long olderThan) {
        return db.update("DELETE FROM " + db.table("sync") + " WHERE created_at < ?", olderThan);
    }

    // ------------------------------------------------------------------ meta

    public String meta(String key) {
        return db.queryOne("SELECT meta_value FROM " + db.table("meta") + " WHERE meta_key = ?", rs -> rs.getString(1), key);
    }

    public void setMeta(String key, String value) {
        db.update("DELETE FROM " + db.table("meta") + " WHERE meta_key = ?", key);
        if (value != null) {
            db.update("INSERT INTO " + db.table("meta") + " (meta_key, meta_value) VALUES (?, ?)", key, value);
        }
    }
}
