package ingatlanok.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ingatlanok.common.AppLogger;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.presentation.DashboardPresentationService;
import org.springframework.http.ResponseEntity;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class LegacyHttpHandler {

    private static final AppLogger LOGGER = AppLogger.getLogger(LegacyHttpHandler.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final RestApiController REST_CONTROLLER = new RestApiController(
            IngatlanRepository.getInstance(),
            DashboardPresentationService.getInstance(),
            PortalEngineRegistry.getInstance()
    );

    static void register(HttpServer server, int port) {
        server.createContext("/", exchange -> handleStatic(exchange, port));
        server.createContext("/api/status", exchange -> handleStatus(exchange, port));
        server.createContext("/api/portals", LegacyHttpHandler::handlePortals);
        server.createContext("/api/cities", LegacyHttpHandler::handleCities);
        server.createContext("/api/config", LegacyHttpHandler::handleConfig);
        server.createContext("/api/history", LegacyHttpHandler::handleHistory);
        server.createContext("/api/properties", LegacyHttpHandler::handleProperties);
        server.createContext("/api/search", LegacyHttpHandler::handleSearch);
        server.createContext("/api/open-urls", LegacyHttpHandler::handleOpenUrls);
        server.createContext("/api/items/viewed", LegacyHttpHandler::handleMarkViewed);
        server.createContext("/api/history/clear", LegacyHttpHandler::handleClearHistory);
        server.createContext("/api/logs", LegacyHttpHandler::handleLogs);
        server.createContext("/api/logs/clear", LegacyHttpHandler::handleLogsClear);
    }

    private static void setSecurityHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Referrer-Policy", "strict-origin-when-cross-origin");
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; font-src 'self' data:; img-src 'self' data: https:; connect-src 'self' http://localhost:* http://127.0.0.1:*;");
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

    private static void handleStatus(HttpExchange exchange, int port) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>(REST_CONTROLLER.getStatus().getBody());
        body.put("port", port);
        sendJsonResponse(exchange, 200, body);
    }

    private static void handlePortals(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        sendJsonResponse(exchange, 200, REST_CONTROLLER.getPortals().getBody());
    }

    private static void handleCities(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        sendJsonResponse(exchange, 200, REST_CONTROLLER.getCities().getBody());
    }

    private static void handleConfig(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendJsonResponse(exchange, 200, REST_CONTROLLER.getLastCriteria().getBody());
        } else if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            String body = readBody(exchange);
            SearchCriteria sc = body.isBlank() ? new SearchCriteria() : GSON.fromJson(body, SearchCriteria.class);
            sendJsonResponse(exchange, 200, REST_CONTROLLER.saveLastCriteria(sc).getBody());
        } else {
            sendResponse(exchange, 405, "Method Not Allowed");
        }
    }

    private static void handleHistory(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        List<?> list = REST_CONTROLLER.getHistory(
                params.get("city"),
                Boolean.parseBoolean(params.get("onlyDiscounted")),
                params.get("portal"),
                parseDouble(params.get("minPrice")),
                parseDouble(params.get("maxPrice")),
                parseDouble(params.get("minArea")),
                parseDouble(params.get("maxArea")),
                parseDouble(params.get("minRooms")),
                parseDouble(params.get("maxRooms")),
                Boolean.parseBoolean(params.get("excludeViewed")),
                params.getOrDefault("sortBy", "DISCOVERED_DESC"),
                parseInt(params.get("limit")),
                parseInt(params.get("offset"))
        ).getBody();
        sendJsonResponse(exchange, 200, list);
    }

    private static void handleProperties(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String idOrUrl = params.get("id") != null ? params.get("id") : params.get("url");
        if (idOrUrl == null && path.startsWith("/api/properties/")) {
            String sub = path.substring("/api/properties/".length());
            int slash = sub.indexOf('/');
            if (slash > 0) {
                idOrUrl = sub.substring(0, slash);
            }
        }

        if (path.contains("/price-history")) {
            ResponseEntity<?> res = REST_CONTROLLER.getPriceHistory(null, idOrUrl, idOrUrl);
            sendJsonResponse(exchange, res.getStatusCode().value(), res.getBody());
        } else if (path.contains("/status-log")) {
            ResponseEntity<?> res = REST_CONTROLLER.getStatusLog(null, idOrUrl, idOrUrl);
            sendJsonResponse(exchange, res.getStatusCode().value(), res.getBody());
        } else if (path.endsWith("/discounted")) {
            sendJsonResponse(exchange, 200, REST_CONTROLLER.getDiscountedProperties(params.get("city")).getBody());
        } else {
            sendResponse(exchange, 404, "Not Found");
        }
    }

    private static void handleSearch(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        try {
            String body = readBody(exchange);
            SearchCriteria criteria = body.isBlank() ? new SearchCriteria() : GSON.fromJson(body, SearchCriteria.class);
            sendJsonResponse(exchange, 200, REST_CONTROLLER.executeSearch(criteria).getBody());
        } catch (Exception e) {
            LOGGER.error("Hiba a /api/search kérés feldolgozásakor: {}", e.getMessage(), e);
            sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private static void handleOpenUrls(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        List<String> urls = GSON.fromJson(readBody(exchange), new TypeToken<List<String>>() {}.getType());
        sendJsonResponse(exchange, 200, REST_CONTROLLER.openUrls(urls).getBody());
    }

    private static void handleMarkViewed(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        Map<String, Object> body = GSON.fromJson(readBody(exchange), Map.class);
        sendJsonResponse(exchange, 200, REST_CONTROLLER.markViewed(body).getBody());
    }

    private static void handleClearHistory(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
        Map<String, Object> body = null;
        String rawBody = readBody(exchange);
        if (!rawBody.isBlank()) {
            body = GSON.fromJson(rawBody, Map.class);
        }
        sendJsonResponse(exchange, 200, REST_CONTROLLER.clearHistory(body, query.get("city")).getBody());
    }

    private static void handleLogs(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        int limit = parseInt(params.get("limit")) != null ? parseInt(params.get("limit")) : 100;
        sendJsonResponse(exchange, 200, REST_CONTROLLER.getLogs(limit, params.get("level")).getBody());
    }

    private static void handleLogsClear(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        sendJsonResponse(exchange, 200, REST_CONTROLLER.clearLogs().getBody());
    }

    private static void handleStatic(HttpExchange exchange, int port) throws IOException {
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

        if (rawPath.contains("..") || rawPath.contains("\\") || rawPath.contains("//") || rawPath.contains("%")) {
            sendResponse(exchange, 400, "Bad Request");
            return;
        }

        String resourcePath;
        if (rawPath.startsWith("/assets/")) {
            String assetFile = rawPath.substring("/assets/".length());
            if (assetFile.isEmpty() || !assetFile.matches("^[a-zA-Z0-9_.-]+$") || assetFile.startsWith(".")) {
                sendResponse(exchange, 400, "Bad Request");
                return;
            }
            resourcePath = "static/assets/" + assetFile;
        } else if (rawPath.equals("/index.html") || rawPath.equals("/favicon.ico")) {
            resourcePath = "static" + rawPath;
        } else if (rawPath.contains(".")) {
            sendResponse(exchange, 404, "Not Found");
            return;
        } else {
            resourcePath = "static/index.html";
        }

        boolean acceptGzip = Optional.ofNullable(exchange.getRequestHeaders().getFirst("Accept-Encoding"))
                .map(s -> s.toLowerCase().contains("gzip")).orElse(false);

        InputStream rawStream = null;
        boolean isGzipped = false;
        if (acceptGzip) {
            rawStream = WebServer.class.getClassLoader().getResourceAsStream(resourcePath + ".gz");
            if (rawStream != null) isGzipped = true;
        }
        if (rawStream == null) {
            rawStream = WebServer.class.getClassLoader().getResourceAsStream(resourcePath);
        }

        if (rawStream == null) {
            if ("static/index.html".equals(resourcePath)) {
                serveDevPlaceholder(exchange, port);
            } else {
                sendResponse(exchange, 404, "Not Found");
            }
            return;
        }

        try (InputStream in = rawStream) {
            byte[] bytes = in.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", getMimeType(resourcePath));
            exchange.getResponseHeaders().set("Vary", "Accept-Encoding");
            if (isGzipped) {
                exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            }
            exchange.getResponseHeaders().set("Cache-Control", resourcePath.startsWith("static/assets/")
                    ? "public, max-age=31536000, immutable" : "no-cache, no-store, must-revalidate");

            exchange.sendResponseHeaders(200, bytes.length);
            if (!exchange.getRequestMethod().equalsIgnoreCase("HEAD")) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    private static void serveDevPlaceholder(HttpExchange exchange, int port) throws IOException {
        String html = "<!DOCTYPE html><html lang=\"hu\"><head><meta charset=\"UTF-8\"><title>Ingatlan Kereső</title></head><body><h1>Ingatlan Kereső (" + port + ")</h1><div id=\"root\"></div></body></html>";
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String getMimeType(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".gz")) p = p.substring(0, p.length() - 3);
        if (p.endsWith(".html")) return "text/html; charset=UTF-8";
        if (p.endsWith(".js") || p.endsWith(".mjs")) return "text/javascript; charset=UTF-8";
        if (p.endsWith(".css")) return "text/css; charset=UTF-8";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".json")) return "application/json; charset=UTF-8";
        if (p.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> result = new LinkedHashMap<>();
        if (query == null || query.isBlank()) return result;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                result.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return result;
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
        byte[] bytes = GSON.toJson(data).getBytes(StandardCharsets.UTF_8);
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

    private static Double parseDouble(String val) {
        try {
            return val != null ? Double.parseDouble(val) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer parseInt(String val) {
        try {
            return val != null ? Integer.parseInt(val) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
