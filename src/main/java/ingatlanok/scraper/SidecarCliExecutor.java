package ingatlanok.scraper;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import ingatlanok.common.SlugUtils;
import ingatlanok.model.SearchCriteria;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class SidecarCliExecutor {

    private static final int DEFAULT_TIMEOUT_SECONDS = 20;
    private static final Gson GSON = new Gson();

    private static String customExecutable = null;

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
            return Optional.empty();
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(fullCommand);
            pb.redirectErrorStream(false);

            Process process = pb.start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread outThread = Thread.ofVirtual().start(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (Exception ignored) {
                }
            });

            Thread errThread = Thread.ofVirtual().start(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (Exception ignored) {
                }
            });

            boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                System.err.println("[SidecarCliExecutor] Időtúllépés a sidecar futtatása során (" + portalId + ")");
                return Optional.empty();
            }

            outThread.join(2000);
            errThread.join(2000);

            String stdoutJson = stdout.toString().trim();
            if (stdoutJson.isEmpty()) {
                if (!stderr.isEmpty()) {
                    System.err.println("[SidecarCliExecutor] Üres stdout válasz (" + portalId + "). Stderr: " + stderr.toString().trim());
                }
                return Optional.empty();
            }

            int jsonStart = stdoutJson.indexOf("{");
            int jsonEnd = stdoutJson.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                stdoutJson = stdoutJson.substring(jsonStart, jsonEnd + 1);
            }

            SidecarResponse response = GSON.fromJson(stdoutJson, SidecarResponse.class);
            return Optional.ofNullable(response);

        } catch (JsonSyntaxException e) {
            System.err.println("[SidecarCliExecutor] JSON szintaxis hiba a sidecar kimenetében: " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("[SidecarCliExecutor] Hiba a sidecar futtatásakor: " + e.getMessage());
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

        String rawType = criteria.getPropertyType() != null ? criteria.getPropertyType().toLowerCase().trim() : "lakas";
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
            }
        }
        return null;
    }
}
