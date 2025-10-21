package config;

import config.sources.DotEnvFileConfigSource;
import config.sources.EnvConfigSource;
import config.sources.PropertiesFileConfigSource;
import config.sources.SystemPropsConfigSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Composite configuration source that combines multiple sources in priority order.
 * Sources are checked in the exact order: Env > Sysprops > ProfileProps > BaseProps > DotEnv.
 */
public class CompositeConfig implements ConfigSource {

    private final List<ConfigSource> sources;
    private final String profileFilename;
    private final String baseFilename;
    private final String dotEnvPath;
    private final Path profileFilePath;
    private final Path baseFilePath;
    private final Path dotEnvFilePath;

    // Default file naming patterns
    private static final String DEFAULT_PROFILE_PATTERN = "application-%s.properties";
    private static final String DEFAULT_BASE_FILENAME = "application.properties";

    // Legacy file naming patterns for backward compatibility
    private static final String LEGACY_PROFILE_PATTERN = "configuration-%s.properties";
    private static final String LEGACY_BASE_FILENAME = "configuration.properties";

    public CompositeConfig(String profile) {
        this(profile, DEFAULT_PROFILE_PATTERN, DEFAULT_BASE_FILENAME, ".env", List.of());
    }

    public CompositeConfig(String profile, List<ConfigSource> additionalSources) {
        this(profile, DEFAULT_PROFILE_PATTERN, DEFAULT_BASE_FILENAME, ".env", additionalSources);
    }

    /**
     * Creates a composite config with custom file naming patterns.
     *
     * @param profile        the profile name (e.g., "local", "dev", "prod")
     * @param profilePattern the pattern for profile-specific files (e.g., "application-%s.properties")
     * @param baseFilename   the base configuration filename (e.g., "application.properties")
     */
    public CompositeConfig(String profile, String profilePattern, String baseFilename) {
        this(profile, profilePattern, baseFilename, ".env", List.of());
    }

    /**
     * Creates a composite config with custom file naming patterns and custom .env file path.
     *
     * @param profile        the profile name (e.g., "local", "dev", "prod")
     * @param profilePattern the pattern for profile-specific files (e.g., "application-%s.properties")
     * @param baseFilename   the base configuration filename (e.g., "application.properties")
     * @param dotEnvPath     the path to the .env file
     */
    public CompositeConfig(String profile, String profilePattern, String baseFilename, String dotEnvPath) {
        this(profile, profilePattern, baseFilename, dotEnvPath, List.of());
    }

    public CompositeConfig(String profile, String profilePattern, String baseFilename, String dotEnvPath,
                            List<ConfigSource> additionalSources) {
        this.profileFilename = String.format(profilePattern, profile);
        this.baseFilename = baseFilename;
        this.dotEnvPath = dotEnvPath;

        PropertiesFileConfigSource profileSource = new PropertiesFileConfigSource(this.profileFilename, "profile:" + profile);
        PropertiesFileConfigSource baseSource = new PropertiesFileConfigSource(this.baseFilename, "base");
        DotEnvFileConfigSource dotEnvSource = new DotEnvFileConfigSource(this.dotEnvPath);

        this.profileFilePath = profileSource.getResolvedPath().map(Path::toAbsolutePath).map(Path::normalize).orElse(null);
        this.baseFilePath = baseSource.getResolvedPath().map(Path::toAbsolutePath).map(Path::normalize).orElse(null);
        this.dotEnvFilePath = dotEnvSource.getPath();

        List<ConfigSource> orderedSources = new ArrayList<>();
        orderedSources.add(new EnvConfigSource());
        orderedSources.add(new SystemPropsConfigSource());
        if (additionalSources != null) {
            orderedSources.addAll(additionalSources);
        }
        orderedSources.add(profileSource);
        orderedSources.add(baseSource);
        orderedSources.add(dotEnvSource);

        this.sources = List.copyOf(orderedSources);
    }

    /**
     * Creates a composite config using legacy file naming for backward compatibility.
     * Uses configuration-{profile}.properties and configuration.properties patterns.
     */
    public static CompositeConfig withLegacyNaming(String profile) {
        return new CompositeConfig(profile, LEGACY_PROFILE_PATTERN, LEGACY_BASE_FILENAME, ".env", List.of());
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

    /**
     * Returns the profile-specific properties filename being used.
     */
    public String getProfileFilename() {
        return profileFilename;
    }

    public Optional<Path> getProfileFilePath() {
        return Optional.ofNullable(profileFilePath);
    }

    /**
     * Returns the base properties filename being used.
     */
    public String getBaseFilename() {
        return baseFilename;
    }

    public Optional<Path> getBaseFilePath() {
        return Optional.ofNullable(baseFilePath);
    }

    /**
     * Returns the .env file path being used.
     */
    public String getDotEnvPath() {
        return dotEnvPath;
    }

    public Optional<Path> getDotEnvFilePath() {
        return Optional.ofNullable(dotEnvFilePath);
    }
}
