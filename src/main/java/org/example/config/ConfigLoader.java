package org.example.config;

import lombok.extern.slf4j.Slf4j;

import org.example.config.ConfigurationException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigLoader is responsible for loading and managing application configuration
 * properties with support for:
 * <ul>
 *     <li>Default config file (config.properties)</li>
 *     <li>Environment-specific profiles (e.g., config-dev.properties)</li>
 *     <li>Dynamic reload capability at runtime</li>
 *     <li>System properties and environment variable overrides</li>
 * </ul>
 * <p>
 * Thread-safe: reloads and property access are synchronized.
 * Logs loading progress, errors, and fallback behavior.
 * <p>
 * Example usage:
 * <pre>
 *   ConfigLoader loader = new ConfigLoader();
 *   String baseUrl = loader.getProperty("base.url");
 *   loader.reload(); // reloads configs at runtime
 * </pre>
 */
@Slf4j
public class ConfigLoader {

    private static final String DEFAULT_CONFIG_FILE = "config.properties";
    private static final String PROFILE_CONFIG_PATTERN = "config-%s.properties";

    private final Properties properties = new Properties();

    /**
     * Timestamp of the last successful configuration load.
     */
    private volatile long lastLoaded = 0;

    public ConfigLoader() {
        reload();
    }

    /**
     * Reloads configuration from default and profile-specific property files.
     * Thread-safe; clears previous properties before loading.
     */
    public synchronized void reload() {
        properties.clear();

        try {
            // 1️⃣ Load default config.properties (required)
            loadFile(DEFAULT_CONFIG_FILE, true);

            // 2️⃣ Load environment-specific profile (optional)
            String env = detectEnvironment();
            if (env != null && !env.isEmpty()) {
                String profileFile = String.format(PROFILE_CONFIG_PATTERN, env.toLowerCase());
                loadFile(profileFile, false);
                log.info("Loaded configuration profile: {}", env);
            }

            lastLoaded = System.currentTimeMillis();
            log.info("Configuration loaded successfully with {} properties", properties.size());

        } catch (Exception e) {
            log.error("Failed to load configuration", e);
            throw new ConfigurationException("Configuration loading failed", e);
        }
    }

    /**
     * Detects the active environment with priority:
     * <ol>
     *     <li>System property "env"</li>
     *     <li>Environment variable "ENV"</li>
     *     <li>Spring-compatible "SPRING_PROFILES_ACTIVE"</li>
     * </ol>
     *
     * @return the environment string or null if none found
     */
    private String detectEnvironment() {
        String env = System.getProperty("env");
        if (env == null) env = System.getenv("ENV");
        if (env == null) env = System.getenv("SPRING_PROFILES_ACTIVE"); // Spring compatibility
        return env;
    }

    /**
     * Loads a properties file from the classpath.
     *
     * @param fileName the name of the file
     * @param required if true, throws exception if the file is not found
     */
    private void loadFile(String fileName, boolean required) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (input != null) {
                Properties fileProps = new Properties();
                fileProps.load(input);
                properties.putAll(fileProps);
                log.debug("Loaded properties file: {} ({} properties)", fileName, fileProps.size());
            } else if (required) {
                throw new ConfigurationException("Required configuration file not found: " + fileName);
            } else {
                log.debug("Optional configuration file not found: {}", fileName);
            }
        } catch (Exception e) {
            if (required) {
                throw new ConfigurationException("Failed to load required config file: " + fileName, e);
            } else {
                log.warn("Failed to load optional config file: {}", fileName, e);
            }
        }
    }

    /**
     * Retrieves the value of a configuration property.
     *
     * @param key the property key
     * @return property value or null if not found
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Retrieves the timestamp of the last successful configuration load.
     *
     * @return epoch time in milliseconds
     */
    public long getLastLoadedTime() {
        return lastLoaded;
    }
}
