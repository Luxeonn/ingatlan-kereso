package ingatlanok.service;

import ingatlanok.common.BrowserUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.scraper.IngatlanScraper;
import ingatlanok.scraper.PortalScrapeResult;
import ingatlanok.scraper.ScraperRegistry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class IngatlanSearchService {

    private static final IngatlanSearchService INSTANCE = new IngatlanSearchService();

    private IngatlanSearchService() {
    }

    public static IngatlanSearchService getInstance() {
        return INSTANCE;
    }

    public SearchResult executeSearch(SearchCriteria criteria) {
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        List<String> citiesToSearch = criteria.getCities();
        if (citiesToSearch.isEmpty()) {
            citiesToSearch = List.of("Debrecen");
        }

        System.out.println("==================================================");
        System.out.println("Keresés indítása: Városok=" + citiesToSearch + ", Jelleg=" + criteria.getPropertyType());
        System.out.println("Aktív portálok: " + criteria.getPortals());
        System.out.println("==================================================");

        IngatlanRepository repository = IngatlanRepository.getInstance();
        SearchResult result = new SearchResult();

        List<IngatlanItem> allAggregatedItems = new ArrayList<>();
        Map<String, Integer> portalCounts = new LinkedHashMap<>();
        portalCounts.put("Duna House", 0);
        portalCounts.put("Otthon Centrum", 0);
        portalCounts.put("Jófogás Ingatlan", 0);
        portalCounts.put("Ingatlan.com", 0);
        portalCounts.put("Ingatlanbazár", 0);

        Map<String, Integer> cityCounts = new LinkedHashMap<>();

        int cityIndex = 0;
        for (String currentCity : citiesToSearch) {
            cityIndex++;
            if (cityIndex > 1) {

                try {
                    Thread.sleep(400);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            boolean isFirstSearch = !repository.hasCityDatabase(currentCity);
            if (isFirstSearch) {
                result.getNewCitiesInitialized().add(currentCity);
                System.out.println("\n>>> [Új Város]: \"" + currentCity + "\" még nem szerepel az adatbázisban.");
                System.out.println(">>> Első keresés: az aktuális találatok ALAPÁLLAPOTKÉNT (baseline snapshot) kerülnek rögzítésre.");
            } else {
                System.out.println("\n>>> [Ismert Város]: \"" + currentCity + "\" adatbázisa létezik. Differenciál-keresés...");
            }

            SearchCriteria cityCriteria = criteria.cloneForCity(currentCity);
            List<IngatlanItem> cityFoundItems = new ArrayList<>();

            List<IngatlanScraper> activeScrapers = ScraperRegistry.getInstance().getActiveScrapers(cityCriteria);
            for (IngatlanScraper scraper : activeScrapers) {
                try {
                    System.out.println("[" + cityIndex + "/" + citiesToSearch.size() + "] " + scraper.getName() + " lekérdezése (" + currentCity + ")...");
                    PortalScrapeResult scrapeResult = scraper.scrape(cityCriteria);
                    result.getPortalResults().add(scrapeResult);
                    List<IngatlanItem> items = scrapeResult.getItems();
                    cityFoundItems.addAll(items);
                    portalCounts.put(scraper.getName(), portalCounts.getOrDefault(scraper.getName(), 0) + items.size());
                } catch (Exception e) {
                    System.err.println("Hiba a(z) " + scraper.getName() + " keresésekor (" + currentCity + "): " + e.getMessage());
                }
            }

            Map<String, IngatlanItem> uniqueCityMap = new LinkedHashMap<>();
            for (IngatlanItem item : cityFoundItems) {

                if (!cityCriteria.matches(item)) {
                    System.out.println("[Kiszűrve] " + item.getPortal() + " találat: " + item.getTitle()
                            + " (Ár: " + item.getPrice() + ", Lokáció: " + item.getLocation()
                            + ", Link: " + item.getUrl() + ") nem felel meg a keresési feltételeknek ("
                            + currentCity + ", "
                            + (cityCriteria.getMinPriceMillion() != null ? cityCriteria.getMinPriceMillion() : "-") + " - "
                            + (cityCriteria.getMaxPriceMillion() != null ? cityCriteria.getMaxPriceMillion() : "-") + " M Ft)");
                    continue;
                }

                if (item.getCity() == null || item.getCity().isBlank()) {
                    item.setCity(currentCity);
                }
                if (item.getLocation() == null || item.getLocation().isBlank()) {
                    item.setLocation(currentCity);
                }

                if (isFirstSearch) {

                    item.setNew(false);
                } else {

                    boolean isNew = repository.isNewUrl(currentCity, item.getUrl());
                    item.setNew(isNew);
                }

                uniqueCityMap.putIfAbsent(item.getUrl(), item);
            }

            List<IngatlanItem> finalCityItems = new ArrayList<>(uniqueCityMap.values());

            repository.saveCityItems(currentCity, finalCityItems);

            cityCounts.put(currentCity, finalCityItems.size());
            allAggregatedItems.addAll(finalCityItems);
        }

        Map<String, IngatlanItem> uniqueAggregatedMap = new LinkedHashMap<>();
        for (IngatlanItem item : allAggregatedItems) {
            uniqueAggregatedMap.putIfAbsent(item.getUrl(), item);
        }
        List<IngatlanItem> uniqueAggregatedList = new ArrayList<>(uniqueAggregatedMap.values());

        List<IngatlanItem> newItems = uniqueAggregatedList.stream()
                .filter(IngatlanItem::isNew)
                .collect(Collectors.toList());

        repository.saveLastCriteria(criteria);

        if (criteria.isAutoOpenBrowser() && !newItems.isEmpty()) {
            Set<String> urlsToOpen = newItems.stream().map(IngatlanItem::getUrl).collect(Collectors.toCollection(LinkedHashSet::new));
            BrowserUtils.openUrls(urlsToOpen);
        }

        uniqueAggregatedList.sort((a, b) -> {
            if (a.isNew() != b.isNew()) {
                return Boolean.compare(!a.isNew(), !b.isNew());
            }
            if (a.getDiscoveredAt() == null) return 1;
            if (b.getDiscoveredAt() == null) return -1;
            return b.getDiscoveredAt().compareTo(a.getDiscoveredAt());
        });

        result.setItems(uniqueAggregatedList);
        result.setNewCount(newItems.size());
        result.setTotalFound(uniqueAggregatedList.size());
        result.setExecutedAt(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
        result.setPortalCounts(portalCounts);
        result.setCityCounts(cityCounts);

        System.out.println("\n==================================================");
        System.out.println("Keresés befejeződött: " + result.getExecutedAt());
        System.out.println("Összes találat: " + result.getTotalFound() + " | Új: " + result.getNewCount());
        System.out.println("Városok szerint: " + result.getCityCounts());
        if (!result.getNewCitiesInitialized().isEmpty()) {
            System.out.println("Új város(ok) baseline mentve: " + result.getNewCitiesInitialized());
        }
        System.out.println("==================================================");

        return result;
    }
}
