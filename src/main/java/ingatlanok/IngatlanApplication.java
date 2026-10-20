package ingatlanok;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import ingatlanok.common.BrowserUtils;

@SpringBootApplication
public class IngatlanApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(IngatlanApplication.class);

  private final Environment environment;

  public IngatlanApplication(Environment environment) {
    this.environment = environment;
  }

  public static void main(String[] args) {
    boolean isCli = java.util.Arrays.asList(args).contains("--cli") || Boolean.getBoolean("cli");
    if (isCli) {
      System.setProperty("cli", "true");
    }
    SpringApplication application = new SpringApplication(IngatlanApplication.class);
    if (isCli) {
      application.setWebApplicationType(WebApplicationType.NONE);
    }
    ConfigurableApplicationContext context = application.run(args);
    if (isCli) {
      System.exit(SpringApplication.exit(context));
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    String port = environment.getProperty("server.port", "8080");
    String host = environment.getProperty("server.address", "127.0.0.1");
    String displayHost = ("0.0.0.0".equals(host) || "::".equals(host)) ? "localhost" : host;
    String url = "http://" + displayHost + ":" + port;

    LOGGER.info("Ingatlan Kereső (Spring Boot 4.1.1) Aktív! Headless REST API: {}/api/status", url);

    boolean openBrowser =
        Boolean.getBoolean("open.browser")
            || "true".equalsIgnoreCase(System.getenv("OPEN_BROWSER"));

    if (openBrowser && !java.awt.GraphicsEnvironment.isHeadless() && !"0.0.0.0".equals(host)) {
      Thread.ofVirtual()
          .start(
              () -> {
                if (!BrowserUtils.openUrl(url + "/api/status")) {
                  LOGGER.warn("Nyisd meg manuálisan a böngésződben: {}/api/status", url);
                }
              });
    }
  }
}
