package ingatlanok.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
        AppLogger.clearRecentLogs();
    }

    @AfterEach
    void tearDown() {
        AppLogger.setGlobalLevel(originalLevel);
        AppLogger.setLogFilePath(originalFile);
        AppLogger.setFileLoggingEnabled(true);
        AppLogger.setConsoleLoggingEnabled(true);
        AppLogger.clearRecentLogs();
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
    @DisplayName("Logging formats and circular buffer recording")
    void testMessageFormattingAndBuffer() throws IOException {
        AppLogger logger = AppLogger.getLogger("BufferTest");

        logger.debug("Debug üzenet: {}", 123);
        logger.info("Info üzenet: {} és {}", "alma", "körte");
        logger.warn("Figyelmeztetés: {}", "érték");
        logger.error("Hiba történt: {}", "szerver");
        logger.error("Kivétel történt", new IllegalArgumentException("Teszt kivétel"));

        List<String> logs = AppLogger.getRecentLogs(10);
        assertEquals(5, logs.size());

        assertTrue(logs.get(0).contains("[DEBUG] [BufferTest] - Debug üzenet: 123"));
        assertTrue(logs.get(1).contains("[INFO] [BufferTest] - Info üzenet: alma és körte"));
        assertTrue(logs.get(2).contains("[WARN] [BufferTest] - Figyelmeztetés: érték"));
        assertTrue(logs.get(3).contains("[ERROR] [BufferTest] - Hiba történt: szerver"));
        assertTrue(logs.get(4).contains("IllegalArgumentException: Teszt kivétel"));

        assertTrue(Files.exists(testLogFile));
        List<String> fileLines = Files.readAllLines(testLogFile);
        assertEquals(5, fileLines.size());
    }

    @Test
    @DisplayName("Recent logs limit and clearing")
    void testRecentLogsLimitAndClear() {
        AppLogger logger = AppLogger.getLogger("LimitTest");

        for (int i = 0; i < 20; i++) {
            logger.info("Log index {}", i);
        }

        List<String> lastFive = AppLogger.getRecentLogs(5);
        assertEquals(5, lastFive.size());
        assertTrue(lastFive.getLast().contains("Log index 19"));

        List<String> allLogs = AppLogger.getRecentLogs(100);
        assertEquals(20, allLogs.size());

        AppLogger.clearRecentLogs();
        assertEquals(0, AppLogger.getRecentLogs(10).size());
    }

    @Test
    @DisplayName("LogLevel filtering ignores lower priority messages")
    void testLevelFiltering() {
        AppLogger logger = AppLogger.getLogger("FilterTest");
        AppLogger.setGlobalLevel(AppLogger.LogLevel.WARN);

        logger.debug("Ezt ki kell hagyni");
        logger.info("Ezt is ki kell hagyni");
        logger.warn("Ez megjelenik");
        logger.error("Ez is megjelenik");

        List<String> logs = AppLogger.getRecentLogs(10);
        assertEquals(2, logs.size());
        assertTrue(logs.get(0).contains("[WARN]"));
        assertTrue(logs.get(1).contains("[ERROR]"));
    }

    @Test
    @DisplayName("Multi-threaded concurrent logging safety")
    void testConcurrentLogging() throws InterruptedException {
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

        List<String> recent = AppLogger.getRecentLogs(500);
        assertFalse(recent.isEmpty());
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

        List<String> recent = AppLogger.getRecentLogs(10);
        assertEquals(3, recent.size());
    }

    @Test
    @DisplayName("LogEntry record creation, details and level filtering")
    void testLogEntryRecordsAndLevelFiltering() {
        AppLogger logger = AppLogger.getLogger("StructuredTest");
        AppLogger.setGlobalLevel(AppLogger.LogLevel.DEBUG);

        logger.debug("Debug msg");
        logger.info("Info msg 1");
        logger.info("Info msg 2");
        logger.warn("Warn msg");
        logger.error("Error msg");

        List<AppLogger.LogEntry> entries = AppLogger.getRecentLogEntries(10);
        assertEquals(5, entries.size());

        AppLogger.LogEntry infoEntry = entries.get(1);
        assertEquals("INFO", infoEntry.level());
        assertEquals("StructuredTest", infoEntry.logger());
        assertEquals("Info msg 1", infoEntry.message());
        assertFalse(infoEntry.timestamp().isBlank());
        assertTrue(infoEntry.rawLine().contains("[INFO] [StructuredTest] - Info msg 1"));

        List<AppLogger.LogEntry> infoOnly = AppLogger.getRecentLogEntries(10, "INFO");
        assertEquals(2, infoOnly.size());
        assertTrue(infoOnly.stream().allMatch(e -> e.level().equals("INFO")));

        List<AppLogger.LogEntry> warnOnly = AppLogger.getRecentLogEntries(10, "WARN");
        assertEquals(1, warnOnly.size());
        assertEquals("Warn msg", warnOnly.getFirst().message());

        List<AppLogger.LogEntry> errorOnly = AppLogger.getRecentLogEntries(10, "ERROR");
        assertEquals(1, errorOnly.size());

        AppLogger.clearRecentLogs();
        assertEquals(0, AppLogger.getRecentLogEntries(10).size());
        assertEquals(0, AppLogger.getRecentLogs(10).size());
    }
}
