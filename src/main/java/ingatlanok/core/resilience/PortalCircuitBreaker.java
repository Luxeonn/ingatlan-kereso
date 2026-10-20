package ingatlanok.core.resilience;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Per-portal circuit breaker with a single half-open probe after the cooldown. */
public final class PortalCircuitBreaker {

  private static final int FAILURE_THRESHOLD =
      Math.max(1, Integer.getInteger("scraper.circuit.failure-threshold", 3));
  private static final long COOLDOWN_MILLIS =
      Duration.ofSeconds(Math.max(1L, Long.getLong("scraper.circuit.cooldown-seconds", 60L)))
          .toMillis();
  private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

  public boolean allowRequest(String portal) {
    State state = states.computeIfAbsent(portal, ignored -> new State());
    synchronized (state) {
      if (state.openedAt == 0) {
        return true;
      }
      if (System.currentTimeMillis() - state.openedAt < COOLDOWN_MILLIS) {
        return false;
      }
      return state.probeInProgress.compareAndSet(false, true);
    }
  }

  public void recordSuccess(String portal) {
    State state = states.computeIfAbsent(portal, ignored -> new State());
    synchronized (state) {
      state.failures = 0;
      state.openedAt = 0;
      state.probeInProgress.set(false);
    }
  }

  public void recordFailure(String portal) {
    State state = states.computeIfAbsent(portal, ignored -> new State());
    synchronized (state) {
      state.probeInProgress.set(false);
      if (++state.failures >= FAILURE_THRESHOLD) {
        state.openedAt = System.currentTimeMillis();
      }
    }
  }

  private static final class State {
    private int failures;
    private long openedAt;
    private final AtomicBoolean probeInProgress = new AtomicBoolean();
  }
}
