package logging;

import org.slf4j.MDC;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 * Universal logging utility for the entire project.
 * 
 * <p>Provides structured logging with SLF4J backend and MDC context support.
 * This is the main logging facade that should be used throughout the application.
 * 
 * <p><strong>Usage:</strong>
 * <pre>
 * Logger log = Logger.getLogger(MyClass.class);
 * log.info("Application started");
 * log.debug("Processing request: {}", requestId);
 * log.warn("Deprecated method used: {}", methodName);
 * log.error("Failed to process request", exception);
 * 
 * // With context
 * log.withContext("userId", "12345")
 *    .withContext("operation", "login")
 *    .info("User login successful");
 * </pre>
 */
public class Logger {
    
    private final org.slf4j.Logger slf4jLogger;
    private final String loggerName;
    
    private Logger(org.slf4j.Logger slf4jLogger, String loggerName) {
        this.slf4jLogger = slf4jLogger;
        this.loggerName = loggerName;
    }
    
    /**
     * Get logger for specific class.
     */
    public static Logger getLogger(Class<?> clazz) {
        org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(clazz);
        return new Logger(slf4jLogger, clazz.getSimpleName());
    }
    
    /**
     * Get logger with specific name.
     */
    public static Logger getLogger(String name) {
        org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(name);
        return new Logger(slf4jLogger, name);
    }
    
    // Standard logging methods
    public void trace(String message, Object... args) {
        slf4jLogger.trace(message, args);
    }
    
    public void debug(String message, Object... args) {
        slf4jLogger.debug(message, args);
    }
    
    public void info(String message, Object... args) {
        slf4jLogger.info(message, args);
    }
    
    public void warn(String message, Object... args) {
        slf4jLogger.warn(message, args);
    }
    
    public void warn(String message, Throwable throwable) {
        slf4jLogger.warn(message, throwable);
    }
    
    public void error(String message, Object... args) {
        slf4jLogger.error(message, args);
    }
    
    public void error(String message, Throwable throwable) {
        slf4jLogger.error(message, throwable);
    }
    
    // Level checks
    public boolean isTraceEnabled() {
        return slf4jLogger.isTraceEnabled();
    }
    
    public boolean isDebugEnabled() {
        return slf4jLogger.isDebugEnabled();
    }
    
    public boolean isInfoEnabled() {
        return slf4jLogger.isInfoEnabled();
    }
    
    public boolean isWarnEnabled() {
        return slf4jLogger.isWarnEnabled();
    }
    
    public boolean isErrorEnabled() {
        return slf4jLogger.isErrorEnabled();
    }
    
    /**
     * Create a context builder for structured logging.
     */
    public ContextBuilder withContext(String key, String value) {
        return new ContextBuilder(this).withContext(key, value);
    }
    
    /**
     * Builder for adding MDC context to log messages.
     */
    public static class ContextBuilder {
        private final Logger logger;
        private final Map<String, String> contextKeys = new HashMap<>();
        private final Map<String, String> previousValues = new HashMap<>();
        
        private ContextBuilder(Logger logger) {
            this.logger = logger;
        }
        
        public ContextBuilder withContext(String key, String value) {
            // Save previous value if it exists
            String previousValue = MDC.get(key);
            if (previousValue != null) {
                previousValues.put(key, previousValue);
            }
            
            // Set new value and track the key
            MDC.put(key, value);
            contextKeys.put(key, value);
            return this;
        }
        
        public void trace(String message, Object... args) {
            try {
                logger.trace(message, args);
            } finally {
                restoreContext();
            }
        }
        
        public void debug(String message, Object... args) {
            try {
                logger.debug(message, args);
            } finally {
                restoreContext();
            }
        }
        
        public void info(String message, Object... args) {
            try {
                logger.info(message, args);
            } finally {
                restoreContext();
            }
        }
        
        public void warn(String message, Object... args) {
            try {
                logger.warn(message, args);
            } finally {
                restoreContext();
            }
        }
        
        public void warn(String message, Throwable throwable) {
            try {
                logger.warn(message, throwable);
            } finally {
                restoreContext();
            }
        }
        
        public void error(String message, Object... args) {
            try {
                logger.error(message, args);
            } finally {
                restoreContext();
            }
        }
        
        public void error(String message, Throwable throwable) {
            try {
                logger.error(message, throwable);
            } finally {
                restoreContext();
            }
        }
        
        private void restoreContext() {
            // Remove keys we added
            for (String key : contextKeys.keySet()) {
                MDC.remove(key);
            }
            
            // Restore previous values
            for (Map.Entry<String, String> entry : previousValues.entrySet()) {
                MDC.put(entry.getKey(), entry.getValue());
            }
        }
    }
    
    /**
     * Get the underlying SLF4J logger (for advanced use cases).
     */
    public org.slf4j.Logger getSlf4jLogger() {
        return slf4jLogger;
    }
    
    /**
     * Get logger name.
     */
    public String getName() {
        return loggerName;
    }
}
