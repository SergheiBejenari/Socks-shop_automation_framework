/**
 * Universal logging infrastructure for the entire project.
 *
 * <p>This package provides a centralized logging solution built on top of SLF4J + Logback
 * with structured logging capabilities and MDC context support.
 *
 * <h2>Key Components:</h2>
 * <ul>
 *   <li>{@link logging.Logger} - Main logging facade with context support</li>
 *   <li>{@link logging.LoggerFactory} - Factory for creating and caching loggers</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 * <pre>
 * // Basic logging
 * Logger log = LoggerFactory.getLogger(MyClass.class);
 * log.info("Application started");
 * log.debug("Processing request: {}", requestId);
 * log.error("Failed to process request", exception);
 *
 * // Structured logging with context
 * log.withContext("userId", "12345")
 *    .withContext("operation", "login")
 *    .info("User login successful");
 *
 * // For specific modules
 * Logger apiLog = LoggerFactory.getLogger("api");
 * Logger dbLog = LoggerFactory.getLogger("database");
 * </pre>
 *
 * <h2>Configuration:</h2>
 * <p>Logging is configured via logback.xml and logback-test.xml files.
 * Different log levels can be set for different environments using system properties
 * or environment variables.
 *
 * @since 1.0
 */
package logging;
