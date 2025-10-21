package config;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * File watcher service that monitors configuration files for changes
 * and triggers automatic reloads of the ConfigProvider.
 */
public class FileWatcher {

    private static final FileWatcher INSTANCE = new FileWatcher();
    private final WatchService watchService;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Set<Path> watchedFiles = ConcurrentHashMap.newKeySet();
    private final Set<Path> watchedDirectories = ConcurrentHashMap.newKeySet();
    private final Object reloadLock = new Object();
    private ScheduledFuture<?> scheduledReload;
    private String pendingReloadReason;
    private static final long RELOAD_DELAY_MS = 100L;

    private FileWatcher() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ConfigFileWatcher");
                t.setDaemon(true);
                return t;
            });
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ConfigReloadScheduler");
                t.setDaemon(true);
                return t;
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize file watcher", e);
        }
    }

    public static FileWatcher getInstance() {
        return INSTANCE;
    }

    /**
     * Start watching configuration files for changes.
     * This method is idempotent - calling it multiple times has no effect.
     */
    public synchronized void start() {
        if (running.compareAndSet(false, true)) {
            executor.submit(this::watchLoop);
            ConfigLogging.info("Configuration file watcher started");
        }
    }

    /**
     * Stop the file watcher service.
     */
    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            try {
                watchService.close();
                executor.shutdown();
                cancelScheduledReload();
                scheduler.shutdownNow();
                ConfigLogging.info("Configuration file watcher stopped");
            } catch (IOException e) {
                ConfigLogging.error("Error stopping file watcher: {}", e.getMessage());
            }
        }
    }

    /**
     * Add a file to be watched for changes.
     *
     * @param filePath Path to the file to watch
     */
    public void watchFile(Path filePath) {
        Path normalizedFilePath = filePath.toAbsolutePath().normalize();
        Path directory = normalizedFilePath.getParent();

        if (directory == null) {
            ConfigLogging.debug("Cannot watch file without parent directory: {}", normalizedFilePath);
            return;
        }

        // Only register if file is not already being watched
        if (watchedFiles.add(normalizedFilePath)) {
            if (Files.notExists(directory)) {
                ConfigLogging.debug("Directory for configuration file {} does not exist: {}", normalizedFilePath, directory);
                watchedFiles.remove(normalizedFilePath);
                return;
            }

            boolean directoryRegisteredHere = false;

            try {
                if (watchedDirectories.add(directory)) {
                    directory.register(watchService,
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE);
                    directoryRegisteredHere = true;
                }

                if (Files.exists(normalizedFilePath)) {
                    ConfigLogging.debug("Now watching configuration file: {}", normalizedFilePath);
                } else {
                    ConfigLogging.debug("Watching for configuration file to appear: {}", normalizedFilePath);
                }
            } catch (IOException | RuntimeException e) {
                ConfigLogging.error("Failed to watch file {}: {}", normalizedFilePath, e.getMessage());
                watchedFiles.remove(normalizedFilePath); // Remove from set if registration failed
                if (directoryRegisteredHere) {
                    watchedDirectories.remove(directory);
                }
            }
        } else {
            ConfigLogging.debug("File already being watched: {}", normalizedFilePath);
        }
    }

    /**
     * Add a resource file to be watched (converts resource path to actual file path).
     *
     * @param resourcePath Resource path (e.g., "application.properties")
     */
    public void watchResource(String resourcePath) {
        try {
            var resource = getClass().getClassLoader().getResource(resourcePath);
            if (resource != null && "file".equals(resource.getProtocol())) {
                Path filePath = Paths.get(resource.toURI());
                watchFile(filePath);
            } else {
                ConfigLogging.debug("Cannot watch resource (not a file): {}", resourcePath);
            }
        } catch (Exception e) {
            ConfigLogging.debug("Cannot watch resource {}: {}", resourcePath, e.getMessage());
        }
    }

    private void watchLoop() {
        ConfigLogging.debug("File watcher loop started");

        while (running.get()) {
            try {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path fileName = pathEvent.context();
                    Path directory = ((Path) key.watchable()).toAbsolutePath().normalize();
                    Path fullPath = directory.resolve(fileName).toAbsolutePath().normalize();

                    // Check if this is one of our watched files
                    if (watchedFiles.contains(fullPath)) {
                        ConfigLogging.info("Configuration file changed: {} - scheduling reload", fullPath);
                        scheduleReload(fullPath);
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    ConfigLogging.warn("Watch key no longer valid, stopping file watcher");
                    break;
                }

            } catch (InterruptedException e) {
                ConfigLogging.debug("File watcher interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                ConfigLogging.error("Error in file watcher loop: {}", e.getMessage());
                ConfigLogging.debug("File watcher error details", e);
            }
        }

        ConfigLogging.debug("File watcher loop ended");
    }

    private void scheduleReload(Path fullPath) {
        synchronized (reloadLock) {
            pendingReloadReason = "File change detected: " + fullPath;

            if (scheduledReload != null && !scheduledReload.isDone()) {
                scheduledReload.cancel(false);
            }

            try {
                scheduledReload = scheduler.schedule(() -> {
                    String reason;
                    synchronized (reloadLock) {
                        reason = pendingReloadReason;
                        pendingReloadReason = null;
                        scheduledReload = null;
                    }

                    try {
                        ConfigProvider.reload(reason != null ? reason : "File change detected");
                    } catch (Exception e) {
                        ConfigLogging.error("Failed to reload configuration after file change: {}", e.getMessage());
                        ConfigLogging.debug("Configuration reload error details", e);
                    }
                }, RELOAD_DELAY_MS, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException e) {
                ConfigLogging.debug("Reload scheduler rejected task for {}: {}", fullPath, e.getMessage());
            }
        }
    }

    private void cancelScheduledReload() {
        synchronized (reloadLock) {
            if (scheduledReload != null) {
                scheduledReload.cancel(false);
                scheduledReload = null;
            }
            pendingReloadReason = null;
        }
    }
}
