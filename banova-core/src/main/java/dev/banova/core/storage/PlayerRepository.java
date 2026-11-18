package dev.banova.core.storage;

import dev.banova.core.model.PlayerRecord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class PlayerRepository {

    private final Database db;
    private final String players;
    private final String ips;

    public PlayerRepository(Database db) {
        this.db = db;
        this.players = db.table("players");
        this.ips = db.table("ips");
    }

    private static PlayerRecord map(ResultSet rs) throws SQLException {
        return new PlayerRecord(Database.uuid(rs, "uuid"), rs.getString("name"), rs.getString("last_ip"),
                rs.getLong("first_seen"), rs.getLong("last_seen"));
    }

    /** Records a login: name, last IP and IP history. */
    public void recordLogin(UUID uuid, String name, String ip) {
        long now = System.currentTimeMillis();
        String lower = name.toLowerCase(Locale.ROOT);
        int updated = db.update("UPDATE " + players + " SET name = ?, name_lower = ?, last_ip = ?, last_seen = ? WHERE uuid = ?",
                name, lower, ip, now, uuid);
        if (updated == 0) {
            try {
                db.update("INSERT INTO " + players + " (uuid, name, name_lower, last_ip, first_seen, last_seen) VALUES (?,?,?,?,?,?)",
                        uuid, name, lower, ip, now, now);
            } catch (StorageException e) {
                // Another server inserted at the same time.
                db.update("UPDATE " + players + " SET name = ?, name_lower = ?, last_ip = ?, last_seen = ? WHERE uuid = ?",
                        name, lower, ip, now, uuid);
            }
        }
        if (ip == null || ip.isEmpty()) {
            return;
        }
        int ipUpdated = db.update("UPDATE " + ips + " SET last_seen = ? WHERE uuid = ? AND ip = ?", now, uuid, ip);
        if (ipUpdated == 0) {
            try {
                db.update("INSERT INTO " + ips + " (uuid, ip, first_seen, last_seen) VALUES (?,?,?,?)", uuid, ip, now, now);
            } catch (StorageException ignored) {
                // duplicate from a concurrent login
            }
        }
    }

    /** Inserts a player only if unknown (used by importers). */
    public void ensure(UUID uuid, String name, long seen) {
        if (uuid == null || name == null) {
            return;
        }
        if (byUuid(uuid) != null) {
            return;
        }
        try {
            db.update("INSERT INTO " + players + " (uuid, name, name_lower, last_ip, first_seen, last_seen) VALUES (?,?,?,?,?,?)",
                    uuid, name, name.toLowerCase(Locale.ROOT), null, seen, seen);
        } catch (StorageException ignored) {
            // already there
        }
    }

    public PlayerRecord byUuid(UUID uuid) {
        return db.queryOne("SELECT * FROM " + players + " WHERE uuid = ?", PlayerRepository::map, uuid);
    }

    public PlayerRecord byName(String name) {
        return db.queryOne("SELECT * FROM " + players + " WHERE name_lower = ? ORDER BY last_seen DESC LIMIT 1",
                PlayerRepository::map, name.toLowerCase(Locale.ROOT));
    }

    public List<PlayerRecord> searchByPrefix(String prefix, int limit) {
        String like = prefix.toLowerCase(Locale.ROOT).replace("%", "").replace("_", "\\_") + "%";
        return db.query("SELECT * FROM " + players + " WHERE name_lower LIKE ? ORDER BY last_seen DESC LIMIT ?",
                PlayerRepository::map, like, limit);
    }

    public List<PlayerRecord> recent(int limit) {
        return db.query("SELECT * FROM " + players + " ORDER BY last_seen DESC LIMIT ?", PlayerRepository::map, limit);
    }

    /** Other accounts that used any IP this player used. */
    public List<PlayerRecord> alts(UUID uuid) {
        return db.query("SELECT DISTINCT p.* FROM " + players + " p JOIN " + ips + " a ON a.uuid = p.uuid"
                        + " WHERE a.ip IN (SELECT ip FROM " + ips + " WHERE uuid = ?) AND p.uuid <> ? ORDER BY p.last_seen DESC LIMIT 100",
                PlayerRepository::map, uuid, uuid);
    }

    public List<PlayerRecord> byIp(String ip) {
        return db.query("SELECT DISTINCT p.* FROM " + players + " p JOIN " + ips + " a ON a.uuid = p.uuid WHERE a.ip = ?"
                + " ORDER BY p.last_seen DESC LIMIT 100", PlayerRepository::map, ip);
    }

    public record IpEntry(String ip, long firstSeen, long lastSeen) {
    }

    public List<IpEntry> ips(UUID uuid) {
        return db.query("SELECT ip, first_seen, last_seen FROM " + ips + " WHERE uuid = ? ORDER BY last_seen DESC LIMIT 50",
                rs -> new IpEntry(rs.getString("ip"), rs.getLong("first_seen"), rs.getLong("last_seen")), uuid);
    }

    public long count() {
        return db.queryLong("SELECT COUNT(*) FROM " + players);
    }
}
