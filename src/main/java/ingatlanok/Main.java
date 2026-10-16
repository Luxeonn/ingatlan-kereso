package ingatlanok;

import ingatlanok.common.BrowserUtils;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.service.IngatlanSearchService;
import ingatlanok.web.WebServer;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        boolean isCli = Arrays.asList(args).contains("--cli") || Boolean.getBoolean("cli");

        if (isCli) {
            runCliMode();
        } else {
            IngatlanApplication.main(args);
        }
    }

    private static void runWebDashboard() {
        System.out.println("==================================================");
        System.out.println("  Ingatlan Kereső - Web Dashboard Indítása");
        System.out.println("==================================================");

        IngatlanRepository.getInstance();

        String host = WebServer.resolveDefaultHost();
        int defaultPort = WebServer.resolveDefaultPort();
        WebServer server = new WebServer(host, defaultPort);

        try {
            server.start();
            int actualPort = server.getPort();
            String displayHost = ("0.0.0.0".equals(host) || "::".equals(host)) ? "localhost" : host;
            String url = "http://" + displayHost + ":" + actualPort;

            System.out.println("\n[Siker] A felület elérhető a következő címen: " + url);

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

            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Nem sikerült elindítani a webszervert: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static void runCliMode() {
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
