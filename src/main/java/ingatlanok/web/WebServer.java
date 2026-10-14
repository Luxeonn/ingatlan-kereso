package ingatlanok.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import ingatlanok.common.AppLogger;
import ingatlanok.common.BrowserUtils;
import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.presentation.DashboardPresentationService;
import ingatlanok.presentation.DashboardResponseDto;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Beágyazott REST API szerver Java 21 Virtual Threads alapon.
 * Hardened localhost kötés, szigorú HTTP metódus ellenőrzés és biztonsági fejlécek.
 */
public class WebServer {

    private static final AppLogger LOGGER = AppLogger.getLogger(WebServer.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private HttpServer server;
    private final String host;
    private int port = 8080;

    public WebServer() {
        this(resolveDefaultHost(), resolveDefaultPort());
    }

    public WebServer(int port) {
        this(resolveDefaultHost(), port);
    }

    public WebServer(String host, int port) {
        this.host = (host != null && !host.isBlank()) ? host.trim() : resolveDefaultHost();
        this.port = port > 0 ? port : resolveDefaultPort();
    }

    public static String resolveDefaultHost() {
        String envHost = System.getenv("SERVER_HOST");
        if (envHost != null && !envHost.isBlank()) {
            return envHost.trim();
        }
        String propHost = System.getProperty("server.host");
        if (propHost != null && !propHost.isBlank()) {
            return propHost.trim();
        }
        return "127.0.0.1";
    }

    public static int resolveDefaultPort() {
        String envPort = System.getenv("SERVER_PORT");
        if (envPort != null && !envPort.isBlank()) {
            try {
                return Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        String propPort = System.getProperty("server.port");
        if (propPort != null && !propPort.isBlank()) {
            try {
                return Integer.parseInt(propPort.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 8080;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void start() throws IOException {
        int attempts = 0;
        int initialPort = port;
        while (attempts < 5) {
            try {
                server = HttpServer.create(new InetSocketAddress(host, port), 0);
                break;
            } catch (IOException e) {
                port++;
                attempts++;
            }
        }

        if (server == null) {
            LOGGER.error("Nem sikerült elindítani a webszervert a portokon: {}-{}", initialPort, port);
            throw new IOException("Nem sikerült elindítani a webszervert a portokon: " + initialPort + "-" + port);
        }

        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/", new RootHandler(port));
        server.createContext("/api/status", new StatusHandler(port));
        server.createContext("/api/config", new ConfigHandler());
        server.createContext("/api/cities", new CitiesHandler());
        server.createContext("/api/portals", new PortalsHandler());
        server.createContext("/api/history", new HistoryHandler());
        server.createContext("/api/properties", new PropertiesHandler());
        server.createContext("/api/search", new SearchHandler());
        server.createContext("/api/open-urls", new OpenUrlsHandler());
        server.createContext("/api/items/viewed", new MarkViewedHandler());
        server.createContext("/api/history/clear", new ClearHistoryHandler());
        server.createContext("/api/logs", new LogsHandler());
        server.createContext("/api/logs/clear", new LogsClearHandler());

        server.start();
        LOGGER.info("==================================================");
        LOGGER.info("  Web Dashboard elindult: http://localhost:{}", port);
        LOGGER.info("==================================================");
    }

    public void stop() {
        if (server != null) {
            LOGGER.info("Web Dashboard leállítása...");
            server.stop(0);
        }
    }

    private static void setSecurityHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Referrer-Policy", "strict-origin-when-cross-origin");
        exchange.getResponseHeaders().set("Content-Security-Policy", "default-src 'self' 'unsafe-inline'; img-src 'self' data: https:;");
    }

    private static class RootHandler implements HttpHandler {
        private final int port;

        public RootHandler(int port) {
            this.port = port;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setSecurityHeaders(exchange);
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            String html = WebDashboardHtml.getHtml(port);
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static class StatusHandler implements HttpHandler {
        private final int port;

        public StatusHandler(int port) {
            this.port = port;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            Map<String, Object> status = new LinkedHashMap<>();
            status.put("status", "UP");
            status.put("port", port);
            status.put("totalInDb", IngatlanRepository.getInstance().getTotalCount());
            status.put("lastRunTime", IngatlanRepository.getInstance().getLastRunTime());
            status.put("trackedCities", IngatlanRepository.getInstance().getTrackedCitiesInfo());
            sendJsonResponse(exchange, 200, status);
        }
    }

    private static class PortalsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            Map<String, Long> portalCounts = IngatlanRepository.getInstance().countByPortal();
            List<PortalDescriptor> descriptors = PortalEngineRegistry.getInstance().getAllDescriptors().stream()
                    .map(d -> {
                        long count = 0L;
                        String dId = d.id().trim().toLowerCase();
                        String dName = d.name().trim().toLowerCase();
                        for (Map.Entry<String, Long> entry : portalCounts.entrySet()) {
                            String k = entry.getKey().trim().toLowerCase();
                            if (k.equals(dId) || k.equals(dName) || k.contains(dId) || dName.contains(k)) {
                                count += entry.getValue();
                            }
                        }
                        return d.withSavedListingCount(count);
                    })
                    .sorted(Comparator.comparingLong(PortalDescriptor::savedListingCount).reversed()
                            .thenComparing(PortalDescriptor::name))
                    .toList();
            sendJsonResponse(exchange, 200, descriptors);
        }
    }

    private static class CitiesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("predefined", CityUtils.getPredefinedCities());
            data.put("presets", CityUtils.getPresets());
            data.put("trackedCities", IngatlanRepository.getInstance().getTrackedCitiesInfo());
            sendJsonResponse(exchange, 200, data);
        }
    }

    private static class ConfigHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                SearchCriteria sc = IngatlanRepository.getInstance().getLastCriteria();
                sendJsonResponse(exchange, 200, sc);
            } else if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String body = readBody(exchange);
                SearchCriteria sc = GSON.fromJson(body, SearchCriteria.class);
                if (sc != null) {
                    IngatlanRepository.getInstance().saveLastCriteria(sc);
                }
                sendJsonResponse(exchange, 200, Map.of("success", true));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    private static class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String cityParam = null;
            boolean onlyDiscounted = false;
            if (query != null) {
                for (String part : query.split("&")) {
                    if (part.startsWith("city=")) {
                        cityParam = URLDecoder.decode(part.substring(5), StandardCharsets.UTF_8);
                    } else if (part.equals("onlyDiscounted=true")) {
                        onlyDiscounted = true;
                    }
                }
            }

            List<IngatlanItem> items;
            if (onlyDiscounted) {
                items = IngatlanRepository.getInstance().getDiscountedProperties(cityParam);
            } else if (cityParam != null && !cityParam.isBlank()) {
                items = IngatlanRepository.getInstance().getItemsByCity(cityParam);
            } else {
                items = IngatlanRepository.getInstance().getAll();
            }
            sendJsonResponse(exchange, 200, items);
        }
    }

    private static class PropertiesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            String idOrUrlParam = null;

            if (query != null) {
                for (String part : query.split("&")) {
                    if (part.startsWith("id=")) {
                        idOrUrlParam = URLDecoder.decode(part.substring(3), StandardCharsets.UTF_8);
                        break;
                    } else if (part.startsWith("url=")) {
                        idOrUrlParam = URLDecoder.decode(part.substring(4), StandardCharsets.UTF_8);
                        break;
                    }
                }
            }

            if (path.contains("/price-history")) {
                if (idOrUrlParam == null) {
                    String[] parts = path.split("/");
                    if (parts.length >= 5 && !parts[3].isBlank() && !parts[3].equals("price-history")) {
                        idOrUrlParam = URLDecoder.decode(parts[3], StandardCharsets.UTF_8);
                    }
                }

                if (idOrUrlParam != null && !idOrUrlParam.isBlank()) {
                    List<ingatlanok.model.PriceHistoryEntry> history = IngatlanRepository.getInstance().getPriceHistory(idOrUrlParam);
                    sendJsonResponse(exchange, 200, history);
                } else {
                    sendJsonResponse(exchange, 400, Map.of("error", "Hiányzó 'id' vagy 'url' paraméter!"));
                }
                return;
            }

            if (path.contains("/status-log")) {
                if (idOrUrlParam == null) {
                    String[] parts = path.split("/");
                    if (parts.length >= 5 && !parts[3].isBlank() && !parts[3].equals("status-log")) {
                        idOrUrlParam = URLDecoder.decode(parts[3], StandardCharsets.UTF_8);
                    }
                }

                if (idOrUrlParam != null && !idOrUrlParam.isBlank()) {
                    List<ingatlanok.model.StatusLogEntry> log = IngatlanRepository.getInstance().getStatusLog(idOrUrlParam);
                    sendJsonResponse(exchange, 200, log);
                } else {
                    sendJsonResponse(exchange, 400, Map.of("error", "Hiányzó 'id' vagy 'url' paraméter!"));
                }
                return;
            }

            if (path.endsWith("/discounted")) {
                String cityParam = null;
                if (query != null) {
                    for (String part : query.split("&")) {
                        if (part.startsWith("city=")) {
                            cityParam = URLDecoder.decode(part.substring(5), StandardCharsets.UTF_8);
                            break;
                        }
                    }
                }
                List<IngatlanItem> discounted = IngatlanRepository.getInstance().getDiscountedProperties(cityParam);
                sendJsonResponse(exchange, 200, discounted);
                return;
            }

            sendResponse(exchange, 404, "Not Found");
        }
    }

    private static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String body = readBody(exchange);
                SearchCriteria criteria = GSON.fromJson(body, SearchCriteria.class);
                if (criteria == null) {
                    criteria = new SearchCriteria();
                }

                DashboardResponseDto result = DashboardPresentationService.getInstance().executeSearch(criteria);
                sendJsonResponse(exchange, 200, result);
            } catch (Exception e) {
                LOGGER.error("Váratlan hiba a keresés feldolgozásakor: " + e.getMessage(), e);
                sendJsonResponse(exchange, 500, Map.of("error", "Keresési hiba: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
            }
        }
    }

    private static class OpenUrlsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String body = readBody(exchange);
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> urls = GSON.fromJson(body, listType);
            if (urls != null && !urls.isEmpty()) {
                for (String url : urls) {
                    IngatlanRepository.getInstance().markAsViewed(url);
                }
                // Zero-Server-Desktop-Call: a kliensoldali böngészőfül-nyitás a felhasználó gépén fut.
                // A szerveroldali nyitás kizárólag akkor engedélyezett, ha nem headless és explicit kérték.
                boolean forceServerOpen = "true".equalsIgnoreCase(System.getProperty("server.open.desktop", "false"));
                if (forceServerOpen && !java.awt.GraphicsEnvironment.isHeadless()) {
                    BrowserUtils.openUrls(new LinkedHashSet<>(urls));
                }
            }
            sendJsonResponse(exchange, 200, Map.of("success", true, "count", urls != null ? urls.size() : 0));
        }
    }

    private static class MarkViewedHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String body = readBody(exchange);
            String url = null;
            if (body != null && !body.isBlank()) {
                try {
                    Map<?, ?> map = GSON.fromJson(body, Map.class);
                    if (map != null && map.get("url") != null) {
                        url = map.get("url").toString();
                    }
                } catch (Exception ignored) {
                }
            }

            boolean marked = false;
            if (url != null && !url.isBlank()) {
                marked = IngatlanRepository.getInstance().markAsViewed(url);
            }
            sendJsonResponse(exchange, 200, Map.of("success", true, "marked", marked, "url", url != null ? url : ""));
        }
    }

    private static class ClearHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String cityParam = null;
            if (query != null && query.contains("city=")) {
                for (String part : query.split("&")) {
                    if (part.startsWith("city=")) {
                        cityParam = URLDecoder.decode(part.substring(5), StandardCharsets.UTF_8);
                        break;
                    }
                }
            }

            if (cityParam == null || cityParam.isBlank()) {
                String body = readBody(exchange);
                if (body != null && !body.isBlank()) {
                    try {
                        Map<?, ?> map = GSON.fromJson(body, Map.class);
                        if (map != null && map.get("city") != null) {
                            cityParam = map.get("city").toString();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            if (cityParam != null && !cityParam.isBlank()) {
                IngatlanRepository.getInstance().clearCityHistory(cityParam);
                sendJsonResponse(exchange, 200, Map.of("success", true, "clearedCity", cityParam));
            } else {
                IngatlanRepository.getInstance().clearHistory();
                sendJsonResponse(exchange, 200, Map.of("success", true, "clearedAll", true));
            }
        }
    }

    private static class LogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            int limit = 100;
            String levelFilter = null;
            String query = exchange.getRequestURI().getQuery();
            if (query != null && !query.isBlank()) {
                for (String part : query.split("&")) {
                    if (part.startsWith("limit=")) {
                        try {
                            limit = Integer.parseInt(part.substring(6));
                        } catch (NumberFormatException ignored) {
                        }
                    } else if (part.startsWith("level=")) {
                        levelFilter = URLDecoder.decode(part.substring(6), StandardCharsets.UTF_8);
                    }
                }
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("logs", AppLogger.getRecentLogEntries(limit, levelFilter));
            data.put("rawLogs", AppLogger.getRecentLogs(limit));
            data.put("logFilePath", AppLogger.getLogFilePath() != null ? AppLogger.getLogFilePath().toString() : "");
            data.put("level", AppLogger.getGlobalLevel().name());
            sendJsonResponse(exchange, 200, data);
        }
    }

    private static class LogsClearHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            AppLogger.clearRecentLogs();
            sendJsonResponse(exchange, 200, Map.of("success", true, "message", "Naplók sikeresen kiürítve"));
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            is.transferTo(baos);
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        setSecurityHeaders(exchange);
        String json = GSON.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        setSecurityHeaders(exchange);
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
