package ingatlanok.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;

public class AppLogger {

    public enum LogLevel {
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4);

        private final int priority;

        LogLevel(int priority) {
            this.priority = priority;
        }

        public boolean isLoggable(LogLevel currentLevel) {
            return this.priority >= currentLevel.priority;
        }
    }

    public record LogEntry(String timestamp, String level, String logger, String message, String rawLine) {
        public String loggerName() {
            return logger;
        }

        public String getLoggerName() {
            return logger;
        }
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final ConcurrentHashMap<String, AppLogger> LOGGERS = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedDeque<String> RECENT_LOGS = new ConcurrentLinkedDeque<>();
    private static final ConcurrentLinkedDeque<LogEntry> RECENT_LOG_ENTRIES = new ConcurrentLinkedDeque<>();
    private static final int MAX_RECENT_LOGS = 500;
    private static final ReentrantLock FILE_LOCK = new ReentrantLock();
    private static final ThreadLocal<Boolean> IN_APP_LOGGER = ThreadLocal.withInitial(() -> false);

    static {
        RingBufferAppender.ensureRegistered();
    }

    private static volatile LogLevel globalLevel = LogLevel.INFO;
    private static volatile Path logFilePath = Paths.get("logs", "scraper.log");
    private static volatile boolean fileLoggingEnabled = true;
    private static volatile boolean consoleLoggingEnabled = true;

    private final String name;
    private final Logger slf4jLogger;

    private AppLogger(String name) {
        this.name = name != null ? name : "App";
        this.slf4jLogger = LoggerFactory.getLogger(this.name);
    }

    public static boolean isInAppLogger() {
        return Boolean.TRUE.equals(IN_APP_LOGGER.get());
    }

    public static void recordLogEntry(LogEntry entry) {
        if (entry != null) {
            recordRecentLog(entry);
        }
    }

    public static AppLogger getLogger(Class<?> clazz) {
        return getLogger(clazz != null ? clazz.getSimpleName() : "App");
    }

    public static AppLogger getLogger(String name) {
        return LOGGERS.computeIfAbsent(name != null ? name : "App", AppLogger::new);
    }

    public static void setGlobalLevel(LogLevel level) {
        if (level != null) {
            globalLevel = level;
        }
    }

    public static LogLevel getGlobalLevel() {
        return globalLevel;
    }

    public static void setLogFilePath(Path path) {
        FILE_LOCK.lock();
        try {
            logFilePath = path;
        } finally {
            FILE_LOCK.unlock();
        }
    }

    public static Path getLogFilePath() {
        return logFilePath;
    }

    public static void setFileLoggingEnabled(boolean enabled) {
        fileLoggingEnabled = enabled;
    }

    public static void setConsoleLoggingEnabled(boolean enabled) {
        consoleLoggingEnabled = enabled;
    }

    public static List<String> getRecentLogs(int limit) {
        int max = Math.min(Math.max(limit, 0), RECENT_LOGS.size());
        List<String> list = new ArrayList<>(RECENT_LOGS);
        if (list.size() <= max) {
            return list;
        }
        return list.subList(list.size() - max, list.size());
    }

    public static List<LogEntry> getRecentLogEntries(int limit) {
        return getRecentLogEntries(limit, null);
    }

    public static List<LogEntry> getRecentLogEntries(int limit, String levelFilter) {
        List<LogEntry> list = new ArrayList<>(RECENT_LOG_ENTRIES);
        if (levelFilter != null && !levelFilter.isBlank() && !levelFilter.equalsIgnoreCase("ALL")) {
            String filter = levelFilter.trim().toUpperCase();
            list = list.stream()
                    .filter(e -> e.level().equalsIgnoreCase(filter))
                    .toList();
        }
        int max = Math.min(Math.max(limit, 0), list.size());
        if (list.size() <= max) {
            return list;
        }
        return list.subList(list.size() - max, list.size());
    }

    public static void clearRecentLogs() {
        RECENT_LOG_ENTRIES.clear();
        RECENT_LOGS.clear();
    }

    public boolean isDebugEnabled() {
        return LogLevel.DEBUG.isLoggable(globalLevel);
    }

    public boolean isInfoEnabled() {
        return LogLevel.INFO.isLoggable(globalLevel);
    }

    public boolean isWarnEnabled() {
        return LogLevel.WARN.isLoggable(globalLevel);
    }

    public boolean isErrorEnabled() {
        return LogLevel.ERROR.isLoggable(globalLevel);
    }

    public void debug(String message, Object... args) {
        log(LogLevel.DEBUG, message, null, args);
    }

    public void info(String message, Object... args) {
        log(LogLevel.INFO, message, null, args);
    }

    public void warn(String message, Object... args) {
        log(LogLevel.WARN, message, null, args);
    }

    public void error(String message, Object... args) {
        log(LogLevel.ERROR, message, null, args);
    }

    public void error(String message, Throwable throwable, Object... args) {
        log(LogLevel.ERROR, message, throwable, args);
    }

    private void log(LogLevel level, String message, Throwable throwable, Object... args) {
        if (!level.isLoggable(globalLevel)) {
            return;
        }

        String formattedMessage = formatMessage(message, args);
        if (throwable != null) {
            formattedMessage += " [" + throwable.getClass().getSimpleName() + ": " + throwable.getMessage() + "]";
        }

        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logLine = "[" + timestamp + "] [" + level.name() + "] [" + name + "] - " + formattedMessage;
        LogEntry entry = new LogEntry(timestamp, level.name(), name, formattedMessage, logLine);

        recordRecentLog(entry);

        if (consoleLoggingEnabled) {
            IN_APP_LOGGER.set(true);
            try {
                switch (level) {
                    case DEBUG -> {
                        if (throwable != null) {
                            slf4jLogger.debug(formattedMessage, throwable);
                        } else {
                            slf4jLogger.debug(formattedMessage);
                        }
                    }
                    case INFO -> {
                        if (throwable != null) {
                            slf4jLogger.info(formattedMessage, throwable);
                        } else {
                            slf4jLogger.info(formattedMessage);
                        }
                    }
                    case WARN -> {
                        if (throwable != null) {
                            slf4jLogger.warn(formattedMessage, throwable);
                        } else {
                            slf4jLogger.warn(formattedMessage);
                        }
                    }
                    case ERROR -> {
                        if (throwable != null) {
                            slf4jLogger.error(formattedMessage, throwable);
                        } else {
                            slf4jLogger.error(formattedMessage);
                        }
                    }
                }
            } finally {
                IN_APP_LOGGER.set(false);
            }
        }

        if (fileLoggingEnabled && logFilePath != null) {
            writeToFile(logLine);
        }
    }

    private static void recordRecentLog(LogEntry entry) {
        RECENT_LOG_ENTRIES.add(entry);
        while (RECENT_LOG_ENTRIES.size() > MAX_RECENT_LOGS) {
            RECENT_LOG_ENTRIES.poll();
        }
        RECENT_LOGS.add(entry.rawLine());
        while (RECENT_LOGS.size() > MAX_RECENT_LOGS) {
            RECENT_LOGS.poll();
        }
    }

    private static void writeToFile(String line) {
        FILE_LOCK.lock();
        try {
            Path parent = logFilePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(
                    logFilePath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException ignored) {
        } finally {
            FILE_LOCK.unlock();
        }
    }

    private static String formatMessage(String message, Object... args) {
        if (message == null) {
            return "";
        }
        if (args == null || args.length == 0) {
            return message;
        }

        StringBuilder sb = new StringBuilder(message.length() + 32);
        int argIndex = 0;
        int i = 0;
        while (i < message.length()) {
            if (i < message.length() - 1 && message.charAt(i) == '{' && message.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    sb.append(args[argIndex++]);
                } else {
                    sb.append("{}");
                }
                i += 2;
            } else {
                sb.append(message.charAt(i++));
            }
        }
        return sb.toString();
    }
}
