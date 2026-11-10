package ingatlanok.web;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.ListingObservation;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchPreset;
import ingatlanok.model.StatusLogEntry;
import ingatlanok.persistence.ConfigStorage;
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
  private final ConfigStorage configStorage;
  private final DashboardPresentationService dashboardPresentationService;
  private final PortalEngineRegistry portalEngineRegistry;

  @Value("${server.port:8080}")
  private int port;

  @Autowired
  public RestApiController(
      ListingStorage listingStorage,
      @Autowired(required = false) ConfigStorage configStorage,
      DashboardPresentationService dashboardPresentationService,
      PortalEngineRegistry portalEngineRegistry) {
    this.listingStorage = listingStorage;
    this.configStorage = configStorage;
    this.dashboardPresentationService = dashboardPresentationService;
    this.portalEngineRegistry = portalEngineRegistry;
  }

  @Deprecated
  public RestApiController(
      ListingStorage listingStorage,
      DashboardPresentationService dashboardPresentationService,
      PortalEngineRegistry portalEngineRegistry) {
    this(
        listingStorage,
        listingStorage instanceof ConfigStorage cs ? cs : null,
        dashboardPresentationService,
        portalEngineRegistry);
  }

  @GetMapping("/status")
  public ResponseEntity<StatusResponseDto> getStatus() {
    StatusResponseDto status =
        new StatusResponseDto(
            "UP",
            port,
            listingStorage.getTotalCount(),
            listingStorage.getLastRunTime(),
            listingStorage.getTrackedCitiesInfo());
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
  public ResponseEntity<CitiesResponseDto> getCities() {
    CitiesResponseDto data =
        new CitiesResponseDto(
            CityUtils.getPredefinedCities(),
            CityUtils.getPresets(),
            listingStorage.getTrackedCitiesInfo());
    return ResponseEntity.ok(data);
  }

  @GetMapping("/config")
  public ResponseEntity<SearchCriteria> getLastCriteria() {
    ConfigStorage cs = getConfigStorage();
    SearchCriteria criteria = null;
    if (cs != null) {
      criteria = cs.getLastCriteria();
    }
    if (criteria == null) {
      criteria = new SearchCriteria();
    }
    return ResponseEntity.ok(criteria);
  }

  @PostMapping("/config")
  public ResponseEntity<ActionSuccessResponseDto> saveLastCriteria(
      @RequestBody(required = false) SearchCriteria criteria) {
    ConfigStorage cs = getConfigStorage();
    if (criteria != null) {
      criteria.validate();
      if (cs != null) {
        cs.saveLastCriteria(criteria);
      }
    }
    return ResponseEntity.ok(ActionSuccessResponseDto.of(true));
  }

  @GetMapping("/presets")
  public ResponseEntity<List<SearchPreset>> getPresets() {
    ConfigStorage cs = getConfigStorage();
    if (cs == null) {
      return ResponseEntity.ok(List.of());
    }
    return ResponseEntity.ok(cs.getPresets());
  }

  @PostMapping("/presets")
  public ResponseEntity<SearchPreset> createPreset(@RequestBody SearchPreset preset) {
    ConfigStorage cs = getConfigStorage();
    if (cs == null) {
      return ResponseEntity.badRequest().build();
    }
    if (preset == null) {
      throw new IllegalArgumentException("A preset nem lehet üres!");
    }
    if (preset.name() == null || preset.name().isBlank()) {
      throw new IllegalArgumentException("A preset neve kötelező!");
    }
    if (preset.name().length() > 100) {
      throw new IllegalArgumentException("A preset neve legfeljebb 100 karakter lehet!");
    }
    if (preset.criteria() != null) {
      preset.criteria().validate();
    }
    SearchPreset created = cs.savePreset(preset);
    return ResponseEntity.ok(created);
  }

  @PutMapping("/presets/{id}")
  public ResponseEntity<SearchPreset> updatePreset(
      @PathVariable("id") Long id, @RequestBody SearchPreset preset) {
    ConfigStorage cs = getConfigStorage();
    if (cs == null || id == null || preset == null) {
      return ResponseEntity.badRequest().build();
    }
    if (id <= 0) {
      throw new IllegalArgumentException("Érvénytelen preset azonosító: " + id);
    }
    if (preset.name() != null && preset.name().isBlank()) {
      throw new IllegalArgumentException("A preset neve nem lehet üres!");
    }
    if (preset.name() != null && preset.name().length() > 100) {
      throw new IllegalArgumentException("A preset neve legfeljebb 100 karakter lehet!");
    }
    if (preset.criteria() != null) {
      preset.criteria().validate();
    }
    return cs.updatePreset(id, preset)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @DeleteMapping("/presets/{id}")
  public ResponseEntity<ActionSuccessResponseDto> deletePreset(@PathVariable("id") Long id) {
    ConfigStorage cs = getConfigStorage();
    if (cs == null || id == null) {
      return ResponseEntity.badRequest().build();
    }
    if (id <= 0) {
      throw new IllegalArgumentException("Érvénytelen preset azonosító: " + id);
    }
    boolean deleted = cs.deletePreset(id);
    if (!deleted) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(ActionSuccessResponseDto.of(true));
  }

  @PostMapping("/presets/{id}/use")
  public ResponseEntity<SearchPreset> usePreset(@PathVariable("id") Long id) {
    ConfigStorage cs = getConfigStorage();
    if (cs == null || id == null) {
      return ResponseEntity.badRequest().build();
    }
    if (id <= 0) {
      throw new IllegalArgumentException("Érvénytelen preset azonosító: " + id);
    }
    return cs.markPresetAsUsed(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private ConfigStorage getConfigStorage() {
    if (configStorage != null) {
      return configStorage;
    }
    return (listingStorage instanceof ConfigStorage cs) ? cs : null;
  }

  @GetMapping("/history")
  public ResponseEntity<List<IngatlanItem>> getHistory(@ModelAttribute HistoryQueryDto query) {
    HistoryQueryDto effectiveQuery =
        query != null
            ? query
            : new HistoryQueryDto(
                null, false, null, null, null, null, null, null, null, false, false, "DISCOVERED_DESC", 50, 0);
    List<IngatlanItem> dtoList = listingStorage.findByCriteria(effectiveQuery.toCriteria());
    return ResponseEntity.ok(dtoList);
  }

  @GetMapping({"/properties/price-history", "/properties/{id}/price-history"})
  public ResponseEntity<List<PriceHistoryEntry>> getPriceHistory(
      @PathVariable(value = "id", required = false) String pathId,
      @RequestParam(value = "id", required = false) String paramId,
      @RequestParam(value = "url", required = false) String paramUrl) {
    String key =
        pathId != null && !pathId.isBlank()
            ? pathId
            : (paramId != null && !paramId.isBlank() ? paramId : paramUrl);
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("Hiányzó 'id' vagy 'url' paraméter!");
    }
    if (key.length() > 1000) {
      throw new IllegalArgumentException("A megadott azonosító vagy URL túl hosszú!");
    }
    List<PriceHistoryEntry> history = listingStorage.getPriceHistory(key);
    return ResponseEntity.ok(history);
  }

  @GetMapping({"/properties/status-log", "/properties/{id}/status-log"})
  public ResponseEntity<List<StatusLogEntry>> getStatusLog(
      @PathVariable(value = "id", required = false) String pathId,
      @RequestParam(value = "id", required = false) String paramId,
      @RequestParam(value = "url", required = false) String paramUrl) {
    String key =
        pathId != null && !pathId.isBlank()
            ? pathId
            : (paramId != null && !paramId.isBlank() ? paramId : paramUrl);
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("Hiányzó 'id' vagy 'url' paraméter!");
    }
    if (key.length() > 1000) {
      throw new IllegalArgumentException("A megadott azonosító vagy URL túl hosszú!");
    }
    List<StatusLogEntry> log = listingStorage.getStatusLog(key);
    return ResponseEntity.ok(log);
  }

  @GetMapping({"/properties/observations", "/properties/{id}/observations"})
  public ResponseEntity<List<ListingObservation>> getListingObservations(
      @PathVariable(value = "id", required = false) String pathId,
      @RequestParam(value = "id", required = false) String paramId,
      @RequestParam(value = "url", required = false) String paramUrl) {
    String key =
        pathId != null && !pathId.isBlank()
            ? pathId
            : (paramId != null && !paramId.isBlank() ? paramId : paramUrl);
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("Hiányzó 'id' vagy 'url' paraméter!");
    }
    if (key.length() > 1000) {
      throw new IllegalArgumentException("A megadott azonosító vagy URL túl hosszú!");
    }
    List<ListingObservation> observations = listingStorage.getListingObservations(key);
    return ResponseEntity.ok(observations);
  }

  @GetMapping("/properties/hidden")
  public ResponseEntity<List<IngatlanItem>> getHiddenProperties() {
    List<IngatlanItem> hidden = listingStorage.getHiddenProperties();
    return ResponseEntity.ok(hidden);
  }

  @PostMapping("/properties/{id}/hide")
  public ResponseEntity<PropertyHideResponseDto> hideProperty(
      @PathVariable("id") String id,
      @RequestBody(required = false) PropertyBatchActionRequest body) {
    validatePropertyId(id);
    boolean success = processBatchAction(id, body, listingStorage::hideProperty);
    if (!success) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(PropertyHideResponseDto.of(true, true, id));
  }

  @PostMapping("/properties/{id}/restore")
  public ResponseEntity<PropertyHideResponseDto> restoreProperty(
      @PathVariable("id") String id,
      @RequestBody(required = false) PropertyBatchActionRequest body) {
    validatePropertyId(id);
    boolean success = processBatchAction(id, body, listingStorage::restoreProperty);
    if (!success) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(PropertyHideResponseDto.of(true, false, id));
  }

  private void validatePropertyId(String id) {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Az azonosító nem lehet üres!");
    }
    if (id.length() > 500) {
      throw new IllegalArgumentException("Az azonosító túl hosszú!");
    }
  }

  private boolean processBatchAction(
      String id, PropertyBatchActionRequest body, Predicate<String> action) {
    boolean success = action.test(id);
    if (body != null) {
      if (body.url() != null && !body.url().isBlank()) {
        success = action.test(body.url()) || success;
      }
      if (body.urls() != null) {
        for (String item : body.urls()) {
          if (item != null && !item.isBlank()) {
            success = action.test(item) || success;
          }
        }
      }
    }
    return success;
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
    SearchCriteria effectiveCriteria = criteria != null ? criteria : new SearchCriteria();
    effectiveCriteria.validate();
    DashboardResponseDto result = dashboardPresentationService.executeSearch(effectiveCriteria);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/open-urls")
  public ResponseEntity<OpenUrlsResponseDto> openUrls(
      @RequestBody(required = false) List<String> urls) {
    if (urls != null && !urls.isEmpty()) {
      for (String url : urls) {
        listingStorage.markAsViewed(url);
      }
    }
    return ResponseEntity.ok(OpenUrlsResponseDto.of(true, urls != null ? urls.size() : 0));
  }

  @PostMapping("/items/viewed")
  public ResponseEntity<MarkViewedResponseDto> markViewed(
      @RequestBody(required = false) MarkViewedRequest body) {
    String url = body != null ? body.url() : null;
    boolean marked = false;
    if (url != null && !url.isBlank()) {
      marked = listingStorage.markAsViewed(url);
    }
    return ResponseEntity.ok(MarkViewedResponseDto.of(true, marked, url != null ? url : ""));
  }

  @PostMapping("/history/clear")
  public ResponseEntity<ClearHistoryResponseDto> clearHistory(
      @RequestBody(required = false) ClearHistoryRequest body,
      @RequestParam(value = "city", required = false) String paramCity) {
    String city = paramCity;
    if ((city == null || city.isBlank()) && body != null && body.city() != null) {
      city = body.city();
    }

    if (city != null && !city.isBlank()) {
      listingStorage.clearCityHistory(city);
      return ResponseEntity.ok(ClearHistoryResponseDto.forCity(city));
    } else {
      listingStorage.clearHistory();
      return ResponseEntity.ok(ClearHistoryResponseDto.forAll());
    }
  }
}
