package ingatlanok.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AppLoggerTest {

    @TempDir
    Path tempDir;

    private Path testLogFile;
    private AppLogger.LogLevel originalLevel;
    private Path originalFile;

    @BeforeEach
    void setUp() {
        originalLevel = AppLogger.getGlobalLevel();
        originalFile = AppLogger.getLogFilePath();
        testLogFile = tempDir.resolve("test-scraper.log");
        AppLogger.setLogFilePath(testLogFile);
        AppLogger.setGlobalLevel(AppLogger.LogLevel.DEBUG);
        AppLogger.setFileLoggingEnabled(true);
        AppLogger.setConsoleLoggingEnabled(false);
    }

    @AfterEach
    void tearDown() {
        AppLogger.setGlobalLevel(originalLevel);
        AppLogger.setLogFilePath(originalFile);
        AppLogger.setFileLoggingEnabled(true);
        AppLogger.setConsoleLoggingEnabled(true);
    }

    @Test
    @DisplayName("AppLogger instance creation and caching")
    void testLoggerCaching() {
        AppLogger logger1 = AppLogger.getLogger(AppLoggerTest.class);
        AppLogger logger2 = AppLogger.getLogger(AppLoggerTest.class);
        AppLogger logger3 = AppLogger.getLogger("CustomCategory");

        assertSame(logger1, logger2);
        assertNotNull(logger3);
        assertNotSame(logger1, logger3);
    }

    @Test
    @DisplayName("Log levels enabled state")
    void testLogLevelsEnabled() {
        AppLogger logger = AppLogger.getLogger("TestLevel");

        AppLogger.setGlobalLevel(AppLogger.LogLevel.DEBUG);
        assertTrue(logger.isDebugEnabled());
        assertTrue(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());

        AppLogger.setGlobalLevel(AppLogger.LogLevel.WARN);
        assertFalse(logger.isDebugEnabled());
        assertFalse(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());

        AppLogger.setGlobalLevel(AppLogger.LogLevel.ERROR);
        assertFalse(logger.isDebugEnabled());
        assertFalse(logger.isInfoEnabled());
        assertFalse(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());
    }

    @Test
    @DisplayName("Logging formats and file output recording")
    void testMessageFormattingAndFileOutput() throws IOException {
        AppLogger logger = AppLogger.getLogger("FileTest");

        logger.debug("Debug üzenet: {}", 123);
        logger.info("Info üzenet: {} és {}", "alma", "körte");
        logger.warn("Figyelmeztetés: {}", "érték");
        logger.error("Hiba történt: {}", "szerver");
        logger.error("Kivétel történt", new IllegalArgumentException("Teszt kivétel"));

        assertTrue(Files.exists(testLogFile));
        List<String> fileLines = Files.readAllLines(testLogFile);
        assertEquals(5, fileLines.size());

        assertTrue(fileLines.get(0).contains("[DEBUG] [FileTest] - Debug üzenet: 123"));
        assertTrue(fileLines.get(1).contains("[INFO] [FileTest] - Info üzenet: alma és körte"));
        assertTrue(fileLines.get(2).contains("[WARN] [FileTest] - Figyelmeztetés: érték"));
        assertTrue(fileLines.get(3).contains("[ERROR] [FileTest] - Hiba történt: szerver"));
        assertTrue(fileLines.get(4).contains("IllegalArgumentException: Teszt kivétel"));
    }

    @Test
    @DisplayName("LogLevel filtering ignores lower priority messages")
    void testLevelFiltering() throws IOException {
        AppLogger logger = AppLogger.getLogger("FilterTest");
        AppLogger.setGlobalLevel(AppLogger.LogLevel.WARN);

        logger.debug("Ezt ki kell hagyni");
        logger.info("Ezt is ki kell hagyni");
        logger.warn("Ez megjelenik");
        logger.error("Ez is megjelenik");

        assertTrue(Files.exists(testLogFile));
        List<String> fileLines = Files.readAllLines(testLogFile);
        assertEquals(2, fileLines.size());
        assertTrue(fileLines.get(0).contains("[WARN]"));
        assertTrue(fileLines.get(1).contains("[ERROR]"));
    }

    @Test
    @DisplayName("Multi-threaded concurrent file logging safety")
    void testConcurrentLogging() throws InterruptedException, IOException {
        AppLogger logger = AppLogger.getLogger("ConcurrentTest");
        int threadCount = 20;
        int logsPerThread = 25;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            Thread.ofVirtual().start(() -> {
                for (int i = 0; i < logsPerThread; i++) {
                    logger.info("Thread {} log {}", threadId, i);
                }
                latch.countDown();
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed);

        assertTrue(Files.exists(testLogFile));
        List<String> lines = Files.readAllLines(testLogFile);
        assertEquals(threadCount * logsPerThread, lines.size());
    }

    @Test
    @DisplayName("Null and edge case argument handling")
    void testEdgeCases() {
        AppLogger logger = AppLogger.getLogger((String) null);
        assertNotNull(logger);
        AppLogger classLogger = AppLogger.getLogger((Class<?>) null);
        assertNotNull(classLogger);

        assertDoesNotThrow(() -> {
            logger.info(null);
            logger.info("Túl sok placeholder: {} {} {}", "egy");
            logger.info("Nincs placeholder", "felesleges", "argumentumok");
        });
    }
}
