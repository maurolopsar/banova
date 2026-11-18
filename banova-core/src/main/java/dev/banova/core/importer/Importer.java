package dev.banova.core.importer;

import dev.banova.core.BanovaCore;

import java.util.List;

/**
 * Reads punishments from another plugin.
 */
public interface Importer {

    String id();

    /** Human readable name of the source. */
    String displayName();

    /**
     * Reads everything from the source.
     *
     * @throws ImportException with a user friendly message when the source cannot be read
     */
    List<Imported> read(BanovaCore core) throws ImportException;

    final class ImportException extends Exception {
        public ImportException(String message) {
            super(message);
        }

        public ImportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
