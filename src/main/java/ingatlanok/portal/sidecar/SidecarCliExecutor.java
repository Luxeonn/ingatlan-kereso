package ingatlanok.portal.sidecar;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import ingatlanok.common.AppLogger;
import ingatlanok.common.JsonSupport;
import ingatlanok.common.SlugUtils;
import ingatlanok.model.SearchCriteria;

public class SidecarCliExecutor {

  private static final AppLogger LOGGER = AppLogger.getLogger(SidecarCliExecutor.class);
  private static final int DEFAULT_TIMEOUT_SECONDS = 20;
  private static String customExecutable = null;

  private SidecarCliExecutor() {}

  public static void setCustomExecutable(String executable) {
    customExecutable = executable;
  }

  public static void resetCustomExecutable() {
    customExecutable = null;
  }

  public static boolean isAvailable() {
    return resolveCommand(List.of("--help")) != null;
  }

  public static Optional<SidecarResponse> execute(String portalId, SearchCriteria criteria) {
    if (portalId == null || criteria == null) {
      return Optional.empty();
    }

    List<String> cliArgs = buildArguments(portalId, criteria);
    List<String> fullCommand = resolveCommand(cliArgs);
    if (fullCommand == null || fullCommand.isEmpty()) {
      LOGGER.debug("Sidecar parancs nem érhető el a(z) {} portálhoz", portalId);
      return Optional.empty();
    }

    LOGGER.info("Sidecar futtatása: {}", String.join(" ", fullCommand));
    try {
      ProcessBuilder pb = new ProcessBuilder(fullCommand);
      pb.redirectErrorStream(false);

      Process process = pb.start();

      StringBuilder stdout = new StringBuilder();
      StringBuilder stderr = new StringBuilder();

      Thread outThread =
          Thread.ofVirtual()
              .start(
                  () -> {
                    try (BufferedReader reader =
                        new BufferedReader(
                            new InputStreamReader(
                                process.getInputStream(), StandardCharsets.UTF_8))) {
                      String line;
                      while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                      }
                    } catch (Exception ignored) {
                       // A failed stream read is reflected in the process result and diagnostics.
                    }
                  });

      Thread errThread =
          Thread.ofVirtual()
              .start(
                  () -> {
                    try (BufferedReader reader =
                        new BufferedReader(
                            new InputStreamReader(
                                process.getErrorStream(), StandardCharsets.UTF_8))) {
                      String line;
                      while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                      }
                    } catch (Exception ignored) {
                       // A failed stream read is reflected in the process result and diagnostics.
                    }
                  });

      boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        LOGGER.warn("Időtúllépés a sidecar futtatása során ({})", portalId);
        return Optional.empty();
      }

      outThread.join(2000);
      errThread.join(2000);

      String stdoutJson = stdout.toString().trim();
      if (stdoutJson.isEmpty()) {
        if (!stderr.isEmpty()) {
          LOGGER.warn("Üres stdout válasz ({}). Stderr: {}", portalId, stderr.toString().trim());
        }
        return Optional.empty();
      }

      int jsonStart = stdoutJson.indexOf("{");
      int jsonEnd = stdoutJson.lastIndexOf("}");
      if (jsonStart >= 0 && jsonEnd > jsonStart) {
        stdoutJson = stdoutJson.substring(jsonStart, jsonEnd + 1);
      }

      SidecarResponse response = JsonSupport.mapper().readValue(stdoutJson, SidecarResponse.class);
      return Optional.ofNullable(response);

    } catch (tools.jackson.core.JacksonException e) {
      LOGGER.error("JSON szintaxis hiba a sidecar kimenetében: {}", e.getMessage(), e);
      return Optional.empty();
    } catch (Exception e) {
      LOGGER.error("Hiba a sidecar futtatásakor: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  public static List<String> buildArguments(String portalId, SearchCriteria criteria) {
    List<String> args = new ArrayList<>();
    args.add("--portal");
    args.add(portalId);

    String city = criteria.getCity();
    String citySlug = SlugUtils.toSlug(city);
    if (citySlug.isEmpty()) {
      citySlug = "debrecen";
    }
    args.add("--city");
    args.add(citySlug);

    String rawType =
        criteria.getPropertyType() != null
            ? criteria.getPropertyType().toLowerCase().trim()
            : "lakas";
    args.add("--type");
    args.add(rawType);

    if (criteria.getMinPriceMillion() != null) {
      args.add("--min-price");
      args.add(String.valueOf(criteria.getMinPriceMillion()));
    }

    if (criteria.getMaxPriceMillion() != null) {
      args.add("--max-price");
      args.add(String.valueOf(criteria.getMaxPriceMillion()));
    }

    return args;
  }

  private static List<String> resolveCommand(List<String> args) {
    if (customExecutable != null) {
      List<String> cmd = new ArrayList<>();
      cmd.add(customExecutable);
      cmd.addAll(args);
      return cmd;
    }

    File exe = new File("tools/scraper-sidecar.exe");
    if (exe.exists() && exe.canExecute()) {
      List<String> cmd = new ArrayList<>();
      cmd.add(exe.getAbsolutePath());
      cmd.addAll(args);
      return cmd;
    }

    File script = new File("tools/sidecar.py");
    if (script.exists()) {
      String pythonCmd = detectPythonCommand();
      if (pythonCmd != null) {
        List<String> cmd = new ArrayList<>();
        cmd.add(pythonCmd);
        cmd.add(script.getAbsolutePath());
        cmd.addAll(args);
        return cmd;
      }
    }

    return null;
  }

  private static String cachedPythonCmd = null;
  private static boolean pythonChecked = false;

  private static synchronized String detectPythonCommand() {
    if (pythonChecked) {
      return cachedPythonCmd;
    }
    pythonChecked = true;

    String[] candidates = {"python", "py", "python3"};
    for (String candidate : candidates) {
      try {
        Process p = new ProcessBuilder(candidate, "--version").start();
        boolean finished = p.waitFor(2, TimeUnit.SECONDS);
        if (finished && p.exitValue() == 0) {
          cachedPythonCmd = candidate;
          return cachedPythonCmd;
        }
      } catch (Exception ignored) {
        // Try the next supported Python launcher.
      }
    }
    return null;
  }
}
