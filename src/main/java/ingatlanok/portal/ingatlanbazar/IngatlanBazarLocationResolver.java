package ingatlanok.portal.ingatlanbazar;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.common.SlugUtils;

public final class IngatlanBazarLocationResolver {

  private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanBazarLocationResolver.class);
  private static final String GEO_MATCH_URL =
      "https://www.ingatlanbazar.hu/api/geo/get-matches?name=%s&types=3&needLocation=1";
  private static final Map<String, Integer> LOCATION_CACHE = new ConcurrentHashMap<>();

  static {
    registerLocation("budapest", 1050);
    registerLocation("debrecen", 1435);
    registerLocation("szeged", 1099);
    registerLocation("miskolc", 617);
    registerLocation("pecs", 896);
    registerLocation("gyor", 1260);
    registerLocation("nyiregyhaza", 2499);
    registerLocation("kecskemet", 255);
    registerLocation("szekesfehervar", 1216);
    registerLocation("szombathely", 2842);
    registerLocation("szolnok", 1654);
    registerLocation("tatabanya", 1499);
    registerLocation("kaposvar", 2190);
    registerLocation("bekescsaba", 404);
    registerLocation("veszprem", 3074);
    registerLocation("zalaegerszeg", 3300);
    registerLocation("eger", 1745);
    registerLocation("salgotarjan", 1870);
    registerLocation("szekszard", 2624);
    registerLocation("hajduszoboszlo", 1445);
    registerLocation("dunaujvaros", 1000000001);
    registerLocation("nagykanizsa", 1000000003);
    registerLocation("sopron", 1260);
    registerLocation("erd", 1050);
    registerLocation("budaors", 1050);
    registerLocation("godollo", 1050);
    registerLocation("vac", 1050);
    registerLocation("dunakeszi", 1050);
    registerLocation("szentendre", 1050);
    registerLocation("siofok", 2190);
  }

  private IngatlanBazarLocationResolver() {}

  public static void registerLocation(String key, int id) {
    LOCATION_CACHE.put(key, id);
  }

  public static Optional<Integer> resolveLocationId(String cityName) {
    if (cityName == null || cityName.isBlank()) {
      return Optional.empty();
    }

    String slug = SlugUtils.toSlug(cityName);
    if (LOCATION_CACHE.containsKey(slug)) {
      return Optional.of(LOCATION_CACHE.get(slug));
    }

    try {
      String encoded = URLEncoder.encode(cityName.trim(), StandardCharsets.UTF_8);
      String url = String.format(GEO_MATCH_URL, encoded);
      LOGGER.debug("Ingatlanbazár településkód lekérdezése: {} -> {}", cityName, url);
      String json = HttpFetcher.fetchJson(url);

      if (json != null && !json.isBlank()) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (root.has("data") && root.get("data").isJsonArray()) {
          JsonArray data = root.getAsJsonArray("data");
          for (JsonElement el : data) {
            if (el.isJsonObject()) {
              JsonObject obj = el.getAsJsonObject();
              if (obj.has("id") && !obj.get("id").isJsonNull()) {
                int id = obj.get("id").getAsInt();
                LOCATION_CACHE.put(slug, id);
                LOGGER.debug("Ingatlanbazár településkód megtalálva: {} -> {}", cityName, id);
                return Optional.of(id);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      LOGGER.warn("Nem sikerült feloldani a településkódot ({}): {}", cityName, e.getMessage());
    }

    return Optional.empty();
  }
}
