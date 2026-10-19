package ingatlanok.portal.jofogas;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import ingatlanok.model.SearchCriteria;

/** Jófogás Ingatlan keresési URL összeállító komponens. */
public class JofogasUrlBuilder {

  public static final String BASE_URL = "https://ingatlan.jofogas.hu";

  public String buildSearchUrl(SearchCriteria criteria) {
    if (criteria == null) {
      criteria = new SearchCriteria();
    }

    String rawType =
        criteria.getPropertyType() != null
            ? criteria.getPropertyType().toLowerCase().trim()
            : "lakas";
    String typeWord =
        switch (rawType) {
          case "haz" -> "haz";
          case "telek" -> "telek";
          default -> "lakas";
        };

    String query = criteria.getCity() + " elado " + typeWord;
    String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
    StringBuilder urlBuilder =
        new StringBuilder(BASE_URL).append("/magyarorszag/ingatlan?q=").append(encodedQuery);

    if (criteria.getMinPriceMillion() != null) {
      urlBuilder.append("&min_price=").append(criteria.getMinPriceMillion() * 1_000_000L);
    }
    if (criteria.getMaxPriceMillion() != null) {
      urlBuilder.append("&max_price=").append(criteria.getMaxPriceMillion() * 1_000_000L);
    }

    return urlBuilder.toString();
  }
}
