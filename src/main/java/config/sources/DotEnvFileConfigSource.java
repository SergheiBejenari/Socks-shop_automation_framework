package config.sources;

import config.ConfigSource;
import logging.Logger;
import logging.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Configuration source that reads from .env file.
 * Implements minimal .env file parsing in pure Java without external dependencies.
 * The .env file is optional and has the lowest priority.
 */
public class DotEnvFileConfigSource implements ConfigSource {
    
    private static final Logger logger = LoggerFactory.getLogger(DotEnvFileConfigSource.class);
    
    private final Map<String, String> envVars;
    private final Path dotEnvPath;
    
    /**
     * Creates a DotEnvFileConfigSource that reads from .env in the project root.
     */
    public DotEnvFileConfigSource() {
        this(Paths.get(".env"));
    }
    
    /**
     * Creates a DotEnvFileConfigSource that reads from the specified path.
     * This constructor is useful for testing with custom .env files.
     * 
     * @param dotEnvPath the path to the .env file
     */
    public DotEnvFileConfigSource(Path dotEnvPath) {
        this.dotEnvPath = dotEnvPath;
        this.envVars = loadDotEnvFile();
    }
    
    /**
     * Creates a DotEnvFileConfigSource that reads from the specified file path.
     * Convenience constructor for string paths.
     * 
     * @param dotEnvFilePath the file path to the .env file
     */
    public DotEnvFileConfigSource(String dotEnvFilePath) {
        this(Paths.get(dotEnvFilePath));
    }
    
    @Override
    public Optional<String> get(String name) {
        return Optional.ofNullable(envVars.get(name));
    }
    
    @Override
    public String id() {
        return ".env";
    }
    
    @Override
    public Set<String> getAllKeys() {
        return envVars.keySet();
    }
    
    private Map<String, String> loadDotEnvFile() {
        Map<String, String> vars = new HashMap<>();
        
        if (!Files.exists(dotEnvPath)) {
            logger.debug("DotEnv file not found: {} (this is optional)", dotEnvPath);
            return vars; // .env file is optional
        }
        
        try (var lines = Files.lines(dotEnvPath)) {
            logger.debug("Loading .env file: {}", dotEnvPath);
            
            lines.map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("#"))
                .forEach(line -> {
                    if (!line.contains("=")) {
                        logger.warn("Malformed .env line (missing '='): '{}' in file: {}", line, dotEnvPath);
                        return;
                    }
                    parseLine(line, vars);
                });
                
            logger.debug("Loaded {} variables from .env file: {}", vars.size(), dotEnvPath);
        } catch (IOException e) {
            logger.error("Failed to read .env file: {} - {}", dotEnvPath, e.getMessage());
            logger.debug("IOException details for .env file: {}", dotEnvPath, e);
        }
        
        return vars;
    }
    
    private void parseLine(String line, Map<String, String> vars) {
        int equalsIndex = line.indexOf('=');
        if (equalsIndex <= 0) {
            logger.warn("Invalid .env line format (key cannot be empty): '{}' in file: {}", line, dotEnvPath);
            return;
        }
        
        String key = line.substring(0, equalsIndex).trim();
        String value = line.substring(equalsIndex + 1).trim();
        
        // Validate key format
        if (key.isEmpty()) {
            logger.warn("Empty key in .env line: '{}' in file: {}", line, dotEnvPath);
            return;
        }
        
        // Warn about potentially problematic key names
        if (!key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            logger.warn("Non-standard key format in .env: '{}' (should match [A-Za-z_][A-Za-z0-9_]*) in file: {}", key, dotEnvPath);
        }
        
        // Remove surrounding quotes if present
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }
        
        // Check for duplicate keys
        if (vars.containsKey(key)) {
            logger.warn("Duplicate key '{}' in .env file: {} (previous value will be overwritten)", key, dotEnvPath);
        }
        
        vars.put(key, value);
        logger.trace("Parsed .env variable: {}={}", key, value.isEmpty() ? "<empty>" : "<value>");
    }
}
