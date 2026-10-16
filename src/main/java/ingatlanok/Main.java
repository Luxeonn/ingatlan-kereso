package ingatlanok;

import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.service.IngatlanSearchService;
import org.springframework.boot.SpringApplication;

import java.util.Arrays;

/**
 * Ingatlan kereső belépési pont és futtatási módválasztó.
 * Támogatja a parancssori kötegelt futtatást (--cli) és a modern Spring Boot Web Dashboardot (--web vagy alapértelmezett).
 */
public class Main {

    public static void main(String[] args) {
        if (isCliRequested(args)) {
            runCliMode();
        } else {
            SpringApplication.run(IngatlanApplication.class, filterLauncherArgs(args));
        }
    }

    /**
     * Megvizsgálja, hogy a parancssori (CLI) kötegelt mód futtatását kérték-e.
     *
     * @param args parancssori argumentumok
     * @return true, ha a --cli kapcsoló vagy a cli rendszerjellemző be van állítva
     */
    public static boolean isCliRequested(String[] args) {
        if (args != null) {
            for (String arg : args) {
                if ("--cli".equalsIgnoreCase(arg)) {
                    return true;
                }
            }
        }
        return Boolean.getBoolean("cli");
    }

    /**
     * Kiszűri a futtató kapcsolókat (--web, --cli), hogy ne szennyezzék a Spring környezeti beállításait.
     *
     * @param args az eredeti argumentum tömb
     * @return szűrt argumentum tömb a Spring Boot számára
     */
    public static String[] filterLauncherArgs(String[] args) {
        if (args == null) {
            return new String[0];
        }
        return Arrays.stream(args)
                .filter(arg -> !"--web".equalsIgnoreCase(arg) && !"--cli".equalsIgnoreCase(arg))
                .toArray(String[]::new);
    }

    /**
     * Parancssori (CLI) kötegelt keresési mód futtatása.
     */
    public static void runCliMode() {
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
