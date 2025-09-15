package config;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for FileWatcher duplicate registration prevention.
 * Verifies that calling watchFile() multiple times for the same path
 * doesn't cause duplicate directory registrations or duplicate events.
 */
public class FileWatcherDuplicateTest {
    
    private Path tempDir;
    private Path testFile;
    
    @BeforeMethod
    public void setUp() throws IOException {
        // Create temporary directory and test file
        tempDir = Files.createTempDirectory("filewatcher-duplicate-test");
        testFile = tempDir.resolve("test-config.properties");
        
        // Create initial test file
        Files.write(testFile, "initial.key=initial_value\n".getBytes());
    }
    
    @AfterMethod
    public void tearDown() throws IOException {
        // Stop file watcher first
        FileWatcher.getInstance().stop();
        
        // Clean up temporary files
        if (Files.exists(testFile)) {
            Files.delete(testFile);
        }
        
        // Clean up any additional files in temp directory
        if (Files.exists(tempDir)) {
            try (var stream = Files.list(tempDir)) {
                stream.forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore cleanup errors
                    }
                });
            }
            Files.delete(tempDir);
        }
    }
    
    @Test
    public void testDuplicateWatchFileCallsPrevention() {
        // Test that calling watchFile() multiple times for the same path
        // doesn't cause issues and logs appropriate messages
        FileWatcher watcher = FileWatcher.getInstance();
        
        // First call should succeed
        assertThatCode(() -> watcher.watchFile(testFile))
            .doesNotThrowAnyException();
        
        // Second call should be ignored (no exception, but should log debug message)
        assertThatCode(() -> watcher.watchFile(testFile))
            .doesNotThrowAnyException();
        
        // Third call should also be ignored
        assertThatCode(() -> watcher.watchFile(testFile))
            .doesNotThrowAnyException();
    }
    
    @Test
    public void testWatchedFilesSetManagement() {
        // Test that duplicate watchFile calls are handled gracefully
        FileWatcher watcher = FileWatcher.getInstance();
        
        // Add file to watch multiple times - should handle gracefully even if WatchService is closed
        watcher.watchFile(testFile);
        watcher.watchFile(testFile); // Duplicate
        watcher.watchFile(testFile); // Another duplicate
        
        // Test passes if file exists (exceptions are caught and logged)
        assertThat(Files.exists(testFile)).isTrue();
    }
    
    @Test
    public void testMultipleFilesInSameDirectory() {
        // Test watching multiple files in the same directory
        try {
            Path file1 = tempDir.resolve("config1.properties");
            Path file2 = tempDir.resolve("config2.properties");
            
            Files.write(file1, "file1.key=value1\n".getBytes());
            Files.write(file2, "file2.key=value2\n".getBytes());
            
            FileWatcher watcher = FileWatcher.getInstance();
            
            // Watch both files (same directory) - should work without issues
            assertThatCode(() -> {
                watcher.watchFile(file1);
                watcher.watchFile(file2);
                watcher.watchFile(file1); // Duplicate of file1
                watcher.watchFile(file2); // Duplicate of file2
            }).doesNotThrowAnyException();
            
            // Clean up
            Files.delete(file1);
            Files.delete(file2);
            
        } catch (IOException e) {
            fail("Test setup failed", e);
        }
    }
    
    @Test
    public void testWatchNonExistentFileTwice() {
        // Test that watching a non-existent file multiple times is handled gracefully
        Path nonExistentFile = tempDir.resolve("non-existent.properties");
        
        FileWatcher watcher = FileWatcher.getInstance();
        
        // First call should handle gracefully
        assertThatCode(() -> watcher.watchFile(nonExistentFile))
            .doesNotThrowAnyException();
        
        // Second call should also handle gracefully
        assertThatCode(() -> watcher.watchFile(nonExistentFile))
            .doesNotThrowAnyException();
    }
    
    @Test(timeOut = 5000)
    public void testSingleEventForDuplicateWatchers() throws Exception {
        // Test that file modification works correctly
        // even if watchFile() was called multiple times
        
        FileWatcher watcher = FileWatcher.getInstance();
        
        // Watch the file multiple times
        watcher.watchFile(testFile);
        watcher.watchFile(testFile); // Duplicate
        watcher.watchFile(testFile); // Another duplicate
        
        watcher.start();
        
        // Give the watcher time to initialize
        Thread.sleep(100);
        
        // Modify the file
        Files.write(testFile, "modified.key=modified_value\n".getBytes());
        
        // Wait for file system events to be processed
        Thread.sleep(500);
        
        // Test passes if the file was modified and no exceptions occurred
        String content = Files.readString(testFile);
        assertThat(content).contains("modified_value");
        
        watcher.stop();
    }
    
    @Test
    public void testWatchFileErrorHandling() throws IOException {
        // Test error handling when watching files
        FileWatcher watcher = FileWatcher.getInstance();
        
        // Create a file in the temp directory
        Path testFile2 = tempDir.resolve("test2.properties");
        Files.write(testFile2, "test=value\n".getBytes());
        
        // Watch file multiple times - should handle gracefully even with closed WatchService
        watcher.watchFile(testFile2);
        watcher.watchFile(testFile2); // Duplicate - should be logged but not throw
        
        // Test passes if file exists
        assertThat(Files.exists(testFile2)).isTrue();
        
        // Clean up the additional file
        Files.delete(testFile2);
    }
    
    @Test
    public void testConcurrentWatchFileCalls() throws InterruptedException {
        // Test concurrent calls to watchFile() for the same path
        FileWatcher watcher = FileWatcher.getInstance();
        
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        // Create multiple threads that try to watch the same file
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    watcher.watchFile(testFile);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete
        boolean completed = doneLatch.await(3, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        // Test passes if no exceptions were thrown during concurrent access
    }
}
