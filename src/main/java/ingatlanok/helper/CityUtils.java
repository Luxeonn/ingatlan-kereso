package ingatlanok.helper;

import java.text.Normalizer;
import java.util.*;

/**
 * Településnevek normalizálását, előre definiált településlistát és
 * gyorsválasztó sablonokat biztosító segédosztály.
 */
public class CityUtils {

    /**
     * Normalizálja a megadott településnevet fájlnév- és URL-biztos azonosítóvá.
     * Pl.: "Hajdúszoboszló" -> "hajduszoboszlo", "Debrecen - Józsa" -> "debrecen-jozsa".
     */
    public static String normalizeCityKey(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return "debrecen";
        }
        String normalized = Normalizer.normalize(cityName.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        normalized = normalized.replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("^-+|-+$", "");
        return normalized.isEmpty() ? "debrecen" : normalized;
    }

    /**
     * Előre definiált magyarországi települések listája a frontend autocomplete / dropdown menühöz.
     */
    public static List<Map<String, String>> getPredefinedCities() {
        List<Map<String, String>> list = new ArrayList<>();

        // Debrecen és agglomerációja
        addCity(list, "Debrecen", "Hajdú-Bihar", "Agglomeráció / Székhely");
        addCity(list, "Hajdúszoboszló", "Hajdú-Bihar", "Debrecen környéke");
        addCity(list, "Hajdúböszörmény", "Hajdú-Bihar", "Debrecen környéke");
        addCity(list, "Balmazújváros", "Hajdú-Bihar", "Debrecen környéke");
        addCity(list, "Berettyóújfalu", "Hajdú-Bihar", "Debrecen környéke");
        addCity(list, "Derecske", "Hajdú-Bihar", "Debrecen környéke");
        addCity(list, "Ebes", "Hajdú-Bihar", "Debrecen agglomeráció");
        addCity(list, "Mikepércs", "Hajdú-Bihar", "Debrecen agglomeráció");
        addCity(list, "Bocskaikert", "Hajdú-Bihar", "Debrecen agglomeráció");
        addCity(list, "Józsa", "Hajdú-Bihar", "Debrecen agglomeráció");
        addCity(list, "Téglás", "Hajdú-Bihar", "Debrecen agglomeráció");
        addCity(list, "Hajdúhadház", "Hajdú-Bihar", "Debrecen agglomeráció");
        addCity(list, "Nyíradony", "Hajdú-Bihar", "Debrecen környéke");
        addCity(list, "Vámospércs", "Hajdú-Bihar", "Debrecen környéke");
        addCity(list, "Sáránd", "Hajdú-Bihar", "Debrecen agglomeráció");
        addCity(list, "Püspökladány", "Hajdú-Bihar", "Hajdú-Bihar");
        addCity(list, "Kaba", "Hajdú-Bihar", "Hajdú-Bihar");

        // Nagyvárosok és egyéb megyeszékhelyek
        addCity(list, "Budapest", "Pest", "Főváros");
        addCity(list, "Nyíregyháza", "Szabolcs-Szatmár-Bereg", "Megyeszékhely");
        addCity(list, "Miskolc", "Borsod-Abaúj-Zemplén", "Megyeszékhely");
        addCity(list, "Szeged", "Csongrád-Csanád", "Megyeszékhely");
        addCity(list, "Pécs", "Baranya", "Megyeszékhely");
        addCity(list, "Győr", "Győr-Moson-Sopron", "Megyeszékhely");
        addCity(list, "Kecskemét", "Bács-Kiskun", "Megyeszékhely");
        addCity(list, "Székesfehérvár", "Fejér", "Megyeszékhely");
        addCity(list, "Szombathely", "Vas", "Megyeszékhely");
        addCity(list, "Szolnok", "Jász-Nagykun-Szolnok", "Megyeszékhely");
        addCity(list, "Eger", "Heves", "Megyeszékhely");
        addCity(list, "Veszprém", "Veszprém", "Megyeszékhely");
        addCity(list, "Békéscsaba", "Békés", "Megyeszékhely");
        addCity(list, "Tatabánya", "Komárom-Esztergom", "Megyeszékhely");
        addCity(list, "Sopron", "Győr-Moson-Sopron", "Nagyváros");
        addCity(list, "Zalaegerszeg", "Zala", "Megyeszékhely");
        addCity(list, "Kaposvár", "Somogy", "Megyeszékhely");
        addCity(list, "Dunaújváros", "Fejér", "Nagyváros");

        return list;
    }

    private static void addCity(List<Map<String, String>> list, String name, String county, String group) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("key", normalizeCityKey(name));
        item.put("county", county);
        item.put("group", group);
        list.add(item);
    }

    /**
     * Gyorsválasztó sablonok (Presets).
     */
    public static Map<String, List<String>> getPresets() {
        Map<String, List<String>> presets = new LinkedHashMap<>();
        presets.put("Debrecen és környéke", List.of(
                "Debrecen", "Hajdúszoboszló", "Hajdúböszörmény", "Balmazújváros",
                "Ebes", "Mikepércs", "Bocskaikert", "Józsa"
        ));
        presets.put("Hajdú-Bihar nagyobb városai", List.of(
                "Debrecen", "Hajdúszoboszló", "Hajdúböszörmény", "Balmazújváros",
                "Berettyóújfalu", "Püspökladány"
        ));
        presets.put("Kelet-Magyarország megyeszékhelyek", List.of(
                "Debrecen", "Nyíregyháza", "Miskolc", "Szolnok", "Eger", "Békéscsaba"
        ));
        presets.put("Főbb egyetemvárosok", List.of(
                "Debrecen", "Budapest", "Szeged", "Pécs", "Győr", "Miskolc"
        ));
        return presets;
    }
}
