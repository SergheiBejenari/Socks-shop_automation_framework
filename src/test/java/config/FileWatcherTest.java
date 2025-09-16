package config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Test(timeOut = 5000)
    public void testRapidFileChangesTriggerSingleReload() throws Exception {
        FileWatcher watcher = FileWatcher.getInstance();

        watcher.watchFile(testConfigFile);

        Logger logbackLogger = (Logger) org.slf4j.LoggerFactory.getLogger("config");
        ReloadCountingAppender appender = new ReloadCountingAppender();
        appender.start();
        logbackLogger.addAppender(appender);

        try {
            watcher.start();

            int initialCount = appender.getCount();

            Files.writeString(testConfigFile, "test.key=value1\n");
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
            Files.writeString(testConfigFile, "test.key=value2\n");
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
            Files.writeString(testConfigFile, "test.key=value3\n");

            boolean reloaded = waitForReload(appender, initialCount, 2, TimeUnit.SECONDS);

            assertThat(reloaded)
                .as("Configuration reload should occur after rapid file changes")
                .isTrue();

            assertThat(appender.getCount() - initialCount)
                .as("Reload should only be triggered once for rapid successive changes")
                .isEqualTo(1);
        } finally {
            watcher.stop();
            logbackLogger.detachAppender(appender);
            appender.stop();
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

    private boolean waitForReload(ReloadCountingAppender appender, int initialCount, long timeout, TimeUnit unit) {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            if (appender.getCount() > initialCount) {
                return true;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
        return false;
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

            Field watchedDirectoriesField = FileWatcher.class.getDeclaredField("watchedDirectories");
            watchedDirectoriesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<Path> watchedDirectories = (Set<Path>) watchedDirectoriesField.get(watcher);
            watchedDirectories.clear();

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

            Field schedulerField = FileWatcher.class.getDeclaredField("scheduler");
            schedulerField.setAccessible(true);
            ScheduledExecutorService oldScheduler = (ScheduledExecutorService) schedulerField.get(watcher);
            if (oldScheduler != null && !oldScheduler.isShutdown()) {
                oldScheduler.shutdownNow();
            }
            ScheduledExecutorService newScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "ConfigReloadScheduler");
                thread.setDaemon(true);
                return thread;
            });
            schedulerField.set(watcher, newScheduler);

            Field scheduledReloadField = FileWatcher.class.getDeclaredField("scheduledReload");
            scheduledReloadField.setAccessible(true);
            Object scheduledReload = scheduledReloadField.get(watcher);
            if (scheduledReload instanceof ScheduledFuture<?> future) {
                future.cancel(true);
            }
            scheduledReloadField.set(watcher, null);

            Field pendingReasonField = FileWatcher.class.getDeclaredField("pendingReloadReason");
            pendingReasonField.setAccessible(true);
            pendingReasonField.set(watcher, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset FileWatcher singleton for tests", e);
        }
    }

    private static class ReloadCountingAppender extends AppenderBase<ILoggingEvent> {
        private final AtomicInteger reloadCount = new AtomicInteger();

        @Override
        protected void append(ILoggingEvent event) {
            String message = event.getFormattedMessage();
            if (message != null && message.startsWith("Configuration reload triggered")) {
                reloadCount.incrementAndGet();
            }
        }

        int getCount() {
            return reloadCount.get();
        }
    }
}
