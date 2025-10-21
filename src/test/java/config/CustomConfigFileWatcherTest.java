package config;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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

        FileWatcherTest.resetFileWatcherForTests();
    }

    @Test
    public void testCompositeConfigStoresActualFilePaths() {
        // Test that CompositeConfig stores and exposes the actual file paths being used
        String profilePattern = tempDir.resolve("my-app-%s.properties").toString();
        CompositeConfig config = new CompositeConfig(
                "test",
                profilePattern,
                customBaseFile.toString(),
                customEnvFile.toString()
        );

        // Verify that the getters return the actual paths
        assertThat(config.getBaseFilename()).isEqualTo(customBaseFile.toString());
        assertThat(config.getProfileFilename()).isEqualTo(String.format(profilePattern, "test"));
        assertThat(config.getDotEnvPath()).isEqualTo(customEnvFile.toString());

        assertThat(config.getBaseFilePath()).contains(customBaseFile.toAbsolutePath().normalize());
        assertThat(config.getProfileFilePath()).contains(customProfileFile.toAbsolutePath().normalize());
        assertThat(config.getDotEnvFilePath()).contains(customEnvFile.toAbsolutePath().normalize());
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
            watcher.watchResource("my-app-base.properties", customBaseFile);
            watcher.watchResource("my-app-test.properties", customProfileFile);
            watcher.watchResource("custom.env", customEnvFile);
        }).doesNotThrowAnyException();
    }

    @Test
    public void testCustomConfigurationFileAccess() {
        // Test that custom configuration files can be accessed through resource paths
        // This simulates how the actual system would work with custom file names

        String profilePattern = tempDir.resolve("my-app-%s.properties").toString();
        CompositeConfig config = new CompositeConfig(
                "test",
                profilePattern,
                customBaseFile.toString(),
                customEnvFile.toString()
        );

        // Verify the configuration knows about the custom files
        assertThat(config.getBaseFilename()).isEqualTo(customBaseFile.toString());
        assertThat(config.getProfileFilename()).isEqualTo(String.format(profilePattern, "test"));
        assertThat(config.getDotEnvPath()).isEqualTo(customEnvFile.toString());

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
            watcher.watchResource("my-app-test.properties", customProfileFile);
            watcher.watchResource("custom.env", customEnvFile);
        }).doesNotThrowAnyException();
    }

    @Test(timeOut = 5000)
    public void testFileWatcherWithCustomPaths() throws Exception {
        FileWatcher watcher = FileWatcher.getInstance();
        String propertyName = ConfigKey.WRITE_TIMEOUT_MS.getSysPropName();
        String originalValue = System.getProperty(propertyName);

        ExecutorService monitorExecutor = null;
        AtomicBoolean monitorRunning = new AtomicBoolean(false);

        try {
            System.setProperty(propertyName, "3100");
            ConfigProvider.reload();
            assertThat(ConfigProvider.writeTimeoutMs()).isEqualTo(3100);

            CountDownLatch valueUpdatedLatch = new CountDownLatch(1);
            monitorRunning.set(true);
            monitorExecutor = Executors.newSingleThreadExecutor();
            monitorExecutor.submit(() -> {
                try {
                    while (monitorRunning.get()) {
                        if (ConfigProvider.writeTimeoutMs() == 3200) {
                            valueUpdatedLatch.countDown();
                            break;
                        }
                        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
                    }
                } finally {
                    monitorRunning.set(false);
                }
            });

            watcher.watchResource("my-app-base.properties", customBaseFile);
            watcher.watchResource("my-app-test.properties", customProfileFile);
            watcher.start();

            System.setProperty(propertyName, "3200");
            assertThat(ConfigProvider.writeTimeoutMs()).isEqualTo(3100);

            Files.writeString(customBaseFile, "base.key=modified_value\nshared.key=from_base_modified\n");

            assertThat(valueUpdatedLatch.await(3, TimeUnit.SECONDS))
                    .as("Configuration value should update for custom paths")
                    .isTrue();

            assertThat(ConfigProvider.writeTimeoutMs()).isEqualTo(3200);
            assertThat(Files.exists(customBaseFile)).isTrue();
            assertThat(Files.exists(customProfileFile)).isTrue();
        } finally {
            monitorRunning.set(false);
            if (monitorExecutor != null) {
                monitorExecutor.shutdownNow();
            }
            if (originalValue != null) {
                System.setProperty(propertyName, originalValue);
            } else {
                System.clearProperty(propertyName);
            }
            ConfigProvider.reload();
        }
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
            watcher.watchResource(config.getBaseFilename(), config.getBaseFilePath().orElse(null));
            watcher.watchResource(config.getProfileFilename(), config.getProfileFilePath().orElse(null));
            watcher.watchResource(config.getDotEnvPath(), config.getDotEnvFilePath().orElse(null));
        }).doesNotThrowAnyException();
    }
}
