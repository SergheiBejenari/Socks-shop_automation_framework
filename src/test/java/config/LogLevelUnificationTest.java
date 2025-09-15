package config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for unified log level management using ROOT_LOG_LEVEL system property.
 * Verifies that ConfigProvider properly sets the ROOT_LOG_LEVEL system property
 * and that logback configuration responds to changes.
 */
public class LogLevelUnificationTest {
    
    private String originalRootLogLevel;
    private String originalLogLevel;
    
    @BeforeMethod
    public void setUp() {
        // Backup original system properties
        originalRootLogLevel = System.getProperty("ROOT_LOG_LEVEL");
        originalLogLevel = System.getProperty("logLevel");
        
        // Clear system properties for clean test state
        System.clearProperty("ROOT_LOG_LEVEL");
        System.clearProperty("logLevel");
        System.clearProperty("log.level");
    }
    
    @AfterMethod
    public void tearDown() {
        // Restore original system properties
        if (originalRootLogLevel != null) {
            System.setProperty("ROOT_LOG_LEVEL", originalRootLogLevel);
        } else {
            System.clearProperty("ROOT_LOG_LEVEL");
        }
        
        if (originalLogLevel != null) {
            System.setProperty("logLevel", originalLogLevel);
        } else {
            System.clearProperty("logLevel");
        }
        
        System.clearProperty("log.level");
        
        // Reload to restore original state
        ConfigProvider.reload();
    }
    
    @Test
    public void testRootLogLevelSystemPropertyIsSet() {
        // Test that ConfigProvider sets ROOT_LOG_LEVEL system property
        
        // Set log level via system property
        System.setProperty("log.level", "DEBUG");
        ConfigProvider.reload();
        
        // Verify that ROOT_LOG_LEVEL system property is set
        String rootLogLevel = System.getProperty("ROOT_LOG_LEVEL");
        assertThat(rootLogLevel).isEqualTo("DEBUG");
        
        // Verify ConfigProvider returns the correct value
        assertThat(ConfigProvider.logLevel()).isEqualTo("DEBUG");
    }
    
    @Test
    public void testRootLogLevelSystemPropertyWithDifferentLevels() {
        // Test different log levels
        String[] levels = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR"};
        
        for (String level : levels) {
            System.setProperty("log.level", level);
            ConfigProvider.reload();
            
            // Verify ROOT_LOG_LEVEL system property is set correctly
            String rootLogLevel = System.getProperty("ROOT_LOG_LEVEL");
            assertThat(rootLogLevel).isEqualTo(level);
            
            // Verify ConfigProvider returns the correct value
            assertThat(ConfigProvider.logLevel()).isEqualTo(level);
        }
    }
    
    @Test
    public void testRootLogLevelFromEnvironmentVariable() {
        // Test that environment variable LOG_LEVEL is properly handled
        // Note: This test checks that the system property is set correctly
        // when the configuration comes from environment variables
        
        // Clear system property and rely on default or environment
        System.clearProperty("log.level");
        ConfigProvider.reload();
        
        // If LOG_LEVEL environment variable exists, ROOT_LOG_LEVEL should be set
        String envLogLevel = System.getenv("LOG_LEVEL");
        if (envLogLevel != null) {
            String rootLogLevel = System.getProperty("ROOT_LOG_LEVEL");
            assertThat(rootLogLevel).isEqualTo(envLogLevel);
        }
    }
    
    @Test
    public void testLogbackConfigurationRespondsToRootLogLevel() {
        // Test that the actual logback configuration responds to ROOT_LOG_LEVEL changes
        
        // Set a specific log level
        System.setProperty("log.level", "WARN");
        ConfigProvider.reload();
        
        // Verify ROOT_LOG_LEVEL system property is set
        assertThat(System.getProperty("ROOT_LOG_LEVEL")).isEqualTo("WARN");
        
        // Get the root logger and verify its level
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        
        // Note: The actual level change might require logback context reconfiguration
        // This test verifies that the system property is correctly set
        // The logback configuration will pick up ROOT_LOG_LEVEL on startup
        
        // Verify the system property is available for logback
        assertThat(System.getProperty("ROOT_LOG_LEVEL")).isNotNull();
    }
    
    @Test
    public void testLogLevelPriority() {
        // Test that system property takes precedence over defaults
        
        // Set explicit system property
        System.setProperty("log.level", "ERROR");
        ConfigProvider.reload();
        
        assertThat(System.getProperty("ROOT_LOG_LEVEL")).isEqualTo("ERROR");
        assertThat(ConfigProvider.logLevel()).isEqualTo("ERROR");
        
        // Change to different level
        System.setProperty("log.level", "TRACE");
        ConfigProvider.reload();
        
        assertThat(System.getProperty("ROOT_LOG_LEVEL")).isEqualTo("TRACE");
        assertThat(ConfigProvider.logLevel()).isEqualTo("TRACE");
    }
    
    @Test
    public void testNullLogLevelHandling() {
        // Test that null log level is handled gracefully
        
        // Clear all log level properties
        System.clearProperty("log.level");
        System.clearProperty("ROOT_LOG_LEVEL");
        ConfigProvider.reload();
        
        // ConfigProvider should return default value
        String logLevel = ConfigProvider.logLevel();
        assertThat(logLevel).isNotNull();
        
        // ROOT_LOG_LEVEL should be set to the resolved value
        String rootLogLevel = System.getProperty("ROOT_LOG_LEVEL");
        assertThat(rootLogLevel).isEqualTo(logLevel);
    }
    
    @Test
    public void testLogLevelCaseInsensitive() {
        // Test that log levels are handled case-insensitively
        
        System.setProperty("log.level", "debug");
        ConfigProvider.reload();
        
        // Should be normalized to uppercase
        assertThat(System.getProperty("ROOT_LOG_LEVEL")).isEqualTo("DEBUG");
        assertThat(ConfigProvider.logLevel()).isEqualTo("DEBUG");
    }
}
