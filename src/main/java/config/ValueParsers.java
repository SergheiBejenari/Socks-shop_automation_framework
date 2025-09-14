package config;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utility class for parsing string values into typed objects.
 * Provides descriptive error messages that include the invalid value.
 */
public class ValueParsers {
    
    /**
     * Parses a string value to an integer.
     */
    public static Integer toInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Integer value cannot be null or empty");
        }
        
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value: '" + value + "'");
        }
    }
    
    /**
     * Parses a string value to a boolean.
     * Accepts: true/false (case-insensitive), 1/0, yes/no, on/off.
     */
    public static Boolean toBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Boolean value cannot be null or empty");
        }
        
        String normalized = value.trim().toLowerCase();
        
        switch (normalized) {
            case "true":
            case "1":
            case "yes":
            case "on":
                return true;
            case "false":
            case "0":
            case "no":
            case "off":
                return false;
            default:
                throw new IllegalArgumentException("Invalid boolean value: '" + value + "'. Expected: true/false, 1/0, yes/no, on/off");
        }
    }
    
    /**
     * Parses a string value to a URI.
     */
    public static URI toUri(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("URI value cannot be null or empty");
        }
        
        try {
            return new URI(value.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI value: '" + value + "'. " + e.getMessage());
        }
    }
    
    /**
     * Validates and normalizes log level strings.
     */
    public static String toLogLevel(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Log level cannot be null or empty");
        }
        
        return value.trim().toUpperCase();
    }
}
