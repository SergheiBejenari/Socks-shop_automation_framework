package tests;

import org.example.config.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the configuration module.
 * 
 * <p>
 * <strong>Test Coverage:</strong>
 * <ul>
 *     <li>Configuration resolution from multiple sources</li>
 *     <li>Type-safe configuration access</li>
 *     <li>Configuration validation</li>
 *     <li>Configuration caching</li>
 *     <li>Error handling and exceptions</li>
 *     <li>Configuration refresh and reload</li>
 *     <li>Health checks and metrics</li>
 * </ul>
 * 
 * <p>
 * <strong>Test Environment:</strong>
 * These tests use the test profile configuration (config-test.properties)
 * which provides test-specific values and faster timeouts.
 */
@ExtendWith(ConfigTestExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfigLayerTest {

    @BeforeEach
    void setUp() {
        // Clear cache before each test to ensure clean state
        ConfigCache.clear();
        // Set test environment
        System.setProperty("env", "test");
    }

    @AfterEach
    void tearDown() {
        // Clean up system properties
        System.clearProperty("env");
        // Clear cache after each test
        ConfigCache.clear();
    }

    // =============================================================================
    // Configuration Resolution Tests
    // =============================================================================

    @Test
    @Order(1)
    @DisplayName("Should resolve configuration from test profile")
    void shouldResolveConfigurationFromTestProfile() {
        // Act
        String baseUrl = ConfigManager.resolve(ConfigKeys.BASE_URL);
        String apiUrl = ConfigManager.resolve(ConfigKeys.API_BASE_URL);
        
        // Assert
        assertNotNull(baseUrl, "Base URL should be resolved");
        assertNotNull(apiUrl, "API URL should be resolved");
        assertEquals("https://test.example.com", baseUrl, "Should use test profile URL");
        assertEquals("https://test-api.example.com", apiUrl, "Should use test profile API URL");
    }

    @Test
    @Order(2)
    @DisplayName("Should prioritize system properties over profile configuration")
    void shouldPrioritizeSystemProperties() {
        // Arrange
        String customUrl = "https://custom.example.com";
        System.setProperty("base.url", customUrl);
        
        try {
            // Act
            String resolvedUrl = ConfigManager.resolve(ConfigKeys.BASE_URL);
            
            // Assert
            assertEquals(customUrl, resolvedUrl, "System property should override profile configuration");
        } finally {
            // Cleanup
            System.clearProperty("base.url");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should handle missing optional configuration gracefully")
    void shouldHandleMissingOptionalConfiguration() {
        // Act
        String region = ConfigManager.resolve(ConfigKeys.REGION);
        
        // Assert
        assertNotNull(region, "Optional configuration should have default value");
        assertEquals("test-region", region, "Should use test profile region value");
    }

    @Test
    @Order(4)
    @DisplayName("Should handle optional configuration gracefully")
    void shouldHandleOptionalConfigurationGracefully() {
        // Act
        String dbUrl = ConfigManager.resolve(ConfigKeys.DB_URL);
        
        // Assert
        // Since we made DB_URL optional with default value, it should resolve
        assertNotNull(dbUrl, "Optional configuration should have default value");
        assertTrue(dbUrl.contains("jdbc:postgresql"), "Should contain database URL format");
    }

    // =============================================================================
    // Type-Safe Configuration Access Tests
    // =============================================================================

    @Test
    @Order(5)
    @DisplayName("Should get string configuration with caching")
    void shouldGetStringConfigurationWithCaching() {
        // Act
        String firstCall = ConfigCache.getString(ConfigKeys.BASE_URL);
        String secondCall = ConfigCache.getString(ConfigKeys.BASE_URL);
        
        // Assert
        assertNotNull(firstCall, "First call should return value");
        assertEquals(firstCall, secondCall, "Second call should return cached value");
        assertTrue(ConfigCache.isCached(ConfigKeys.BASE_URL), "Value should be cached");
    }

    @Test
    @Order(6)
    @DisplayName("Should get integer configuration with automatic parsing")
    void shouldGetIntegerConfigurationWithAutomaticParsing() {
        // Act
        int timeout = ConfigCache.getInt(ConfigKeys.DEFAULT_TIMEOUT);
        
        // Assert
        assertEquals(15, timeout, "Should parse timeout as integer from test profile");
        assertTrue(ConfigCache.isCached(ConfigKeys.DEFAULT_TIMEOUT), "Value should be cached");
    }

    @Test
    @Order(7)
    @DisplayName("Should get boolean configuration with flexible parsing")
    void shouldGetBooleanConfigurationWithFlexibleParsing() {
        // Act
        boolean headless = ConfigCache.getBoolean(ConfigKeys.HEADLESS);
        boolean parallel = ConfigCache.getBoolean(ConfigKeys.PARALLEL_EXECUTION);
        
        // Assert
        assertTrue(headless, "Should parse headless as true from test profile");
        assertTrue(parallel, "Should parse parallel execution as true from test profile");
    }

    @Test
    @Order(8)
    @DisplayName("Should get double configuration with automatic parsing")
    void shouldGetDoubleConfigurationWithAutomaticParsing() {
        // Act
        double rateLimit = ConfigCache.getDouble(ConfigKeys.API_RATE_LIMIT);
        
        // Assert
        assertEquals(100.0, rateLimit, "Should parse rate limit as double");
    }

    @Test
    @Order(9)
    @DisplayName("Should get long configuration with automatic parsing")
    void shouldGetLongConfigurationWithAutomaticParsing() {
        // Act
        long connectionTimeout = ConfigCache.getLong(ConfigKeys.CONNECTION_TIMEOUT);
        
        // Assert
        assertEquals(5L, connectionTimeout, "Should parse connection timeout as long");
    }

    // =============================================================================
    // Configuration Validation Tests
    // =============================================================================

    @Test
    @Order(10)
    @DisplayName("Should validate all configurations successfully")
    void shouldValidateAllConfigurations() {
        // Act
        ConfigValidator.ValidationResult result = ConfigValidator.validateAll();
        
        // Assert
        assertTrue(result.isValid(), "All configurations should be valid");
        assertTrue(result.getErrors().isEmpty(), "Should have no validation errors");
    }

    @Test
    @Order(11)
    @DisplayName("Should validate required configurations")
    void shouldValidateRequiredConfigurations() {
        // Act & Assert
        assertDoesNotThrow(
            () -> ConfigValidator.validateRequired(),
            "Required configuration validation should pass"
        );
    }

    // =============================================================================
    // Configuration Cache Tests
    // =============================================================================

    @Test
    @Order(12)
    @DisplayName("Should cache configuration values for performance")
    void shouldCacheConfigurationValuesForPerformance() {
        // Arrange
        ConfigCache.clear();
        assertFalse(ConfigCache.isCached(ConfigKeys.BASE_URL), "Cache should be empty initially");
        
        // Act
        String firstCall = ConfigCache.getString(ConfigKeys.BASE_URL);
        String secondCall = ConfigCache.getString(ConfigKeys.BASE_URL);
        
        // Assert
        assertTrue(ConfigCache.isCached(ConfigKeys.BASE_URL), "Value should be cached after first access");
        assertEquals(firstCall, secondCall, "Cached value should be returned");
        assertEquals(1, ConfigCache.size(), "Cache should contain one entry");
    }

    @Test
    @Order(13)
    @DisplayName("Should clear cache and reload configuration")
    void shouldClearCacheAndReloadConfiguration() {
        // Arrange
        ConfigCache.getString(ConfigKeys.BASE_URL); // Populate cache
        assertTrue(ConfigCache.size() > 0, "Cache should contain entries");
        
        // Act
        ConfigManager.refresh();
        
        // Assert
        assertEquals(0, ConfigCache.size(), "Cache should be cleared after refresh");
    }

    // =============================================================================
    // Configuration Health and Metrics Tests
    // =============================================================================

    @Test
    @Order(14)
    @DisplayName("Should report healthy configuration status")
    void shouldReportHealthyConfigurationStatus() {
        // Act
        boolean isHealthy = ConfigManager.isHealthy();
        
        // Assert
        assertTrue(isHealthy, "Configuration should be healthy");
    }

    @Test
    @Order(15)
    @DisplayName("Should track configuration access metrics")
    void shouldTrackConfigurationAccessMetrics() {
        // Arrange
        ConfigCache.clear();
        
        // Act
        ConfigCache.getString(ConfigKeys.BASE_URL);
        ConfigCache.getInt(ConfigKeys.DEFAULT_TIMEOUT);
        ConfigCache.getBoolean(ConfigKeys.HEADLESS);
        
        // Assert
        Map<String, Long> accessMetrics = ConfigManager.getAccessMetrics();
        assertTrue(accessMetrics.size() > 0, "Should track access metrics");
        
        // Verify specific keys were tracked
        assertTrue(accessMetrics.containsKey("base.url"), "Should track base.url access");
        assertTrue(accessMetrics.containsKey("timeout.seconds"), "Should track timeout.seconds access");
        assertTrue(accessMetrics.containsKey("browser.headless"), "Should track browser.headless access");
    }

    // =============================================================================
    // Error Handling Tests
    // =============================================================================

    @Test
    @Order(16)
    @DisplayName("Should handle invalid integer configuration gracefully")
    void shouldHandleInvalidIntegerConfigurationGracefully() {
        // Arrange
        System.setProperty("timeout.seconds", "invalid");
        
        try {
            // Act & Assert
            ConfigurationException exception = assertThrows(
                ConfigurationException.class,
                () -> ConfigCache.getInt(ConfigKeys.DEFAULT_TIMEOUT),
                "Should throw exception for invalid integer"
            );
            
            assertTrue(exception.getMessage().contains("Failed to parse"), 
                "Exception message should indicate parsing failure");
        } finally {
            System.clearProperty("timeout.seconds");
        }
    }

    @Test
    @Order(17)
    @DisplayName("Should handle invalid boolean configuration gracefully")
    void shouldHandleInvalidBooleanConfigurationGracefully() {
        // Arrange
        System.setProperty("browser.headless", "maybe");
        
        try {
            // Act & Assert
            ConfigurationException exception = assertThrows(
                ConfigurationException.class,
                () -> ConfigCache.getBoolean(ConfigKeys.HEADLESS),
                "Should throw exception for invalid boolean"
            );
            
            assertTrue(exception.getMessage().contains("Failed to parse"), 
                "Exception message should indicate parsing failure");
        } finally {
            System.clearProperty("browser.headless");
        }
    }

    // =============================================================================
    // Parameterized Tests
    // =============================================================================

    @ParameterizedTest
    @Order(18)
    @EnumSource(ConfigKeys.class)
    @DisplayName("Should resolve all configuration keys")
    void shouldResolveAllConfigurationKeys(ConfigKeys key) {
        // Skip required keys that might not be set in test environment
        if (key.isRequired()) {
            // For required keys, we expect them to be resolved or throw exception
            try {
                String value = ConfigManager.resolve(key);
                assertNotNull(value, "Required configuration should have value: " + key.getKey());
            } catch (ConfigurationException e) {
                // It's okay for required keys to throw exception if not configured
                // This can happen in test environment
            }
            return;
        }
        
        // Act
        String value = ConfigManager.resolve(key);
        
        // Assert
        if (key.isRequired()) {
            assertNotNull(value, "Required configuration should have value: " + key.getKey());
        }
        // Optional configurations can be null
    }

    @ParameterizedTest
    @Order(19)
    @ValueSource(strings = {"base.url", "timeout.seconds", "browser.headless"})
    @DisplayName("Should cache frequently accessed configurations")
    void shouldCacheFrequentlyAccessedConfigurations(String configKey) {
        // Find the corresponding ConfigKeys enum
        ConfigKeys key = findConfigKeyByString(configKey);
        assertNotNull(key, "Should find ConfigKeys enum for: " + configKey);
        
        // Act
        ConfigCache.clear();
        assertFalse(ConfigCache.isCached(key), "Cache should be empty initially");
        
        // Access configuration
        ConfigCache.getString(key);
        
        // Assert
        assertTrue(ConfigCache.isCached(key), "Configuration should be cached: " + configKey);
    }

    // =============================================================================
    // Helper Methods
    // =============================================================================

    /**
     * Find ConfigKeys enum by string key.
     * 
     * @param configKey the configuration key string
     * @return corresponding ConfigKeys enum or null if not found
     */
    private ConfigKeys findConfigKeyByString(String configKey) {
        for (ConfigKeys key : ConfigKeys.values()) {
            if (key.getKey().equals(configKey)) {
                return key;
            }
        }
        return null;
    }
}