package ingatlanok.common;

import java.util.*;

public final class CityUtils {

    private CityUtils() {

    }

    public static String normalizeCityKey(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return "debrecen";
        }
        String normalized = SlugUtils.toSlug(cityName);
        return normalized.isEmpty() ? "debrecen" : normalized;
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

    private static final Map<String, String> CITY_COUNTY_SLUG_MAP = new LinkedHashMap<>();
    private static final Map<String, String> CITY_COUNTY_NAME_MAP = new LinkedHashMap<>();

    static {

        for (Map<String, String> cityInfo : getPredefinedCities()) {
            String citySlug = cityInfo.get("key");
            String countyName = cityInfo.get("county");
            String countySlug = SlugUtils.toSlug(countyName);
            CITY_COUNTY_SLUG_MAP.put(citySlug, countySlug);
            CITY_COUNTY_NAME_MAP.put(citySlug, countyName);
        }

        registerCityCounty("cegled", "pest", "Pest");
        registerCityCounty("erd", "pest", "Pest");
        registerCityCounty("dunakeszi", "pest", "Pest");
        registerCityCounty("godollo", "pest", "Pest");
        registerCityCounty("szentendre", "pest", "Pest");
        registerCityCounty("vac", "pest", "Pest");
        registerCityCounty("vecses", "pest", "Pest");
        registerCityCounty("pomaz", "pest", "Pest");
        registerCityCounty("god", "pest", "Pest");
        registerCityCounty("budakeszi", "pest", "Pest");
        registerCityCounty("szigetszentmiklos", "pest", "Pest");
        registerCityCounty("budaors", "pest", "Pest");
        registerCityCounty("monor", "pest", "Pest");
        registerCityCounty("kisnana", "heves", "Heves");
        registerCityCounty("gyongyospata", "heves", "Heves");
        registerCityCounty("gyongyos", "heves", "Heves");
        registerCityCounty("hatvan", "heves", "Heves");
        registerCityCounty("baja", "bacs-kiskun", "Bács-Kiskun");
        registerCityCounty("kiskunfelegyhaza", "bacs-kiskun", "Bács-Kiskun");
        registerCityCounty("kiskunhalas", "bacs-kiskun", "Bács-Kiskun");
        registerCityCounty("kalocsa", "bacs-kiskun", "Bács-Kiskun");
        registerCityCounty("hodmezovasarhely", "csongrad-csanad", "Csongrád-Csanád");
        registerCityCounty("szentes", "csongrad-csanad", "Csongrád-Csanád");
        registerCityCounty("mako", "csongrad-csanad", "Csongrád-Csanád");
        registerCityCounty("mor", "fejer", "Fejér");
        registerCityCounty("bicske", "fejer", "Fejér");
        registerCityCounty("mosonmagyarovar", "gyor-moson-sopron", "Győr-Moson-Sopron");
        registerCityCounty("csorna", "gyor-moson-sopron", "Győr-Moson-Sopron");
        registerCityCounty("jaszbereny", "jasz-nagykun-szolnok", "Jász-Nagykun-Szolnok");
        registerCityCounty("karcag", "jasz-nagykun-szolnok", "Jász-Nagykun-Szolnok");
        registerCityCounty("esztergom", "komarom-esztergom", "Komárom-Esztergom");
        registerCityCounty("tata", "komarom-esztergom", "Komárom-Esztergom");
        registerCityCounty("salgotarjan", "nograd", "Nógrád");
        registerCityCounty("siofok", "somogy", "Somogy");
        registerCityCounty("marcali", "somogy", "Somogy");
        registerCityCounty("mateszalka", "szabolcs-szatmar-bereg", "Szabolcs-Szatmár-Bereg");
        registerCityCounty("kisvarda", "szabolcs-szatmar-bereg", "Szabolcs-Szatmár-Bereg");
        registerCityCounty("nyirbator", "szabolcs-szatmar-bereg", "Szabolcs-Szatmár-Bereg");
        registerCityCounty("szekszard", "tolna", "Tolna");
        registerCityCounty("paks", "tolna", "Tolna");
        registerCityCounty("sarvar", "vas", "Vas");
        registerCityCounty("papa", "veszprem", "Veszprém");
        registerCityCounty("ajka", "veszprem", "Veszprém");
        registerCityCounty("balatonfured", "veszprem", "Veszprém");
        registerCityCounty("tapolca", "veszprem", "Veszprém");
        registerCityCounty("nagykanizsa", "zala", "Zala");
        registerCityCounty("keszthely", "zala", "Zala");
        registerCityCounty("hajdunanas", "hajdu-bihar", "Hajdú-Bihar");
        registerCityCounty("polgar", "hajdu-bihar", "Hajdú-Bihar");
        registerCityCounty("nadudvar", "hajdu-bihar", "Hajdú-Bihar");
        registerCityCounty("letavertes", "hajdu-bihar", "Hajdú-Bihar");
        registerCityCounty("ozd", "borsod-abauj-zemplen", "Borsod-Abaúj-Zemplén");
        registerCityCounty("kazincbarcika", "borsod-abauj-zemplen", "Borsod-Abaúj-Zemplén");
        registerCityCounty("tiszaujvaros", "borsod-abauj-zemplen", "Borsod-Abaúj-Zemplén");
        registerCityCounty("mezokovesd", "borsod-abauj-zemplen", "Borsod-Abaúj-Zemplén");
        registerCityCounty("gyula", "bekes", "Békés");
        registerCityCounty("oroshaza", "bekes", "Békés");
        registerCityCounty("komlo", "baranya", "Baranya");
        registerCityCounty("mohacs", "baranya", "Baranya");
    }

    private static void registerCityCounty(String citySlug, String countySlug, String countyName) {
        CITY_COUNTY_SLUG_MAP.put(citySlug, countySlug);
        CITY_COUNTY_NAME_MAP.put(citySlug, countyName);
    }

    public static Optional<String> getCountySlug(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return Optional.empty();
        }
        String key = normalizeCityKey(cityName);
        String countySlug = CITY_COUNTY_SLUG_MAP.get(key);
        if (countySlug != null) {
            return Optional.of(countySlug);
        }

        for (Map.Entry<String, String> entry : CITY_COUNTY_SLUG_MAP.entrySet()) {
            if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public static Optional<String> getCountyName(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return Optional.empty();
        }
        String key = normalizeCityKey(cityName);
        String countyName = CITY_COUNTY_NAME_MAP.get(key);
        if (countyName != null) {
            return Optional.of(countyName);
        }
        for (Map.Entry<String, String> entry : CITY_COUNTY_NAME_MAP.entrySet()) {
            if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public static boolean isKnownCity(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return false;
        }
        String key = normalizeCityKey(cityName);
        return CITY_COUNTY_SLUG_MAP.containsKey(key);
    }

    public static boolean isSameOrSubLocation(String locA, String locB) {
        if (locA == null || locB == null || locA.isBlank() || locB.isBlank()) {
            return false;
        }
        String slugA = SlugUtils.toSlug(locA);
        String slugB = SlugUtils.toSlug(locB);
        if (slugA.equals(slugB)) {
            return true;
        }
        if (slugA.contains(slugB) || slugB.contains(slugA)) {
            return true;
        }
        return false;
    }

    public static boolean containsDifferentKnownCity(String textSlug, String targetCitySlug) {
        if (textSlug == null || targetCitySlug == null || textSlug.isBlank()) {
            return false;
        }
        if (textSlug.contains(targetCitySlug)) {
            return false;
        }
        for (String knownCity : CITY_COUNTY_SLUG_MAP.keySet()) {
            if (!knownCity.equals(targetCitySlug) && knownCity.length() >= 4) {
                if (textSlug.contains(knownCity)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void addCity(List<Map<String, String>> list, String name, String county, String group) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("key", normalizeCityKey(name));
        item.put("county", county);
        item.put("group", group);
        list.add(item);
    }

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
