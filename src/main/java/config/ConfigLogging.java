package config;

import logging.Logger;
import logging.LoggerFactory;

/**
 * Configuration-specific logging utilities.
 *
 * <p>Provides specialized logging methods for configuration module events
 * while using the universal logging infrastructure.
 */
public class ConfigLogging {

    private static final Logger logger = LoggerFactory.getLogger("config");

    /**
     * Log configuration initialization events.
     */
    public static void logConfigInit(String profile, String message) {
        logger.withContext("configProfile", profile)
                .info("Configuration initialization: {}", message);
    }

    /**
     * Log configuration loading from specific source.
     */
    public static void logConfigLoad(String source, String key, String value, boolean isSecret) {
        if (logger.isDebugEnabled()) {
            logger.withContext("configSource", source)
                    .withContext("configKey", key)
                    .debug("Loaded configuration: {}={}", key, isSecret ? "***" : value);
        }
    }

    /**
     * Log configuration validation events.
     */
    public static void logValidation(String key, String message) {
        logger.withContext("configKey", key)
                .debug("Validation: {}", message);
    }

    /**
     * Log configuration validation errors.
     */
    public static void logValidationError(String key, String value, String error) {
        logger.withContext("configKey", key)
                .error("Validation failed for {}={}: {}", key, value, error);
    }

    /**
     * Log configuration fallback to default values.
     */
    public static void logDefaultValue(String key, Object defaultValue) {
        logger.withContext("configKey", key)
                .debug("Using default value for {}: {}", key, defaultValue);
    }

    /**
     * Log configuration reload events.
     */
    public static void logReload(String reason) {
        logger.info("Configuration reload triggered: {}", reason);
    }

    /**
     * Log configuration dump for debugging.
     */
    public static void logConfigDump(String profile, String configDump) {
        logger.withContext("configProfile", profile)
                .info("Active configuration profile: {}", profile);

        if (logger.isDebugEnabled()) {
            logger.withContext("configProfile", profile)
                    .debug("Configuration dump:\n{}", configDump);
        }
    }

    /**
     * Log configuration source priority information.
     */
    public static void logSourcePriority(String message) {
        logger.debug("Configuration source priority: {}", message);
    }

    // Delegate methods for convenience
    public static void trace(String message, Object... args) {
        logger.trace(message, args);
    }

    public static void debug(String message, Object... args) {
        logger.debug(message, args);
    }

    public static void info(String message, Object... args) {
        logger.info(message, args);
    }

    public static void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    public static void error(String message, Object... args) {
        logger.error(message, args);
    }

    public static void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public static boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public static boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }
}
