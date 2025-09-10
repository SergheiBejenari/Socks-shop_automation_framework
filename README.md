# Configuration Manager Module

Профессиональный модуль для работы с конфигурационными файлами, разработанный с учетом лучших практик SDET.

## 🎯 Основные возможности

- **Многоуровневое разрешение конфигурации** с приоритетом источников
- **Профили окружения** (dev, test, qa, stage, prod)
- **Кэширование** для повышения производительности
- **Валидация конфигурации** с детальной отчетностью
- **Безопасная работа** с чувствительными данными
- **Метрики и мониторинг** доступа к конфигурации
- **Thread-safe** операции для многопоточных сред

## 🚀 Быстрый старт

### 1. Сборка и тестирование

```bash
# Сборка проекта
./gradlew clean build

# Запуск всех тестов
./gradlew test

# Запуск тестов с профилем
./gradlew test -Denv=test

# Запуск конкретного теста
./gradlew test --tests ConfigLayerTest

# Запуск с переопределением конфигурации
./gradlew test -Dbase.url=https://custom.example.com -Dtimeout.seconds=60
```

### 2. Запуск тестов

```bash
# Запуск всех тестов
./gradlew test

# Запуск конкретного теста
./gradlew test --tests ConfigLayerTest

# Запуск с детальным выводом
./gradlew test --info
```

### 3. Сборка проекта

```bash
# Очистка и сборка
./gradlew clean build

# Создание JAR файла
./gradlew jar
```

## 🏗️ Архитектура

### Основные компоненты

```
ConfigManager     - Основной менеджер конфигурации
├── ConfigCache   - Кэш для типизированных значений
├── ConfigLoader  - Загрузчик файлов конфигурации
├── ConfigValidator - Валидатор конфигурации
├── ConfigLogger  - Логирование конфигурации
└── ConfigKeys    - Enum с ключами конфигурации
```

### Приоритет источников конфигурации

1. **System Properties** (`-Dkey=value`) - высший приоритет
2. **Environment Variables** (`ENV_VAR_NAME`)
3. **`.env` файл** (для локальной разработки)
4. **Профильные файлы** (`config-{env}.properties`)
5. **Основной файл** (`config.properties`)
6. **Значения по умолчанию** из `ConfigKeys` enum

## 📁 Структура конфигурационных файлов

### Основной файл: `config.properties`
```properties
# Базовые настройки для всех окружений
base.url=https://example.com
timeout.seconds=30
browser=chrome
```

### Профильные файлы: `config-{env}.properties`
```properties
# config-dev.properties
base.url=http://localhost:3000
log.level=DEBUG

# config-test.properties
base.url=https://test.example.com
timeout.seconds=15
browser.headless=true

# config-prod.properties
base.url=https://app.example.com
log.level=WARN
browser.headless=true
```

## 🔧 API Reference

### ConfigCache - Кэшированный доступ

```java
// Типизированные геттеры
String value = ConfigCache.getString(ConfigKeys.KEY);
int number = ConfigCache.getInt(ConfigKeys.NUMBER);
boolean flag = ConfigCache.getBoolean(ConfigKeys.FLAG);
double decimal = ConfigCache.getDouble(ConfigKeys.DECIMAL);
long bigNumber = ConfigCache.getLong(ConfigKeys.BIG_NUMBER);

// Кастомные типы
Duration timeout = ConfigCache.get(ConfigKeys.TIMEOUT, 
    value -> Duration.ofSeconds(Long.parseLong(value)));

// Управление кэшем
ConfigCache.clear();           // Очистить весь кэш
ConfigCache.remove(ConfigKeys.KEY); // Удалить конкретный ключ
int size = ConfigCache.size(); // Размер кэша
boolean cached = ConfigCache.isCached(ConfigKeys.KEY); // Проверить наличие в кэше
```

### ConfigManager - Основные операции

```java
// Разрешение конфигурации
String value = ConfigManager.resolve(ConfigKeys.KEY);

// Обновление конфигурации
ConfigManager.refresh();

// Проверка здоровья
boolean healthy = ConfigManager.isHealthy();

// Безопасное логирование (маскирует чувствительные данные)
String safeValue = ConfigManager.getForLogging(ConfigKeys.KEY);

// Метрики
Map<String, Long> accessMetrics = ConfigManager.getAccessMetrics();
Map<String, Long> errorMetrics = ConfigManager.getErrorMetrics();
int cacheSize = ConfigManager.getCacheSize();
```

### ConfigValidator - Валидация

```java
// Полная валидация с детальным отчетом
ConfigValidator.ValidationResult result = ConfigValidator.validateAll();
if (result.isValid()) {
    System.out.println("✅ Конфигурация валидна");
} else {
    result.getErrors().forEach(System.err::println);
    result.getWarnings().forEach(System.out::println);
}

// Быстрая валидация (бросает исключение при ошибке)
ConfigValidator.validateRequired();
```

### ConfigLogger - Логирование

```java
// Логирование при старте
ConfigLogger.logStartupConfiguration();

// Детальное логирование (DEBUG уровень)
ConfigLogger.logDetailedConfiguration();

// Метрики доступа
ConfigLogger.logMetrics();

// Статус здоровья
ConfigLogger.logHealthStatus();
```

## 🧪 Тестирование

### Запуск тестов

```bash
# Запуск всех тестов
./gradlew test

# Запуск конкретного теста
./gradlew test --tests ConfigLayerTest

# Запуск с детальным выводом
./gradlew test --info
```

## 🚀 GitLab CI/CD

### Автоматизированный Pipeline

Проект включает готовый GitLab CI/CD pipeline с следующими возможностями:

- **Multi-stage pipeline**: validate → test → report
- **Parallel test execution** для ускорения выполнения
- **Environment-specific testing** (dev, staging)
- **Allure reporting** с красивыми отчетами
- **Coverage reporting** с JaCoCo
- **Artifact management** для результатов тестов

### Доступные версии Pipeline

1. **`.gitlab-ci.yml`** - основная упрощенная версия (рекомендуется для начала)
2. **`.gitlab-ci-full.yml`** - полная версия с расширенными возможностями
3. **`.gitlab-ci-test.yml`** - минимальная версия для тестирования

### Pipeline Stages

```yaml
stages:
  - validate      # Валидация кода и конфигурации
  - test         # Параллельное выполнение тестов
  - report       # Генерация отчетов (Allure)
```

### Запуск Pipeline

Pipeline автоматически запускается при:
- Push в `main` или `develop` ветки
- Создании Merge Request
- Ручном запуске через GitLab UI

### Переменные окружения

```bash
# Обязательные переменные
TEST_ENV=ci                    # Окружение для тестов
PARALLEL_TESTS=true           # Параллельное выполнение
TEST_THREAD_COUNT=4           # Количество потоков

# Опциональные переменные
SLACK_WEBHOOK_URL=            # Webhook для уведомлений
```

### Структура тестов

- **ConfigLayerTest** - Основные тесты конфигурационного модуля
- **ConfigTestExtension** - JUnit 5 расширение для настройки тестового окружения

### Примеры тестов

```java
@Test
@DisplayName("Should resolve configuration from test profile")
void shouldResolveConfigurationFromTestProfile() {
    String baseUrl = ConfigManager.resolve(ConfigKeys.BASE_URL);
    assertEquals("https://test.example.com", baseUrl);
}

@Test
@DisplayName("Should cache configuration values for performance")
void shouldCacheConfigurationValuesForPerformance() {
    ConfigCache.clear();
    String firstCall = ConfigCache.getString(ConfigKeys.BASE_URL);
    String secondCall = ConfigCache.getString(ConfigKeys.BASE_URL);
    
    assertEquals(firstCall, secondCall);
    assertTrue(ConfigCache.isCached(ConfigKeys.BASE_URL));
}
```

## 🔒 Безопасность

### Маскирование чувствительных данных

Автоматически маскируются ключи, содержащие:
- `password`
- `secret`
- `token`
- `key`
- `credential`
- `auth`

### Примеры

```java
// В логах чувствительные значения отображаются как *****
String dbPassword = ConfigManager.getForLogging(ConfigKeys.DB_PASSWORD);
// Результат: *****

// При прямом доступе получаем реальное значение
String actualPassword = ConfigManager.resolve(ConfigKeys.DB_PASSWORD);
// Результат: actualPassword123
```

## 📊 Мониторинг и метрики

### Метрики доступа

```java
Map<String, Long> accessMetrics = ConfigManager.getAccessMetrics();
// Результат: { "base.url" -> 15, "timeout.seconds" -> 8, ... }
```

### Метрики ошибок

```java
Map<String, Long> errorMetrics = ConfigManager.getErrorMetrics();
// Результат: { "missing.key" -> 3, "invalid.value" -> 1, ... }
```

### Размер кэша

```java
int cacheSize = ConfigCache.size();
// Результат: количество закэшированных значений
```

## 🌍 Поддержка окружений

### Переменные окружения

```bash
# Установка переменных окружения
export BASE_URL=https://staging.example.com
export ENV=stage
export LOG_LEVEL=DEBUG

# Запуск приложения
java -jar app.jar
```

### System Properties

```bash
# Передача через командную строку
java -Dbase.url=https://prod.example.com -Denv=prod -jar app.jar

# Установка в коде
System.setProperty("timeout.seconds", "60");
```

### .env файл

```bash
# .env файл в корне проекта
BASE_URL=http://localhost:3000
DB_URL=jdbc:postgresql://localhost:5432/dev_db
LOG_LEVEL=DEBUG
```

## 🚨 Обработка ошибок

### ConfigurationException

```java
try {
    String value = ConfigCache.getString(ConfigKeys.REQUIRED_KEY);
} catch (ConfigurationException e) {
    log.error("Configuration error: {}", e.getMessage());
    // Обработка ошибки конфигурации
}
```

### Типы ошибок

1. **Missing Required Configuration** - отсутствует обязательная конфигурация
2. **Invalid Value** - неверное значение (нельзя распарсить)
3. **Configuration Loading Failed** - ошибка загрузки файлов конфигурации

## 🔄 Динамическое обновление

### Обновление конфигурации

```java
// Обновить конфигурацию в runtime
ConfigManager.refresh();

// Очистить кэш
ConfigCache.clear();

// Проверить здоровье после обновления
boolean healthy = ConfigManager.isHealthy();
```

## 📈 Производительность

### Кэширование

- **Автоматическое кэширование** при первом обращении
- **Типизированное хранение** (без повторного парсинга)
- **Thread-safe операции** с ConcurrentHashMap

### Оптимизации

- **Lazy loading** конфигурации
- **Efficient parsing** с кэшированием результатов
- **Minimal memory footprint** для неиспользуемых значений

## 🛠️ Лучшие практики

### 1. Использование кэша

```java
// ✅ Хорошо - использует кэш
String url = ConfigCache.getString(ConfigKeys.BASE_URL);

// ❌ Плохо - каждый раз разрешает заново
String url = ConfigManager.resolve(ConfigKeys.BASE_URL);
```

### 2. Валидация при старте

```java
// ✅ Хорошо - проверяем конфигурацию при старте
public static void main(String[] args) {
    try {
        ConfigValidator.validateRequired();
        // Продолжаем работу
    } catch (ConfigurationException e) {
        System.err.println("Configuration error: " + e.getMessage());
        System.exit(1);
    }
}
```

### 3. Обработка ошибок

```java
// ✅ Хорошо - обрабатываем ошибки конфигурации
try {
    String value = ConfigCache.getString(ConfigKeys.KEY);
} catch (ConfigurationException e) {
    log.error("Failed to get configuration: {}", e.getMessage());
    // Fallback или graceful degradation
}
```

### 4. Логирование

```java
// ✅ Хорошо - логируем безопасно
log.info("Base URL: {}", ConfigManager.getForLogging(ConfigKeys.BASE_URL));

// ❌ Плохо - может залогировать чувствительные данные
log.info("Base URL: {}", ConfigManager.resolve(ConfigKeys.BASE_URL));
```

## 🔧 Настройка для разных проектов

### Test Automation Framework

```properties
# config-test.properties
browser.headless=true
test.parallel=true
test.thread.count=4
ui.screenshot.on.failure=true
mock.external.services=true
```

### CI/CD Pipeline

```bash
# Установка переменных для CI/CD
export ENV=ci
export BASE_URL=$CI_BASE_URL
export API_TOKEN=$CI_API_TOKEN

# Запуск тестов
./gradlew test
```

### Local Development

```properties
# config-dev.properties
base.url=http://localhost:3000
log.level=DEBUG
browser.headless=false
ui.video.recording=true
```

## 📚 Примеры использования в Test Framework

### 1. WebDriver Configuration

```java
public class WebDriverConfig {
    public static WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();
        
        if (ConfigCache.getBoolean(ConfigKeys.HEADLESS)) {
            options.addArguments("--headless");
        }
        
        options.addArguments("--window-size=" + ConfigCache.getString(ConfigKeys.WINDOW_SIZE));
        
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts()
            .implicitlyWait(ConfigCache.getInt(ConfigKeys.IMPLICIT_WAIT), TimeUnit.SECONDS)
            .pageLoadTimeout(ConfigCache.getInt(ConfigKeys.PAGE_LOAD_TIMEOUT), TimeUnit.SECONDS);
        
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
        // Валидация конфигурации перед запуском тестов
        ConfigValidator.validateRequired();
        
        // Настройка параллельного выполнения
        if (ConfigCache.getBoolean(ConfigKeys.PARALLEL_EXECUTION)) {
            int threadCount = ConfigCache.getInt(ConfigKeys.THREAD_COUNT);
            System.setProperty("junit.jupiter.execution.parallel.enabled", "true");
            System.setProperty("junit.jupiter.execution.parallel.config.strategy", "fixed");
            System.setProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", 
                String.valueOf(threadCount));
        }
    }
    
    @AfterAll
    void tearDown() {
        // Очистка ресурсов
        ConfigCache.clear();
    }
}
```

### 3. Environment-specific Test Classes

```java
@Tag("dev")
@TestPropertySource(properties = {"env=dev"})
public class DevEnvironmentTest extends BaseTestSuite {
    
    @Test
    void shouldWorkWithDevEnvironment() {
        String baseUrl = ConfigCache.getString(ConfigKeys.BASE_URL);
        assertTrue(baseUrl.contains("localhost") || baseUrl.contains("dev"));
    }
}

@Tag("staging")
@TestPropertySource(properties = {"env=stage"})
public class StagingEnvironmentTest extends BaseTestSuite {
    
    @Test
    void shouldWorkWithStagingEnvironment() {
        String baseUrl = ConfigCache.getString(ConfigKeys.BASE_URL);
        assertTrue(baseUrl.contains("staging"));
    }
}
```

### API Client Configuration

```java
public class ApiClient {
    private final String baseUrl;
    private final int timeout;
    private final String apiToken;
    
    public ApiClient() {
        this.baseUrl = ConfigCache.getString(ConfigKeys.API_BASE_URL);
        this.timeout = ConfigCache.getInt(ConfigKeys.API_TIMEOUT);
        this.apiToken = ConfigCache.getString(ConfigKeys.API_TOKEN);
    }
    
    public Response makeRequest(String endpoint) {
        // Использование конфигурации
        return client.newCall(request)
            .timeout(timeout, TimeUnit.SECONDS)
            .execute();
    }
}
```

### Test Suite Configuration

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestSuite {
    
    @BeforeAll
    void setUp() {
        // Валидация конфигурации перед запуском тестов
        ConfigValidator.validateRequired();
        
        // Настройка параллельного выполнения
        if (ConfigCache.getBoolean(ConfigKeys.PARALLEL_EXECUTION)) {
            int threadCount = ConfigCache.getInt(ConfigKeys.THREAD_COUNT);
            System.setProperty("junit.jupiter.execution.parallel.enabled", "true");
            System.setProperty("junit.jupiter.execution.parallel.config.strategy", "fixed");
            System.setProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", 
                String.valueOf(threadCount));
        }
    }
}
```

## 🚀 Заключение

Этот конфигурационный модуль предоставляет:

- **Профессиональный подход** к управлению конфигурацией
- **Гибкость** для различных окружений
- **Производительность** через кэширование
- **Безопасность** при работе с чувствительными данными
- **Надежность** через валидацию и обработку ошибок
- **Мониторинг** для отслеживания использования

Модуль готов для использования в production средах и тестовых фреймворках, обеспечивая стабильную и эффективную работу с конфигурацией.

## 📋 Чек-лист для использования в Test Framework

- [ ] Настроены профили окружения (`config-{env}.properties`)
- [ ] Создан `.env` файл для локальной разработки (добавлен в `.gitignore`)
- [ ] Валидация конфигурации в `@BeforeAll` тестов
- [ ] Использование `ConfigCache` для частого доступа к конфигурации
- [ ] Обработка `ConfigurationException` в критических местах
- [ ] Безопасное логирование через `ConfigManager.getForLogging()`
- [ ] Настроены GitLab CI/CD переменные окружения
- [ ] Протестированы все профили окружения
- [ ] Настроен базовый класс `BaseTestSuite` для тестов
- [ ] Созданы environment-specific тестовые классы
- [ ] Настроено параллельное выполнение тестов
- [ ] Интегрирован Allure для отчетности
