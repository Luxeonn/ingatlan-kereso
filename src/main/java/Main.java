import ingatlanok.helper.IngatlanHelper;
import ingatlanok.service.impl.DunaHause;
import ingatlanok.service.impl.IngatlanBazar;
import ingatlanok.service.impl.IngatlanCom;
import ingatlanok.service.impl.JofogasIngatlan;
import ingatlanok.service.impl.OtthonCentrum;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Ingatlan kereső fő belépési pont.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  Ingatlan Kereső - Debreceni Eladó Lakások Scraper");
        System.out.println("==================================================");

        List<String> collection = new LinkedList<>();
        Set<String> urlList = new LinkedHashSet<>();

        // Adatbázis/történet inicializálás
        IngatlanHelper.startContent();

        System.out.println("\n[1/5] Duna House lekérdezése...");
        urlList.addAll(new DunaHause().getUrls());

        System.out.println("\n[2/5] Otthon Centrum lekérdezése...");
        urlList.addAll(new OtthonCentrum().getUrls());

        System.out.println("\n[3/5] Jófogás Ingatlan lekérdezése...");
        urlList.addAll(new JofogasIngatlan().getUrls());

        System.out.println("\n[4/5] Ingatlan.com lekérdezése...");
        urlList.addAll(new IngatlanCom().getUrls());

        System.out.println("\n[5/5] Ingatlanbazár lekérdezése...");
        urlList.addAll(new IngatlanBazar().getUrls());

        System.out.println("\n==================================================");
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.println("Lefutva: " + timestamp);
        System.out.println("Összesített ÚJ URL száma: " + urlList.size());
        System.out.println("==================================================");

        collection.add("======================================");
        collection.add("Elindítva : " + timestamp);
        collection.add("Új URL Száma : " + urlList.size());

        if (!urlList.isEmpty()) {
            collection.addAll(IngatlanHelper.openUrl(urlList));
        } else {
            System.out.println("Nem található új ingatlanhirdetés az előző futtatás óta.");
        }

        IngatlanHelper.saveIngatlanToFile(collection);
        System.out.println("\nFolyamat sikeresen befejeződött.");
    }

}
