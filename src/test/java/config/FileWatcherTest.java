package config;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * Tests for FileWatcher functionality including automatic configuration reloading.
 */
public class FileWatcherTest {
    
    private Path tempDir;
    private Path testConfigFile;
    
    @BeforeMethod
    public void setUp() throws IOException {
        // Create temporary directory for test files
        tempDir = Files.createTempDirectory("config-test");
        testConfigFile = tempDir.resolve("test-config.properties");
        
        // Create initial test configuration file
        Files.write(testConfigFile, "test.key=initial_value\n".getBytes());
    }
    
    @AfterMethod
    public void tearDown() throws IOException {
        // Clean up temporary files
        if (Files.exists(testConfigFile)) {
            Files.delete(testConfigFile);
        }
        if (Files.exists(tempDir)) {
            Files.delete(tempDir);
        }

        resetFileWatcherForTests();
    }
    
    @Test
    public void testFileWatcherSingleton() {
        // Test that FileWatcher is a singleton
        FileWatcher watcher1 = FileWatcher.getInstance();
        FileWatcher watcher2 = FileWatcher.getInstance();
        
        assertThat(watcher1).isSameAs(watcher2);
    }
    
    @Test
    public void testWatchNonExistentFile() {
        // Test watching a non-existent file (should handle gracefully)
        FileWatcher watcher = FileWatcher.getInstance();
        Path nonExistentFile = Paths.get("/non/existent/file.properties");
        
        // Should not throw exception
        assertThatCode(() -> watcher.watchFile(nonExistentFile))
            .doesNotThrowAnyException();
    }
    
    @Test
    public void testWatchExistingFile() {
        // Test watching an existing file
        FileWatcher watcher = FileWatcher.getInstance();
        
        // Should not throw exception and file should exist
        assertThat(Files.exists(testConfigFile)).isTrue();
        assertThatCode(() -> watcher.watchFile(testConfigFile))
            .doesNotThrowAnyException();
    }
    
    @Test
    public void testStartStopWatcher() {
        // Test starting and stopping the watcher
        FileWatcher watcher = FileWatcher.getInstance();
        
        // Should not throw exception
        assertThatCode(() -> {
            watcher.start();
            Thread.sleep(50); // Give time for initialization
            watcher.stop();
        }).doesNotThrowAnyException();
    }
    
    @Test
    public void testMultipleStartCalls() {
        // Test that multiple start calls are idempotent
        FileWatcher watcher = FileWatcher.getInstance();
        
        assertThatCode(() -> {
            watcher.start();
            Thread.sleep(50); // Give time for initialization
            watcher.start(); // Second call should be ignored
            watcher.start(); // Third call should be ignored
            watcher.stop();
        }).doesNotThrowAnyException();
    }
    
    @Test
    public void testWatchResource() {
        // Test watching a resource file
        FileWatcher watcher = FileWatcher.getInstance();
        
        // Should handle non-existent resources gracefully
        assertThatCode(() -> watcher.watchResource("non-existent-resource.properties"))
            .doesNotThrowAnyException();
    }
    
    @Test(timeOut = 5000)
    public void testFileChangeDetection() throws Exception {
        FileWatcher watcher = FileWatcher.getInstance();
        String propertyName = ConfigKey.READ_TIMEOUT_MS.getSysPropName();
        String originalValue = System.getProperty(propertyName);

        ExecutorService monitorExecutor = null;
        AtomicBoolean monitorRunning = new AtomicBoolean(false);

        try {
            System.setProperty(propertyName, "1111");
            ConfigProvider.reload();
            assertThat(ConfigProvider.readTimeoutMs()).isEqualTo(1111);

            CountDownLatch valueUpdatedLatch = new CountDownLatch(1);
            monitorRunning.set(true);
            monitorExecutor = Executors.newSingleThreadExecutor();
            monitorExecutor.submit(() -> {
                try {
                    while (monitorRunning.get()) {
                        if (ConfigProvider.readTimeoutMs() == 2222) {
                            valueUpdatedLatch.countDown();
                            break;
                        }
                        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
                    }
                } finally {
                    monitorRunning.set(false);
                }
            });

            watcher.watchFile(testConfigFile);
            watcher.start();

            System.setProperty(propertyName, "2222");
            assertThat(ConfigProvider.readTimeoutMs()).isEqualTo(1111);

            Files.writeString(testConfigFile, "test.key=modified_value\n");

            assertThat(valueUpdatedLatch.await(3, TimeUnit.SECONDS))
                .as("Configuration value should update after reload")
                .isTrue();

            assertThat(ConfigProvider.readTimeoutMs()).isEqualTo(2222);
        } finally {
            // Ensure background monitor thread stops promptly
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
    public void testConfigProviderReloadMethod() {
        // Test that ConfigProvider has both reload methods
        assertThatCode(() -> {
            ConfigProvider.reload();
            ConfigProvider.reload("Test reload reason");
        }).doesNotThrowAnyException();
    }

    static void resetFileWatcherForTests() {
        try {
            FileWatcher watcher = FileWatcher.getInstance();

            Field runningField = FileWatcher.class.getDeclaredField("running");
            runningField.setAccessible(true);
            ((AtomicBoolean) runningField.get(watcher)).set(false);

            Field watchedFilesField = FileWatcher.class.getDeclaredField("watchedFiles");
            watchedFilesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<Path> watchedFiles = (Set<Path>) watchedFilesField.get(watcher);
            watchedFiles.clear();

            Field watchServiceField = FileWatcher.class.getDeclaredField("watchService");
            watchServiceField.setAccessible(true);
            WatchService oldWatchService = (WatchService) watchServiceField.get(watcher);
            if (oldWatchService != null) {
                try {
                    oldWatchService.close();
                } catch (IOException ignored) {
                    // Ignore errors while closing old watch service during reset
                }
            }
            WatchService newWatchService = FileSystems.getDefault().newWatchService();
            watchServiceField.set(watcher, newWatchService);

            Field executorField = FileWatcher.class.getDeclaredField("executor");
            executorField.setAccessible(true);
            ExecutorService oldExecutor = (ExecutorService) executorField.get(watcher);
            if (oldExecutor != null && !oldExecutor.isShutdown()) {
                oldExecutor.shutdownNow();
            }
            ExecutorService newExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "ConfigFileWatcher");
                thread.setDaemon(true);
                return thread;
            });
            executorField.set(watcher, newExecutor);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset FileWatcher singleton for tests", e);
        }
    }
}
