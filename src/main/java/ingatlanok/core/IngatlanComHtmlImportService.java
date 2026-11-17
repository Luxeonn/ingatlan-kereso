package ingatlanok.core;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ingatlanok.common.AppLogger;
import ingatlanok.common.CityUtils;
import ingatlanok.common.SlugUtils;
import ingatlanok.core.normalization.ListingNormalizer;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PredefinedCity;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.portal.ingatlancom.IngatlanComPortalEngine;
import ingatlanok.portal.ingatlancom.IngatlanComRawParser;
import ingatlanok.portal.ingatlancom.IngatlanComUrlBuilder;

/**
 * Parses and persists user-supplied Ingatlan.com search result HTML without outbound requests or
 * anti-bot circumvention (#455).
 */
@Service
public class IngatlanComHtmlImportService {

  static final int MAX_HTML_BYTES = 5 * 1024 * 1024;
  private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanComHtmlImportService.class);
  private static final DateTimeFormatter TS_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final IngatlanComRawParser rawParser;
  private final ListingNormalizer listingNormalizer;
  private final DeduplicationEngine dedupEngine;
  private final DataQualityEngine qualityEngine;
  private final ListingStorage listingStorage;
  private final SearchPersistenceSynchronizer persistenceSynchronizer;
  private final PortalDescriptor portalDescriptor;

  @Autowired
  public IngatlanComHtmlImportService(
      PortalEngineRegistry portalRegistry,
      ListingNormalizer listingNormalizer,
      DeduplicationEngine dedupEngine,
      DataQualityEngine qualityEngine,
      ListingStorage listingStorage) {
    this.rawParser = new IngatlanComRawParser();
    this.listingNormalizer = Objects.requireNonNull(listingNormalizer);
    this.dedupEngine = Objects.requireNonNull(dedupEngine);
    this.qualityEngine = Objects.requireNonNull(qualityEngine);
    this.listingStorage = Objects.requireNonNull(listingStorage);
    this.persistenceSynchronizer =
        new SearchPersistenceSynchronizer(Objects.requireNonNull(portalRegistry), listingStorage);
    this.portalDescriptor =
        portalRegistry
            .getEngine("ingatlancom")
            .map(e -> e.getDescriptor())
            .orElseGet(() -> new IngatlanComPortalEngine().getDescriptor());
  }

  public record ImportSummary(
      int parsedCount,
      int savedCount,
      int newlyAddedCount,
      int duplicateCount,
      String city,
      List<IngatlanItem> items) {}

  public ImportSummary importHtml(
      String html, String sourceUrl, String explicitCity, PropertyType explicitType) {
    validateHtml(html);
    validateSourceUrl(sourceUrl);

    Document document = Jsoup.parse(html, IngatlanComUrlBuilder.BASE_URL);
    String resolvedCity = resolveCity(explicitCity, sourceUrl, document);
    PropertyType resolvedType = resolvePropertyType(explicitType, sourceUrl, document);

    List<RawPropertyListing> rawListings = rawParser.parseRawListings(document);
    if (rawListings.isEmpty()) {
      LOGGER.warn(
          "Ingatlan.com HTML import: 0 hirdetéskártya található (város={})", resolvedCity);
      return new ImportSummary(0, 0, 0, 0, resolvedCity, List.of());
    }

    SearchCriteria criteria = buildNormalizationCriteria(resolvedCity, resolvedType);
    String timestamp = LocalDateTime.now().format(TS_FORMAT);
    List<IngatlanItem> normalized = normalizeListings(rawListings, criteria, resolvedType, timestamp);
    List<IngatlanItem> uniqueItems = dedupEngine.deduplicateByUrl(normalized);

    int newCount = countNewListings(uniqueItems);
    dedupEngine.applyNewStatus(resolvedCity, uniqueItems, false);
    listingStorage.saveCityItems(resolvedCity, uniqueItems);
    persistenceSynchronizer.enrichWithPersistedState(uniqueItems);
    qualityEngine.evaluateAll(uniqueItems);

    int existingCount = Math.max(0, uniqueItems.size() - newCount);
    LOGGER.info(
        "Ingatlan.com HTML import ({}): {} nyers -> {} egyedi ({} új, {} frissített)",
        resolvedCity,
        rawListings.size(),
        uniqueItems.size(),
        newCount,
        existingCount);

    return new ImportSummary(
        rawListings.size(),
        uniqueItems.size(),
        newCount,
        existingCount,
        resolvedCity,
        List.copyOf(uniqueItems));
  }

  private void validateHtml(String html) {
    if (html == null || html.isBlank()) {
      throw new IllegalArgumentException("Az importálandó HTML tartalom nem lehet üres");
    }
    if (html.length() > MAX_HTML_BYTES) {
      throw new IllegalArgumentException("A HTML tartalom mérete meghaladja a 5 MB korlátot");
    }
  }

  private void validateSourceUrl(String sourceUrl) {
    if (sourceUrl == null || sourceUrl.isBlank()) {
      return;
    }
    URI parsed;
    try {
      parsed = URI.create(sourceUrl.trim());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Érvénytelen forrás URL formátum", e);
    }
    String scheme = parsed.getScheme() != null ? parsed.getScheme().toLowerCase(Locale.ROOT) : "";
    String host = parsed.getHost() != null ? parsed.getHost().toLowerCase(Locale.ROOT) : "";
    boolean validScheme = "https".equals(scheme) || "http".equals(scheme);
    boolean validHost = "ingatlan.com".equals(host) || host.endsWith(".ingatlan.com");
    if (!validScheme || !validHost) {
      throw new IllegalArgumentException(
          "Érvénytelen forrás URL: kizárólag ingatlan.com oldal importálható");
    }
  }

  private String resolveCity(String explicitCity, String sourceUrl, Document document) {
    if (explicitCity != null && !explicitCity.isBlank()) {
      return explicitCity.trim();
    }
    String hintText = collectDocumentHint(sourceUrl, document);
    String hintSlug = SlugUtils.toSlug(hintText);
    if (!hintSlug.isBlank()) {
      List<PredefinedCity> known = new ArrayList<>(CityUtils.getPredefinedCities());
      known.sort(Comparator.comparingInt((PredefinedCity c) -> c.key().length()).reversed());
      for (PredefinedCity city : known) {
        if (city.key() != null && !city.key().isBlank() && hintSlug.contains(city.key())) {
          return city.name();
        }
      }
    }
    return "Debrecen";
  }

  private PropertyType resolvePropertyType(
      PropertyType explicitType, String sourceUrl, Document document) {
    if (explicitType != null && explicitType != PropertyType.EGYEB_ISMERETLEN) {
      return explicitType;
    }
    String hintSlug = SlugUtils.toSlug(collectDocumentHint(sourceUrl, document));
    if (hintSlug.contains("-haz") || hintSlug.contains("elado-haz")) {
      return PropertyType.HAZ;
    }
    if (hintSlug.contains("-telek") || hintSlug.contains("elado-telek")) {
      return PropertyType.TELEK;
    }
    if (hintSlug.contains("-garazs") || hintSlug.contains("elado-garazs")) {
      return PropertyType.GARAZS;
    }
    if (hintSlug.contains("-lakas") || hintSlug.contains("elado-lakas")) {
      return PropertyType.LAKAS;
    }
    return null;
  }

  private String collectDocumentHint(String sourceUrl, Document document) {
    StringBuilder sb = new StringBuilder();
    if (sourceUrl != null) {
      sb.append(sourceUrl).append(' ');
    }
    if (document != null) {
      Element h1 = document.selectFirst("h1");
      if (h1 != null) {
        sb.append(h1.text()).append(' ');
      }
      sb.append(document.title());
    }
    return sb.toString();
  }

  private SearchCriteria buildNormalizationCriteria(String city, PropertyType type) {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity(city);
    if (type != null) {
      criteria.setPropertyType(type);
    }
    return criteria;
  }

  private List<IngatlanItem> normalizeListings(
      List<RawPropertyListing> rawListings,
      SearchCriteria criteria,
      PropertyType resolvedType,
      String timestamp) {
    List<IngatlanItem> items = new ArrayList<>(rawListings.size());
    for (RawPropertyListing raw : rawListings) {
      IngatlanItem item = listingNormalizer.normalize(raw, portalDescriptor, criteria, timestamp);
      if (item != null) {
        if (resolvedType != null
            && (item.getPropertyType() == null
                || item.getPropertyType() == PropertyType.EGYEB_ISMERETLEN)) {
          item.setPropertyType(resolvedType);
        }
        items.add(item);
      }
    }
    return items;
  }

  private int countNewListings(List<IngatlanItem> uniqueItems) {
    int count = 0;
    for (IngatlanItem item : uniqueItems) {
      if (item != null && item.getUrl() != null && !listingStorage.containsUrl(item.getUrl())) {
        count++;
      }
    }
    return count;
  }
}
