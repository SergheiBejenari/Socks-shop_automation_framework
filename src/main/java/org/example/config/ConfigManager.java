package org.example.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Core configuration manager that resolves configuration values from multiple sources.
 * 
 * <p>
 * <strong>Configuration Source Priority (highest to lowest):</strong>
 * <ol>
 *     <li>System properties (-Dkey=value)</li>
 *     <li>Environment variables (CI_VARIABLE_NAME)</li>
 *     <li>.env file (local development secrets)</li>
 *     <li>Profile-specific properties (config-{env}.properties)</li>
 *     <li>Default properties (config.properties)</li>
 *     <li>Default values from ConfigKeys enum</li>
 * </ol>
 * 
 * <p>
 * <strong>Key Features:</strong>
 * <ul>
 *     <li>Multi-source configuration resolution</li>
 *     <li>Environment-aware profile loading</li>
 *     <li>Secure handling of sensitive data</li>
 *     <li>Performance metrics and monitoring</li>
 *     <li>Thread-safe operations</li>
 * </ul>
 * 
 * <p>
 * <strong>Usage Examples:</strong>
 * <pre>
 * // Get configuration values (use ConfigCache for better performance)
 * String baseUrl = ConfigManager.resolve(ConfigKeys.BASE_URL);
 * 
 * // Check configuration health
 * boolean healthy = ConfigManager.isHealthy();
 * 
 * // Refresh configuration
 * ConfigManager.refresh();
 * </pre>
 * 
 * <p>
 * <strong>Note:</strong> For regular configuration access, use {@link ConfigCache} 
 * which provides caching and type-safe getters.
 */
@Slf4j
public class ConfigManager {

    private static final ConfigLoader loader = new ConfigLoader();
    private static final Dotenv dotenv = initializeDotenv();

    // Metrics for monitoring configuration access and errors
    private static final Map<String, Long> accessMetrics = new ConcurrentHashMap<>();
    private static final Map<String, Long> errorMetrics = new ConcurrentHashMap<>();

    // Pattern to identify sensitive configuration keys
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            ".*(password|secret|token|key|credential|auth).*",
            Pattern.CASE_INSENSITIVE
    );

    // Private constructor to prevent instantiation
    private ConfigManager() {}

    /**
     * Initialize .env file loader for local development.
     * 
     * @return Dotenv instance or null if not available
     */
    private static Dotenv initializeDotenv() {
        try {
            return Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
        } catch (Exception e) {
            log.warn("Failed to load .env file: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Main configuration resolution method with full source hierarchy.
     * 
     * This method checks each configuration source in priority order until
     * a valid value is found. Required configurations will throw an exception
     * if no value is found in any source.
     *
     * @param key the configuration key to resolve
     * @return the resolved value or null if not found (for optional keys)
     * @throws ConfigurationException if required key is missing or resolution fails
     */
    public static String resolve(ConfigKeys key) {
        String propKey = key.getKey();
        recordAccess(propKey);

        try {
            // 1️⃣ System property (-Dprop=value) - highest priority
            String value = System.getProperty(propKey);
            if (isValidValue(value)) {
                log.debug("Found config '{}' in System Properties", propKey);
                return value;
            }

            // 2️⃣ Environment variable (PROP_NAME or prop.name)
            value = getEnvironmentVariable(propKey);
            if (isValidValue(value)) {
                log.debug("Found config '{}' in Environment Variables", propKey);
                return value;
            }

            // 3️⃣ .env file (local development)
            if (dotenv != null) {
                value = dotenv.get(propKey.toUpperCase().replace(".", "_"));
                if (isValidValue(value)) {
                    log.debug("Found config '{}' in .env file", propKey);
                    return value;
                }
            }

            // 4️⃣ Properties files (profile-specific + default)
            value = loader.getProperty(propKey);
            if (isValidValue(value)) {
                log.debug("Found config '{}' in properties file", propKey);
                return value;
            }

            // 5️⃣ Default value from enum
            if (key.getDefaultValue() != null) {
                log.debug("Using default value for config '{}'", propKey);
                return key.getDefaultValue();
            }

            // 6️⃣ Required key missing - throw exception
            if (key.isRequired()) {
                String errorMsg = String.format(
                        "❌ MISSING REQUIRED CONFIG: '%s' not found in any source. " +
                                "Sources checked: System Properties, Environment Variables, .env file, Properties files",
                        propKey
                );
                log.error(errorMsg);
                recordError(propKey);
                throw new ConfigurationException(errorMsg);
            }

            // Optional key missing - return null
            log.debug("Optional config '{}' not found, returning null", propKey);
            return null;

        } catch (ConfigurationException e) {
            throw e; // Re-throw our custom exception
        } catch (Exception e) {
            recordError(propKey);
            throw new ConfigurationException("Failed to resolve config: " + propKey, e);
        }
    }

    /**
     * Get environment variable value with fallback to CI/CD format.
     * 
     * @param key the configuration key
     * @return environment variable value or null if not found
     */
    private static String getEnvironmentVariable(String key) {
        // Try exact key first
        String value = System.getenv(key);
        if (isValidValue(value)) return value;

        // Try uppercase with underscores (CI/CD standard)
        String ciKey = key.toUpperCase().replace(".", "_");
        return System.getenv(ciKey);
    }

    /**
     * Check if a configuration value is valid (not null and not empty).
     * 
     * @param value the value to check
     * @return true if valid, false otherwise
     */
    private static boolean isValidValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    // ============= Management Operations =============

    /**
     * Clear configuration cache and reload configuration files.
     * 
     * Useful for:
     * <ul>
     *     <li>Dynamic configuration updates</li>
     *     <li>Testing scenarios</li>
     *     <li>Runtime configuration changes</li>
     * </ul>
     */
    public static synchronized void refresh() {
        log.info("🔄 Refreshing configuration...");
        
        // Clear ConfigCache
        ConfigCache.clear();
        
        // Reload properties files
        loader.reload();
        
        log.info("✅ Configuration refreshed successfully");
    }

    /**
     * Check if all required configurations are available.
     * 
     * This method validates that all required configuration keys
     * can be resolved without throwing exceptions.
     *
     * @return true if all required configs are available, false otherwise
     */
    public static boolean isHealthy() {
        try {
            for (ConfigKeys key : ConfigKeys.values()) {
                if (key.isRequired()) {
                    resolve(key);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("❌ Configuration health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get configuration value safely for logging (masks sensitive data).
     * 
     * @param key the configuration key
     * @return masked value for sensitive keys, actual value for others, or error indicator
     */
    public static String getForLogging(ConfigKeys key) {
        try {
            String value = resolve(key);
            if (value == null) return "<not set>";

            return isSensitive(key.getKey()) ? "*****" : value;
        } catch (Exception e) {
            return "<error>";
        }
    }

    /**
     * Check if a configuration key contains sensitive information.
     * 
     * @param key the configuration key to check
     * @return true if the key is sensitive, false otherwise
     */
    private static boolean isSensitive(String key) {
        return SENSITIVE_PATTERN.matcher(key).matches();
    }

    // ============= Metrics & Monitoring =============

    /**
     * Record configuration access for metrics.
     * 
     * @param key the configuration key that was accessed
     */
    private static void recordAccess(String key) {
        accessMetrics.merge(key, 1L, Long::sum);
    }

    /**
     * Record configuration error for metrics.
     * 
     * @param key the configuration key that caused an error
     */
    private static void recordError(String key) {
        errorMetrics.merge(key, 1L, Long::sum);
    }

    /**
     * Get configuration access metrics.
     * 
     * @return map of configuration keys to access counts
     */
    public static Map<String, Long> getAccessMetrics() {
        return Map.copyOf(accessMetrics);
    }

    /**
     * Get configuration error metrics.
     * 
     * @return map of configuration keys to error counts
     */
    public static Map<String, Long> getErrorMetrics() {
        return Map.copyOf(errorMetrics);
    }

    /**
     * Get current cache size from ConfigCache.
     * 
     * @return number of cached configuration values
     */
    public static int getCacheSize() {
        return ConfigCache.size();
    }
}