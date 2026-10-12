package ingatlanok.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import ingatlanok.helper.IngatlanHelper;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.service.IngatlanSearchService;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Beágyazott könnyűsúlyú HTTP szerver a Webes Dashboard kiszolgálásához.
 */
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
        // Port foglaltság esetén automatikus próbálkozás
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

        // Java 21 Virtual Threads a villámgyors és hatékony kérések kezeléséhez
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // Útvonalak beállítása
        server.createContext("/", new RootHandler(port));
        server.createContext("/api/status", new StatusHandler(port));
        server.createContext("/api/config", new ConfigHandler());
        server.createContext("/api/history", new HistoryHandler());
        server.createContext("/api/search", new SearchHandler());
        server.createContext("/api/open-urls", new OpenUrlsHandler());
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

    // Handlers
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
            sendJsonResponse(exchange, 200, status);
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
            List<IngatlanItem> items = IngatlanRepository.getInstance().getAll();
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

            SearchResult result = IngatlanSearchService.getInstance().executeSearch(criteria);
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
                IngatlanHelper.openUrl(new LinkedHashSet<>(urls));
            }
            sendJsonResponse(exchange, 200, Map.of("success", true, "count", urls != null ? urls.size() : 0));
        }
    }

    private static class ClearHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            IngatlanRepository.getInstance().clearHistory();
            sendJsonResponse(exchange, 200, Map.of("success", true));
        }
    }

    // Utilities
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
