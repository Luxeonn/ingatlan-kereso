package ingatlanok.web;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ingatlanok.common.BrowserUtils;
import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.StatusLogEntry;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.presentation.DashboardPresentationService;
import ingatlanok.presentation.DashboardResponseDto;

@RestController
@RequestMapping("/api")
public class RestApiController {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestApiController.class);

  private final ListingStorage listingStorage;
  private final DashboardPresentationService dashboardPresentationService;
  private final PortalEngineRegistry portalEngineRegistry;

  @Value("${server.port:8080}")
  private int port;

  public RestApiController(
      ListingStorage listingStorage,
      DashboardPresentationService dashboardPresentationService,
      PortalEngineRegistry portalEngineRegistry) {
    this.listingStorage = listingStorage;
    this.dashboardPresentationService = dashboardPresentationService;
    this.portalEngineRegistry = portalEngineRegistry;
  }

  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getStatus() {
    Map<String, Object> status = new LinkedHashMap<>();
    status.put("status", "UP");
    status.put("port", port);
    status.put("totalInDb", listingStorage.getTotalCount());
    status.put("lastRunTime", listingStorage.getLastRunTime());
    status.put("trackedCities", listingStorage.getTrackedCitiesInfo());
    return ResponseEntity.ok(status);
  }

  @GetMapping("/portals")
  @Cacheable("portals")
  public ResponseEntity<List<PortalDescriptor>> getPortals() {
    Map<String, Long> portalCounts = listingStorage.countByPortal();
    List<PortalDescriptor> descriptors =
        portalEngineRegistry.getAllDescriptors().stream()
            .map(
                d -> {
                  long count = 0L;
                  String dId = d.id().trim().toLowerCase(Locale.ROOT);
                  String dName = d.name().trim().toLowerCase(Locale.ROOT);
                  for (Map.Entry<String, Long> entry : portalCounts.entrySet()) {
                    String k = entry.getKey().trim().toLowerCase(Locale.ROOT);
                    if (k.equals(dId) || k.equals(dName) || k.contains(dId) || dName.contains(k)) {
                      count += entry.getValue();
                    }
                  }
                  return d.withSavedListingCount(count);
                })
            .sorted(
                Comparator.comparingLong(PortalDescriptor::savedListingCount)
                    .reversed()
                    .thenComparing(PortalDescriptor::name))
            .toList();
    return ResponseEntity.ok(descriptors);
  }

  @GetMapping("/cities")
  @Cacheable("cities")
  public ResponseEntity<Map<String, Object>> getCities() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("predefined", CityUtils.getPredefinedCities());
    data.put("presets", CityUtils.getPresets());
    data.put("trackedCities", listingStorage.getTrackedCitiesInfo());
    return ResponseEntity.ok(data);
  }

  @GetMapping("/config")
  public ResponseEntity<SearchCriteria> getLastCriteria() {
    SearchCriteria criteria = null;
    if (listingStorage instanceof ingatlanok.persistence.ConfigStorage cs) {
      criteria = cs.getLastCriteria();
    }
    if (criteria == null) {
      criteria = new SearchCriteria();
    }
    return ResponseEntity.ok(criteria);
  }

  @PostMapping("/config")
  public ResponseEntity<Map<String, Object>> saveLastCriteria(
      @RequestBody(required = false) SearchCriteria criteria) {
    if (criteria != null && listingStorage instanceof ingatlanok.persistence.ConfigStorage cs) {
      cs.saveLastCriteria(criteria);
    }
    return ResponseEntity.ok(Map.of("success", true));
  }

  @GetMapping("/history")
  public ResponseEntity<List<IngatlanItem>> getHistory(
      @RequestParam(value = "city", required = false) String city,
      @RequestParam(value = "onlyDiscounted", defaultValue = "false") boolean onlyDiscounted,
      @RequestParam(value = "portal", required = false) String portal,
      @RequestParam(value = "minPrice", required = false) Double minPrice,
      @RequestParam(value = "maxPrice", required = false) Double maxPrice,
      @RequestParam(value = "minArea", required = false) Double minArea,
      @RequestParam(value = "maxArea", required = false) Double maxArea,
      @RequestParam(value = "minRooms", required = false) Double minRooms,
      @RequestParam(value = "maxRooms", required = false) Double maxRooms,
      @RequestParam(value = "excludeViewed", defaultValue = "false") boolean excludeViewed,
      @RequestParam(value = "sortBy", defaultValue = "DISCOVERED_DESC") String sortBy,
      @RequestParam(value = "limit", required = false) Integer limit,
      @RequestParam(value = "offset", required = false) Integer offset) {
    List<String> cities = (city != null && !city.isBlank()) ? List.of(city) : null;
    List<String> portals = (portal != null && !portal.isBlank()) ? List.of(portal) : null;

    int pageSize = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
    PropertySearchCriteria filter =
        new PropertySearchCriteria(
            cities,
            portals,
            minPrice,
            maxPrice,
            minArea,
            maxArea,
            minRooms,
            maxRooms,
            excludeViewed,
            onlyDiscounted,
            sortBy,
            offset == null || offset <= 0 ? 0 : offset,
            pageSize);

    List<IngatlanItem> dtoList = listingStorage.findByCriteria(filter);
    return ResponseEntity.ok(dtoList);
  }

  @GetMapping({"/properties/price-history", "/properties/{id}/price-history"})
  public ResponseEntity<?> getPriceHistory(
      @PathVariable(value = "id", required = false) String pathId,
      @RequestParam(value = "id", required = false) String paramId,
      @RequestParam(value = "url", required = false) String paramUrl) {
    String key =
        pathId != null && !pathId.isBlank()
            ? pathId
            : (paramId != null && !paramId.isBlank() ? paramId : paramUrl);
    if (key == null || key.isBlank()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Hiányzó 'id' vagy 'url' paraméter!"));
    }
    List<PriceHistoryEntry> history = listingStorage.getPriceHistory(key);
    return ResponseEntity.ok(history);
  }

  @GetMapping({"/properties/status-log", "/properties/{id}/status-log"})
  public ResponseEntity<?> getStatusLog(
      @PathVariable(value = "id", required = false) String pathId,
      @RequestParam(value = "id", required = false) String paramId,
      @RequestParam(value = "url", required = false) String paramUrl) {
    String key =
        pathId != null && !pathId.isBlank()
            ? pathId
            : (paramId != null && !paramId.isBlank() ? paramId : paramUrl);
    if (key == null || key.isBlank()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Hiányzó 'id' vagy 'url' paraméter!"));
    }
    List<StatusLogEntry> log = listingStorage.getStatusLog(key);
    return ResponseEntity.ok(log);
  }

  @GetMapping("/properties/discounted")
  public ResponseEntity<List<IngatlanItem>> getDiscountedProperties(
      @RequestParam(value = "city", required = false) String city) {
    List<IngatlanItem> discounted = listingStorage.getDiscountedProperties(city);
    return ResponseEntity.ok(discounted);
  }

  @PostMapping("/search")
  public ResponseEntity<DashboardResponseDto> executeSearch(
      @RequestBody(required = false) SearchCriteria criteria) {
    if (criteria == null) {
      criteria = new SearchCriteria();
    }
    DashboardResponseDto result = dashboardPresentationService.executeSearch(criteria);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/open-urls")
  public ResponseEntity<Map<String, Object>> openUrls(
      @RequestBody(required = false) List<String> urls) {
    if (urls != null && !urls.isEmpty()) {
      for (String url : urls) {
        listingStorage.markAsViewed(url);
      }
      boolean forceServerOpen =
          "true".equalsIgnoreCase(System.getProperty("server.open.desktop", "false"));
      if (forceServerOpen && !java.awt.GraphicsEnvironment.isHeadless()) {
        BrowserUtils.openUrls(new LinkedHashSet<>(urls));
      }
    }
    return ResponseEntity.ok(Map.of("success", true, "count", urls != null ? urls.size() : 0));
  }

  @PostMapping("/items/viewed")
  public ResponseEntity<Map<String, Object>> markViewed(
      @RequestBody(required = false) Map<String, Object> body) {
    String url = body != null && body.get("url") != null ? body.get("url").toString() : null;
    boolean marked = false;
    if (url != null && !url.isBlank()) {
      marked = listingStorage.markAsViewed(url);
    }
    return ResponseEntity.ok(
        Map.of("success", true, "marked", marked, "url", url != null ? url : ""));
  }

  @PostMapping("/history/clear")
  public ResponseEntity<Map<String, Object>> clearHistory(
      @RequestBody(required = false) Map<String, Object> body,
      @RequestParam(value = "city", required = false) String paramCity) {
    String city = paramCity;
    if ((city == null || city.isBlank()) && body != null && body.get("city") != null) {
      city = body.get("city").toString();
    }

    if (city != null && !city.isBlank()) {
      listingStorage.clearCityHistory(city);
      return ResponseEntity.ok(Map.of("success", true, "clearedCity", city));
    } else {
      listingStorage.clearHistory();
      return ResponseEntity.ok(Map.of("success", true, "clearedAll", true));
    }
  }
}
