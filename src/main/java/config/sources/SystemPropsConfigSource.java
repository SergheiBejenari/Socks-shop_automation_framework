package config.sources;

import config.ConfigSource;
import java.util.Optional;
import java.util.Set;

/**
 * Configuration source that reads from JVM system properties.
 */
public class SystemPropsConfigSource implements ConfigSource {
    
    @Override
    public Optional<String> get(String name) {
        String value = System.getProperty(name);
        return Optional.ofNullable(value);
    }
    
    @Override
    public String id() {
        return "sysprops";
    }
    
    @Override
    public Set<String> getAllKeys() {
        return System.getProperties().stringPropertyNames();
    }
}
