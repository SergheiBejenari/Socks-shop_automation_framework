package tests;

import org.example.config.ConfigCache;
import org.example.config.ConfigManager;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension for configuring test environment.
 * 
 * <p>
 * This extension ensures that:
 * <ul>
 *     <li>Test environment is properly set</li>
 *     <li>Configuration cache is cleared before all tests</li>
 *     <li>Test profile is loaded</li>
 * </ul>
 * 
 * <p>
 * <strong>Usage:</strong>
 * <pre>
 * @ExtendWith(ConfigTestExtension.class)
 * class ConfigLayerTest {
 *     // Tests will run with proper test environment setup
 * }
 * </pre>
 */
public class ConfigTestExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // Set test environment
        System.setProperty("env", "test");
        
        // Clear configuration cache
        ConfigCache.clear();
        
        // Refresh configuration to load test profile
        ConfigManager.refresh();
        
        // Verify test environment is loaded
        String env = ConfigManager.resolve(org.example.config.ConfigKeys.ENVIRONMENT);
        if (!"test".equals(env)) {
            throw new IllegalStateException(
                "Test environment not properly loaded. Expected 'test', got: " + env);
        }
    }
}
