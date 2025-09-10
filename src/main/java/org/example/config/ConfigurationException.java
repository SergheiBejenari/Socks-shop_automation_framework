package org.example.config;

/**
 * Custom exception for configuration-related errors.
 * 
 * This exception is thrown when:
 * <ul>
 *     <li>Required configuration values are missing</li>
 *     <li>Configuration values cannot be parsed</li>
 *     <li>Configuration files cannot be loaded</li>
 *     <li>Configuration validation fails</li>
 * </ul>
 * 
 * Usage example:
 * <pre>
 * try {
 *     String apiUrl = ConfigManager.getString(ConfigKeys.API_BASE_URL);
 * } catch (ConfigurationException e) {
 *     log.error("Failed to get API URL: {}", e.getMessage());
 *     // Handle configuration error
 * }
 * </pre>
 */
public class ConfigurationException extends RuntimeException {

    /**
     * Constructs a new ConfigurationException with the specified detail message.
     *
     * @param message the detail message
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a new ConfigurationException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause (which is saved for later retrieval by the getCause() method)
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new ConfigurationException with the specified cause.
     *
     * @param cause the cause (which is saved for later retrieval by the getCause() method)
     */
    public ConfigurationException(Throwable cause) {
        super(cause);
    }
}
