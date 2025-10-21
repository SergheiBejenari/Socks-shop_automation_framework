package config.sources;

import config.ConfigSource;

import java.util.Optional;
import java.util.Set;

/**
 * Configuration source that reads from environment variables.
 */
public class EnvConfigSource implements ConfigSource {

    @Override
    public Optional<String> get(String name) {
        String value = System.getenv(name);
        return Optional.ofNullable(value);
    }

    @Override
    public String id() {
        return "env";
    }

    @Override
    public Set<String> getAllKeys() {
        return System.getenv().keySet();
    }
}
