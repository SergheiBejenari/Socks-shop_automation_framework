package config;

import java.net.URI;
import java.util.Set;

/**
 * Utility class for validating configuration values.
 * Provides descriptive error messages that include the key, source, and invalid value.
 */
public class Validators {
    
    private static final Set<String> VALID_LOG_LEVELS = Set.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR");
    private static final Set<String> VALID_APP_ENVS = Set.of("local", "dev", "ci", "qa", "stage", "prod");
    
    /**
     * No-op validator for values that don't need validation.
     */
    public static <T> T noValidation(T value) {
        return value;
    }
    
    /**
     * Validates that an integer is within the specified range (inclusive).
     */
    public static Object validateIntRange(Integer value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                String.format("Value %d is not in valid range [%d, %d]", value, min, max)
            );
        }
        return value;
    }
    
    /**
     * Validates that a URI is absolute and uses http or https scheme.
     */
    public static URI validateHttpUri(URI uri) {
        if (!uri.isAbsolute()) {
            throw new IllegalStateException("URI must be absolute: " + uri);
        }
        
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new IllegalStateException("URI must use http or https scheme: " + uri);
        }
        
        return uri;
    }
    
    /**
     * Validates log level against allowed values.
     */
    public static String validateLogLevel(String logLevel) {
        if (!VALID_LOG_LEVELS.contains(logLevel)) {
            throw new IllegalStateException(
                "Invalid log level: '" + logLevel + "'. Must be one of: " + VALID_LOG_LEVELS
            );
        }
        return logLevel;
    }
    
    /**
     * Validates app environment against allowed values.
     */
    public static String validateAppEnv(String appEnv) {
        if (!VALID_APP_ENVS.contains(appEnv)) {
            throw new IllegalStateException(
                "Invalid app environment: '" + appEnv + "'. Must be one of: " + VALID_APP_ENVS
            );
        }
        return appEnv;
    }
    
    /**
     * Validates browser type against supported browsers.
     */
    public static String validateBrowserType(String browserType) {
        Set<String> validBrowsers = Set.of("chrome", "firefox", "edge", "safari", "chromium");
        if (!validBrowsers.contains(browserType.toLowerCase())) {
            throw new IllegalArgumentException(
                String.format("Invalid browser type '%s'. Valid options: %s", browserType, validBrowsers)
            );
        }
        return browserType;
    }
    
    /**
     * Validates proxy configuration consistency.
     * If proxy is enabled, host must be non-empty and port must be > 0.
     * If proxy is disabled, port should be 0.
     */
    public static void validateProxyConfig(boolean proxyEnabled, String proxyHost, int proxyPort) {
        if (proxyEnabled) {
            if (proxyHost == null || proxyHost.trim().isEmpty()) {
                throw new IllegalStateException("Proxy host must be non-empty when proxy is enabled");
            }
            if (proxyPort <= 0) {
                throw new IllegalStateException("Proxy port must be > 0 when proxy is enabled, got: " + proxyPort);
            }
        } else {
            if (proxyPort != 0) {
                throw new IllegalStateException("Proxy port should be 0 when proxy is disabled, got: " + proxyPort);
            }
        }
    }
}
