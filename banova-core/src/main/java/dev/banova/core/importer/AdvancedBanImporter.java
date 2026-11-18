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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * AdvancedBan (MySQL storage). Reads plugins/AdvancedBan/MySQL.yml; notes become Banova notes.
 */
public final class AdvancedBanImporter implements Importer {

    @Override
    public String id() {
        return "advancedban";
    }

    @Override
    public String displayName() {
        return "AdvancedBan";
    }

    @Override
    public List<Imported> read(BanovaCore core) throws ImportException {
        Path folder = core.platform().serverRoot().resolve("plugins").resolve("AdvancedBan");
        String host;
        int port;
        String database;
        String user;
        String password;
        if (Files.exists(folder.resolve("config.yml"))) {
            YamlConfig cfg;
            YamlConfig mysql;
            try {
                cfg = YamlConfig.load(folder.resolve("config.yml"), null);
                mysql = Files.exists(folder.resolve("MySQL.yml")) ? YamlConfig.load(folder.resolve("MySQL.yml"), null) : YamlConfig.empty();
            } catch (Exception e) {
                throw new ImportException("Cannot read AdvancedBan config: " + e.getMessage(), e);
            }
            if (!cfg.getBoolean("UseMySQL", false)) {
                throw new ImportException(core.messages().raw("import.advancedban-local"));
            }
            host = mysql.getString("MySQL.IP", "localhost");
            port = mysql.getInt("MySQL.Port", 3306);
            database = mysql.getString("MySQL.DB-Name", "advancedban");
            user = mysql.getString("MySQL.Username", "root");
            password = mysql.getString("MySQL.Password", "");
        } else {
            YamlConfig manual = core.config().section("import");
            host = manual.getString("host", "localhost");
            port = manual.getInt("port", 3306);
            database = manual.getString("database", "advancedban");
            user = manual.getString("username", "root");
            password = manual.getString("password", "");
        }

        List<Imported> out = new ArrayList<>();
        try (Connection c = SqlSource.open(host, port, database, user, password); Statement s = c.createStatement()) {
            Set<String> activeKeys = new HashSet<>();
            try (ResultSet rs = s.executeQuery("SELECT * FROM Punishments")) {
                Set<String> cols = SqlSource.columns(rs);
                while (rs.next()) {
                    activeKeys.add(key(rs, cols));
                }
            }
            try (ResultSet rs = s.executeQuery("SELECT * FROM PunishmentHistory")) {
                Set<String> cols = SqlSource.columns(rs);
                while (rs.next()) {
                    Imported e = map(rs, cols, activeKeys.contains(key(rs, cols)));
                    if (e != null) {
                        out.add(e);
                    }
                }
            }
        } catch (SQLException e) {
            throw new ImportException("AdvancedBan query failed: " + e.getMessage(), e);
        }
        if (out.isEmpty()) {
            throw new ImportException("No AdvancedBan punishments found");
        }
        return out;
    }

    private static String key(ResultSet rs, Set<String> cols) throws SQLException {
        return SqlSource.str(rs, cols, "uuid") + "|" + SqlSource.str(rs, cols, "punishmenttype") + "|" + SqlSource.num(rs, cols, "start");
    }

    private static Imported map(ResultSet rs, Set<String> cols, boolean active) throws SQLException {
        String rawType = SqlSource.str(rs, cols, "punishmenttype");
        if (rawType == null) {
            return null;
        }
        String t = rawType.toUpperCase(Locale.ROOT);
        Imported e = new Imported();
        switch (t) {
            case "BAN", "TEMP_BAN" -> e.type = PunishmentType.BAN;
            case "IP_BAN", "TEMP_IP_BAN" -> {
                e.type = PunishmentType.BAN;
                e.ipBased = true;
            }
            case "MUTE", "TEMP_MUTE" -> e.type = PunishmentType.MUTE;
            case "WARNING", "TEMP_WARNING" -> e.type = PunishmentType.WARN;
            case "KICK" -> e.type = PunishmentType.KICK;
            case "NOTE" -> {
                e.type = PunishmentType.WARN;
                e.note = true;
            }
            default -> {
                return null;
            }
        }
        String rawUuid = SqlSource.str(rs, cols, "uuid");
        if (e.ipBased) {
            e.ip = rawUuid != null && Text.isIp(rawUuid) ? rawUuid : null;
            if (e.ip == null) {
                return null;
            }
        } else {
            e.uuid = Text.parseUuid(rawUuid);
            if (e.uuid == null) {
                return null;
            }
        }
        e.name = SqlSource.str(rs, cols, "name");
        e.reason = SqlSource.str(rs, cols, "reason");
        e.actorName = SqlSource.str(rs, cols, "operator");
        if (e.actorName != null && e.actorName.equalsIgnoreCase("CONSOLE")) {
            e.actorName = "Console";
        }
        e.createdAt = SqlSource.num(rs, cols, "start");
        long end = SqlSource.num(rs, cols, "end");
        e.expiresAt = end <= 0 ? 0 : end;
        e.active = active && e.type.isLasting();
        e.ref = "advancedban:" + t + ":" + rawUuid + ":" + e.createdAt;
        return e;
    }
}
