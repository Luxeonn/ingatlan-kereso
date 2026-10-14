package ingatlanok.web.controller;

import ingatlanok.web.WebDashboardHtml;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller serving the Single Page Application (SPA) dashboard.
 */
@Controller
public class DashboardController {

    private final ServletWebServerApplicationContext webServerAppCtxt;

    @Value("${server.port:8080}")
    private int configuredPort;

    public DashboardController(ServletWebServerApplicationContext webServerAppCtxt) {
        this.webServerAppCtxt = webServerAppCtxt;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> getDashboard() {
        int port = (webServerAppCtxt != null && webServerAppCtxt.getWebServer() != null)
                ? webServerAppCtxt.getWebServer().getPort()
                : configuredPort;
        String html = WebDashboardHtml.getHtml(port);
        return ResponseEntity.ok(html);
    }
}
