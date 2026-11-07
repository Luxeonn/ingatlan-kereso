package ingatlanok.common.resilience;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** Reservation-based token bucket rate limiter keyed by portal host. */
public final class PortalRateLimiter {

  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  public void acquire(String host) throws InterruptedException {
    int defaultRate = host.contains("jofogas") ? 2 : 3;
    String property = System.getProperty("scraper.rate." + host);
    String envKey = "SCRAPER_RATE_" + host.toUpperCase(Locale.ROOT).replace('.', '_');
    String configuredRate = property != null ? property : System.getenv(envKey);
    int permitsPerSecond =
        configuredRate == null ? defaultRate : parseRate(configuredRate, defaultRate);
    Bucket bucket = buckets.computeIfAbsent(host, ignored -> new Bucket(permitsPerSecond));

    long waitNanos = bucket.reserveSlot(permitsPerSecond);
    if (waitNanos > 0) {
      long waitMillis = waitNanos / 1_000_000L;
      int remainingNanos = (int) (waitNanos % 1_000_000L);
      Thread.sleep(waitMillis, remainingNanos);
    }
  }

  public void reset() {
    buckets.clear();
  }

  private int parseRate(String configuredRate, int defaultRate) {
    try {
      return Math.max(1, Integer.parseInt(configuredRate));
    } catch (NumberFormatException ignored) {
      return defaultRate;
    }
  }

  private static final class Bucket {
    private final ReentrantLock lock = new ReentrantLock();
    private double tokens;
    private long lastRefillNanos = System.nanoTime();

    private Bucket(int capacity) {
      tokens = capacity;
    }

    private long reserveSlot(int permitsPerSecond) {
      lock.lock();
      try {
        long now = System.nanoTime();
        if (now > lastRefillNanos) {
          tokens =
              Math.min(
                  permitsPerSecond,
                  tokens + (now - lastRefillNanos) * permitsPerSecond / 1_000_000_000.0);
          lastRefillNanos = now;
        }
        tokens -= 1.0;
        if (tokens >= 0.0) {
          return 0L;
        }
        return (long) Math.ceil((-tokens) * 1_000_000_000.0 / permitsPerSecond);
      } finally {
        lock.unlock();
      }
    }
  }
}
