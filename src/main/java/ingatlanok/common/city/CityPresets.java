package ingatlanok.common.city;

import java.util.ArrayList;
import java.util.List;

import ingatlanok.common.SlugUtils;
import ingatlanok.model.PredefinedCity;

/**
 * Static registry of predefined Hungarian municipalities (#401 / JAVA-023).
 */
public final class CityPresets {

  private CityPresets() {}

  public static List<PredefinedCity> getPredefinedCities() {
    List<PredefinedCity> list = new ArrayList<>();

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

  private static void addCity(
      List<PredefinedCity> list, String name, String county, String description) {
    list.add(new PredefinedCity(SlugUtils.toSlug(name), name, county, description));
  }
}
