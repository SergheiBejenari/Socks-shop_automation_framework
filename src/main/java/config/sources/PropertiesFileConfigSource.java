package config.sources;

import config.ConfigLogging;
import config.ConfigSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration source that reads from properties files with variable expansion support.
 * Supports ${KEY} syntax with cycle detection.
 */
public class PropertiesFileConfigSource implements ConfigSource {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private final Properties properties;
    private final String sourceId;
    private final Path resolvedPath;

    public PropertiesFileConfigSource(String resourcePath, String sourceId) {
        this(loadFromLocation(resourcePath, null, null), sourceId);
    }

    public PropertiesFileConfigSource(Path filePath, String sourceId) {
        this(loadFromLocation(null, filePath, null), sourceId);
    }

    public PropertiesFileConfigSource(URL url, String sourceId) {
        this(loadFromLocation(null, null, url), sourceId);
    }

    private PropertiesFileConfigSource(LoadedProperties loaded, String sourceId) {
        this.sourceId = sourceId;
        this.properties = loaded.properties();
        this.resolvedPath = loaded.path();
        expandVariables();
    }

    @Override
    public Optional<String> get(String name) {
        String value = properties.getProperty(name);
        return Optional.ofNullable(value);
    }

    @Override
    public String id() {
        return sourceId;
    }

    @Override
    public Set<String> getAllKeys() {
        return properties.stringPropertyNames();
    }

    public Optional<Path> getResolvedPath() {
        return Optional.ofNullable(resolvedPath);
    }

    private static LoadedProperties loadFromLocation(String resourcePath, Path explicitPath, URL explicitUrl) {
        if (explicitPath != null) {
            return loadFromPath(explicitPath);
        }

        if (explicitUrl != null) {
            return loadFromUrl(explicitUrl);
        }

        if (resourcePath == null) {
            return new LoadedProperties(new Properties(), null);
        }

        URL classpathUrl = PropertiesFileConfigSource.class.getClassLoader().getResource(resourcePath);
        if (classpathUrl != null) {
            return loadFromUrl(classpathUrl);
        }

        try {
            URL url = new URL(resourcePath);
            return loadFromUrl(url);
        } catch (MalformedURLException ignored) {
            // Not a URL, try as file system path below.
        }

        try {
            Path path = Paths.get(resourcePath);
            return loadFromPath(path);
        } catch (RuntimeException e) {
            ConfigLogging.warn("Invalid properties file path '{}': {}", resourcePath, e.getMessage());
            ConfigLogging.debug("Invalid path details for properties file: {}", resourcePath, e);
            return new LoadedProperties(new Properties(), null);
        }
    }

    private static LoadedProperties loadFromUrl(URL url) {
        Properties props = new Properties();
        Path resolvedPath = null;

        if ("file".equalsIgnoreCase(url.getProtocol())) {
            try {
                Path candidate = Paths.get(url.toURI()).toAbsolutePath().normalize();
                resolvedPath = candidate;
                if (!Files.exists(candidate)) {
                    ConfigLogging.warn("Properties file not found: {} (this may be optional)", candidate);
                    return new LoadedProperties(props, candidate);
                }
            } catch (URISyntaxException | RuntimeException ignored) {
                // Ignore path resolution errors for non-standard URLs.
            }
        }

        try (InputStream is = url.openStream()) {
            props.load(is);
            ConfigLogging.debug("Successfully loaded properties file: {} with {} properties", url, props.size());
            if ("file".equalsIgnoreCase(url.getProtocol())) {
                try {
                    resolvedPath = Paths.get(url.toURI()).toAbsolutePath().normalize();
                } catch (URISyntaxException | RuntimeException ignored) {
                    // Ignore path resolution errors for non-standard URLs.
                }
            }
        } catch (IOException e) {
            ConfigLogging.error("Failed to read properties file: {} - {}", url, e.getMessage());
            ConfigLogging.debug("IOException details for properties file: {}", url, e);
        }

        return new LoadedProperties(props, resolvedPath);
    }

    private static LoadedProperties loadFromPath(Path path) {
        Path normalizedPath = path.toAbsolutePath().normalize();
        Properties props = new Properties();

        if (Files.exists(normalizedPath)) {
            try (InputStream is = Files.newInputStream(normalizedPath)) {
                props.load(is);
                ConfigLogging.debug("Successfully loaded properties file: {} with {} properties", normalizedPath, props.size());
            } catch (IOException e) {
                ConfigLogging.error("Failed to read properties file: {} - {}", normalizedPath, e.getMessage());
                ConfigLogging.debug("IOException details for properties file: {}", normalizedPath, e);
            }
        } else {
            ConfigLogging.warn("Properties file not found: {} (this may be optional)", normalizedPath);
        }

        return new LoadedProperties(props, normalizedPath);
    }

    private void expandVariables() {
        Map<String, String> expanded = new HashMap<>();
        Set<String> processing = new HashSet<>();

        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            expanded.put(key, expandValue(key, value, expanded, processing));
        }

        // Update properties with expanded values
        properties.clear();
        expanded.forEach(properties::setProperty);
    }

    private String expandValue(String currentKey, String value, Map<String, String> expanded, Set<String> processing) {
        if (processing.contains(currentKey)) {
            throw new IllegalStateException("Circular reference detected in property expansion: " + currentKey);
        }

        processing.add(currentKey);

        try {
            Matcher matcher = VARIABLE_PATTERN.matcher(value);
            StringBuffer result = new StringBuffer();

            while (matcher.find()) {
                String varName = matcher.group(1);
                String replacement;

                if (expanded.containsKey(varName)) {
                    replacement = expanded.get(varName);
                } else {
                    String varValue = properties.getProperty(varName);
                    if (varValue != null) {
                        replacement = expandValue(varName, varValue, expanded, processing);
                        expanded.put(varName, replacement);
                    } else {
                        // Variable not found, leave as-is
                        replacement = matcher.group(0);
                    }
                }

                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }

            matcher.appendTail(result);
            return result.toString();
        } finally {
            processing.remove(currentKey);
        }
    }

    private record LoadedProperties(Properties properties, Path path) {
    }
}
