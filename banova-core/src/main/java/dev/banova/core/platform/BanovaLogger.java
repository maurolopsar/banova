package dev.banova.core.platform;

public interface BanovaLogger {

    void info(String message);

    void warn(String message);

    void error(String message, Throwable error);

    static BanovaLogger ofJul(java.util.logging.Logger logger) {
        return new BanovaLogger() {
            @Override
            public void info(String message) {
                logger.info(message);
            }

            @Override
            public void warn(String message) {
                logger.warning(message);
            }

            @Override
            public void error(String message, Throwable error) {
                logger.log(java.util.logging.Level.SEVERE, message, error);
            }
        };
    }
}
