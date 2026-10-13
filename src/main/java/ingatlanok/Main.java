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
            runWebDashboard();
        }
    }

    private static void runWebDashboard() {
        System.out.println("==================================================");
        System.out.println("  Ingatlan Kereső - Web Dashboard Indítása");
        System.out.println("==================================================");

        IngatlanRepository.getInstance();

        int defaultPort = Integer.getInteger("server.port", 8080);
        WebServer server = new WebServer(defaultPort);

        try {
            server.start();
            int actualPort = server.getPort();
            String url = "http://localhost:" + actualPort;

            System.out.println("\n[Siker] A felület elérhető a következő címen: " + url);
            System.out.println("Böngésző megnyitása folyamatban...");

            if (!BrowserUtils.openUrl(url)) {
                System.out.println("Nyisd meg manuálisan a böngésződben a fenti linket: " + url);
            }

            System.out.println("\nA szerver a háttérben fut. Nyomj Ctrl+C billentyűkombinációt a leállításhoz.");

            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Nem sikerült elindítani a webszervert: " + e.getMessage());
            e.printStackTrace();
        }
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
