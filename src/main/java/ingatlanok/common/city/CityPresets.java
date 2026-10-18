package ingatlanok.common.city;

import ingatlanok.common.SlugUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Előre definiált magyarországi kiemelt települések listája és metaadatai.
 */
public final class CityPresets {

    private CityPresets() {
    }

    public static List<Map<String, String>> getPredefinedCities() {
        List<Map<String, String>> list = new ArrayList<>();

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

    private static void addCity(List<Map<String, String>> list, String name, String county, String description) {
        Map<String, String> city = new LinkedHashMap<>();
        city.put("key", SlugUtils.toSlug(name));
        city.put("name", name);
        city.put("county", county);
        city.put("description", description);
        list.add(city);
    }
}
