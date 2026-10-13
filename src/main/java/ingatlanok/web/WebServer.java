package ingatlanok.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import ingatlanok.common.BrowserUtils;
import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.presentation.DashboardPresentationService;
import ingatlanok.presentation.DashboardResponseDto;
import ingatlanok.service.IngatlanSearchService;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public class WebServer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private HttpServer server;
    private int port = 8080;

    public WebServer(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void start() throws IOException {

        int attempts = 0;
        while (attempts < 5) {
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
                break;
            } catch (IOException e) {
                port++;
                attempts++;
            }
        }

        if (server == null) {
            throw new IOException("Nem sikerült elindítani a webszervert a portokon: 8080-" + port);
        }

        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/", new RootHandler(port));
        server.createContext("/api/status", new StatusHandler(port));
        server.createContext("/api/config", new ConfigHandler());
        server.createContext("/api/cities", new CitiesHandler());
        server.createContext("/api/history", new HistoryHandler());
        server.createContext("/api/search", new SearchHandler());
        server.createContext("/api/open-urls", new OpenUrlsHandler());
        server.createContext("/api/items/viewed", new MarkViewedHandler());
        server.createContext("/api/history/clear", new ClearHistoryHandler());

        server.start();
        System.out.println("==================================================");
        System.out.println("  Web Dashboard elindult: http://localhost:" + port);
        System.out.println("==================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static class RootHandler implements HttpHandler {
        private final int port;

        public RootHandler(int port) {
            this.port = port;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
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
            Map<String, Object> status = new LinkedHashMap<>();
            status.put("status", "UP");
            status.put("port", port);
            status.put("totalInDb", IngatlanRepository.getInstance().getTotalCount());
            status.put("lastRunTime", IngatlanRepository.getInstance().getLastRunTime());
            status.put("trackedCities", IngatlanRepository.getInstance().getTrackedCitiesInfo());
            sendJsonResponse(exchange, 200, status);
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

            List<IngatlanItem> items;
            if (cityParam != null && !cityParam.isBlank()) {
                items = IngatlanRepository.getInstance().getItemsByCity(cityParam);
            } else {
                items = IngatlanRepository.getInstance().getAll();
            }
            sendJsonResponse(exchange, 200, items);
        }
    }

    private static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String body = readBody(exchange);
            SearchCriteria criteria = GSON.fromJson(body, SearchCriteria.class);
            if (criteria == null) {
                criteria = new SearchCriteria();
            }

            DashboardResponseDto result = DashboardPresentationService.getInstance().executeSearch(criteria);
            sendJsonResponse(exchange, 200, result);
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
                BrowserUtils.openUrls(new LinkedHashSet<>(urls));
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

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            is.transferTo(baos);
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
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
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
