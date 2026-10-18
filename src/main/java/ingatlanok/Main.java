package ingatlanok;

import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.service.IngatlanSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;

import java.util.Arrays;

/**
 * Ingatlan kereső belépési pont és futtatási módválasztó.
 * Támogatja a parancssori kötegelt futtatást (--cli) és a modern Spring Boot Web Dashboardot (--web vagy alapértelmezett).
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

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
        log.info("==================================================");
        log.info("  Ingatlan Kereső - Parancssori Keresés (CLI Mód)");
        log.info("==================================================");

        IngatlanRepository.getInstance();
        SearchCriteria criteria = IngatlanRepository.getInstance().getLastCriteria();
        criteria.setAutoOpenBrowser(true);

        SearchResult result = IngatlanSearchService.getInstance().executeSearch(criteria);

        log.info("==================================================");
        log.info("Lefutva: {}", result.getExecutedAt());
        log.info("Összesített találat: {}", result.getTotalFound());
        log.info("ÚJ hirdetések száma: {}", result.getNewCount());
        log.info("==================================================");
        log.info("Folyamat sikeresen befejeződött.");
    }
}
