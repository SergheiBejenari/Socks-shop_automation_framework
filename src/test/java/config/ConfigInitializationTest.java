package config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigInitializationTest {

    @Test
    public void testWatcherStartsAndLogsOnFirstAccess() throws Exception {
        String original = System.getProperty("CONFIG_LOG_LEVEL");
        System.setProperty("CONFIG_LOG_LEVEL", "DEBUG");
        try {
            Logger logger = (Logger) LoggerFactory.getLogger("config");
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);

            try {
                ConfigProvider.reload("test init");
                ConfigProvider.baseUrlUi();

                assertThat(waitForLog(appender, "Configuration file watcher started"))
                        .as("File watcher should log start message")
                        .isTrue();

                assertThat(waitForLog(appender, "Configuration dump"))
                        .as("Configuration dump should be logged once")
                        .isTrue();
            } finally {
                logger.detachAppender(appender);
                appender.stop();
            }
        } finally {
            if (original != null) {
                System.setProperty("CONFIG_LOG_LEVEL", original);
            } else {
                System.clearProperty("CONFIG_LOG_LEVEL");
            }
        }
    }

    private boolean waitForLog(ListAppender<ILoggingEvent> appender, String expectedFragment) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            boolean found = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(msg -> msg.contains(expectedFragment));
            if (found) {
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }
}
