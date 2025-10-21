package config;

import java.net.URI;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Enumeration of all configuration keys with their metadata including
 * environment variable names, system property names, types, defaults, and validators.
 */
public enum ConfigKey {
    BASE_URL_UI(
            "BASE_URL_UI",
            "baseUrlUi",
            URI.class,
            () -> URI.create("http://localhost:8080"),
            ValueParsers::toUri,
            value -> Validators.validateHttpUri((URI) value),
            false
    ),

    BASE_URL_API(
            "BASE_URL_API",
            "baseUrlApi",
            URI.class,
            () -> URI.create("http://localhost:8080"),
            ValueParsers::toUri,
            value -> Validators.validateHttpUri((URI) value),
            false
    ),

    CONNECT_TIMEOUT_MS(
            "CONNECT_TIMEOUT_MS",
            "connectTimeoutMs",
            Integer.class,
            () -> 3000,
            ValueParsers::toInt,
            value -> Validators.validateIntRange((Integer) value, 0, 120000),
            false
    ),

    READ_TIMEOUT_MS(
            "READ_TIMEOUT_MS",
            "readTimeoutMs",
            Integer.class,
            () -> 5000,
            ValueParsers::toInt,
            value -> Validators.validateIntRange((Integer) value, 0, 120000),
            false
    ),

    WRITE_TIMEOUT_MS(
            "WRITE_TIMEOUT_MS",
            "writeTimeoutMs",
            Integer.class,
            () -> 5000,
            ValueParsers::toInt,
            value -> Validators.validateIntRange((Integer) value, 0, 120000),
            false
    ),

    MAX_RESPONSE_TIME_MS(
            "MAX_RESPONSE_TIME_MS",
            "maxResponseTimeMs",
            Integer.class,
            () -> 1500,
            ValueParsers::toInt,
            value -> Validators.validateIntRange((Integer) value, 0, 60000),
            false
    ),

    RETRIES(
            "RETRIES",
            "retries",
            Integer.class,
            () -> 2,
            ValueParsers::toInt,
            value -> Validators.validateIntRange((Integer) value, 0, 10),
            false
    ),

    RETRY_BACKOFF_MS(
            "RETRY_BACKOFF_MS",
            "retryBackoffMs",
            Integer.class,
            () -> 250,
            ValueParsers::toInt,
            value -> Validators.validateIntRange((Integer) value, 0, 10000),
            false
    ),

    HEADLESS(
            "HEADLESS",
            "headless",
            Boolean.class,
            () -> true,
            ValueParsers::toBoolean,
            Validators::noValidation,
            false
    ),

    LOG_LEVEL(
            "LOG_LEVEL",
            "logLevel",
            String.class,
            () -> "INFO",
            ValueParsers::toLogLevel,
            value -> Validators.validateLogLevel((String) value),
            false
    ),

    ALLURE_ATTACH_HTTP(
            "ALLURE_ATTACH_HTTP",
            "allureAttachHttp",
            Boolean.class,
            () -> true,
            ValueParsers::toBoolean,
            Validators::noValidation,
            false
    ),

    PROXY_ENABLED(
            "PROXY_ENABLED",
            "proxyEnabled",
            Boolean.class,
            () -> false,
            ValueParsers::toBoolean,
            Validators::noValidation,
            false
    ),

    PROXY_HOST(
            "PROXY_HOST",
            "proxyHost",
            String.class,
            () -> "",
            value -> value,
            Validators::noValidation,
            false
    ),

    PROXY_PORT(
            "PROXY_PORT",
            "proxyPort",
            Integer.class,
            () -> 0,
            ValueParsers::toInt,
            value -> Validators.validateIntRange((Integer) value, 0, 65535),
            false
    ),

    APP_ENV(
            "APP_ENV",
            "app.env",
            String.class,
            () -> "local",
            ValueParsers::toAppEnv,
            value -> Validators.validateAppEnv((String) value),
            false
    ),

    BASIC_AUTH_USER(
            "BASIC_AUTH_USER",
            "basicAuthUser",
            String.class,
            () -> "",
            value -> value,
            Validators::noValidation,
            true
    ),

    BASIC_AUTH_PASSWORD(
            "BASIC_AUTH_PASSWORD",
            "basicAuthPassword",
            String.class,
            () -> "",
            value -> value,
            Validators::noValidation,
            true
    ),

    API_TOKEN(
            "API_TOKEN",
            "apiToken",
            String.class,
            () -> "",
            value -> value,
            Validators::noValidation,
            true
    ),

    // Socks-shop specific service URLs
    CATALOGUE_SERVICE_URL(
            "CATALOGUE_SERVICE_URL",
            "catalogueServiceUrl",
            URI.class,
            () -> URI.create("http://localhost:8080/catalogue"),
            ValueParsers::toUri,
            value -> Validators.validateHttpUri((URI) value),
            false
    ),

    USER_SERVICE_URL(
            "USER_SERVICE_URL",
            "userServiceUrl",
            URI.class,
            () -> URI.create("http://localhost:8080/customers"),
            ValueParsers::toUri,
            value -> Validators.validateHttpUri((URI) value),
            false
    ),

    ORDERS_SERVICE_URL(
            "ORDERS_SERVICE_URL",
            "ordersServiceUrl",
            URI.class,
            () -> URI.create("http://localhost:8080/orders"),
            ValueParsers::toUri,
            value -> Validators.validateHttpUri((URI) value),
            false
    ),

    CART_SERVICE_URL(
            "CART_SERVICE_URL",
            "cartServiceUrl",
            URI.class,
            () -> URI.create("http://localhost:8080/carts"),
            ValueParsers::toUri,
            value -> Validators.validateHttpUri((URI) value),
            false
    ),

    PAYMENT_SERVICE_URL(
            "PAYMENT_SERVICE_URL",
            "paymentServiceUrl",
            URI.class,
            () -> URI.create("http://localhost:8080/payment"),
            ValueParsers::toUri,
            value -> Validators.validateHttpUri((URI) value),
            false
    ),

    SHIPPING_SERVICE_URL(
            "SHIPPING_SERVICE_URL",
            "shippingServiceUrl",
            URI.class,
            () -> URI.create("http://localhost:8080/shipping"),
            ValueParsers::toUri,
            value -> Validators.validateHttpUri((URI) value),
            false
    ),

    // Database configuration
    DATABASE_URL(
            "DATABASE_URL",
            "databaseUrl",
            String.class,
            () -> "jdbc:mysql://localhost:3306/socksdb",
            value -> value,
            Validators::noValidation,
            false
    ),

    DATABASE_USER(
            "DATABASE_USER",
            "databaseUser",
            String.class,
            () -> "catalogue_user",
            value -> value,
            Validators::noValidation,
            true
    ),

    DATABASE_PASSWORD(
            "DATABASE_PASSWORD",
            "databasePassword",
            String.class,
            () -> "default_password",
            value -> value,
            Validators::noValidation,
            true
    ),

    // Browser configuration
    BROWSER_TYPE(
            "BROWSER_TYPE",
            "browserType",
            String.class,
            () -> "chrome",
            value -> value,
            value -> Validators.validateBrowserType((String) value),
            false
    ),

    BROWSER_VERSION(
            "BROWSER_VERSION",
            "browserVersion",
            String.class,
            () -> "latest",
            value -> value,
            Validators::noValidation,
            false
    ),

    SELENIUM_GRID_URL(
            "SELENIUM_GRID_URL",
            "seleniumGridUrl",
            String.class,
            () -> "",
            value -> value,
            Validators::noValidation,
            false
    ),

    // Test data management
    TEST_DATA_PATH(
            "TEST_DATA_PATH",
            "testDataPath",
            String.class,
            () -> "src/test/resources/testdata",
            value -> value,
            Validators::noValidation,
            false
    ),

    CLEANUP_AFTER_TESTS(
            "CLEANUP_AFTER_TESTS",
            "cleanupAfterTests",
            Boolean.class,
            () -> true,
            ValueParsers::toBoolean,
            Validators::noValidation,
            false
    ),

    // Test execution settings
    PARALLEL_EXECUTION(
            "PARALLEL_EXECUTION",
            "parallelExecution",
            Boolean.class,
            () -> false,
            ValueParsers::toBoolean,
            Validators::noValidation,
            false
    ),

    THREAD_COUNT(
            "THREAD_COUNT",
            "threadCount",
            Integer.class,
            () -> 1,
            ValueParsers::toInt,
            value -> Validators.validateIntRange((Integer) value, 1, 10),
            false
    ),

    // Screenshots and reporting
    SCREENSHOT_ON_FAILURE(
            "SCREENSHOT_ON_FAILURE",
            "screenshotOnFailure",
            Boolean.class,
            () -> true,
            ValueParsers::toBoolean,
            Validators::noValidation,
            false
    ),

    SCREENSHOT_PATH(
            "SCREENSHOT_PATH",
            "screenshotPath",
            String.class,
            () -> "build/screenshots",
            value -> value,
            Validators::noValidation,
            false
    );

    private final String envVarName;
    private final String sysPropName;
    private final Class<?> type;
    private final Supplier<Object> defaultSupplier;
    private final Function<String, Object> parser;
    private final Function<Object, Object> validator;
    private final boolean isSecret;

    ConfigKey(String envVarName, String sysPropName, Class<?> type,
              Supplier<Object> defaultSupplier, Function<String, Object> parser,
              Function<Object, Object> validator, boolean isSecret) {
        this.envVarName = envVarName;
        this.sysPropName = sysPropName;
        this.type = type;
        this.defaultSupplier = defaultSupplier;
        this.parser = parser;
        this.validator = validator;
        this.isSecret = isSecret;
    }

    public String getEnvVarName() {
        return envVarName;
    }

    public String getSysPropName() {
        return sysPropName;
    }

    public Class<?> getType() {
        return type;
    }

    public Object getDefault() {
        return defaultSupplier.get();
    }

    public Object parse(String value) {
        return parser.apply(value);
    }

    public Object validate(Object value) {
        return validator.apply(value);
    }

    public boolean isSecret() {
        return isSecret;
    }
}
