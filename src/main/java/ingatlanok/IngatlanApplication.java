package ingatlanok;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@Slf4j
@SpringBootApplication
@EnableCaching
@RequiredArgsConstructor
public class IngatlanApplication {

  private final Environment environment;

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

    log.info("Ingatlan Kereső (Spring Boot 4.1.2) Aktív! Headless REST API: {}/api/status", url);
  }
}
