package ingatlanok.core.resilience;

import java.util.concurrent.ConcurrentHashMap;

/** Small token bucket limiter keyed by portal host. */
public final class PortalRateLimiter {

  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  public void acquire(String host) throws InterruptedException {
    int defaultRate = host.contains("jofogas") ? 2 : 3;
    String property = System.getProperty("scraper.rate." + host);
    String envKey = "SCRAPER_RATE_" + host.toUpperCase(java.util.Locale.ROOT).replace('.', '_');
    String configuredRate = property != null ? property : System.getenv(envKey);
    int permitsPerSecond = configuredRate == null ? defaultRate : parseRate(configuredRate, defaultRate);
    Bucket bucket = buckets.computeIfAbsent(host, ignored -> new Bucket(permitsPerSecond));
    while (true) {
      long waitNanos;
      synchronized (bucket) {
        long now = System.nanoTime();
        bucket.tokens =
            Math.min(
                permitsPerSecond,
                bucket.tokens + (now - bucket.lastRefillNanos) * permitsPerSecond / 1_000_000_000.0);
        bucket.lastRefillNanos = now;
        if (bucket.tokens >= 1) {
          bucket.tokens -= 1;
          return;
        }
        waitNanos = (long) Math.ceil((1 - bucket.tokens) * 1_000_000_000L / permitsPerSecond);
      }
      long waitMillis = waitNanos / 1_000_000L;
      int remainingNanos = (int) (waitNanos % 1_000_000L);
      Thread.sleep(waitMillis, remainingNanos);
    }
  }

  private int parseRate(String configuredRate, int defaultRate) {
    try {
      return Math.max(1, Integer.parseInt(configuredRate));
    } catch (NumberFormatException ignored) {
      return defaultRate;
    }
  }

  private static final class Bucket {
    private double tokens;
    private long lastRefillNanos = System.nanoTime();

    private Bucket(int capacity) {
      tokens = capacity;
    }
  }
}
