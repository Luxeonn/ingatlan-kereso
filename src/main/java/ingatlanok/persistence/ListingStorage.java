package ingatlanok.persistence;

import ingatlanok.model.IngatlanItem;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ingatlanhirdetések perzisztenciájáért és állapotkezeléséért felelős interfész.
 */
public interface ListingStorage {

    /**
     * Adott városhoz tartozó hirdetések elmentése vagy frissítése.
     */
    void saveCityItems(String city, Collection<IngatlanItem> items);

    /**
     * Több hirdetés csoportos mentése városonkénti bontásban.
     */
    void saveAll(Collection<IngatlanItem> items);

    /**
     * Adott városhoz tartozó hirdetések lekérése.
     */
    List<IngatlanItem> getItemsByCity(String city);

    /**
     * Az összes tárolt hirdetés lekérése felfedezési dátum szerint rendezve.
     */
    List<IngatlanItem> getAll();

    /**
     * Ellenőrzi, hogy egy adott településhez létezik-e már korábban mentett adatbázis.
     */
    boolean hasCityDatabase(String city);

    /**
     * Ellenőrzi, hogy egy adott URL új találatnak minősül-e a megadott településen.
     */
    boolean isNewUrl(String city, String url);

    /**
     * Ellenőrzi, hogy egy adott URL szerepel-e a globális adatbázisban.
     */
    boolean containsUrl(String url);

    /**
     * Megjelöl egy hirdetést megtekintettként.
     */
    boolean markAsViewed(String url);

    /**
     * Lekérdezi egy adott hirdetés megtekintettségi állapotát.
     */
    boolean isViewed(String url);

    /**
     * Visszaadja az összes ismert URL halmazát.
     */
    Set<String> getAllUrls();

    /**
     * Visszaadja a nyilvántartott városok statisztikai adatait.
     */
    List<Map<String, Object>> getTrackedCitiesInfo();

    /**
     * Törli egy adott város keresési előzményeit és adatbázisát.
     */
    void clearCityHistory(String city);

    /**
     * Törli a teljes hirdetésadatbázist és az összes város előzményeit.
     */
    void clearHistory();

    /**
     * Visszaadja a tárolt hirdetések teljes darabszámát.
     */
    int getTotalCount();

    /**
     * Visszaadja a tárolt hirdetések számát forrásportálonként csoportosítva.
     */
    default Map<String, Long> countByPortal() {
        return Map.of();
    }

    /**
     * Visszaadja a legutóbbi keresési futás időbélyegét.
     */
    String getLastRunTime();
}
