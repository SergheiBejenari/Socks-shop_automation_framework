package config;

import config.sources.*;
import java.util.List;
import java.util.Optional;

/**
 * Composite configuration source that combines multiple sources in priority order.
 * Sources are checked in the exact order: Env > Sysprops > ProfileProps > BaseProps > DotEnv.
 */
public class CompositeConfig implements ConfigSource {
    
    private final List<ConfigSource> sources;
    
    // Default file naming patterns
    private static final String DEFAULT_PROFILE_PATTERN = "application-%s.properties";
    private static final String DEFAULT_BASE_FILENAME = "application.properties";
    
    // Legacy file naming patterns for backward compatibility
    private static final String LEGACY_PROFILE_PATTERN = "configuration-%s.properties";
    private static final String LEGACY_BASE_FILENAME = "configuration.properties";
    
    public CompositeConfig(String profile) {
        this(profile, DEFAULT_PROFILE_PATTERN, DEFAULT_BASE_FILENAME);
    }
    
    /**
     * Creates a composite config with custom file naming patterns.
     * 
     * @param profile the profile name (e.g., "local", "dev", "prod")
     * @param profilePattern the pattern for profile-specific files (e.g., "application-%s.properties")
     * @param baseFilename the base configuration filename (e.g., "application.properties")
     */
    public CompositeConfig(String profile, String profilePattern, String baseFilename) {
        this(profile, profilePattern, baseFilename, ".env");
    }
    
    /**
     * Creates a composite config with custom file naming patterns and custom .env file path.
     * 
     * @param profile the profile name (e.g., "local", "dev", "prod")
     * @param profilePattern the pattern for profile-specific files (e.g., "application-%s.properties")
     * @param baseFilename the base configuration filename (e.g., "application.properties")
     * @param dotEnvPath the path to the .env file
     */
    public CompositeConfig(String profile, String profilePattern, String baseFilename, String dotEnvPath) {
        String profileFilename = String.format(profilePattern, profile);
        
        this.sources = List.of(
            new EnvConfigSource(),
            new SystemPropsConfigSource(),
            new PropertiesFileConfigSource(profileFilename, "profile:" + profile),
            new PropertiesFileConfigSource(baseFilename, "base"),
            new DotEnvFileConfigSource(dotEnvPath)
        );
    }
    
    /**
     * Creates a composite config using legacy file naming for backward compatibility.
     * Uses configuration-{profile}.properties and configuration.properties patterns.
     */
    public static CompositeConfig withLegacyNaming(String profile) {
        return new CompositeConfig(profile, LEGACY_PROFILE_PATTERN, LEGACY_BASE_FILENAME);
    }
    
    @Override
    public Optional<String> get(String name) {
        for (ConfigSource source : sources) {
            Optional<String> value = source.get(name);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }
    
    @Override
    public String id() {
        return "composite";
    }
    
    /**
     * Returns the configuration source that provides the value for the given name.
     * Used for debugging and logging to show where values come from.
     */
    public Optional<ConfigSource> getSourceFor(String name) {
        for (ConfigSource source : sources) {
            if (source.get(name).isPresent()) {
                return Optional.of(source);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Returns all sources in priority order for debugging.
     */
    public List<ConfigSource> getSources() {
        return sources;
    }
}
