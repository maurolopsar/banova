package dev.banova.core.storage;

/**
 * Unchecked wrapper for SQL failures so services stay readable.
 */
public final class StorageException extends RuntimeException {

    public StorageException(String sql, Throwable cause) {
        super("Database error: " + cause.getMessage() + " [" + abbreviate(sql) + "]", cause);
    }

    private static String abbreviate(String sql) {
        return sql.length() > 120 ? sql.substring(0, 120) + "..." : sql;
    }
}
