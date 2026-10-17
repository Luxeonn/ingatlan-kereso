package ingatlanok.common;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Logback Appender, amely a szabványos SLF4J naplóeseményeket átirányítja az
 * {@link AppLogger} körkörös memóriapufferébe a Web Dashboard (/api/logs) számára.
 */
public class RingBufferAppender extends AppenderBase<ILoggingEvent> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static volatile boolean registered = false;

    /**
     * Biztosítja, hogy az appender regisztrálva legyen a Logback root loggerén.
     */
    public static synchronized void ensureRegistered() {
        if (registered) {
            return;
        }
        try {
            ILoggerFactory factory = LoggerFactory.getILoggerFactory();
            if (factory instanceof LoggerContext context) {
                ch.qos.logback.classic.Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
                if (rootLogger.getAppender("RING_BUFFER") == null) {
                    RingBufferAppender appender = new RingBufferAppender();
                    appender.setName("RING_BUFFER");
                    appender.setContext(context);
                    appender.start();
                    rootLogger.addAppender(appender);
                }
                registered = true;
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (eventObject == null || AppLogger.isInAppLogger()) {
            return;
        }

        String level = eventObject.getLevel() != null ? eventObject.getLevel().toString() : "INFO";
        String logger = eventObject.getLoggerName();
        String message = eventObject.getFormattedMessage();
        String timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(eventObject.getTimeStamp()),
                ZoneId.systemDefault()
        ).format(FORMATTER);

        String rawLine = "[" + timestamp + "] [" + level + "] [" + logger + "] - " + message;
        AppLogger.LogEntry entry = new AppLogger.LogEntry(timestamp, level, logger, message, rawLine);
        AppLogger.recordLogEntry(entry);
    }
}
