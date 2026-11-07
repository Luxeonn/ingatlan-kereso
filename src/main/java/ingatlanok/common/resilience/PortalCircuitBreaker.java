package ingatlanok.common.resilience;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

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
    state.lock.lock();
    try {
      if (state.openedAt == 0) {
        return true;
      }
      if (System.currentTimeMillis() - state.openedAt < COOLDOWN_MILLIS) {
        return false;
      }
      if (state.probeInProgress) {
        return false;
      }
      state.probeInProgress = true;
      return true;
    } finally {
      state.lock.unlock();
    }
  }

  public void recordSuccess(String portal) {
    State state = states.computeIfAbsent(portal, ignored -> new State());
    state.lock.lock();
    try {
      state.failures = 0;
      state.openedAt = 0;
      state.probeInProgress = false;
    } finally {
      state.lock.unlock();
    }
  }

  public void recordFailure(String portal) {
    State state = states.computeIfAbsent(portal, ignored -> new State());
    state.lock.lock();
    try {
      state.probeInProgress = false;
      if (++state.failures >= FAILURE_THRESHOLD) {
        state.openedAt = System.currentTimeMillis();
      }
    } finally {
      state.lock.unlock();
    }
  }

  public void releaseProbeIfActive(String portal) {
    State state = states.get(portal);
    if (state == null) {
      return;
    }
    state.lock.lock();
    try {
      state.probeInProgress = false;
    } finally {
      state.lock.unlock();
    }
  }

  public void reset() {
    states.clear();
  }

  private static final class State {
    private final ReentrantLock lock = new ReentrantLock();
    private int failures;
    private long openedAt;
    private boolean probeInProgress;
  }
}
