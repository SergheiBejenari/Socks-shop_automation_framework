package org.example.config;

import lombok.Getter;

/**
 * Comprehensive configuration keys for a modern test automation framework.
 * <p>
 * This enum centralizes all configuration keys used in the system, including:
 * <ul>
 *     <li>Environment & Infrastructure</li>
 *     <li>Timeouts & Retries</li>
 *     <li>Database configuration</li>
 *     <li>API Testing</li>
 *     <li>UI Testing (Selenium/Playwright)</li>
 *     <li>Test Execution</li>
 *     <li>Security & Authentication</li>
 *     <li>Monitoring & Observability</li>
 *     <li>Cloud & CI/CD</li>
 *     <li>Test Environment Specific</li>
 *     <li>Notifications</li>
 * </ul>
 * <p>
 * Each enum value stores:
 * <ul>
 *     <li><b>key</b>: the string key for the property (matches environment variable or property file key)</li>
 *     <li><b>defaultValue</b>: optional default value if not provided externally</li>
 *     <li><b>required</b>: whether this config is mandatory (throws exception if missing)</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>
 *   String apiBase = ConfigCache.getString(ConfigKeys.API_BASE_URL);
 *   int timeout = ConfigCache.getInt(ConfigKeys.DEFAULT_TIMEOUT);
 * </pre>
 */
@Getter
public enum ConfigKeys {

    // 🌍 Environment & Infrastructure
    ENVIRONMENT("env", "dev", false),
    BASE_URL("base.url", null, true),
    REGION("region", "us-east-1", false),

    // ⏱️ Timeouts & Retries
    DEFAULT_TIMEOUT("timeout.seconds", "30", false),
    CONNECTION_TIMEOUT("connection.timeout.seconds", "10", false),
    READ_TIMEOUT("read.timeout.seconds", "30", false),
    RETRY_ATTEMPTS("retry.attempts", "3", false),
    RETRY_DELAY("retry.delay.seconds", "1", false),

    // 🗄️ Database Configuration
    DB_URL("db.url", "jdbc:postgresql://localhost:5432/test_db", false),
    DB_USER("db.user", "test_user", false),
    DB_PASSWORD("db.password", "test_password", false),
    DB_DRIVER("db.driver", "org.postgresql.Driver", false),
    DB_POOL_SIZE("db.pool.size", "10", false),

    // 🌐 API Testing
    API_BASE_URL("api.base.url", null, true),
    API_VERSION("api.version", "v1", false),
    API_TOKEN("api.token", null, false),
    API_RATE_LIMIT("api.rate.limit", "100", false),
    API_TIMEOUT("api.timeout.seconds", "60", false),

    // 🖥️ UI Testing (Selenium/Playwright)
    BROWSER("browser", "chrome", false),
    BROWSER_VERSION("browser.version", "latest", false),
    HEADLESS("browser.headless", "false", false),
    WINDOW_SIZE("browser.window.size", "1920x1080", false),
    IMPLICIT_WAIT("ui.implicit.wait.seconds", "10", false),
    PAGE_LOAD_TIMEOUT("ui.page.load.timeout.seconds", "30", false),
    SCRIPT_TIMEOUT("ui.script.timeout.seconds", "30", false),

    // 📸 Screenshots & Reports
    SCREENSHOT_ON_FAILURE("ui.screenshot.on.failure", "true", false),
    SCREENSHOT_ON_SUCCESS("ui.screenshot.on.success", "false", false),
    SCREENSHOT_DIRECTORY("ui.screenshot.directory", "target/screenshots", false),
    VIDEO_RECORDING("ui.video.recording", "false", false),

    // 🔄 Test Execution
    PARALLEL_EXECUTION("test.parallel", "false", false),
    THREAD_COUNT("test.thread.count", "1", false),
    TEST_DATA_PATH("test.data.path", "src/test/resources/testdata", false),

    // 🔐 Security & Authentication
    OAUTH_CLIENT_ID("oauth.client.id", null, false),
    OAUTH_CLIENT_SECRET("oauth.client.secret", null, false),
    JWT_SECRET("jwt.secret", null, false),
    ENCRYPTION_KEY("encryption.key", null, false),

    // 📊 Monitoring & Observability
    METRICS_ENABLED("metrics.enabled", "false", false),
    HEALTH_CHECK_ENABLED("health.check.enabled", "true", false),
    LOG_LEVEL("log.level", "INFO", false),

    // ☁️ Cloud & CI/CD
    GRID_URL("selenium.grid.url", null, false),
    DOCKER_ENABLED("docker.enabled", "false", false),
    CI_PIPELINE("ci.pipeline", "false", false),

    // 🧪 Test Environment Specific
    MOCK_EXTERNAL_SERVICES("mock.external.services", "false", false),
    STUB_RESPONSES("stub.responses", "false", false),

    // 📧 Notifications
    SLACK_WEBHOOK("slack.webhook.url", null, false),
    EMAIL_NOTIFICATIONS("email.notifications.enabled", "false", false);

    private final String key;
    private final String defaultValue;
    private final boolean required;

    ConfigKeys(String key, String defaultValue, boolean required) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.required = required;
    }

    /**
     * Check if the configuration key is optional.
     *
     * @return true if the key is not required, false otherwise
     */
    public boolean isOptional() {
        return !required;
    }

    /**
     * Retrieve the effective value, falling back to default if needed.
     * This can be handy for optional configs without using the cache directly.
     *
     * @return effective String value
     */
    public String getEffectiveValue() {
        String value = ConfigCache.getString(this);
        return value != null ? value : defaultValue;
    }
}
