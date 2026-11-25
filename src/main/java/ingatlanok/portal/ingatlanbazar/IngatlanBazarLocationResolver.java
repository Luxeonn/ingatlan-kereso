package ingatlanok.portal.ingatlanbazar;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import ingatlanok.common.HttpFetcher;
import ingatlanok.common.JsonSupport;
import ingatlanok.common.SlugUtils;

public final class IngatlanBazarLocationResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(IngatlanBazarLocationResolver.class);
  private static final String GEO_MATCH_URL =
      "https://www.ingatlanbazar.hu/api/geo/get-matches?name=%s&types=3&needLocation=1";
  private static final Map<String, Integer> LOCATION_CACHE = new ConcurrentHashMap<>();

  static {
    registerLocation("budapest", 1050);
    registerLocation("debrecen", 1435);
    registerLocation("szeged", 1099);
    registerLocation("miskolc", 896);
    registerLocation("pecs", 404);
    registerLocation("gyor", 1260);
    registerLocation("nyiregyhaza", 2499);
    registerLocation("kecskemet", 255);
    registerLocation("szekesfehervar", 1216);
    registerLocation("szombathely", 2842);
    registerLocation("szolnok", 1654);
    registerLocation("tatabanya", 1745);
    registerLocation("kaposvar", 2190);
    registerLocation("bekescsaba", 617);
    registerLocation("veszprem", 3074);
    registerLocation("zalaegerszeg", 3300);
    registerLocation("eger", 1499);
    registerLocation("salgotarjan", 1870);
    registerLocation("szekszard", 2624);
    registerLocation("hajduszoboszlo", 1462);
    registerLocation("dunaujvaros", 1147);
    registerLocation("nagykanizsa", 3213);
    registerLocation("sopron", 1375);
    registerLocation("erd", 1939);
    registerLocation("budaors", 1902);
    registerLocation("godollo", 1944);
    registerLocation("vac", 2065);
    registerLocation("dunakeszi", 1934);
    registerLocation("szentendre", 2033);
    registerLocation("siofok", 2295);
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
      Optional<Integer> resolved = parseLocationIdFromJson(json, cityName);
      if (resolved.isPresent()) {
        int id = resolved.get();
        LOCATION_CACHE.put(slug, id);
        LOGGER.debug("Ingatlanbazár településkód megtalálva: {} -> {}", cityName, id);
        return resolved;
      }
    } catch (Exception e) {
      LOGGER.warn("Nem sikerült feloldani a településkódot ({}): {}", cityName, e.getMessage());
    }

    return Optional.empty();
  }

  static Optional<Integer> parseLocationIdFromJson(String json, String cityName) {
    if (json == null || json.isBlank()) {
      return Optional.empty();
    }
    JsonNode root = JsonSupport.parse(json);
    if (!root.has("data") || !root.get("data").isArray()) {
      return Optional.empty();
    }
    String targetSlug = SlugUtils.toSlug(cityName);
    Integer fallbackId = null;
    for (JsonNode el : root.get("data")) {
      if (!el.isObject() || !el.has("id") || el.get("id").isNull()) {
        continue;
      }
      int id = el.get("id").asInt();
      if (fallbackId == null) {
        fallbackId = id;
      }
      if (isExactCityMatch(el, targetSlug)) {
        return Optional.of(id);
      }
    }
    return Optional.ofNullable(fallbackId);
  }

  private static boolean isExactCityMatch(JsonNode el, String targetSlug) {
    if (targetSlug.isEmpty() || !el.has("name") || el.get("name").isNull()) {
      return false;
    }
    return targetSlug.equals(SlugUtils.toSlug(el.get("name").asString()));
  }
}
