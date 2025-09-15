package config;

import org.testng.annotations.Test;
import static org.assertj.core.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigInitializationTest {

    @Test
    public void testWatcherStartsAndLogsOnFirstAccess() throws Exception {
        String original = System.getProperty("CONFIG_LOG_LEVEL");
        System.setProperty("CONFIG_LOG_LEVEL", "DEBUG");
        try {
            Path logFile = Path.of("build/logs/config.log");
            Files.deleteIfExists(logFile);

            ConfigProvider.reload("test init");
            ConfigProvider.baseUrlUi();

            String content = Files.readString(logFile);
            assertThat(content).contains("Configuration file watcher started");
            assertThat(content).contains("Configuration dump");
        } finally {
            if (original != null) {
                System.setProperty("CONFIG_LOG_LEVEL", original);
            } else {
                System.clearProperty("CONFIG_LOG_LEVEL");
            }
        }
    }
}
