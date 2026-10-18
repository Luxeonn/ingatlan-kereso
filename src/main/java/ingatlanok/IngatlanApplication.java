package ingatlanok;

import ingatlanok.common.BrowserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * Spring Boot 4.1.0 (Spring Framework 7.0.8) Fő Alkalmazásbelépési pont GraalVM Native Image támogatással.
 */
@SpringBootApplication
public class IngatlanApplication {

    private static final Logger log = LoggerFactory.getLogger(IngatlanApplication.class);

    private final Environment environment;

    public IngatlanApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        boolean isCli = Arrays.asList(args).contains("--cli") || Boolean.getBoolean("cli");
        if (isCli) {
            Main.runCliMode();
        } else {
            SpringApplication.run(IngatlanApplication.class, args);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = environment.getProperty("server.port", "8080");
        String host = environment.getProperty("server.address", "127.0.0.1");
        String displayHost = ("0.0.0.0".equals(host) || "::".equals(host)) ? "localhost" : host;
        String url = "http://" + displayHost + ":" + port;

        log.info("Ingatlan Kereső (Spring Boot 4.1.0) Aktív! Headless REST API: {}/api/status", url);

        boolean openBrowser = Boolean.getBoolean("open.browser")
                || "true".equalsIgnoreCase(System.getenv("OPEN_BROWSER"));

        if (openBrowser && !java.awt.GraphicsEnvironment.isHeadless() && !"0.0.0.0".equals(host)) {
            Thread.ofVirtual().start(() -> {
                if (!BrowserUtils.openUrl(url + "/api/status")) {
                    log.warn("Nyisd meg manuálisan a böngésződben: {}/api/status", url);
                }
            });
        }
    }
}
