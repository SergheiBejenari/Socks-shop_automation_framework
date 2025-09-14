# Universal Logging System

Professional logging system for the entire project, built on SLF4J + Logback with structured logging and MDC context support.

## 🎯 Key Features

- **Universal usage**: One logger for the entire project
- **Structured logging**: MDC context for better tracing
- **Logger caching**: Optimized performance
- **Flexible configuration**: Different settings for different environments
- **Professional practices**: Follows logging best practices

## 🚀 Quick Start

### Basic Usage

```java
import logging.Logger;
import logging.LoggerFactory;

public class MyClass {
    private static final Logger log = LoggerFactory.getLogger(MyClass.class);
    
    public void doSomething() {
        log.info("Application started");
        log.debug("Processing request: {}", requestId);
        log.error("Failed to process request", exception);
    }
}
```

### Structured Logging

```java
// Logging with context
log.withContext("userId", "12345")
   .withContext("operation", "login")
   .info("User login successful");

// API testing
Logger apiLog = LoggerFactory.getLogger("api");
apiLog.withContext("endpoint", "/api/catalogue")
      .withContext("statusCode", "200")
      .withContext("responseTime", "150")
      .info("API response received");
```

## 📁 Architecture

```
logging/
├── Logger.java              # Main logger with MDC support
├── LoggerFactory.java       # Factory with caching
└── package-info.java        # Package documentation

config/
└── ConfigLogging.java       # Specialized configuration logging

examples/
└── LoggingExamples.java     # Usage examples
```

## 🔧 Configuration

### Logback Configuration

**logback-test.xml** (for development):
```xml
<logger name="config" level="DEBUG" additivity="false">
    <appender-ref ref="CONSOLE" />
    <appender-ref ref="FILE" />
</logger>
```

**logback.xml** (for production):
```xml
<logger name="config" level="${CONFIG_LOG_LEVEL:-INFO}" additivity="false">
    <appender-ref ref="CONSOLE" />
    <appender-ref ref="FILE" />
</logger>
```

### Environment Variables

```bash
# Configuration logging level
export CONFIG_LOG_LEVEL=DEBUG

# Root logging level
export ROOT_LOG_LEVEL=WARN
```

## 📊 Logging Patterns

### 1. API Testing

```java
Logger apiLog = LoggerFactory.getLogger("api");

// Request
apiLog.withContext("endpoint", "/api/users")
      .withContext("method", "POST")
      .info("Sending API request");

// Response
apiLog.withContext("endpoint", "/api/users")
      .withContext("statusCode", "201")
      .withContext("responseTime", "250")
      .info("User created successfully");
```

### 2. UI Testing

```java
Logger testLog = LoggerFactory.getLogger("test");

testLog.withContext("testName", "loginTest")
       .withContext("browser", "chrome")
       .withContext("action", "click")
       .withContext("element", "loginButton")
       .debug("Clicking login button");
```

### 3. Configuration

```java
// Use ConfigLogging for configuration events
ConfigLogging.logConfigLoad("file", "baseUrlApi", "http://localhost:8080", false);
ConfigLogging.logValidationError("PROXY_PORT", "invalid", "Port must be > 0");
```

### 4. Performance

```java
Logger perfLog = LoggerFactory.getLogger("performance");

long startTime = System.currentTimeMillis();
// ... operation execution ...
long duration = System.currentTimeMillis() - startTime;

perfLog.withContext("operation", "userRegistration")
       .withContext("duration", String.valueOf(duration))
       .info("Operation completed in {}ms", duration);
```

## 🎨 Logging Levels

| Level | Usage | Example |
|-------|-------|--------|
| `TRACE` | Very detailed debugging | Method execution tracing |
| `DEBUG` | Debug information | Variable values, object state |
| `INFO`  | General information | Operation start/completion, results |
| `WARN`  | Warnings | Deprecated method usage, slow operations |
| `ERROR` | Errors | Exceptions, critical issues |

## 🔍 MDC Context

The system supports the following context keys:

### General
- `userId` - User ID
- `sessionId` - Session ID
- `operation` - Operation name
- `duration` - Operation duration

### API Testing
- `endpoint` - Endpoint URL
- `method` - HTTP method
- `statusCode` - Response code
- `responseTime` - Response time

### UI Testing
- `testName` - Test name
- `browser` - Browser type
- `pageUrl` - Page URL
- `element` - Element selector
- `action` - Action being performed

### Configuration
- `configProfile` - Configuration profile
- `configSource` - Configuration source
- `configKey` - Configuration key

## 📈 Monitoring and Metrics

### Log Files

```
build/logs/
├── config.log              # Main configuration log
├── config.2024-01-15.1.log.gz  # Archived logs
└── ...
```

### Log Rotation

- **Maximum file size**: 10MB
- **History**: 30 days
- **Total size**: 1GB
- **Compression**: gzip

## 🛠 Best Practices

### ✅ Correct

```java
// Parameterized logging
log.info("User {} logged in with role {}", userId, role);

// Level checking for expensive operations
if (log.isDebugEnabled()) {
    log.debug("Expensive debug info: {}", computeExpensiveInfo());
}

// Structured logging
log.withContext("userId", userId)
   .withContext("operation", "login")
   .info("Login successful");
```

### ❌ Incorrect

```java
// String concatenation
log.info("User " + userId + " logged in");  // Bad!

// System.out.println
System.out.println("Debug info");  // Bad!

// Logging without context
log.info("Something happened");  // Bad!
```

## 🔧 Environment Configuration

### Local (development)
```properties
logLevel=DEBUG
CONFIG_LOG_LEVEL=DEBUG
ROOT_LOG_LEVEL=INFO
```

### Dev (test environment)
```properties
logLevel=INFO
CONFIG_LOG_LEVEL=INFO
ROOT_LOG_LEVEL=WARN
```

### CI (continuous integration)
```properties
logLevel=WARN
CONFIG_LOG_LEVEL=ERROR
ROOT_LOG_LEVEL=ERROR
```

## 📚 Examples

For complete usage examples, see:
- `examples/LoggingExamples.java` - Comprehensive examples
- `config/ConfigLogging.java` - Configuration logging
- `src/test/java/config/ConfigProviderTest.java` - Test examples

## ⚙️ IDE Configuration

### IntelliJ IDEA
1. Open **File → Settings → Build → Compiler**
2. Add to **VM options**: `-DlogLevel=DEBUG`
3. Restart IDE

### Eclipse
1. **Run → Run Configurations**
2. Select configuration → **Arguments**
3. Add to **VM arguments**: `-DlogLevel=DEBUG`

### VS Code
Add to `launch.json`:
```json
{
    "vmArgs": "-DlogLevel=DEBUG"
}
```

## 🔍 Troubleshooting

### Common Issues

1. **Logs not appearing**
   - Check logging level
   - Verify `logback.xml` configuration

2. **Duplicate logs**
   - Check `additivity="false"` in configuration

3. **Slow logging**
   - Use parameterized logging
   - Check level before expensive operations

## 🚀 Conclusion

The universal logging system provides:

- **Professional logging** for the entire project
- **Structure** through MDC context
- **Performance** through caching
- **Flexibility** for different environments
- **Ease of use** with minimal code

Use this system to get high-quality and informative logs in your projects!
