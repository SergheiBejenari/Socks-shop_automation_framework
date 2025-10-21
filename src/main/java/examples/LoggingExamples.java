package examples;

import logging.Logger;
import logging.LoggerFactory;

/**
 * Examples demonstrating the universal logging system usage.
 *
 * <p>This class shows various logging patterns and best practices
 * for different scenarios in the socks-shop automation framework.
 */
public class LoggingExamples {

    // Class-based logger (recommended for most cases)
    private static final Logger log = LoggerFactory.getLogger(LoggingExamples.class);

    // Named loggers for specific domains
    private static final Logger apiLog = LoggerFactory.getLogger("api");
    private static final Logger dbLog = LoggerFactory.getLogger("database");
    private static final Logger testLog = LoggerFactory.getLogger("test");

    /**
     * Basic logging examples.
     */
    public static void basicLoggingExamples() {
        // Different log levels
        log.trace("Very detailed debugging information");
        log.debug("Debug information for troubleshooting");
        log.info("General information about application flow");
        log.warn("Warning about potential issues");
        log.error("Error that occurred but application can continue");

        // Parameterized logging (preferred over string concatenation)
        String userId = "12345";
        int attemptCount = 3;
        log.info("User {} login attempt #{}", userId, attemptCount);

        // Exception logging
        try {
            // Some operation that might fail
            throw new RuntimeException("Something went wrong");
        } catch (Exception e) {
            log.error("Failed to process user request", e);
        }
    }

    /**
     * Structured logging with context examples.
     */
    public static void structuredLoggingExamples() {
        String userId = "user123";
        String sessionId = "sess456";
        String operation = "login";

        // Single context
        log.withContext("userId", userId)
                .info("User started login process");

        // Multiple contexts
        log.withContext("userId", userId)
                .withContext("sessionId", sessionId)
                .withContext("operation", operation)
                .info("Authentication successful");

        // Context with different log levels
        log.withContext("userId", userId)
                .withContext("errorCode", "AUTH_FAILED")
                .error("Authentication failed for user");
    }

    /**
     * API testing logging examples.
     */
    public static void apiTestingExamples() {
        String endpoint = "/api/catalogue";
        int responseTime = 150;
        int statusCode = 200;

        // API request logging
        apiLog.withContext("endpoint", endpoint)
                .withContext("method", "GET")
                .info("Sending API request");

        // API response logging
        apiLog.withContext("endpoint", endpoint)
                .withContext("statusCode", String.valueOf(statusCode))
                .withContext("responseTime", String.valueOf(responseTime))
                .info("API response received in {}ms", responseTime);

        // Performance monitoring
        if (responseTime > 1000) {
            apiLog.withContext("endpoint", endpoint)
                    .withContext("responseTime", String.valueOf(responseTime))
                    .warn("Slow API response detected: {}ms", responseTime);
        }
    }

    /**
     * UI testing logging examples.
     */
    public static void uiTestingExamples() {
        String testName = "loginTest";
        String browserType = "chrome";
        String pageUrl = "http://localhost:8080/login";

        // Test execution logging
        testLog.withContext("testName", testName)
                .withContext("browser", browserType)
                .info("Starting UI test");

        // Page interaction logging
        testLog.withContext("testName", testName)
                .withContext("pageUrl", pageUrl)
                .withContext("action", "navigate")
                .debug("Navigating to page: {}", pageUrl);

        // Element interaction logging
        testLog.withContext("testName", testName)
                .withContext("element", "loginButton")
                .withContext("action", "click")
                .debug("Clicking login button");

        // Test result logging
        testLog.withContext("testName", testName)
                .withContext("result", "PASSED")
                .withContext("duration", "2.5s")
                .info("Test completed successfully");
    }

    /**
     * Database operations logging examples.
     */
    public static void databaseExamples() {
        String query = "SELECT * FROM users WHERE id = ?";
        String userId = "123";

        // Query execution logging
        dbLog.withContext("operation", "SELECT")
                .withContext("table", "users")
                .withContext("query", query)
                .debug("Executing database query: {}", query);

        // Performance logging
        long startTime = System.currentTimeMillis();
        // ... execute query ...
        long duration = System.currentTimeMillis() - startTime;

        dbLog.withContext("operation", "SELECT")
                .withContext("table", "users")
                .withContext("duration", String.valueOf(duration))
                .info("Query executed in {}ms", duration);

        // Error logging
        try {
            // Database operation
            throw new RuntimeException("Connection timeout");
        } catch (Exception e) {
            dbLog.withContext("operation", "SELECT")
                    .withContext("table", "users")
                    .withContext("userId", userId)
                    .error("Database operation failed", e);
        }
    }

    /**
     * Configuration logging examples.
     */
    public static void configurationExamples() {
        String profile = "dev";
        String configKey = "baseUrlApi";
        String configValue = "https://dev.socks-shop.com/api";

        // Configuration loading
        log.withContext("configProfile", profile)
                .withContext("configKey", configKey)
                .debug("Loading configuration: {}={}", configKey, configValue);

        // Configuration validation
        log.withContext("configProfile", profile)
                .withContext("configKey", configKey)
                .info("Configuration validation passed");

        // Configuration errors
        log.withContext("configProfile", profile)
                .withContext("configKey", configKey)
                .withContext("configValue", configValue)
                .error("Invalid configuration value");
    }

    /**
     * Performance monitoring examples.
     */
    public static void performanceExamples() {
        String operationName = "userRegistration";

        // Method entry
        log.withContext("operation", operationName)
                .trace("Entering method: {}", operationName);

        // Performance measurement
        long startTime = System.currentTimeMillis();

        try {
            // Simulate some work
            Thread.sleep(100);

            long duration = System.currentTimeMillis() - startTime;

            // Success with timing
            log.withContext("operation", operationName)
                    .withContext("duration", String.valueOf(duration))
                    .info("Operation completed successfully in {}ms", duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Failure with timing
            log.withContext("operation", operationName)
                    .withContext("duration", String.valueOf(duration))
                    .error("Operation failed after {}ms", duration, e);
        }
    }

    /**
     * Conditional logging examples (for performance).
     */
    public static void conditionalLoggingExamples() {
        String expensiveData = "some expensive computation result";

        // Check if debug is enabled before expensive operations
        if (log.isDebugEnabled()) {
            String debugInfo = computeExpensiveDebugInfo();
            log.debug("Debug information: {}", debugInfo);
        }

        // Trace logging for very detailed information
        if (log.isTraceEnabled()) {
            log.trace("Very detailed trace information: {}", expensiveData);
        }
    }

    private static String computeExpensiveDebugInfo() {
        // Simulate expensive computation
        return "expensive debug data";
    }

    /**
     * Main method to run examples.
     */
    public static void main(String[] args) {
        log.info("Starting logging examples demonstration");

        basicLoggingExamples();
        structuredLoggingExamples();
        apiTestingExamples();
        uiTestingExamples();
        databaseExamples();
        configurationExamples();
        performanceExamples();
        conditionalLoggingExamples();

        log.info("Logging examples demonstration completed");
    }
}
