package config;

import config.sources.DotEnvFileConfigSource;
import config.sources.PropertiesFileConfigSource;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Thread-safe configuration provider with immutable snapshot and reload capability.
 * Provides strongly-typed access to configuration values with precedence-based resolution.
 */
public class ConfigProvider {

    private static volatile Map<ConfigKey, Object> snapshot;
    private static final Object LOCK = new Object();
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_ONCE = new AtomicBoolean(false);

    static {
        initialize();
    }

    /**
     * Gets a configuration value by key with type safety.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(ConfigKey key) {
        ensureInitialized();
        return (T) snapshot.get(key);
    }

    // Strongly-typed getters for all configuration keys

    public static URI baseUrlUi() {
        return get(ConfigKey.BASE_URL_UI);
    }

    public static URI baseUrlApi() {
        return get(ConfigKey.BASE_URL_API);
    }

    public static int connectTimeoutMs() {
        return get(ConfigKey.CONNECT_TIMEOUT_MS);
    }

    public static int readTimeoutMs() {
        return get(ConfigKey.READ_TIMEOUT_MS);
    }

    public static int writeTimeoutMs() {
        return get(ConfigKey.WRITE_TIMEOUT_MS);
    }

    public static int maxResponseTimeMs() {
        return get(ConfigKey.MAX_RESPONSE_TIME_MS);
    }

    public static int retries() {
        return get(ConfigKey.RETRIES);
    }

    public static int retryBackoffMs() {
        return get(ConfigKey.RETRY_BACKOFF_MS);
    }

    public static boolean headless() {
        return get(ConfigKey.HEADLESS);
    }

    public static String logLevel() {
        return get(ConfigKey.LOG_LEVEL);
    }

    public static boolean allureAttachHttp() {
        return get(ConfigKey.ALLURE_ATTACH_HTTP);
    }

    public static boolean proxyEnabled() {
        return get(ConfigKey.PROXY_ENABLED);
    }

    public static String proxyHost() {
        return get(ConfigKey.PROXY_HOST);
    }

    public static int proxyPort() {
        return get(ConfigKey.PROXY_PORT);
    }

    public static String appEnv() {
        return get(ConfigKey.APP_ENV);
    }

    public static String basicAuthUser() {
        return get(ConfigKey.BASIC_AUTH_USER);
    }

    public static String basicAuthPassword() {
        return get(ConfigKey.BASIC_AUTH_PASSWORD);
    }

    public static String apiToken() {
        return get(ConfigKey.API_TOKEN);
    }

    // Socks-shop service URLs
    public static URI catalogueServiceUrl() {
        return get(ConfigKey.CATALOGUE_SERVICE_URL);
    }

    public static URI userServiceUrl() {
        return get(ConfigKey.USER_SERVICE_URL);
    }

    public static URI ordersServiceUrl() {
        return get(ConfigKey.ORDERS_SERVICE_URL);
    }

    public static URI cartServiceUrl() {
        return get(ConfigKey.CART_SERVICE_URL);
    }

    public static URI paymentServiceUrl() {
        return get(ConfigKey.PAYMENT_SERVICE_URL);
    }

    public static URI shippingServiceUrl() {
        return get(ConfigKey.SHIPPING_SERVICE_URL);
    }

    // Database configuration
    public static String databaseUrl() {
        return get(ConfigKey.DATABASE_URL);
    }

    public static String databaseUser() {
        return get(ConfigKey.DATABASE_USER);
    }

    public static String databasePassword() {
        return get(ConfigKey.DATABASE_PASSWORD);
    }

    // Browser configuration
    public static String browserType() {
        return get(ConfigKey.BROWSER_TYPE);
    }

    public static String browserVersion() {
        return get(ConfigKey.BROWSER_VERSION);
    }

    public static String seleniumGridUrl() {
        return get(ConfigKey.SELENIUM_GRID_URL);
    }

    // Test configuration
    public static String testDataPath() {
        return get(ConfigKey.TEST_DATA_PATH);
    }

    public static boolean cleanupAfterTests() {
        return get(ConfigKey.CLEANUP_AFTER_TESTS);
    }

    public static boolean parallelExecution() {
        return get(ConfigKey.PARALLEL_EXECUTION);
    }

    public static int threadCount() {
        return get(ConfigKey.THREAD_COUNT);
    }

    public static boolean screenshotOnFailure() {
        return get(ConfigKey.SCREENSHOT_ON_FAILURE);
    }

    public static String screenshotPath() {
        return get(ConfigKey.SCREENSHOT_PATH);
    }

    /**
     * Reloads configuration from all sources. Thread-safe.
     */
    public static void reload() {
        reload("Manual reload requested");
    }

    /**
     * Reloads configuration from all sources with a specific reason. Thread-safe.
     */
    public static void reload(String reason) {
        ConfigLogging.logReload(reason);
        synchronized (LOCK) {
            snapshot = buildSnapshot();
            INITIALIZED.set(true);
            LOGGED_ONCE.set(false); // Reset flag to allow re-logging after reload
            startFileWatcher();
            logConfigurationOnce();
        }
    }

    /**
     * Stops background services and clears initialization state so the provider can be safely
     * discarded. Useful for test frameworks or runners that require explicit teardown.
     */
    public static void shutdown() {
        synchronized (LOCK) {
            try {
                FileWatcher.getInstance().stop();
            } finally {
                snapshot = null;
                INITIALIZED.set(false);
                LOGGED_ONCE.set(false);
            }
        }
    }

    /**
     * Returns a masked dump of all configuration values for debugging.
     * Secrets are masked according to the masking strategy.
     */
    public static String dumpMasked() {
        ensureInitialized();
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration dump:\n");

        for (ConfigKey key : ConfigKey.values()) {
            Object value = snapshot.get(key);
            String displayValue = SecretMasker.maskIfSecret(
                    value != null ? value.toString() : "null",
                    key.isSecret()
            );
            sb.append(String.format("  %s = %s\n", key.name(), displayValue));
        }

        return sb.toString();
    }

    private static void initialize() {
        synchronized (LOCK) {
            if (snapshot == null) {
                snapshot = buildSnapshot();
                INITIALIZED.set(true);
                startFileWatcher();
                logConfigurationOnce();
            }
        }
    }

    /**
     * Initialize configuration if not already done.
     * Thread-safe lazy initialization with double-checked locking.
     */
    private static void ensureInitialized() {
        if (!INITIALIZED.get()) {
            initialize();
        }
    }

    /**
     * Start the file watcher to monitor configuration files for changes.
     */
    private static void startFileWatcher() {
        try {
            FileWatcher watcher = FileWatcher.getInstance();
            String profile = resolveProfile();

            // Create CompositeConfig to get actual file paths being used
            CompositeConfig config = new CompositeConfig(profile);

            // Watch the actual configuration files being used
            watcher.watchResource(config.getBaseFilename());
            watcher.watchResource(config.getProfileFilename());
            watcher.watchResource(config.getDotEnvPath());

            // Start the watcher service
            watcher.start();

            ConfigLogging.debug("File watcher configured for profile: {} - watching: {}, {}, {}",
                    profile, config.getBaseFilename(), config.getProfileFilename(), config.getDotEnvPath());
        } catch (Exception e) {
            ConfigLogging.warn("Failed to start file watcher: {} - automatic reload disabled", e.getMessage());
            ConfigLogging.debug("File watcher startup error details", e);
        }
    }

    private static void logConfigurationOnce() {
        if (LOGGED_ONCE.compareAndSet(false, true)) {
            ConfigLogging.logConfigDump(appEnv(), dumpMasked());
        }
    }

    private static Map<ConfigKey, Object> buildSnapshot() {
        // First resolve the profile
        String profile = resolveProfile();
        ConfigLogging.logConfigInit(profile, "Building configuration snapshot");

        // Create composite config with resolved profile using standard naming
        CompositeConfig config = new CompositeConfig(profile);

        Map<ConfigKey, Object> values = new EnumMap<>(ConfigKey.class);

        // Process all configuration keys
        for (ConfigKey key : ConfigKey.values()) {
            Object value = resolveValue(key, config);
            values.put(key, value);

            // Log configuration loading with appropriate level
            if (ConfigLogging.isDebugEnabled()) {
                ConfigLogging.logConfigLoad("composite", key.name(),
                        value != null ? value.toString() : "null", key.isSecret());
            }
        }

        // Detect unknown keys in configuration sources
        detectUnknownKeys(config);

        // Validate proxy configuration consistency
        validateProxyConfiguration(values);

        // Set system property for logback configuration
        String logLevel = (String) values.get(ConfigKey.LOG_LEVEL);
        if (logLevel != null) {
            System.setProperty("ROOT_LOG_LEVEL", logLevel);
        }

        return Map.copyOf(values); // Immutable snapshot
    }

    /**
     * Detects unknown configuration keys in properties files that don't match any ConfigKey.
     * Logs warnings for potential typos or obsolete configuration keys.
     */
    private static void detectUnknownKeys(CompositeConfig config) {
        // Get all known configuration key names (both enum names and system property names)
        Set<String> knownKeys = Arrays.stream(ConfigKey.values())
                .flatMap(key -> Stream.of(key.name(), key.getSysPropName()))
                .collect(Collectors.toSet());

        // Check each configuration source for unknown keys (only file-based sources)
        List<ConfigSource> fileSources = config.getSources().stream()
                .filter(source -> source instanceof PropertiesFileConfigSource
                        || source instanceof DotEnvFileConfigSource)
                .toList();

        for (ConfigSource source : fileSources) {
            Set<String> sourceKeys = source.getAllKeys();
            if (sourceKeys.isEmpty()) {
                continue; // Skip sources that don't implement getAllKeys()
            }

            for (String sourceKey : sourceKeys) {
                boolean isKnown = false;

                // Check against known property names and enum names
                if (knownKeys.contains(sourceKey)) {
                    isKnown = true;
                } else {
                    // Check if it's a valid environment variable name or system property for a known key
                    for (ConfigKey configKey : ConfigKey.values()) {
                        if (sourceKey.equals(configKey.name()) ||
                                sourceKey.equals(configKey.getSysPropName()) ||
                                sourceKey.equals(configKey.getEnvVarName())) {
                            isKnown = true;
                            break;
                        }
                    }
                }

                if (!isKnown) {
                    // Check for potential typos by finding similar key names
                    String suggestion = findSimilarKey(sourceKey, knownKeys);
                    if (suggestion != null) {
                        ConfigLogging.warn("Unknown configuration key '{}' in {} - Did you mean '{}'?",
                                sourceKey, source.id(), suggestion);
                    } else {
                        ConfigLogging.warn("Unknown configuration key '{}' in {} - This key is not recognized and will be ignored",
                                sourceKey, source.id());
                    }
                }
            }
        }
    }

    /**
     * Finds a similar key name using simple string similarity.
     * Returns the most similar known key if similarity is high enough.
     */
    private static String findSimilarKey(String unknownKey, Set<String> knownKeys) {
        String bestMatch = null;
        int bestScore = Integer.MAX_VALUE;
        int threshold = Math.max(2, unknownKey.length() / 3); // Allow up to 1/3 character differences

        for (String knownKey : knownKeys) {
            int distance = levenshteinDistance(unknownKey.toLowerCase(), knownKey.toLowerCase());
            if (distance < bestScore && distance <= threshold) {
                bestScore = distance;
                bestMatch = knownKey;
            }
        }

        return bestMatch;
    }

    /**
     * Calculates Levenshtein distance between two strings.
     */
    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private static void validateProxyConfiguration(Map<ConfigKey, Object> values) {
        boolean proxyEnabled = (Boolean) values.get(ConfigKey.PROXY_ENABLED);
        String proxyHost = (String) values.get(ConfigKey.PROXY_HOST);
        int proxyPort = (Integer) values.get(ConfigKey.PROXY_PORT);

        try {
            Validators.validateProxyConfig(proxyEnabled, proxyHost, proxyPort);
            ConfigLogging.logValidation("PROXY_CONFIG", "Proxy configuration validation passed");
        } catch (IllegalStateException e) {
            ConfigLogging.logValidationError("PROXY_CONFIG",
                    String.format("enabled=%s, host=%s, port=%d", proxyEnabled, proxyHost, proxyPort),
                    e.getMessage());
            throw new IllegalStateException("Proxy configuration validation failed: " + e.getMessage());
        }
    }

    private static String resolveProfile() {
        // Priority: APP_ENV env var > app.env sysprop > default "local"
        String profile = System.getenv("APP_ENV");
        if (profile == null || profile.trim().isEmpty()) {
            ConfigLogging.debug("APP_ENV not found, checking system property");
            profile = System.getProperty("app.env");
        } else {
            ConfigLogging.debug("Using profile from APP_ENV: {}", profile);
        }

        if (profile == null || profile.trim().isEmpty()) {
            profile = "local";
            ConfigLogging.logDefaultValue("APP_ENV", profile);
        }

        // Validate profile
        try {
            profile = profile.trim().toLowerCase();
            String validatedProfile = Validators.validateAppEnv(profile);
            ConfigLogging.logValidation("APP_ENV", "Profile validation passed: " + validatedProfile);
            return validatedProfile;
        } catch (IllegalStateException e) {
            ConfigLogging.logValidationError("APP_ENV", profile, e.getMessage());
            throw new IllegalStateException("Invalid profile resolution: " + e.getMessage());
        }
    }

    private static Object resolveValue(ConfigKey key, CompositeConfig config) {
        // Try to get value from configuration sources
        String rawValue = null;
        String sourceName = "default";

        ConfigLogging.trace("Resolving value for key: {}", key.name());

        // Check environment variable name
        var envValue = config.get(key.getEnvVarName());
        if (envValue.isPresent()) {
            rawValue = envValue.get();
            sourceName = config.getSourceFor(key.getEnvVarName()).map(ConfigSource::id).orElse("unknown");
        }

        // Check system property name if not found in env
        if (rawValue == null) {
            var sysPropValue = config.get(key.getSysPropName());
            if (sysPropValue.isPresent()) {
                rawValue = sysPropValue.get();
                sourceName = config.getSourceFor(key.getSysPropName()).map(ConfigSource::id).orElse("unknown");
                ConfigLogging.trace("Found value for {} in source: {}", key.name(), sourceName);
            }
        }

        // Use default if no value found
        if (rawValue == null) {
            Object defaultValue = key.getDefault();
            ConfigLogging.logDefaultValue(key.name(), defaultValue);
            return defaultValue;
        }

        // Parse and validate the value
        try {
            Object parsedValue = key.parse(rawValue);
            Object validatedValue = key.validate(parsedValue);

            ConfigLogging.trace("Successfully resolved {} from {}: {}",
                    key.name(), sourceName, key.isSecret() ? "***" : validatedValue);

            return validatedValue;
        } catch (Exception e) {
            String errorMsg = String.format("Invalid value for %s='%s' from source %s: %s",
                    key.name(), rawValue, sourceName, e.getMessage());
            ConfigLogging.logValidationError(key.name(), rawValue, e.getMessage());
            throw new IllegalStateException(errorMsg, e);
        }
    }
}
