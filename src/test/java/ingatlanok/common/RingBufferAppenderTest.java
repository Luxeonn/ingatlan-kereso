package ingatlanok.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RingBufferAppenderTest {

    private static final Logger log = LoggerFactory.getLogger(RingBufferAppenderTest.class);

    @BeforeEach
    void setUp() {
        RingBufferAppender.ensureRegistered();
        AppLogger.clearRecentLogs();
    }

    @AfterEach
    void tearDown() {
        AppLogger.clearRecentLogs();
    }

    @Test
    @DisplayName("SLF4J logs are captured by RingBufferAppender into AppLogger buffer")
    void testSlf4jLoggingCapturedInRingBuffer() {
        String testMessage = "Test SLF4J log entry for verification";
        log.info(testMessage);

        List<AppLogger.LogEntry> entries = AppLogger.getRecentLogEntries(10);
        assertFalse(entries.isEmpty(), "Recent log entries should contain captured SLF4J log");

        boolean found = entries.stream().anyMatch(e ->
                e.message().contains(testMessage) && e.logger().contains("RingBufferAppenderTest"));
        assertTrue(found, "The emitted SLF4J message should be present in AppLogger buffer");
    }

    @Test
    @DisplayName("RingBufferAppender handles null events gracefully")
    void testNullEventSafety() {
        RingBufferAppender appender = new RingBufferAppender();
        assertDoesNotThrow(() -> appender.append(null));
    }

    @Test
    @DisplayName("LogEntry record provides loggerName accessor for JSON serialization")
    void testLogEntryLoggerNameAccessor() {
        AppLogger.LogEntry entry = new AppLogger.LogEntry("2026-10-17 21:00:00.000", "INFO", "TestLogger", "Hello", "[raw]");
        assertEquals("TestLogger", entry.logger());
        assertEquals("TestLogger", entry.loggerName());
        assertEquals("TestLogger", entry.getLoggerName());
    }
}
