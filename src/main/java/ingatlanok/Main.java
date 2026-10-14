package ingatlanok;

import ingatlanok.common.BrowserUtils;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.service.IngatlanSearchService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * Belépési pont: Spring Boot 3.4 Web Dashboard szerver és CLI mód vezérlő.
 */
@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        boolean isCli = Arrays.asList(args).contains("--cli") || Boolean.getBoolean("cli");

        if (isCli) {
            runCliMode();
        } else {
            runWebDashboard(args);
        }
    }

    private static void runWebDashboard(String[] args) {
        System.out.println("==================================================");
        System.out.println("  Ingatlan Kereső - Spring Boot Web Indítása");
        System.out.println("==================================================");

        IngatlanRepository.getInstance();

        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        Environment env = context.getEnvironment();

        String port = env.getProperty("local.server.port", env.getProperty("server.port", "8080"));
        String host = env.getProperty("server.address", "127.0.0.1");
        String displayHost = ("0.0.0.0".equals(host) || "::".equals(host)) ? "localhost" : host;
        String url = "http://" + displayHost + ":" + port;

        System.out.println("\n[Siker] A Spring Boot felület elérhető a következő címen: " + url);

        boolean noBrowser = Boolean.getBoolean("no.browser")
                || "true".equalsIgnoreCase(System.getenv("NO_BROWSER"))
                || java.awt.GraphicsEnvironment.isHeadless()
                || "0.0.0.0".equals(host);

        if (!noBrowser) {
            System.out.println("Böngésző megnyitása a háttérben...");
            Thread.ofVirtual().start(() -> {
                if (!BrowserUtils.openUrl(url)) {
                    System.out.println("Nyisd meg manuálisan a böngésződben a fenti linket: " + url);
                }
            });
        } else {
            System.out.println("[Info] Headless / Konténer környezet. Nyisd meg a böngésződben: " + url);
        }

        System.out.println("\n==================================================");
        System.out.println("  Web Dashboard Kiszolgáló Aktív!");
        System.out.println("  Nyisd meg: " + url);
        System.out.println("  Leállítás: [Ctrl + C]");
        System.out.println("==================================================");
    }

    private static void runCliMode() {
        System.out.println("==================================================");
        System.out.println("  Ingatlan Kereső - Parancssori Keresés (CLI Mód)");
        System.out.println("==================================================");

        IngatlanRepository.getInstance();
        SearchCriteria criteria = IngatlanRepository.getInstance().getLastCriteria();
        criteria.setAutoOpenBrowser(true);

        SearchResult result = IngatlanSearchService.getInstance().executeSearch(criteria);

        System.out.println("\n==================================================");
        System.out.println("Lefutva: " + result.getExecutedAt());
        System.out.println("Összesített találat: " + result.getTotalFound());
        System.out.println("ÚJ hirdetések száma: " + result.getNewCount());
        System.out.println("==================================================");
        System.out.println("Folyamat sikeresen befejeződött.");
    }
}
