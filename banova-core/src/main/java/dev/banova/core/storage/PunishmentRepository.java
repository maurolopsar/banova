package dev.banova.core.storage;

import dev.banova.api.PunishmentType;
import dev.banova.core.model.Punishment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PunishmentRepository {

    private static final String COLUMNS = "id, type, target_uuid, target_name, target_ip, ip_based, reason, actor_uuid, actor_name,"
            + " created_at, expires_at, active, revoked_by_uuid, revoked_by_name, revoked_at, revoke_reason, server_name, silent,"
            + " template, template_step, appeal_code, import_ref";

    private final Database db;
    private final String t;

    public PunishmentRepository(Database db) {
        this.db = db;
        this.t = db.table("punishments");
    }

    static Punishment map(ResultSet rs) throws SQLException {
        Punishment p = new Punishment();
        p.id = rs.getLong("id");
        p.type = PunishmentType.valueOf(rs.getString("type"));
        p.targetUuid = Database.uuid(rs, "target_uuid");
        p.targetName = rs.getString("target_name");
        p.targetIp = rs.getString("target_ip");
        p.ipBased = rs.getBoolean("ip_based");
        p.reason = rs.getString("reason");
        p.actorUuid = Database.uuid(rs, "actor_uuid");
        p.actorName = rs.getString("actor_name");
        p.createdAt = rs.getLong("created_at");
        p.expiresAt = rs.getLong("expires_at");
        p.active = rs.getBoolean("active");
        p.revokedByUuid = Database.uuid(rs, "revoked_by_uuid");
        p.revokedByName = rs.getString("revoked_by_name");
        p.revokedAt = rs.getLong("revoked_at");
        p.revokeReason = rs.getString("revoke_reason");
        p.server = rs.getString("server_name");
        p.silent = rs.getBoolean("silent");
        p.template = rs.getString("template");
        p.templateStep = rs.getInt("template_step");
        p.appealCode = rs.getString("appeal_code");
        p.importRef = rs.getString("import_ref");
        return p;
    }

    public long insert(Punishment p) {
        return insert(p, "");
    }

    public long insert(Punishment p, String context) {
        try (var connection = db.connection()) {
            connection.setAutoCommit(false);
            try {
                long id = Database.insert(connection, "INSERT INTO " + t + " (type, target_uuid, target_name, target_ip, ip_based, reason, actor_uuid, actor_name,"
                        + " created_at, expires_at, active, revoked_by_uuid, revoked_by_name, revoked_at, revoke_reason, server_name, silent,"
                        + " template, template_step, appeal_code, import_ref) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        p.type, p.targetUuid, p.targetName, p.targetIp, p.ipBased, p.reason, p.actorUuid, p.actorName,
                        p.createdAt, p.expiresAt, p.active, p.revokedByUuid, p.revokedByName, p.revokedAt, p.revokeReason, p.server,
                        p.silent, p.template, p.templateStep, p.appealCode, p.importRef);
                if (context != null && !context.isBlank()) {
                    Database.insert(connection, "INSERT INTO " + db.table("evidence")
                            + " (punishment_id, content, actor_uuid, actor_name, created_at) VALUES (?,?,?,?,?)",
                            id, context, p.actorUuid, p.actorName, p.createdAt);
                }
                connection.commit();
                p.id = id;
                return id;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new StorageException("Insert punishment", e);
        }
    }

    public Punishment byId(long id) {
        return db.queryOne("SELECT " + COLUMNS + " FROM " + t + " WHERE id = ?", PunishmentRepository::map, id);
    }

    public Punishment byAppealCode(String code) {
        return db.queryOne("SELECT " + COLUMNS + " FROM " + t + " WHERE appeal_code = ?", PunishmentRepository::map, code);
    }

    public boolean appealCodeExists(String code) {
        return db.queryLong("SELECT COUNT(*) FROM " + t + " WHERE appeal_code = ?", code) > 0;
    }

    public boolean importRefExists(String ref) {
        return db.queryLong("SELECT COUNT(*) FROM " + t + " WHERE import_ref = ?", ref) > 0;
    }

    /** Active punishments of a type affecting a player directly or through their IP. Newest first. */
    public List<Punishment> active(PunishmentType type, UUID uuid, String ip) {
        long now = System.currentTimeMillis();
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM " + t
                + " WHERE type = ? AND active = TRUE AND (expires_at = 0 OR expires_at > ?) AND (");
        List<Object> params = new ArrayList<>(List.of(type, now));
        boolean any = false;
        if (uuid != null) {
            sql.append("target_uuid = ?");
            params.add(uuid);
            any = true;
        }
        if (ip != null && !ip.isEmpty()) {
            if (any) {
                sql.append(" OR ");
            }
            sql.append("(ip_based = TRUE AND target_ip = ?)");
            params.add(ip);
            any = true;
        }
        if (!any) {
            return new ArrayList<>();
        }
        sql.append(") ORDER BY created_at DESC");
        return db.query(sql.toString(), PunishmentRepository::map, params.toArray());
    }

    /** Active IP punishment for an exact IP (used by /unban 1.2.3.4). */
    public List<Punishment> activeByIp(PunishmentType type, String ip) {
        long now = System.currentTimeMillis();
        return db.query("SELECT " + COLUMNS + " FROM " + t + " WHERE type = ? AND active = TRUE AND (expires_at = 0 OR expires_at > ?)"
                + " AND target_ip = ? AND ip_based = TRUE ORDER BY created_at DESC", PunishmentRepository::map, type, now, ip);
    }

    public List<Punishment> history(UUID uuid, PunishmentType type, int limit, int offset) {
        if (type == null) {
            return db.query("SELECT " + COLUMNS + " FROM " + t + " WHERE target_uuid = ? ORDER BY created_at DESC LIMIT ? OFFSET ?",
                    PunishmentRepository::map, uuid, limit, offset);
        }
        return db.query("SELECT " + COLUMNS + " FROM " + t + " WHERE target_uuid = ? AND type = ? ORDER BY created_at DESC LIMIT ? OFFSET ?",
                PunishmentRepository::map, uuid, type, limit, offset);
    }

    public long countHistory(UUID uuid, PunishmentType type) {
        if (type == null) {
            return db.queryLong("SELECT COUNT(*) FROM " + t + " WHERE target_uuid = ?", uuid);
        }
        return db.queryLong("SELECT COUNT(*) FROM " + t + " WHERE target_uuid = ? AND type = ?", uuid, type);
    }

    public List<Punishment> byActor(UUID actor, String actorName, long since, int limit, int offset) {
        if (actor == null) {
            return db.query("SELECT " + COLUMNS + " FROM " + t + " WHERE actor_uuid IS NULL AND actor_name = ? AND created_at >= ?"
                    + " ORDER BY created_at DESC LIMIT ? OFFSET ?", PunishmentRepository::map, actorName, since, limit, offset);
        }
        return db.query("SELECT " + COLUMNS + " FROM " + t + " WHERE actor_uuid = ? AND created_at >= ? ORDER BY created_at DESC LIMIT ? OFFSET ?",
                PunishmentRepository::map, actor, since, limit, offset);
    }

    public long countByActor(UUID actor, String actorName) {
        if (actor == null) {
            return db.queryLong("SELECT COUNT(*) FROM " + t + " WHERE actor_uuid IS NULL AND actor_name = ?", actorName);
        }
        return db.queryLong("SELECT COUNT(*) FROM " + t + " WHERE actor_uuid = ?", actor);
    }

    public List<Punishment> routeHistory(UUID target, String template, int limit, int offset) {
        return db.query("SELECT " + COLUMNS + " FROM " + t + " WHERE target_uuid = ? AND template = ?"
                + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?", PunishmentRepository::map, target, template, limit, offset);
    }

    public record Progress(long offenses, int highestIndex) {}

    public Progress progress(UUID target, String template, long since) {
        return db.queryOne("SELECT COUNT(*), COALESCE(MAX(template_step), -1) FROM " + t
                + " WHERE target_uuid = ? AND template = ? AND revoked_at = 0 AND created_at >= ?",
                rs -> new Progress(rs.getLong(1), rs.getInt(2)), target, template, since);
    }

    public java.util.Map<String, Progress> progress(UUID target, java.util.Map<String, Long> cutoffs) {
        if (cutoffs.isEmpty()) return java.util.Map.of();
        List<Object> args = new ArrayList<>();
        args.add(target);
        var conditions = new java.util.StringJoiner(" OR ");
        cutoffs.forEach((id, since) -> {
            conditions.add("(template = ? AND created_at >= ?)");
            args.add(id);
            args.add(since);
        });
        var rows = db.query("SELECT template, COUNT(*), MAX(template_step) FROM " + t
                        + " WHERE target_uuid = ? AND revoked_at = 0 AND (" + conditions + ") GROUP BY template",
                rs -> java.util.Map.entry(rs.getString(1), new Progress(rs.getLong(2), rs.getInt(3))), args.toArray());
        var result = new java.util.HashMap<String, Progress>();
        rows.forEach(row -> result.put(row.getKey(), row.getValue()));
        return result;
    }

    /** Offenses counted for escalation: same template, not revoked, created after {@code since}. */
    public long countTemplateOffenses(UUID uuid, String template, long since) {
        return db.queryLong("SELECT COUNT(*) FROM " + t + " WHERE target_uuid = ? AND template = ? AND revoked_at = 0 AND created_at >= ?",
                uuid, template, since);
    }

    /** Generic filtered listing used by lists, menus and the web panel. */
    public List<Punishment> search(Filter filter, int limit, int offset) {
        Filter.Query q = filter.toWhere(t);
        List<Object> params = new ArrayList<>(q.params);
        params.add(limit);
        params.add(offset);
        return db.query("SELECT " + COLUMNS + " FROM " + t + q.where + " ORDER BY created_at DESC LIMIT ? OFFSET ?",
                PunishmentRepository::map, params.toArray());
    }

    public long count(Filter filter) {
        Filter.Query q = filter.toWhere(t);
        return db.queryLong("SELECT COUNT(*) FROM " + t + q.where, q.params.toArray());
    }

    public boolean revoke(long id, UUID byUuid, String byName, String reason) {
        return db.update("UPDATE " + t + " SET active = FALSE, revoked_by_uuid = ?, revoked_by_name = ?, revoked_at = ?, revoke_reason = ?"
                + " WHERE id = ? AND revoked_at = 0", byUuid, byName, System.currentTimeMillis(), reason, id) > 0;
    }

    public void updateReason(long id, String reason) {
        db.update("UPDATE " + t + " SET reason = ? WHERE id = ?", reason, id);
    }

    public void updateExpiry(long id, long expiresAt) {
        db.update("UPDATE " + t + " SET expires_at = ?, active = ? WHERE id = ? AND revoked_at = 0",
                expiresAt, expiresAt == 0 || expiresAt > System.currentTimeMillis(), id);
    }

    /** Marks expired punishments inactive so "active" stays meaningful in raw queries. */
    public int deactivateExpired() {
        return db.update("UPDATE " + t + " SET active = FALSE WHERE active = TRUE AND expires_at > 0 AND expires_at <= ?",
                System.currentTimeMillis());
    }

    public long countSince(long since) {
        return db.queryLong("SELECT COUNT(*) FROM " + t + " WHERE created_at >= ?", since);
    }

    /** Search filter. Any field may be null. */
    public static final class Filter {
        public PunishmentType type;
        public Boolean activeOnly;
        public UUID targetUuid;
        public UUID actorUuid;
        public String actorName;
        public String nameQuery;
        public long since;

        record Query(String where, List<Object> params) {
        }

        Query toWhere(String table) {
            StringBuilder where = new StringBuilder();
            List<Object> params = new ArrayList<>();
            if (type != null) {
                add(where, "type = ?");
                params.add(type);
            }
            if (Boolean.TRUE.equals(activeOnly)) {
                add(where, "active = TRUE AND (expires_at = 0 OR expires_at > ?) AND type IN ('BAN','MUTE','WARN')");
                params.add(System.currentTimeMillis());
            }
            if (targetUuid != null) {
                add(where, "target_uuid = ?");
                params.add(targetUuid);
            }
            if (actorUuid != null) {
                add(where, "actor_uuid = ?");
                params.add(actorUuid);
            } else if (actorName != null && !actorName.isBlank()) {
                add(where, "LOWER(actor_name) = ?");
                params.add(actorName.toLowerCase(java.util.Locale.ROOT));
            }
            if (nameQuery != null && !nameQuery.isBlank()) {
                String q = nameQuery.trim().toLowerCase(java.util.Locale.ROOT);
                if (q.startsWith("#")) {
                    add(where, "id = ?");
                    try {
                        params.add(Long.parseLong(q.substring(1)));
                    } catch (NumberFormatException e) {
                        params.add(-1L);
                    }
                } else {
                    String like = "%" + q.replace("\\", "").replace("%", "").replace("_", "\\_") + "%";
                    add(where, "(LOWER(target_name) LIKE ? OR LOWER(reason) LIKE ? OR target_ip = ?)");
                    params.add(like);
                    params.add(like);
                    params.add(q);
                }
            }
            if (since > 0) {
                add(where, "created_at >= ?");
                params.add(since);
            }
            return new Query(where.length() == 0 ? "" : " WHERE " + where, params);
        }

        private static void add(StringBuilder where, String clause) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(clause);
        }
    }
}
