package logging;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and caching Logger instances.
 * 
 * <p>Provides centralized logger creation with caching to avoid creating
 * multiple logger instances for the same class/name.
 * 
 * <p><strong>Usage:</strong>
 * <pre>
 * Logger log = LoggerFactory.getLogger(MyClass.class);
 * Logger namedLog = LoggerFactory.getLogger("custom.logger");
 * </pre>
 */
public class LoggerFactory {
    
    private static final ConcurrentHashMap<String, Logger> loggerCache = new ConcurrentHashMap<>();
    
    /**
     * Get cached logger for specific class.
     */
    public static Logger getLogger(Class<?> clazz) {
        return loggerCache.computeIfAbsent(clazz.getName(), 
            name -> Logger.getLogger(clazz));
    }
    
    /**
     * Get cached logger with specific name.
     */
    public static Logger getLogger(String name) {
        return loggerCache.computeIfAbsent(name, 
            Logger::getLogger);
    }
    
    /**
     * Clear logger cache (mainly for testing).
     */
    public static void clearCache() {
        loggerCache.clear();
    }
    
    /**
     * Get cache size (mainly for monitoring).
     */
    public static int getCacheSize() {
        return loggerCache.size();
    }
}
