# Configuration Provider Module

A professional configuration management module designed with SDET best practices in mind.

## 🎯 Key Features

- **Multi-layered configuration resolution** with source priority
- **Environment profiles** (local, dev, ci, qa, stage, prod)
- **Caching** for improved performance
- **Configuration validation** with detailed reporting
- **Secure handling** of sensitive data
- **Metrics and monitoring** for configuration access
- **Thread-safe** operations for multi-threaded environments

## 🚀 Quick Start

### 1. Build and Testing

```bash
# Build project
./gradlew clean build

# Run all tests
./gradlew test

# Run tests with profile
./gradlew test -Denv=test

# Run specific test
./gradlew test --tests ConfigProviderTest

# Run with configuration override
./gradlew test -DbaseUrlApi=https://custom.example.com -DreadTimeoutMs=60000
```

### 2. Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests ConfigProviderTest

# Run with detailed output
./gradlew test --info
```

### 3. Project Build

```bash
# Clean and build
./gradlew clean build

# Create JAR file
./gradlew jar
```

## 🏗️ Architecture

### Core Components

```
ConfigProvider          - Main configuration provider with type-safe access
├── CompositeConfig     - Composite configuration source with priorities
├── ConfigSource        - Configuration source interface
│   ├── EnvConfigSource           - Environment variables
│   ├── SystemPropsConfigSource   - System properties
│   ├── PropertiesFileConfigSource - Properties files
│   └── DotEnvFileConfigSource     - .env files
├── ConfigKey           - Enum with keys and validation
├── ConfigLogging       - Specialized logging
└── SecretMasker        - Sensitive data masking
```

### Configuration Source Priority

1. **Environment Variables** (`ENV_VAR_NAME`) - highest priority
2. **System Properties** (`-Dkey=value`) 
3. **Profile files** (`application-{env}.properties`)
4. **Base file** (`application.properties`)
5. **`.env` file** (for local development)
6. **Default values** from `ConfigKey` enum

## 📁 Configuration File Structure

### Base file: `application.properties`
```properties
# Base settings for all environments
baseUrlApi=http://localhost:8080/api
baseUrlUi=http://localhost:8080
readTimeoutMs=30000
connectTimeoutMs=10000
logLevel=INFO
headless=true
browserType=CHROME
appEnv=local
basicAuthUser=admin
basicAuthPassword=admin123
```

### Profile files: `application-{env}.properties`

#### `application-local.properties`
```properties
# Local development
baseUrlApi=http://localhost:8080
baseUrlUi=http://localhost:8080
logLevel=DEBUG
headless=false
```

#### `application-dev.properties`
```properties
# Development environment
baseUrlApi=https://dev-api.example.com
baseUrlUi=https://dev.example.com
logLevel=DEBUG
headless=true
```

#### `application-ci.properties`
```properties
# CI/CD environment
baseUrlApi=https://ci-api.example.com
baseUrlUi=https://ci.example.com
logLevel=INFO
headless=true
browserType=CHROME
```

#### `application-qa.properties`
```properties
# QA environment
baseUrlApi=https://qa-api.example.com
baseUrlUi=https://qa.example.com
logLevel=INFO
headless=true
```

#### `application-stage.properties`
```properties
# Staging environment
baseUrlApi=https://stage-api.example.com
baseUrlUi=https://stage.example.com
logLevel=WARN
headless=true
```

#### `application-prod.properties`
```properties
# Production environment
baseUrlApi=https://api.example.com
baseUrlUi=https://example.com
logLevel=ERROR
headless=true
```

## 🔧 API Reference

### ConfigProvider - Main Configuration Access

```java
// Get configuration through type-safe methods
URI apiUrl = ConfigProvider.baseUrlApi();
URI uiUrl = ConfigProvider.baseUrlUi();
int timeout = ConfigProvider.readTimeoutMs();
String logLevel = ConfigProvider.logLevel();
boolean headless = ConfigProvider.headless();
String browserType = ConfigProvider.browserType();
String environment = ConfigProvider.appEnv();

// Authentication
String username = ConfigProvider.basicAuthUser();
String password = ConfigProvider.basicAuthPassword();

// Reload configuration at runtime
ConfigProvider.reload();

// Get full configuration dump with secret masking
String configDump = ConfigProvider.dumpMasked();
System.out.println(configDump);
```

### ConfigKey - Configuration Keys Enumeration

```java
// Each key contains:
// - Environment variable name
// - System property name  
// - Default value
// - Value parser
// - Validator
// - Secret flag

// Usage example
ConfigKey key = ConfigKey.BASE_URL_API;
String envVarName = key.getEnvVarName();     // "BASE_URL_API"
String sysPropName = key.getSysPropName();   // "baseUrlApi"
Object defaultValue = key.getDefault();      // URI.create("http://localhost:8080")
boolean isSecret = key.isSecret();           // false

// Parsing and validation
Object parsed = key.parse("https://api.example.com");
Object validated = key.validate(parsed);
```

### ConfigLogging - Specialized Logging

```java
// Log configuration initialization
ConfigLogging.logConfigInit("local", "Building configuration snapshot");

// Log value loading
ConfigLogging.logConfigLoad("env", "BASE_URL_API", "https://api.example.com", false);

// Log validation
ConfigLogging.logValidation("PROXY_CONFIG", "Proxy configuration validation passed");

// Log validation errors
ConfigLogging.logValidationError("LOG_LEVEL", "INVALID", "Invalid log level");

// Log default values
ConfigLogging.logDefaultValue("APP_ENV", "local");
```

## 🧪 Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests ConfigProviderTest

# Run with detailed output
./gradlew test --info
```

## 🚀 GitLab CI/CD

### Automated Pipeline

The project includes a ready-to-use GitLab CI/CD pipeline with the following capabilities:

- **Multi-stage pipeline**: validate → test → report
- **Parallel test execution** for faster execution
- **Environment-specific testing** (dev, staging)
- **Allure reporting** with beautiful reports
- **Coverage reporting** with JaCoCo
- **Artifact management** for test results

### Available Pipeline Versions

1. **`.gitlab-ci.yml`** - main simplified version (recommended for start)
2. **`.gitlab-ci-full.yml`** - full version with extended capabilities
3. **`.gitlab-ci-test.yml`** - minimal version for testing

### Pipeline Stages

```yaml
stages:
  - validate      # Code and configuration validation
  - test         # Parallel test execution
  - report       # Report generation (Allure)
```

### Pipeline Execution

Pipeline automatically runs on:
- Push to `main` or `develop` branches
- Merge Request creation
- Manual trigger through GitLab UI

### Environment Variables

```bash
# Required variables (updated names)
APP_ENV=ci                    # Test environment (local, dev, ci, qa, stage, prod)
PARALLEL_TESTS=true           # Parallel execution
TEST_THREAD_COUNT=4           # Thread count

# Application configuration
BASE_URL_UI=https://ci.example.com
BASE_URL_API=https://ci-api.example.com
DATABASE_URL=jdbc:mysql://ci-db:3306/socksdb

# Optional variables
SLACK_WEBHOOK_URL=            # Webhook for notifications
```

### Test Structure

- **ConfigProviderTest** - Main configuration module tests (32 tests)
- **TestNG** - Uses TestNG instead of JUnit 5 for testing

### Test Examples

```java
@Test
public void testDefaultValues() {
    // Test default values from local profile
    assertThat(ConfigProvider.baseUrlUi()).isEqualTo(URI.create("http://localhost:8080"));
    assertThat(ConfigProvider.baseUrlApi()).isEqualTo(URI.create("http://localhost:8080"));
    assertThat(ConfigProvider.logLevel()).isEqualTo("DEBUG");
    assertThat(ConfigProvider.appEnv()).isEqualTo("local");
}

@Test
public void testSystemPropertyOverride() {
    // Test system property override
    System.setProperty("readTimeoutMs", "15000");
    ConfigProvider.reload();
    
    assertThat(ConfigProvider.readTimeoutMs()).isEqualTo(15000);
}
```

## 🔒 Security

### Sensitive Data Masking

Keys containing the following are automatically masked:
- `password`
- `secret`
- `token`
- `key`
- `credential`
- `auth`

### Examples

```java
// Safe configuration dump - secrets are automatically masked
String configDump = ConfigProvider.dumpMasked();
System.out.println(configDump);
// Result: BASIC_AUTH_PASSWORD = se*************23

// Direct access returns real value
String actualPassword = ConfigProvider.basicAuthPassword();
// Result: secretpassword123

// Logging non-secret values
log.info("API URL: {}", ConfigProvider.baseUrlApi());
log.info("Environment: {}", ConfigProvider.appEnv());
```

## 📊 Monitoring and Metrics

### Access Metrics

```java
// Access metrics are not available in current ConfigProvider version
// Use ConfigProvider.dumpMasked() for configuration debugging
String configDump = ConfigProvider.dumpMasked();
System.out.println(configDump);
```

### Configuration Debugging

```java
// Get full configuration dump with secret masking
String configDump = ConfigProvider.dumpMasked();
System.out.println(configDump);

// Reload configuration at runtime
ConfigProvider.reload();

// Get specific values
URI apiUrl = ConfigProvider.baseUrlApi();
String environment = ConfigProvider.appEnv();
boolean isHeadless = ConfigProvider.headless();
```

## 🌍 Environment Support

### Environment Variables

```bash
# Set environment variables (new standardized names)
export BASE_URL_UI=https://staging.example.com
export BASE_URL_API=https://staging-api.example.com
export APP_ENV=stage
export LOG_LEVEL=DEBUG
export DATABASE_URL=jdbc:postgresql://staging-db:5432/socksdb

# Run application
java -jar app.jar
```

### System Properties

```bash
# Pass via command line (new property names)
java -DbaseUrlUi=https://prod.example.com -DappEnv=prod -jar app.jar

# Set in code
System.setProperty("connectTimeoutMs", "5000");
System.setProperty("readTimeoutMs", "15000");
```

### .env File

```bash
# .env file in project root (new standardized names)
BASE_URL_UI=http://localhost:3000
BASE_URL_API=http://localhost:8080/api
DATABASE_URL=jdbc:postgresql://localhost:5432/dev_db
LOG_LEVEL=DEBUG
BASIC_AUTH_USERNAME=dev_user
BASIC_AUTH_PASSWORD=dev_password
```

## 🚨 Error Handling

### ConfigurationException

```java
try {
    String value = ConfigProvider.get(ConfigKey.REQUIRED_KEY);
} catch (IllegalStateException e) {
    log.error("Configuration error: {}", e.getMessage());
    // Handle configuration error
}
```

### Error Types

1. **Missing Required Configuration** - required configuration is missing
2. **Invalid Value** - invalid value (cannot be parsed)
3. **Configuration Loading Failed** - configuration file loading error

## 🔄 Dynamic Updates

### Configuration Updates

```java
// Update configuration at runtime
ConfigProvider.reload();

// Get safe configuration dump for debugging
String configDump = ConfigProvider.dumpMasked();
System.out.println(configDump);
```

## 📈 Performance

### Snapshot Architecture

- **Immutable snapshot** - configuration is loaded once into an immutable snapshot
- **EnumMap optimization** - uses EnumMap for better performance and lower memory consumption
- **Thread-safe operations** - safe access from multiple threads

### Optimizations

- **Lazy initialization** - configuration is loaded on first access
- **Efficient parsing** - each value is parsed only once
- **Memory efficient** - EnumMap uses simple array instead of hash table

## 🛠️ Best Practices

### 1. Using Type-Safe Getters

```java
// ✅ Good - uses type-safe methods
URI apiUrl = ConfigProvider.baseUrlApi();
int timeout = ConfigProvider.connectTimeoutMs();
boolean headless = ConfigProvider.headless();

// ✅ Alternative - universal getter
URI apiUrl = ConfigProvider.get(ConfigKey.BASE_URL_API);
```

### 2. Startup Validation

```java
// ✅ Good - configuration is automatically validated on load
public static void main(String[] args) {
    try {
        // Configuration is loaded and validated automatically
        URI apiUrl = ConfigProvider.baseUrlApi();
        System.out.println("API URL: " + apiUrl);
    } catch (IllegalStateException e) {
        System.err.println("Configuration error: " + e.getMessage());
        System.exit(1);
    }
}
```

### 3. Error Handling

```java
// ✅ Good - handle configuration errors
try {
    int timeout = ConfigProvider.readTimeoutMs();
    // Use timeout
} catch (IllegalStateException e) {
    log.error("Configuration validation failed: {}", e.getMessage());
    // Fallback or graceful degradation
}
```

### 4. Safe Logging

```java
// ✅ Good - use safe dump for logging
log.info("Configuration loaded:\n{}", ConfigProvider.dumpMasked());

// ✅ Good - direct access to non-secret values
log.info("API URL: {}", ConfigProvider.baseUrlApi());
log.info("Environment: {}", ConfigProvider.appEnv());

// ❌ Avoid - direct logging of secrets
// log.info("Password: {}", ConfigProvider.basicAuthPassword()); // Don't do this!
```

## 🔧 Configuration for Different Projects

### Test Automation Framework

```properties
# application-test.properties
headless=true
parallelExecution=true
threadCount=4
screenshotOnFailure=true
logLevel=INFO
```

### CI/CD Pipeline

```bash
# Set variables for CI/CD
export ENV=ci
export BASE_URL=$CI_BASE_URL
export API_TOKEN=$CI_API_TOKEN

# Run tests
./gradlew test
```

### Local Development

```properties
# application-local.properties
baseUrlUi=http://localhost:3000
baseUrlApi=http://localhost:8080/api
logLevel=DEBUG
headless=false
screenshotOnFailure=true
```

## 📚 Test Framework Usage Examples

### 1. WebDriver Configuration

```java
public class WebDriverConfig {
    public static WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();
        
        if (ConfigProvider.headless()) {
            options.addArguments("--headless");
        }
        
        // Use reasonable default values for window size
        options.addArguments("--window-size=1920,1080");
        
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts()
            .implicitlyWait(Duration.ofSeconds(10)) // Reasonable default value
            .pageLoadTimeout(Duration.ofSeconds(30));
        
        return driver;
    }
}
```

### 2. Test Suite Configuration

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseTestSuite {
    
    @BeforeAll
    void setUp() {
        // Configuration is automatically validated on load
        System.out.println("Test configuration:");
        System.out.println(ConfigProvider.dumpMasked());
        
        // Setup parallel execution
        if (ConfigProvider.parallelExecution()) {
            int threadCount = ConfigProvider.threadCount();
            System.setProperty("junit.jupiter.execution.parallel.enabled", "true");
            System.setProperty("junit.jupiter.execution.parallel.config.strategy", "fixed");
            System.setProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", 
                String.valueOf(threadCount));
        }
    }
    
    @AfterAll
    void tearDown() {
        // Clean up resources if needed
        System.out.println("Tests completed for environment: " + ConfigProvider.appEnv());
    }
}
```

### 3. Environment-specific Test Classes

```java
@Tag("dev")
@TestPropertySource(properties = {"app.env=dev"})
public class DevEnvironmentTest extends BaseTestSuite {
    
    @Test
    void shouldWorkWithDevEnvironment() {
        URI baseUrl = ConfigProvider.baseUrlUi();
        String url = baseUrl.toString();
        assertTrue(url.contains("localhost") || url.contains("dev"));
        assertEquals("dev", ConfigProvider.appEnv());
    }
}

@Tag("staging")
@TestPropertySource(properties = {"app.env=stage"})
public class StagingEnvironmentTest extends BaseTestSuite {
    
    @Test
    void shouldWorkWithStagingEnvironment() {
        URI baseUrl = ConfigProvider.baseUrlUi();
        String url = baseUrl.toString();
        assertTrue(url.contains("stage"));
        assertEquals("stage", ConfigProvider.appEnv());
    }
}
```

### API Client Configuration

```java
public class ApiClient {
    private final URI baseUrl;
    private final int readTimeout;
    private final int connectTimeout;
    private final String apiToken;
    
    public ApiClient() {
        this.baseUrl = ConfigProvider.baseUrlApi();
        this.readTimeout = ConfigProvider.readTimeoutMs();
        this.connectTimeout = ConfigProvider.connectTimeoutMs();
        this.apiToken = ConfigProvider.apiToken();
    }
    
    public Response makeRequest(String endpoint) {
        // Using configuration
        OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
            .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
            .build();
            
        Request request = new Request.Builder()
            .url(baseUrl.resolve(endpoint).toString())
            .addHeader("Authorization", "Bearer " + apiToken)
            .build();
            
        return client.newCall(request).execute();
    }
}
```

### TestNG Integration

```java
@Test
public class SocksShopTestSuite {
    
    @BeforeClass
    public void setUp() {
        // Configuration is automatically validated on load
        System.out.println("Starting tests with configuration:");
        System.out.println("Environment: " + ConfigProvider.appEnv());
        System.out.println("API URL: " + ConfigProvider.baseUrlApi());
        System.out.println("UI URL: " + ConfigProvider.baseUrlUi());
        System.out.println("Headless mode: " + ConfigProvider.headless());
        
        // Setup parallel execution for TestNG
        if (ConfigProvider.parallelExecution()) {
            System.setProperty("testng.thread.count", String.valueOf(ConfigProvider.threadCount()));
        }
    }
    
    @Test
    public void testApiConnection() {
        URI apiUrl = ConfigProvider.baseUrlApi();
        // Test API connection
        assertNotNull(apiUrl);
        assertTrue(apiUrl.toString().startsWith("http"));
    }
}
```

## 🚀 Conclusion

This configuration module provides:

- **Professional approach** to configuration management
- **Flexibility** for different environments
- **Performance** through caching
- **Security** when working with sensitive data
- **Reliability** through validation and error handling
- **Monitoring** for usage tracking

The module is ready for use in production environments and test frameworks, providing stable and efficient configuration management.

## 📋 Test Framework Usage Checklist

- [ ] Environment profiles configured (`application-{env}.properties` or `configuration-{env}.properties`)
- [ ] `.env` file created for local development (added to `.gitignore`)
- [ ] Configuration automatically validated on load
- [ ] Use type-safe `ConfigProvider` methods for configuration access
- [ ] Handle `IllegalStateException` for configuration errors
- [ ] Safe logging through `ConfigProvider.dumpMasked()`
- [ ] Environment variables configured for CI/CD (ENV_VAR_NAME format)
- [ ] All environment profiles tested (local, dev, ci, qa, stage, prod)
- [ ] Base test class configured using ConfigProvider
- [ ] Environment-specific test classes created
- [ ] Parallel execution configured through `ConfigProvider.parallelExecution()`
- [ ] Allure integrated for reporting through `ConfigProvider.allureAttachHttp()`
