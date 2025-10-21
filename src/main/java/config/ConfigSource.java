package config;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Interface for configuration sources that can provide configuration values.
 * Each source has a unique identifier for logging and debugging purposes.
 */
public interface ConfigSource {

    /**
     * Retrieves a configuration value by name.
     *
     * @param name the configuration key name
     * @return the configuration value if present, empty otherwise
     */
    Optional<String> get(String name);

    /**
     * Returns a unique identifier for this configuration source.
     * Used for logging and debugging to identify where values come from.
     *
     * @return the source identifier (e.g., "env", "sysprops", "profile:local", "base", ".env")
     */
    String id();

    /**
     * Returns all available configuration keys from this source.
     * Used for detecting unknown keys and configuration validation.
     * Default implementation returns empty set for backward compatibility.
     *
     * @return set of all available configuration keys
     */
    default Set<String> getAllKeys() {
        return Collections.emptySet();
    }
}
