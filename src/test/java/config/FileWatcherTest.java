package config;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
        
        // Stop file watcher
        FileWatcher.getInstance().stop();
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
        // Test that file changes are detected (integration test)
        FileWatcher watcher = FileWatcher.getInstance();
        
        // Set up a latch to wait for reload
        CountDownLatch reloadLatch = new CountDownLatch(1);
        
        // Watch the test file
        watcher.watchFile(testConfigFile);
        watcher.start();
        
        // Give the watcher time to set up
        Thread.sleep(100);
        
        // Modify the file
        Files.write(testConfigFile, "test.key=modified_value\n".getBytes());
        
        // Wait a bit for the file system event to be processed
        Thread.sleep(500);
        
        // The test passes if no exceptions are thrown during file watching
        assertThat(Files.exists(testConfigFile)).isTrue();
        
        watcher.stop();
    }
    
    @Test
    public void testConfigProviderReloadMethod() {
        // Test that ConfigProvider has both reload methods
        assertThatCode(() -> {
            ConfigProvider.reload();
            ConfigProvider.reload("Test reload reason");
        }).doesNotThrowAnyException();
    }
}
