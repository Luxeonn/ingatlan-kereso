package ingatlanok.web;

import com.sun.net.httpserver.HttpServer;
import ingatlanok.common.AppLogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * Beágyazott webszerver homlokzat (facade).
 * Megőrzi a publikus életciklus API-t a meglévő tesztek és kliensek számára,
 * miközben a kérések kiszolgálását a Spring MVC RestApiController és
 * a LegacyHttpHandler adapter végzi.
 */
public class WebServer {

    private static final AppLogger LOGGER = AppLogger.getLogger(WebServer.class);
    private HttpServer server;
    private String host;
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
        for (String val : List.of(Objects.toString(System.getenv("SERVER_PORT"), ""),
                                  Objects.toString(System.getProperty("server.port"), ""))) {
            try {
                if (!val.isBlank()) {
                    return Integer.parseInt(val.trim());
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return 8080;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isRunning() {
        return server != null;
    }

    public void start() throws IOException {
        int initialPort = port;
        int attempts = 0;
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
        LegacyHttpHandler.register(server, port);
        server.start();

        LOGGER.info("==================================================");
        LOGGER.info("  Web Dashboard elindult: http://localhost:{}", port);
        LOGGER.info("==================================================");
    }

    public void stop() {
        if (server != null) {
            LOGGER.info("Web Dashboard leállítása...");
            server.stop(0);
            server = null;
        }
    }
}
