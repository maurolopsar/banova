package dev.banova.core.importer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * Opens a MySQL/MariaDB connection to another plugin's database with the bundled driver.
 */
final class SqlSource {

    private SqlSource() {
    }

    static Connection open(String host, int port, String database, String user, String password) throws Importer.ImportException {
        String url = "jdbc:mariadb://" + host + ":" + port + "/" + database
                + "?allowPublicKeyRetrieval=true&useSsl=false&connectTimeout=10000";
        Properties props = new Properties();
        props.setProperty("user", user == null ? "" : user);
        props.setProperty("password", password == null ? "" : password);
        try {
            Connection c = new org.mariadb.jdbc.Driver().connect(url, props);
            if (c == null) {
                throw new Importer.ImportException("Invalid database URL " + url);
            }
            return c;
        } catch (SQLException e) {
            throw new Importer.ImportException("Cannot connect to " + host + ":" + port + "/" + database + ": " + e.getMessage(), e);
        }
    }

    static Set<String> columns(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Set<String> out = new HashSet<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            out.add(meta.getColumnLabel(i).toLowerCase(Locale.ROOT));
        }
        return out;
    }

    static String str(ResultSet rs, Set<String> cols, String column) throws SQLException {
        return cols.contains(column) ? rs.getString(column) : null;
    }

    static long num(ResultSet rs, Set<String> cols, String column) throws SQLException {
        if (!cols.contains(column)) {
            return 0;
        }
        Object value = rs.getObject(column);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.getTime();
        }
        if (value instanceof java.time.LocalDateTime ldt) {
            return java.sql.Timestamp.valueOf(ldt).getTime();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            try {
                return java.sql.Timestamp.valueOf(String.valueOf(value).trim()).getTime();
            } catch (IllegalArgumentException e2) {
                return 0;
            }
        }
    }

    static boolean bool(ResultSet rs, Set<String> cols, String column) throws SQLException {
        if (!cols.contains(column)) {
            return false;
        }
        Object value = rs.getObject(column);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        return value != null && (String.valueOf(value).equals("1") || String.valueOf(value).equalsIgnoreCase("true"));
    }
}
