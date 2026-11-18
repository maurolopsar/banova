package dev.banova.core.importer;

import dev.banova.api.PunishmentType;
import dev.banova.core.BanovaCore;
import dev.banova.core.config.YamlConfig;
import dev.banova.core.util.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * LiteBans (MySQL/MariaDB storage). Connection details are read from LiteBans' own config.yml,
 * or from the {@code import} section of Banova's config.yml.
 */
public final class LiteBansImporter implements Importer {

    @Override
    public String id() {
        return "litebans";
    }

    @Override
    public String displayName() {
        return "LiteBans";
    }

    @Override
    public List<Imported> read(BanovaCore core) throws ImportException {
        String host;
        int port;
        String database;
        String user;
        String password;
        String prefix;
        Path cfg = find(core.platform().serverRoot().resolve("plugins"));
        if (cfg != null) {
            YamlConfig lb;
            try {
                lb = YamlConfig.load(cfg, null);
            } catch (Exception e) {
                throw new ImportException("Cannot read " + cfg + ": " + e.getMessage(), e);
            }
            String driver = lb.getString("sql.driver", "h2").toLowerCase(Locale.ROOT);
            if (!driver.contains("mysql") && !driver.contains("maria")) {
                throw new ImportException(core.messages().raw("import.litebans-h2"));
            }
            String address = lb.getString("sql.address", "localhost:3306");
            int colon = address.lastIndexOf(':');
            host = colon > 0 ? address.substring(0, colon) : address;
            port = colon > 0 ? parseInt(address.substring(colon + 1), 3306) : 3306;
            database = lb.getString("sql.database", "litebans");
            user = lb.getString("sql.username", "root");
            password = lb.getString("sql.password", "");
            prefix = lb.getString("sql.table_prefix", "litebans_");
        } else {
            YamlConfig manual = core.config().section("import");
            host = manual.getString("host", "localhost");
            port = manual.getInt("port", 3306);
            database = manual.getString("database", "litebans");
            user = manual.getString("username", "root");
            password = manual.getString("password", "");
            prefix = manual.getString("table-prefix", "litebans_");
        }
        prefix = prefix.replaceAll("[^A-Za-z0-9_]", "");

        List<Imported> out = new ArrayList<>();
        try (Connection c = SqlSource.open(host, port, database, user, password); Statement s = c.createStatement()) {
            Map<String, String> names = new HashMap<>();
            try (ResultSet rs = s.executeQuery("SELECT uuid, name FROM " + prefix + "history ORDER BY date ASC")) {
                while (rs.next()) {
                    String uuid = rs.getString(1);
                    String name = rs.getString(2);
                    if (uuid != null && name != null) {
                        names.put(uuid, name);
                    }
                }
            } catch (SQLException e) {
                // Old versions without a history table: names stay unknown.
            }
            readTable(s, prefix + "bans", PunishmentType.BAN, names, out);
            readTable(s, prefix + "mutes", PunishmentType.MUTE, names, out);
            readTable(s, prefix + "warnings", PunishmentType.WARN, names, out);
            readTable(s, prefix + "kicks", PunishmentType.KICK, names, out);
        } catch (SQLException e) {
            throw new ImportException("LiteBans query failed: " + e.getMessage(), e);
        }
        if (out.isEmpty()) {
            throw new ImportException("No LiteBans punishments found (prefix '" + prefix + "')");
        }
        return out;
    }

    private static Path find(Path plugins) {
        for (String folder : new String[]{"LiteBans", "litebans"}) {
            Path p = plugins.resolve(folder).resolve("config.yml");
            if (Files.exists(p)) {
                return p;
            }
        }
        return null;
    }

    private static void readTable(Statement s, String table, PunishmentType type, Map<String, String> names, List<Imported> out) throws SQLException {
        try (ResultSet rs = s.executeQuery("SELECT * FROM " + table)) {
            Set<String> cols = SqlSource.columns(rs);
            while (rs.next()) {
                Imported e = new Imported();
                e.type = type;
                String rawUuid = SqlSource.str(rs, cols, "uuid");
                e.uuid = Text.parseUuid(rawUuid);
                e.name = rawUuid == null ? null : names.get(rawUuid);
                e.ip = SqlSource.str(rs, cols, "ip");
                if (e.ip != null && (e.ip.equals("#") || e.ip.startsWith("#") || !Text.isIp(e.ip))) {
                    e.ip = null;
                }
                e.ipBased = SqlSource.bool(rs, cols, "ipban") && e.ip != null;
                if (e.name == null) {
                    e.name = e.uuid == null ? e.ip : e.uuid.toString().substring(0, 8);
                }
                e.reason = SqlSource.str(rs, cols, "reason");
                e.actorUuid = Text.parseUuid(SqlSource.str(rs, cols, "banned_by_uuid"));
                e.actorName = SqlSource.str(rs, cols, "banned_by_name");
                e.createdAt = SqlSource.num(rs, cols, "time");
                long until = SqlSource.num(rs, cols, "until");
                e.expiresAt = until <= 0 ? 0 : until;
                e.active = type.isLasting() && SqlSource.bool(rs, cols, "active");
                String removedBy = SqlSource.str(rs, cols, "removed_by_name");
                if (removedBy != null && !removedBy.isBlank() && !removedBy.equals("#expired")) {
                    e.revokedByName = removedBy;
                    long removedAt = SqlSource.num(rs, cols, "removed_by_date");
                    e.revokedAt = removedAt > 0 ? removedAt : e.createdAt;
                    String removeReason = SqlSource.str(rs, cols, "removed_by_reason");
                    e.revokeReason = removeReason == null || removeReason.isBlank() ? "Removed in LiteBans" : removeReason;
                    e.active = false;
                }
                e.silent = SqlSource.bool(rs, cols, "silent");
                e.server = SqlSource.str(rs, cols, "server_origin");
                e.ref = "litebans:" + type.name().toLowerCase(Locale.ROOT) + ":" + SqlSource.num(rs, cols, "id");
                if (e.uuid == null && e.ip == null) {
                    continue;
                }
                out.add(e);
            }
        } catch (SQLException ex) {
            if (type == PunishmentType.BAN) {
                throw ex;
            }
            // Optional tables (kicks/warnings) may not exist on very old versions.
        }
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
