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
import ingatlanok.dto.ListingDto;
import ingatlanok.dto.ListingFilterDto;
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

        server.createContext("/", new StaticFileHandler(port));
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
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' data: https://fonts.gstatic.com; img-src 'self' data: https:; connect-src 'self' http://localhost:* http://127.0.0.1:*;");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept");
    }

    private static boolean handleCorsPreflight(HttpExchange exchange) throws IOException {
        setSecurityHeaders(exchange);
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static class StaticFileHandler implements HttpHandler {
        private final int port;

        public StaticFileHandler(int port) {
            this.port = port;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setSecurityHeaders(exchange);

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET") && !exchange.getRequestMethod().equalsIgnoreCase("HEAD")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String rawPath = exchange.getRequestURI().getPath();
            if (rawPath == null || rawPath.isBlank() || rawPath.equals("/")) {
                rawPath = "/index.html";
            }

            // Path normalization and Path Traversal attack prevention
            if (rawPath.contains("..") || rawPath.contains("\\") || rawPath.contains("//")) {
                sendResponse(exchange, 400, "Bad Request");
                return;
            }

            String resourcePath = "static" + rawPath;
            InputStream rawStream = WebServer.class.getClassLoader().getResourceAsStream(resourcePath);

            // SPA Routing Fallback: ha a kért útvonal nem közvetlen asset fájl kiterjesztéssel, akkor index.html
            if (rawStream == null) {
                boolean isStaticAsset = rawPath.contains(".") && !rawPath.endsWith(".html");
                if (!isStaticAsset) {
                    resourcePath = "static/index.html";
                    rawStream = WebServer.class.getClassLoader().getResourceAsStream(resourcePath);
                }
            }

            if (rawStream == null) {
                serveDevPlaceholder(exchange, port);
                return;
            }

            try (InputStream in = rawStream) {
                byte[] bytes = in.readAllBytes();
                String mimeType = getMimeType(resourcePath);
                exchange.getResponseHeaders().set("Content-Type", mimeType);

                if (resourcePath.startsWith("static/assets/")) {
                    exchange.getResponseHeaders().set("Cache-Control", "public, max-age=31536000, immutable");
                } else {
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
                }

                exchange.sendResponseHeaders(200, bytes.length);
                if (!exchange.getRequestMethod().equalsIgnoreCase("HEAD")) {
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                }
            }
        }

        private static String getMimeType(String path) {
            String lower = path.toLowerCase();
            if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "text/javascript; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".ico")) return "image/x-icon";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".webp")) return "image/webp";
            if (lower.endsWith(".woff2")) return "font/woff2";
            if (lower.endsWith(".woff")) return "font/woff";
            if (lower.endsWith(".ttf")) return "font/ttf";
            return "application/octet-stream";
        }

        private static void serveDevPlaceholder(HttpExchange exchange, int port) throws IOException {
            String html = """
                <!DOCTYPE html>
                <html lang="hu">
                <head>
                    <meta charset="UTF-8">
                    <title>Ingatlan Kereső - Fejlesztői Mód</title>
                    <style>
                        body { font-family: system-ui, sans-serif; background: #0b0f19; color: #f8fafc; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; }
                        .card { background: #131b2e; border: 1px solid #263352; border-radius: 16px; padding: 32px; max-width: 600px; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.5); }
                        h1 { color: #3b82f6; margin-top: 0; font-size: 22px; }
                        code { background: #1a2238; padding: 3px 8px; border-radius: 6px; color: #38bdf8; font-family: monospace; }
                        ol { padding-left: 20px; line-height: 1.8; color: #94a3b8; font-size: 14px; }
                        .btn { display: inline-block; background: #2563eb; color: #fff; text-decoration: none; padding: 8px 16px; border-radius: 8px; font-weight: bold; font-size: 13px; margin-top: 10px; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>🚀 Ingatlan Kereső Backend (Port: %d)</h1>
                        <p>A Java REST API sikeresen fut, de a statikus React SPA build még nem található a <code>src/main/resources/static/</code> mappában.</p>
                        <h3>Kétféleképpen indíthatod el a felületet:</h3>
                        <ol>
                            <li><strong>Fejlesztői mód azonnali HMR-rel (Ajánlott):</strong><br>
                                Futtasd: <code>cd frontend && npm install && npm run dev</code><br>
                                Majd nyisd meg: <a class="btn" href="http://localhost:5173" target="_blank">http://localhost:5173</a>
                            </li>
                            <li style="margin-top: 10px;"><strong>Produkciós statikus csomagolás:</strong><br>
                                Futtasd: <code>cd frontend && npm run build</code> és másold a <code>dist/</code> tartalmát a <code>src/main/resources/static/</code> mappába.
                            </li>
                        </ol>
                    </div>
                </body>
                </html>
                """.formatted(port);

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
            if (handleCorsPreflight(exchange)) return;
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
            if (handleCorsPreflight(exchange)) return;
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
            if (handleCorsPreflight(exchange)) return;
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
            if (handleCorsPreflight(exchange)) return;
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
            if (handleCorsPreflight(exchange)) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String cityParam = null;
            boolean onlyDiscounted = false;
            String portalParam = null;
            Double minPrice = null;
            Double maxPrice = null;
            Double minArea = null;
            Double maxArea = null;
            Double minRooms = null;
            Double maxRooms = null;
            boolean excludeViewed = false;
            String sortBy = "DISCOVERED_DESC";
            Integer limit = null;
            Integer offset = null;

            if (query != null) {
                for (String part : query.split("&")) {
                    String[] kv = part.split("=", 2);
                    if (kv.length == 2) {
                        String key = kv[0];
                        String val = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        switch (key) {
                            case "city" -> cityParam = val;
                            case "onlyDiscounted" -> onlyDiscounted = Boolean.parseBoolean(val);
                            case "portal" -> portalParam = val;
                            case "minPrice" -> { try { minPrice = Double.parseDouble(val); } catch (Exception ignored) {} }
                            case "maxPrice" -> { try { maxPrice = Double.parseDouble(val); } catch (Exception ignored) {} }
                            case "minArea" -> { try { minArea = Double.parseDouble(val); } catch (Exception ignored) {} }
                            case "maxArea" -> { try { maxArea = Double.parseDouble(val); } catch (Exception ignored) {} }
                            case "minRooms" -> { try { minRooms = Double.parseDouble(val); } catch (Exception ignored) {} }
                            case "maxRooms" -> { try { maxRooms = Double.parseDouble(val); } catch (Exception ignored) {} }
                            case "excludeViewed" -> excludeViewed = Boolean.parseBoolean(val);
                            case "sortBy" -> sortBy = val;
                            case "limit" -> { try { limit = Integer.parseInt(val); } catch (Exception ignored) {} }
                            case "offset" -> { try { offset = Integer.parseInt(val); } catch (Exception ignored) {} }
                        }
                    }
                }
            }

            List<String> cities = (cityParam != null && !cityParam.isBlank()) ? List.of(cityParam) : null;
            List<String> portals = (portalParam != null && !portalParam.isBlank()) ? List.of(portalParam) : null;

            ListingFilterDto filter = new ListingFilterDto(
                    cities, portals, minPrice, maxPrice, minArea, maxArea, minRooms, maxRooms,
                    excludeViewed, onlyDiscounted, sortBy, limit, offset
            );

            List<ListingDto> dtoList = IngatlanRepository.getInstance().findByCriteria(filter);
            sendJsonResponse(exchange, 200, dtoList);
        }
    }

    private static class PropertiesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsPreflight(exchange)) return;
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
            if (handleCorsPreflight(exchange)) return;
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
            if (handleCorsPreflight(exchange)) return;
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
            if (handleCorsPreflight(exchange)) return;
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
            if (handleCorsPreflight(exchange)) return;
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
            if (handleCorsPreflight(exchange)) return;
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
            if (handleCorsPreflight(exchange)) return;
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
