package org.example.config;

import lombok.extern.slf4j.Slf4j;

import org.example.config.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enhanced configuration validator with detailed reporting
 */
@Slf4j
public class ConfigValidator {

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
    }

    /**
     * Comprehensive validation of all configuration keys
     */
    public static ValidationResult validateAll() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (ConfigKeys key : ConfigKeys.values()) {
            try {
                String value = ConfigManager.resolve(key);

                if (value == null) {
                    if (key.isRequired()) {
                        errors.add("Required configuration missing: " + key.getKey());
                    } else {
                        warnings.add("Optional configuration not set: " + key.getKey());
                    }
                } else {
                    // Validate specific key types
                    validateKeyValue(key, value, errors, warnings);
                }

            } catch (Exception e) {
                errors.add("Configuration error for " + key.getKey() + ": " + e.getMessage());
            }
        }

        boolean isValid = errors.isEmpty();
        ValidationResult result = new ValidationResult(isValid, errors, warnings);

        logValidationResult(result);
        return result;
    }

    private static void validateKeyValue(ConfigKeys key, String value,
                                         List<String> errors, List<String> warnings) {
        String keyName = key.getKey().toLowerCase();

        // URL validation
        if (keyName.contains("url") && !value.matches("^https?://.*")) {
            warnings.add("URL should start with http:// or https://: " + key.getKey());
        }

        // Timeout validation
        if (keyName.contains("timeout")) {
            try {
                int timeout = Integer.parseInt(value);
                if (timeout <= 0) {
                    errors.add("Timeout must be positive: " + key.getKey());
                } else if (timeout > 300) {
                    warnings.add("Very high timeout value (>5 minutes): " + key.getKey());
                }
            } catch (NumberFormatException e) {
                errors.add("Invalid timeout value: " + key.getKey());
            }
        }

        // Thread count validation
        if (keyName.contains("thread")) {
            try {
                int threads = Integer.parseInt(value);
                if (threads <= 0) {
                    errors.add("Thread count must be positive: " + key.getKey());
                } else if (threads > Runtime.getRuntime().availableProcessors() * 2) {
                    warnings.add("Thread count higher than 2x CPU cores: " + key.getKey());
                }
            } catch (NumberFormatException e) {
                errors.add("Invalid thread count: " + key.getKey());
            }
        }
    }

    /**
     * Quick validation - throws exception on first error
     */
    public static void validateRequired() {
        List<String> missingRequired = new ArrayList<>();

        for (ConfigKeys key : ConfigKeys.values()) {
            if (key.isRequired()) {
                try {
                    String value = ConfigManager.resolve(key);
                    if (value == null || value.trim().isEmpty()) {
                        missingRequired.add(key.getKey());
                    }
                } catch (Exception e) {
                    missingRequired.add(key.getKey());
                }
            }
        }

        if (!missingRequired.isEmpty()) {
            String errorMsg = "❌ Missing required configuration keys: " +
                    String.join(", ", missingRequired);
            log.error(errorMsg);
            throw new ConfigurationException(errorMsg);
        }

        log.info("✅ All required configurations are present");
    }

    private static void logValidationResult(ValidationResult result) {
        if (result.isValid()) {
            log.info("✅ Configuration validation passed");
            if (!result.getWarnings().isEmpty()) {
                log.warn("⚠️  Configuration warnings:\n{}",
                        result.getWarnings().stream().collect(Collectors.joining("\n")));
            }
        } else {
            log.error("❌ Configuration validation failed:\n{}",
                    result.getErrors().stream().collect(Collectors.joining("\n")));
            if (!result.getWarnings().isEmpty()) {
                log.warn("⚠️  Additional warnings:\n{}",
                        result.getWarnings().stream().collect(Collectors.joining("\n")));
            }
        }
    }
}