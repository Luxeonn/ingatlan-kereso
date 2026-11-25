package ingatlanok.portal.sidecar;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.slf4j.Slf4j;

import ingatlanok.common.JsonSupport;
import ingatlanok.common.SlugUtils;
import ingatlanok.model.SearchCriteria;

@Slf4j
public class SidecarCliExecutor {

  public static final int DEFAULT_MAX_PAGES = 20;

  private static final int MIN_TIMEOUT_SECONDS = 25;
  private static final int MAX_TIMEOUT_SECONDS = 105;
  private static final int MAX_SOFT_TIMEOUT_MS = 90_000;
  private static final ReentrantLock RUNTIME_DETECT_LOCK = new ReentrantLock();

  private static volatile List<String> customExecutables = null;
  private static volatile String cachedNodeCmd = null;
  private static volatile boolean nodeChecked = false;

  private SidecarCliExecutor() {}

  public static void setCustomExecutable(String executable) {
    customExecutables = executable != null ? List.of(executable) : null;
  }

  public static void setCustomExecutables(List<String> executables) {
    customExecutables = executables != null ? List.copyOf(executables) : null;
  }

  public static void resetCustomExecutable() {
    customExecutables = null;
  }

  public static boolean isAvailable() {
    return !resolveCommandCandidates(List.of("--help")).isEmpty();
  }

  public static int calculateTimeoutSeconds(int maxPages) {
    int safePages = maxPages > 0 ? maxPages : DEFAULT_MAX_PAGES;
    return Math.max(MIN_TIMEOUT_SECONDS, Math.min(MAX_TIMEOUT_SECONDS, 20 + safePages * 6));
  }

  public static int calculateSoftTimeoutMs(int maxPages) {
    int timeoutSeconds = calculateTimeoutSeconds(maxPages);
    return Math.min(MAX_SOFT_TIMEOUT_MS, Math.max(10_000, (timeoutSeconds - 15) * 1000));
  }

  public static Optional<SidecarResponse> execute(String portalId, SearchCriteria criteria) {
    return execute(portalId, criteria, DEFAULT_MAX_PAGES);
  }

  public static Optional<SidecarResponse> execute(
      String portalId, SearchCriteria criteria, int maxPages) {
    if (portalId == null || criteria == null) {
      return Optional.empty();
    }

    int safeMaxPages = maxPages > 0 ? maxPages : DEFAULT_MAX_PAGES;
    int timeoutSeconds = calculateTimeoutSeconds(safeMaxPages);
    List<String> cliArgs = buildArguments(portalId, criteria, safeMaxPages);
    List<List<String>> candidates = resolveCommandCandidates(cliArgs);
    if (candidates.isEmpty()) {
      log.debug("Sidecar parancs nem érhető el a(z) {} portálhoz", portalId);
      return Optional.empty();
    }

    long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
    return executeCandidates(portalId, candidates, timeoutSeconds, deadlineNanos);
  }

  private static Optional<SidecarResponse> executeCandidates(
      String portalId, List<List<String>> candidates, int initialBudgetSeconds, long deadlineNanos) {
    SidecarResponse bestSuccess = null;
    SidecarResponse lastFailure = null;
    for (int idx = 0; idx < candidates.size(); idx++) {
      if (Thread.currentThread().isInterrupted()) {
        break;
      }
      int remainingSeconds = computeRemainingSeconds(deadlineNanos, idx == 0 ? initialBudgetSeconds : 5);
      if (idx > 0 && remainingSeconds <= 5) {
        break;
      }
      List<String> candidateCmd = adjustCandidateTimeoutFlag(candidates.get(idx), remainingSeconds);
      Optional<SidecarResponse> current = executeCommand(portalId, candidateCmd, remainingSeconds);
      if (current.isEmpty()) {
        if (Thread.currentThread().isInterrupted()) {
          break;
        }
        continue;
      }
      SidecarResponse resp = current.get();
      boolean hasNext = idx + 1 < candidates.size();
      if (isUsableSuccess(resp)) {
        bestSuccess = pickBetterSuccess(bestSuccess, resp);
        if (!isPartialBotOrRateLimitedStop(resp.getStopReason())) {
          return Optional.of(bestSuccess);
        }
        logPartialFallback(resp, hasNext);
      } else {
        lastFailure = resp;
        logFailureFallback(resp, hasNext);
      }
    }
    return Optional.ofNullable(bestSuccess != null ? bestSuccess : lastFailure);
  }

  private static int computeRemainingSeconds(long deadlineNanos, int minSeconds) {
    long remainingSec = TimeUnit.NANOSECONDS.toSeconds(deadlineNanos - System.nanoTime());
    return (int) Math.max(minSeconds, Math.min(MAX_TIMEOUT_SECONDS, remainingSec));
  }

  private static List<String> adjustCandidateTimeoutFlag(
      List<String> command, int remainingSeconds) {
    int softBudgetMs = Math.min(MAX_SOFT_TIMEOUT_MS, Math.max(5_000, (remainingSeconds - 10) * 1000));
    List<String> adjusted = new ArrayList<>(command);
    for (int i = 0; i < adjusted.size() - 1; i++) {
      if ("--timeout-ms".equals(adjusted.get(i))) {
        adjusted.set(i + 1, String.valueOf(softBudgetMs));
        break;
      }
    }
    return adjusted;
  }

  private static boolean isUsableSuccess(SidecarResponse resp) {
    return resp.isSuccess() && !resp.getItems().isEmpty();
  }

  private static void logPartialFallback(SidecarResponse resp, boolean hasNextCandidate) {
    if (hasNextCandidate) {
      log.warn(
          "Sidecar részleges eredmény ({} db, ok={}), fallback sidecar futtatása...",
          resp.getItems().size(),
          resp.getStopReason());
    }
  }

  private static void logFailureFallback(SidecarResponse resp, boolean hasNextCandidate) {
    if (hasNextCandidate) {
      log.warn(
          "Sidecar sikertelen ({}), fallback sidecar futtatása...",
          resp.getErrorMessage() != null ? resp.getErrorMessage() : resp.getStatus());
    }
  }

  private static SidecarResponse pickBetterSuccess(
      SidecarResponse currentBest, SidecarResponse candidate) {
    if (currentBest == null) {
      return candidate;
    }
    return candidate.getItems().size() > currentBest.getItems().size() ? candidate : currentBest;
  }

  private static boolean isPartialBotOrRateLimitedStop(String stopReason) {
    return "INTERMEDIATE_PAGE_BOT_PROTECTED".equals(stopReason)
        || "INTERMEDIATE_PAGE_RATE_LIMITED".equals(stopReason);
  }

  private static Optional<SidecarResponse> executeCommand(
      String portalId, List<String> fullCommand, int timeoutSeconds) {
    log.info("Sidecar futtatása: {}", String.join(" ", fullCommand));
    Process process = null;
    try {
      ProcessBuilder pb = new ProcessBuilder(fullCommand);
      pb.redirectErrorStream(false);
      process = pb.start();

      StringBuilder stdout = new StringBuilder();
      StringBuilder stderr = new StringBuilder();

      boolean finished = readProcessStreams(process, stdout, stderr, timeoutSeconds);
      if (!finished) {
        log.warn(
            "Időtúllépés a sidecar futtatása során ({}, limit={}s)", portalId, timeoutSeconds);
        return parseResponseIfPresent(stdout.toString().trim());
      }

      String stdoutJson = stdout.toString().trim();
      if (stdoutJson.isEmpty()) {
        if (!stderr.isEmpty()) {
          log.warn("Üres stdout válasz ({}). Stderr: {}", portalId, stderr.toString().trim());
        }
        return Optional.empty();
      }

      return parseResponseIfPresent(stdoutJson);
    } catch (InterruptedException e) {
      if (process != null && process.isAlive()) {
        process.destroyForcibly();
      }
      Thread.currentThread().interrupt();
      log.warn("A sidecar futtatása megszakadt ({})", portalId);
      return Optional.empty();
    } catch (tools.jackson.core.JacksonException e) {
      log.error("JSON szintaxis hiba a sidecar kimenetében: {}", e.getMessage(), e);
      return Optional.empty();
    } catch (Exception e) {
      log.error("Hiba a sidecar futtatásakor: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  private static Optional<SidecarResponse> parseResponseIfPresent(String stdoutJson) {
    if (stdoutJson == null || stdoutJson.isEmpty()) {
      return Optional.empty();
    }
    String payload = extractJsonPayload(stdoutJson);
    if (!payload.startsWith("{") || !payload.endsWith("}")) {
      return Optional.empty();
    }
    SidecarResponse response = JsonSupport.mapper().readValue(payload, SidecarResponse.class);
    return Optional.ofNullable(response);
  }

  private static boolean readProcessStreams(
      Process process, StringBuilder stdout, StringBuilder stderr, int timeoutSeconds)
      throws InterruptedException {
    Thread outThread = Thread.ofVirtual().start(() -> readStream(process.getInputStream(), stdout));
    Thread errThread = Thread.ofVirtual().start(() -> readStream(process.getErrorStream(), stderr));

    boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
    if (!finished) {
      process.destroyForcibly();
      outThread.join(500);
      errThread.join(500);
      return false;
    }

    outThread.join(2000);
    errThread.join(2000);
    return true;
  }

  private static void readStream(java.io.InputStream in, StringBuilder target) {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        target.append(line).append("\n");
      }
    } catch (Exception ignored) {
      // Process stream errors are captured via process exit state.
    }
  }

  private static String extractJsonPayload(String stdoutJson) {
    int jsonStart = stdoutJson.indexOf("{");
    int jsonEnd = stdoutJson.lastIndexOf("}");
    if (jsonStart >= 0 && jsonEnd > jsonStart) {
      return stdoutJson.substring(jsonStart, jsonEnd + 1);
    }
    return stdoutJson;
  }

  public static List<String> buildArguments(String portalId, SearchCriteria criteria) {
    return buildArguments(portalId, criteria, DEFAULT_MAX_PAGES);
  }

  public static List<String> buildArguments(
      String portalId, SearchCriteria criteria, int maxPages) {
    int safeMaxPages = maxPages > 0 ? maxPages : DEFAULT_MAX_PAGES;
    List<String> args = new ArrayList<>();
    args.add("--portal");
    args.add(portalId);

    String citySlug = SlugUtils.toSlug(criteria.getCity());
    args.add("--city");
    args.add(citySlug.isEmpty() ? "debrecen" : citySlug);

    String rawType =
        criteria.getPropertyType() != null
            ? criteria.getPropertyType().getSlug()
            : "lakas";
    args.add("--type");
    args.add(rawType);

    appendPositiveBound(args, "--min-price", criteria.getMinPriceMillion());
    appendMaxBound(args, "--max-price", criteria.getMaxPriceMillion());
    appendPositiveBound(args, "--min-area", criteria.getMinArea());
    appendMaxBound(args, "--max-area", criteria.getMaxArea());

    args.add("--max-pages");
    args.add(String.valueOf(safeMaxPages));
    args.add("--timeout-ms");
    args.add(String.valueOf(calculateSoftTimeoutMs(safeMaxPages)));

    return args;
  }

  private static void appendPositiveBound(List<String> args, String flag, Integer value) {
    if (value != null && value > 0) {
      args.add(flag);
      args.add(String.valueOf(value));
    }
  }

  private static void appendMaxBound(List<String> args, String flag, Integer value) {
    if (value != null && value > 0 && value < 9999) {
      args.add(flag);
      args.add(String.valueOf(value));
    }
  }

  private static List<List<String>> resolveCommandCandidates(List<String> args) {
    List<String> configured = customExecutables;
    if (configured != null && !configured.isEmpty()) {
      return configured.stream()
          .filter(exe -> exe != null && !exe.isBlank())
          .map(exe -> buildCmd(exe, null, args))
          .toList();
    }

    List<List<String>> candidates = new ArrayList<>();
    File nodeScript = new File("tools/sidecar-playwright.mjs");
    String nodeCmd = nodeScript.exists() && hasPlaywrightModule() ? detectNodeCommand() : null;
    if (nodeCmd != null) {
      candidates.add(buildCmd(nodeCmd, nodeScript.getAbsolutePath(), args));
    }
    return candidates;
  }

  private static boolean hasPlaywrightModule() {
    String[] candidatePaths = {
      "tools/node_modules/playwright-core/index.mjs",
      "tools/node_modules/playwright/index.mjs",
      "frontend/node_modules/playwright/index.mjs",
      "frontend/node_modules/playwright-core/index.mjs"
    };
    for (String candidatePath : candidatePaths) {
      if (new File(candidatePath).exists()) {
        return true;
      }
    }
    return false;
  }

  private static List<String> buildCmd(String cmd, String scriptPath, List<String> args) {
    List<String> fullCmd = new ArrayList<>();
    fullCmd.add(cmd);
    if (scriptPath != null) {
      fullCmd.add(scriptPath);
    }
    fullCmd.addAll(args);
    return fullCmd;
  }

  private static String detectNodeCommand() {
    if (nodeChecked) {
      return cachedNodeCmd;
    }
    RUNTIME_DETECT_LOCK.lock();
    try {
      if (nodeChecked) {
        return cachedNodeCmd;
      }
      if (canRunCommand("node", "--version")) {
        cachedNodeCmd = "node";
      }
      return cachedNodeCmd;
    } finally {
      nodeChecked = true;
      RUNTIME_DETECT_LOCK.unlock();
    }
  }

  private static boolean canRunCommand(String... command) {
    try {
      Process p = new ProcessBuilder(command).start();
      boolean finished = p.waitFor(3, TimeUnit.SECONDS);
      if (!finished) {
        p.destroyForcibly();
        return false;
      }
      return p.exitValue() == 0;
    } catch (Exception ignored) {
      return false;
    }
  }
}
