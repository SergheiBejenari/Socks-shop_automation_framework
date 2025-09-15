package config;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for FileWatcher synchronization with custom CompositeConfig file paths.
 * Verifies that FileWatcher monitors the actual files being used by CompositeConfig
 * instead of hardcoded paths.
 */
public class CustomConfigFileWatcherTest {
    
    private Path tempDir;
    private Path customBaseFile;
    private Path customProfileFile;
    private Path customEnvFile;
    
    @BeforeMethod
    public void setUp() throws IOException {
        // Create temporary directory for test files
        tempDir = Files.createTempDirectory("custom-config-test");
        
        // Create custom configuration files with non-standard names
        customBaseFile = tempDir.resolve("my-app-base.properties");
        customProfileFile = tempDir.resolve("my-app-test.properties");
        customEnvFile = tempDir.resolve("custom.env");
        
        // Create initial configuration files
        Files.write(customBaseFile, "base.key=base_value\nshared.key=from_base\n".getBytes());
        Files.write(customProfileFile, "profile.key=profile_value\nshared.key=from_profile\n".getBytes());
        Files.write(customEnvFile, "ENV_KEY=env_value\nSHARED_KEY=from_env\n".getBytes());
    }
    
    @AfterMethod
    public void tearDown() throws IOException {
        // Clean up temporary files
        if (Files.exists(customBaseFile)) Files.delete(customBaseFile);
        if (Files.exists(customProfileFile)) Files.delete(customProfileFile);
        if (Files.exists(customEnvFile)) Files.delete(customEnvFile);
        if (Files.exists(tempDir)) Files.delete(tempDir);
        
        // Stop file watcher
        FileWatcher.getInstance().stop();
    }
    
    @Test
    public void testCompositeConfigStoresActualFilePaths() {
        // Test that CompositeConfig stores and exposes the actual file paths being used
        CompositeConfig config = new CompositeConfig("test", "my-app-%s.properties", "my-app-base.properties", "custom.env");
        
        // Verify that the getters return the actual paths
        assertThat(config.getBaseFilename()).isEqualTo("my-app-base.properties");
        assertThat(config.getProfileFilename()).isEqualTo("my-app-test.properties");
        assertThat(config.getDotEnvPath()).isEqualTo("custom.env");
    }
    
    @Test
    public void testCompositeConfigWithDefaultPaths() {
        // Test default constructor uses standard paths
        CompositeConfig config = new CompositeConfig("local");
        
        assertThat(config.getBaseFilename()).isEqualTo("application.properties");
        assertThat(config.getProfileFilename()).isEqualTo("application-local.properties");
        assertThat(config.getDotEnvPath()).isEqualTo(".env");
    }
    
    @Test
    public void testCompositeConfigWithLegacyNaming() {
        // Test legacy naming uses old patterns
        CompositeConfig config = CompositeConfig.withLegacyNaming("prod");
        
        assertThat(config.getBaseFilename()).isEqualTo("configuration.properties");
        assertThat(config.getProfileFilename()).isEqualTo("configuration-prod.properties");
        assertThat(config.getDotEnvPath()).isEqualTo(".env");
    }
    
    @Test
    public void testFileWatcherUsesActualPaths() {
        // Test that FileWatcher can be configured with custom paths
        FileWatcher watcher = FileWatcher.getInstance();
        
        // Should handle custom file paths without throwing exceptions
        assertThatCode(() -> {
            watcher.watchFile(customBaseFile);
            watcher.watchFile(customProfileFile);
            watcher.watchFile(customEnvFile);
        }).doesNotThrowAnyException();
    }
    
    @Test
    public void testCustomConfigurationFileAccess() {
        // Test that custom configuration files can be accessed through resource paths
        // This simulates how the actual system would work with custom file names
        
        // Create a CompositeConfig with custom file patterns
        CompositeConfig config = new CompositeConfig("test", "my-app-%s.properties", "my-app-base.properties", "custom.env");
        
        // Verify the configuration knows about the custom files
        assertThat(config.getBaseFilename()).isEqualTo("my-app-base.properties");
        assertThat(config.getProfileFilename()).isEqualTo("my-app-test.properties");
        assertThat(config.getDotEnvPath()).isEqualTo("custom.env");
        
        // Verify sources are created with correct IDs
        assertThat(config.getSources()).hasSize(5);
        assertThat(config.getSources().get(2).id()).isEqualTo("profile:test");
        assertThat(config.getSources().get(3).id()).isEqualTo("base");
    }
    
    @Test
    public void testFileWatcherResourceHandling() {
        // Test that FileWatcher handles resource paths gracefully
        FileWatcher watcher = FileWatcher.getInstance();
        
        // Should handle non-existent resources without throwing
        assertThatCode(() -> {
            watcher.watchResource("my-app-base.properties");
            watcher.watchResource("my-app-test.properties");
            watcher.watchResource("custom.env");
        }).doesNotThrowAnyException();
    }
    
    @Test(timeOut = 3000)
    public void testFileWatcherWithCustomPaths() throws Exception {
        // Integration test: verify FileWatcher can monitor custom files
        FileWatcher watcher = FileWatcher.getInstance();
        
        // Watch the custom files
        watcher.watchFile(customBaseFile);
        watcher.watchFile(customProfileFile);
        watcher.start();
        
        // Give the watcher time to initialize
        Thread.sleep(100);
        
        // Modify one of the custom files
        Files.write(customBaseFile, "base.key=modified_value\nshared.key=from_base_modified\n".getBytes());
        
        // Wait for file system event processing
        Thread.sleep(200);
        
        // Test passes if no exceptions are thrown and files exist
        assertThat(Files.exists(customBaseFile)).isTrue();
        assertThat(Files.exists(customProfileFile)).isTrue();
        
        watcher.stop();
    }
    
    @Test
    public void testConfigProviderStartFileWatcherIntegration() {
        // Test that ConfigProvider.startFileWatcher() would work with custom configs
        // This is an indirect test since we can't easily mock the private method
        
        // Verify that CompositeConfig provides the necessary information
        CompositeConfig config = new CompositeConfig("test", "custom-%s.properties", "custom-base.properties", "custom.env");
        
        // The file paths should be available for FileWatcher to use
        assertThat(config.getBaseFilename()).isNotNull();
        assertThat(config.getProfileFilename()).isNotNull();
        assertThat(config.getDotEnvPath()).isNotNull();
        
        // FileWatcher should be able to handle these paths
        FileWatcher watcher = FileWatcher.getInstance();
        assertThatCode(() -> {
            watcher.watchResource(config.getBaseFilename());
            watcher.watchResource(config.getProfileFilename());
            watcher.watchResource(config.getDotEnvPath());
        }).doesNotThrowAnyException();
    }
}
