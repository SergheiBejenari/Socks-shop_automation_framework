package org.example.config;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ConfigLogger provides structured logging for application configuration.
 * <p>
 * Features:
 * <ul>
 *     <li>Startup configuration summary</li>
 *     <li>Detailed configuration logging (safely masking sensitive data)</li>
 *     <li>Configuration access and error metrics</li>
 *     <li>Health check logging</li>
 * </ul>
 * <p>
 * Logging levels:
 * <ul>
 *     <li>INFO – production-safe summary and metrics</li>
 *     <li>DEBUG – detailed values with masking for sensitive keys</li>
 *     <li>WARN/ERROR – configuration errors or health issues</li>
 * </ul>
 */
@Slf4j
public class ConfigLogger {

    /**
     * Logs a high-level configuration summary at startup.
     * Safe for production environments (does not print sensitive values).
     */
    public static void logStartupConfiguration() {
        log.info("=== Configuration Summary ===");

        log.info("Environment: {}", ConfigManager.resolve(ConfigKeys.ENVIRONMENT));
        log.info("Cache size: {}", ConfigManager.getCacheSize());

        // Group configurations by category (prefix before first dot)
        Map<String, Long> categories = Arrays.stream(ConfigKeys.values())
                .collect(Collectors.groupingBy(
                        key -> key.getKey().split("\\.")[0],
                        Collectors.counting()
                ));

        categories.forEach((category, count) ->
                log.info("Configuration category '{}': {} keys", category, count));

        log.info("================================");
    }

    /**
     * Logs detailed configuration values for debugging.
     * Sensitive values are masked to prevent security leaks.
     */
    public static void logDetailedConfiguration() {
        log.debug("=== Detailed Configuration ===");

        for (ConfigKeys key : ConfigKeys.values()) {
            String value = ConfigManager.getForLogging(key); // returns masked value if sensitive
            String source = determineSource(key);
            String status = key.isRequired() ? "REQUIRED" : "optional";

            log.debug("{}={} [{}] ({})", key.getKey(), value, source, status);
        }

        log.debug("==============================");
    }

    /**
     * Determines the source of a configuration value.
     * Checks system properties first, then environment variables, then fallback.
     *
     * @param key Config key to check
     * @return source description (system-prop, env-var, env-var-ci, config-file-or-default)
     */
    private static String determineSource(ConfigKeys key) {
        String keyName = key.getKey();

        if (System.getProperty(keyName) != null) return "system-prop";
        if (System.getenv(keyName) != null) return "env-var";
        if (System.getenv(keyName.toUpperCase().replace(".", "_")) != null) return "env-var-ci";

        // Could be from config file or default value
        return "config-file-or-default";
    }

    /**
     * Logs the top accessed configuration keys and any error metrics.
     */
    public static void logMetrics() {
        Map<String, Long> accessMetrics = ConfigManager.getAccessMetrics();
        Map<String, Long> errorMetrics = ConfigManager.getErrorMetrics();

        if (!accessMetrics.isEmpty()) {
            log.info("=== Configuration Access Metrics (Top 10) ===");
            accessMetrics.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10)
                    .forEach(entry -> log.info("  {}: {} accesses", entry.getKey(), entry.getValue()));
        }

        if (!errorMetrics.isEmpty()) {
            log.warn("=== Configuration Error Metrics ===");
            errorMetrics.forEach((key, count) ->
                    log.warn("  {}: {} errors", key, count));
        }
    }

    /**
     * Logs configuration health status.
     * Provides immediate visibility if required keys are missing or invalid.
     */
    public static void logHealthStatus() {
        boolean healthy = ConfigManager.isHealthy();
        if (healthy) {
            log.info("✅ Configuration health check: PASSED");
        } else {
            log.error("❌ Configuration health check: FAILED");
        }
    }
}
