package org.example.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Simple cache for configuration values to avoid repeated parsing.
 * 
 * This class provides thread-safe, in-memory caching of configuration properties.
 * Values are parsed into the required type once and cached for subsequent access.
 * 
 * <p>
 * <strong>Key Benefits:</strong>
 * <ul>
 *     <li>Performance: Avoids repeated parsing of the same values</li>
 *     <li>Thread-safe: Uses ConcurrentHashMap for concurrent access</li>
 *     <li>Memory efficient: Only caches accessed values</li>
 *     <li>Simple: Easy to understand and maintain</li>
 * </ul>
 * 
 * <p>
 * <strong>Usage Examples:</strong>
 * <pre>
 * // Get cached String value
 * String apiUrl = ConfigCache.getString(ConfigKeys.API_BASE_URL);
 * 
 * // Get cached int value
 * int timeout = ConfigCache.getInt(ConfigKeys.DEFAULT_TIMEOUT);
 * 
 * // Get cached boolean value
 * boolean headless = ConfigCache.getBoolean(ConfigKeys.HEADLESS);
 * 
 * // Get cached custom type
 * Duration duration = ConfigCache.get(ConfigKeys.CONNECTION_TIMEOUT, 
 *     value -> Duration.ofSeconds(Long.parseLong(value)));
 * </pre>
 * 
 * <p>
 * <strong>Thread Safety:</strong>
 * This class is thread-safe and can be used in multi-threaded environments
 * like test frameworks without additional synchronization.
 */
public final class ConfigCache {

    /**
     * Thread-safe cache storing parsed configuration values.
     * Key: ConfigKeys enum
     * Value: Parsed Object (cached after first access)
     */
    private static final Map<ConfigKeys, Object> cache = new ConcurrentHashMap<>();

    // Private constructor to prevent instantiation
    private ConfigCache() {}

    /**
     * Retrieve a configuration value from cache or compute it if absent.
     * 
     * This method ensures that each configuration value is parsed only once.
     * Subsequent calls return the cached value, improving performance.
     *
     * @param key    the configuration key to retrieve
     * @param parser function to convert the String value to the desired type
     * @param <T>    the type of the configuration value
     * @return the cached or newly parsed value
     * @throws ConfigurationException if the configuration value is missing or cannot be parsed
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(ConfigKeys key, Function<String, T> parser) {
        // computeIfAbsent ensures atomic check-and-compute operation
        return (T) cache.computeIfAbsent(key, k -> {
            try {
                // Get the raw String value from ConfigManager
                String value = ConfigManager.resolve(k);
                if (value == null) {
                    if (k.isRequired()) {
                        throw new ConfigurationException(
                            "Required configuration value for key '" + k.getKey() + "' is missing");
                    }
                    return null; // Optional configs can be null
                }
                
                // Parse the value into the required type
                return parser.apply(value);
            } catch (Exception e) {
                if (e instanceof ConfigurationException) {
                    throw e; // Re-throw our custom exception
                }
                // Wrap other exceptions
                throw new ConfigurationException(
                    "Failed to parse configuration for key '" + k.getKey() + "'", e);
            }
        });
    }

    // ============= Convenience Methods =============
    
    /**
     * Retrieve a String configuration value.
     *
     * @param key configuration key
     * @return String value or null if optional and not set
     * @throws ConfigurationException if required key is missing
     */
    public static String getString(ConfigKeys key) {
        return get(key, Function.identity());
    }

    /**
     * Retrieve an int configuration value.
     *
     * @param key configuration key
     * @return int value
     * @throws ConfigurationException if required key is missing or value cannot be parsed
     */
    public static int getInt(ConfigKeys key) {
        return get(key, value -> {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    String.format("Invalid integer value for '%s': '%s'", key.getKey(), value));
            }
        });
    }

    /**
     * Retrieve a boolean configuration value.
     *
     * @param key configuration key
     * @return boolean value
     * @throws ConfigurationException if required key is missing or value cannot be parsed
     */
    public static boolean getBoolean(ConfigKeys key) {
        return get(key, value -> {
            String trimmed = value.trim().toLowerCase();
            if ("true".equals(trimmed) || "1".equals(trimmed) || "yes".equals(trimmed)) {
                return true;
            } else if ("false".equals(trimmed) || "0".equals(trimmed) || "no".equals(trimmed)) {
                return false;
            }
            throw new IllegalArgumentException(
                String.format("Invalid boolean value for '%s': '%s'", key.getKey(), value));
        });
    }

    /**
     * Retrieve a double configuration value.
     *
     * @param key configuration key
     * @return double value
     * @throws ConfigurationException if required key is missing or value cannot be parsed
     */
    public static double getDouble(ConfigKeys key) {
        return get(key, value -> {
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    String.format("Invalid double value for '%s': '%s'", key.getKey(), value));
            }
        });
    }

    /**
     * Retrieve a long configuration value.
     *
     * @param key configuration key
     * @return long value
     * @throws ConfigurationException if required key is missing or value cannot be parsed
     */
    public static long getLong(ConfigKeys key) {
        return get(key, value -> {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    String.format("Invalid long value for '%s': '%s'", key.getKey(), value));
            }
        });
    }

    // ============= Cache Management =============

    /**
     * Clear the entire configuration cache.
     * 
     * Useful for:
     * <ul>
     *     <li>Testing scenarios where configs need to be reloaded</li>
     *     <li>Dynamic configuration updates</li>
     *     <li>Memory cleanup in long-running applications</li>
     * </ul>
     */
    public static void clear() {
        cache.clear();
    }

    /**
     * Remove a specific configuration key from cache.
     * 
     * @param key the configuration key to remove from cache
     */
    public static void remove(ConfigKeys key) {
        cache.remove(key);
    }

    /**
     * Get the current cache size.
     * 
     * @return number of cached configuration values
     */
    public static int size() {
        return cache.size();
    }

    /**
     * Check if a configuration key is cached.
     * 
     * @param key the configuration key to check
     * @return true if the key is in cache, false otherwise
     */
    public static boolean isCached(ConfigKeys key) {
        return cache.containsKey(key);
    }
}
