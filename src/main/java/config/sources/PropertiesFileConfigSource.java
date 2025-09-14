package config.sources;

import config.ConfigSource;
import config.ConfigLogging;
import java.io.IOException;
import java.io.InputStream;
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
    
    public PropertiesFileConfigSource(String resourcePath, String sourceId) {
        this.sourceId = sourceId;
        this.properties = loadProperties(resourcePath);
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
    
    private Properties loadProperties(String resourcePath) {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
                props.load(is);
                ConfigLogging.debug("Successfully loaded properties file: {} with {} properties", resourcePath, props.size());
            } else {
                ConfigLogging.warn("Properties file not found: {} (this may be optional)", resourcePath);
            }
        } catch (IOException e) {
            ConfigLogging.error("Failed to read properties file: {} - {}", resourcePath, e.getMessage());
            ConfigLogging.debug("IOException details for properties file: {}", resourcePath, e);
        }
        return props;
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
}
