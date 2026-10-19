package ingatlanok.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
  private static final ConcurrentHashMap<String, AppLogger> LOGGERS = new ConcurrentHashMap<>();
  private static final ReentrantLock FILE_LOCK = new ReentrantLock();

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
      formattedMessage +=
          " [" + throwable.getClass().getSimpleName() + ": " + throwable.getMessage() + "]";
    }

    if (consoleLoggingEnabled) {
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
        default -> throw new IllegalStateException("Unsupported log level: " + level);
      }
    }

    if (fileLoggingEnabled && logFilePath != null) {
      String timestamp = LocalDateTime.now().format(FORMATTER);
      String logLine =
          "[" + timestamp + "] [" + level.name() + "] [" + name + "] - " + formattedMessage;
      writeToFile(logLine);
    }
  }

  private static void writeToFile(String line) {
    FILE_LOCK.lock();
    try {
      Path parent = logFilePath.getParent();
      if (parent != null && !Files.exists(parent)) {
        Files.createDirectories(parent);
      }
      try (BufferedWriter writer =
          Files.newBufferedWriter(
              logFilePath,
              StandardCharsets.UTF_8,
              StandardOpenOption.CREATE,
              StandardOpenOption.APPEND)) {
        writer.write(line);
        writer.newLine();
      }
    } catch (IOException ignored) {
      // Logging must not interrupt the operation being logged.
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
