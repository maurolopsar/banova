package dev.banova.core.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.banova.core.config.YamlConfig;
import dev.banova.core.platform.BanovaLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Connection pool, schema migrations and small JDBC helpers.
 * Uses a SQL subset that works on H2 (MySQL mode), MySQL and MariaDB.
 */
public final class Database implements AutoCloseable {

    public static final int SCHEMA_VERSION = 1;

    private final HikariDataSource dataSource;
    private final String prefix;
    private final boolean shared;
    private final String kind;

    public Database(YamlConfig storage, Path dataFolder, BanovaLogger logger) throws SQLException {
        String type = storage.getString("type", "h2").toLowerCase(Locale.ROOT).trim();
        String rawPrefix = storage.getString("table-prefix", "banova_");
        this.prefix = rawPrefix.replaceAll("[^A-Za-z0-9_]", "");
        HikariConfig config = new HikariConfig();
        config.setPoolName("Banova");
        config.setConnectionTimeout(10_000);
        config.setMaxLifetime(1_500_000);
        config.setKeepaliveTime(300_000);
        switch (type) {
            case "mysql", "mariadb" -> {
                this.shared = true;
                this.kind = type;
                String host = storage.getString("host", "localhost");
                int port = storage.getInt("port", 3306);
                String db = storage.getString("database", "banova");
                StringBuilder url = new StringBuilder("jdbc:mariadb://").append(host).append(':').append(port).append('/').append(db)
                        .append("?useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true");
                YamlConfig props = storage.section("properties");
                for (String key : props.keys("")) {
                    url.append('&').append(key).append('=').append(props.getString(key));
                }
                config.setJdbcUrl(url.toString());
                config.setUsername(storage.getString("username", "root"));
                config.setPassword(storage.getString("password", ""));
                config.setDriverClassName(org.mariadb.jdbc.Driver.class.getName());
                config.setMaximumPoolSize(Math.max(2, storage.getInt("pool-size", 6)));
            }
            default -> {
                this.shared = false;
                this.kind = "h2";
                Path dir = dataFolder.resolve("data");
                try {
                    Files.createDirectories(dir);
                } catch (Exception e) {
                    throw new SQLException("Cannot create data folder " + dir, e);
                }
                String file = dir.resolve("banova").toAbsolutePath().toString().replace('\\', '/');
                config.setJdbcUrl("jdbc:h2:file:" + file
                        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_ON_EXIT=FALSE");
                config.setUsername("sa");
                config.setPassword("");
                config.setDriverClassName(org.h2.Driver.class.getName());
                config.setMaximumPoolSize(Math.max(2, Math.min(4, storage.getInt("pool-size", 4))));
            }
        }
        // Make sure the shaded driver is loaded through our class loader.
        Thread current = Thread.currentThread();
        ClassLoader previous = current.getContextClassLoader();
        current.setContextClassLoader(Database.class.getClassLoader());
        try {
            this.dataSource = new HikariDataSource(config);
        } catch (RuntimeException e) {
            throw new SQLException("Could not connect to the " + kind + " database: " + rootMessage(e), e);
        } finally {
            current.setContextClassLoader(previous);
        }
        migrate(logger);
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }

    /** True when several servers can share this database (network mode). */
    public boolean isShared() {
        return shared;
    }

    public String kind() {
        return kind;
    }

    public String table(String name) {
        return prefix + name;
    }

    public Connection connection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        if (!shared) {
            try (Connection c = connection(); Statement s = c.createStatement()) {
                s.execute("CHECKPOINT");
            } catch (SQLException ignored) {
                // best effort
            }
        }
        dataSource.close();
    }

    // ------------------------------------------------------------------ helpers

    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapper.map(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new StorageException(sql, e);
        }
    }

    public <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? null : list.get(0);
    }

    public long queryLong(String sql, Object... params) {
        Long value = queryOne(sql, rs -> rs.getLong(1), params);
        return value == null ? 0 : value;
    }

    public int update(String sql, Object... params) {
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException(sql, e);
        }
    }

    /** Executes an insert and returns the generated id. */
    public long insert(String sql, Object... params) {
        try (Connection c = connection()) {
            return insert(c, sql, params);
        } catch (SQLException e) {
            throw new StorageException(sql, e);
        }
    }

    static long insert(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new SQLException("Insert did not return an ID");
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p instanceof java.util.UUID uuid) {
                ps.setString(i + 1, uuid.toString());
            } else if (p instanceof Enum<?> e) {
                ps.setString(i + 1, e.name());
            } else if (p == null) {
                ps.setNull(i + 1, java.sql.Types.VARCHAR);
            } else {
                ps.setObject(i + 1, p);
            }
        }
    }

    public static java.util.UUID uuid(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return java.util.UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ schema

    private void migrate(BanovaLogger logger) throws SQLException {
        int current = currentVersion();
        if (current >= SCHEMA_VERSION) {
            return;
        }
        if (current < 1) {
            logger.info("Creating database tables (" + kind + ")...");
            for (String statement : schemaV1()) {
                try (Connection c = connection(); Statement s = c.createStatement()) {
                    s.execute(statement);
                }
            }
        }
        update("DELETE FROM " + table("meta") + " WHERE meta_key = ?", "schema_version");
        update("INSERT INTO " + table("meta") + " (meta_key, meta_value) VALUES (?, ?)", "schema_version", String.valueOf(SCHEMA_VERSION));
    }

    private int currentVersion() throws SQLException {
        try (Connection c = connection()) {
            boolean exists;
            try (ResultSet rs = c.getMetaData().getTables(null, null, table("meta"), null)) {
                exists = rs.next();
            }
            if (!exists) {
                try (ResultSet rs = c.getMetaData().getTables(null, null, table("meta").toUpperCase(Locale.ROOT), null)) {
                    exists = rs.next();
                }
            }
            if (!exists) {
                return 0;
            }
        }
        String value = queryOne("SELECT meta_value FROM " + table("meta") + " WHERE meta_key = ?", rs -> rs.getString(1), "schema_version");
        try {
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<String> schemaV1() {
        String suffix = shared ? " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci" : "";
        String p = prefix;
        List<String> sql = new ArrayList<>();
        sql.add("CREATE TABLE IF NOT EXISTS " + p + "meta (meta_key VARCHAR(64) NOT NULL PRIMARY KEY, meta_value VARCHAR(1024))" + suffix);
        sql.add("CREATE TABLE IF NOT EXISTS " + p + "players (uuid CHAR(36) NOT NULL PRIMARY KEY, name VARCHAR(16) NOT NULL,"
                + " name_lower VARCHAR(16) NOT NULL, last_ip VARCHAR(45), first_seen BIGINT NOT NULL, last_seen BIGINT NOT NULL)" + suffix);
        sql.add("CREATE INDEX " + p + "players_name ON " + p + "players (name_lower)");
        sql.add("CREATE TABLE IF NOT EXISTS " + p + "ips (uuid CHAR(36) NOT NULL, ip VARCHAR(45) NOT NULL, first_seen BIGINT NOT NULL,"
                + " last_seen BIGINT NOT NULL, PRIMARY KEY (uuid, ip))" + suffix);
        sql.add("CREATE INDEX " + p + "ips_ip ON " + p + "ips (ip)");
        sql.add("CREATE TABLE IF NOT EXISTS " + p + "punishments (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + " type VARCHAR(8) NOT NULL, target_uuid CHAR(36), target_name VARCHAR(45), target_ip VARCHAR(45),"
                + " ip_based BOOLEAN NOT NULL DEFAULT FALSE, reason VARCHAR(512) NOT NULL, actor_uuid CHAR(36),"
                + " actor_name VARCHAR(32) NOT NULL, created_at BIGINT NOT NULL, expires_at BIGINT NOT NULL DEFAULT 0,"
                + " active BOOLEAN NOT NULL, revoked_by_uuid CHAR(36), revoked_by_name VARCHAR(32),"
                + " revoked_at BIGINT NOT NULL DEFAULT 0, revoke_reason VARCHAR(512), server_name VARCHAR(64),"
                + " silent BOOLEAN NOT NULL DEFAULT FALSE, template VARCHAR(64), template_step INT NOT NULL DEFAULT -1,"
                + " appeal_code VARCHAR(16), import_ref VARCHAR(96))" + suffix);
        sql.add("CREATE INDEX " + p + "pun_target ON " + p + "punishments (target_uuid)");
        sql.add("CREATE INDEX " + p + "pun_ip ON " + p + "punishments (target_ip)");
        sql.add("CREATE INDEX " + p + "pun_type_active ON " + p + "punishments (type, active)");
        sql.add("CREATE INDEX " + p + "pun_actor ON " + p + "punishments (actor_uuid)");
        sql.add("CREATE INDEX " + p + "pun_created ON " + p + "punishments (created_at)");
        sql.add("CREATE UNIQUE INDEX " + p + "pun_appeal ON " + p + "punishments (appeal_code)");
        sql.add("CREATE INDEX " + p + "pun_import ON " + p + "punishments (import_ref)");
        sql.add("CREATE TABLE IF NOT EXISTS " + p + "evidence (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + " punishment_id BIGINT NOT NULL, content VARCHAR(1024) NOT NULL, actor_uuid CHAR(36),"
                + " actor_name VARCHAR(32) NOT NULL, created_at BIGINT NOT NULL)" + suffix);
        sql.add("CREATE INDEX " + p + "evidence_pun ON " + p + "evidence (punishment_id)");
        sql.add("CREATE TABLE IF NOT EXISTS " + p + "notes (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + " target_uuid CHAR(36) NOT NULL, content VARCHAR(1024) NOT NULL, actor_uuid CHAR(36),"
                + " actor_name VARCHAR(32) NOT NULL, created_at BIGINT NOT NULL)" + suffix);
        sql.add("CREATE INDEX " + p + "notes_target ON " + p + "notes (target_uuid)");
        sql.add("CREATE TABLE IF NOT EXISTS " + p + "appeals (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + " punishment_id BIGINT NOT NULL, status VARCHAR(10) NOT NULL, message VARCHAR(2000) NOT NULL,"
                + " contact VARCHAR(128), source VARCHAR(8) NOT NULL, created_at BIGINT NOT NULL, handled_by_uuid CHAR(36),"
                + " handled_by_name VARCHAR(32), handled_at BIGINT NOT NULL DEFAULT 0, response VARCHAR(1000),"
                + " creator_ip VARCHAR(45))" + suffix);
        sql.add("CREATE INDEX " + p + "appeals_pun ON " + p + "appeals (punishment_id)");
        sql.add("CREATE INDEX " + p + "appeals_status ON " + p + "appeals (status)");
        sql.add("CREATE TABLE IF NOT EXISTS " + p + "audit (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + " created_at BIGINT NOT NULL, actor_uuid CHAR(36), actor_name VARCHAR(32) NOT NULL, source VARCHAR(8) NOT NULL,"
                + " action VARCHAR(32) NOT NULL, target_uuid CHAR(36), target_name VARCHAR(45),"
                + " punishment_id BIGINT NOT NULL DEFAULT 0, details VARCHAR(1024))" + suffix);
        sql.add("CREATE INDEX " + p + "audit_created ON " + p + "audit (created_at)");
        sql.add("CREATE INDEX " + p + "audit_actor ON " + p + "audit (actor_uuid)");
        sql.add("CREATE INDEX " + p + "audit_target ON " + p + "audit (target_uuid)");
        sql.add("CREATE TABLE IF NOT EXISTS " + p + "web_sessions (id VARCHAR(16) NOT NULL PRIMARY KEY, token_hash CHAR(64) NOT NULL,"
                + " uuid CHAR(36), name VARCHAR(32) NOT NULL, permissions VARCHAR(2048) NOT NULL, created_at BIGINT NOT NULL,"
                + " last_used BIGINT NOT NULL, expires_at BIGINT NOT NULL, remember BOOLEAN NOT NULL, ip VARCHAR(45),"
                + " user_agent VARCHAR(256))" + suffix);
        sql.add("CREATE UNIQUE INDEX " + p + "sessions_token ON " + p + "web_sessions (token_hash)");
        sql.add("CREATE TABLE IF NOT EXISTS " + p + "sync (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, created_at BIGINT NOT NULL,"
                + " origin VARCHAR(64) NOT NULL, type VARCHAR(16) NOT NULL, data VARCHAR(1024))" + suffix);
        return sql;
    }
}
