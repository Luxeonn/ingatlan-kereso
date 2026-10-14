package ingatlanok.web.controller;

import ingatlanok.common.AppLogger;
import ingatlanok.common.BrowserUtils;
import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.presentation.DashboardPresentationService;
import ingatlanok.presentation.DashboardResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Spring Boot REST Controller handling all Ingatlan dashboard API endpoints.
 */
@RestController
@RequestMapping("/api")
public class IngatlanApiController {

    private final ServletWebServerApplicationContext webServerAppCtxt;

    @Value("${server.port:8080}")
    private int configuredPort;

    public IngatlanApiController(ServletWebServerApplicationContext webServerAppCtxt) {
        this.webServerAppCtxt = webServerAppCtxt;
    }

    private int resolvePort() {
        return (webServerAppCtxt != null && webServerAppCtxt.getWebServer() != null)
                ? webServerAppCtxt.getWebServer().getPort()
                : configuredPort;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("port", resolvePort());
        status.put("totalInDb", IngatlanRepository.getInstance().getTotalCount());
        status.put("lastRunTime", IngatlanRepository.getInstance().getLastRunTime());
        status.put("trackedCities", IngatlanRepository.getInstance().getTrackedCitiesInfo());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/portals")
    public ResponseEntity<List<PortalDescriptor>> getPortals() {
        return ResponseEntity.ok(PortalEngineRegistry.getInstance().getAllDescriptors());
    }

    @GetMapping("/cities")
    public ResponseEntity<Map<String, Object>> getCities() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("predefined", CityUtils.getPredefinedCities());
        data.put("presets", CityUtils.getPresets());
        data.put("trackedCities", IngatlanRepository.getInstance().getTrackedCitiesInfo());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/config")
    public ResponseEntity<SearchCriteria> getConfig() {
        SearchCriteria sc = IngatlanRepository.getInstance().getLastCriteria();
        return ResponseEntity.ok(sc);
    }

    @PostMapping("/config")
    public ResponseEntity<Map<String, Object>> saveConfig(@RequestBody(required = false) SearchCriteria sc) {
        if (sc != null) {
            IngatlanRepository.getInstance().saveLastCriteria(sc);
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/history")
    public ResponseEntity<List<IngatlanItem>> getHistory(@RequestParam(value = "city", required = false) String city) {
        List<IngatlanItem> items;
        if (city != null && !city.isBlank()) {
            items = IngatlanRepository.getInstance().getItemsByCity(city);
        } else {
            items = IngatlanRepository.getInstance().getAll();
        }
        return ResponseEntity.ok(items);
    }

    @PostMapping("/search")
    public ResponseEntity<DashboardResponseDto> search(@RequestBody(required = false) SearchCriteria criteria) {
        if (criteria == null) {
            criteria = new SearchCriteria();
        }
        DashboardResponseDto result = DashboardPresentationService.getInstance().executeSearch(criteria);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/open-urls")
    public ResponseEntity<Map<String, Object>> openUrls(@RequestBody(required = false) List<String> urls) {
        if (urls != null && !urls.isEmpty()) {
            for (String url : urls) {
                IngatlanRepository.getInstance().markAsViewed(url);
            }
            boolean forceServerOpen = "true".equalsIgnoreCase(System.getProperty("server.open.desktop", "false"));
            if (forceServerOpen && !java.awt.GraphicsEnvironment.isHeadless()) {
                BrowserUtils.openUrls(new LinkedHashSet<>(urls));
            }
        }
        return ResponseEntity.ok(Map.of("success", true, "count", urls != null ? urls.size() : 0));
    }

    @PostMapping("/items/viewed")
    public ResponseEntity<Map<String, Object>> markViewed(@RequestBody(required = false) Map<String, Object> body) {
        String url = (body != null && body.get("url") != null) ? body.get("url").toString() : null;
        boolean marked = false;
        if (url != null && !url.isBlank()) {
            marked = IngatlanRepository.getInstance().markAsViewed(url);
        }
        return ResponseEntity.ok(Map.of("success", true, "marked", marked, "url", url != null ? url : ""));
    }

    @PostMapping("/history/clear")
    public ResponseEntity<Map<String, Object>> clearHistory(
            @RequestParam(value = "city", required = false) String queryCity,
            @RequestBody(required = false) Map<String, Object> body) {
        String cityParam = (queryCity != null && !queryCity.isBlank()) ? queryCity : null;
        if (cityParam == null && body != null && body.get("city") != null) {
            cityParam = body.get("city").toString();
        }

        if (cityParam != null && !cityParam.isBlank()) {
            IngatlanRepository.getInstance().clearCityHistory(cityParam);
            return ResponseEntity.ok(Map.of("success", true, "clearedCity", cityParam));
        } else {
            IngatlanRepository.getInstance().clearHistory();
            return ResponseEntity.ok(Map.of("success", true, "clearedAll", true));
        }
    }

    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getLogs(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("logs", AppLogger.getRecentLogs(limit));
        data.put("logFilePath", AppLogger.getLogFilePath() != null ? AppLogger.getLogFilePath().toString() : "");
        data.put("level", AppLogger.getGlobalLevel().name());
        return ResponseEntity.ok(data);
    }

    @PostMapping("/logs/clear")
    public ResponseEntity<Map<String, Object>> clearLogs() {
        AppLogger.clearRecentLogs();
        return ResponseEntity.ok(Map.of("success", true, "message", "Naplók sikeresen kiürítve"));
    }
}
