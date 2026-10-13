package ingatlanok.service;

import ingatlanok.common.BrowserUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.scraper.DunaHouseScraper;
import ingatlanok.scraper.IngatlanComScraper;
import ingatlanok.scraper.JofogasScraper;
import ingatlanok.scraper.OtthonCentrumScraper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Központi keresési szolgáltatás, amely vezérli az egyes portálok lekérdezését
 * több kiválasztott városra, szegmentált adatbázis-kezeléssel és aggregációval.
 */
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
        Map<String, Integer> portalCounts = new HashMap<>();
        portalCounts.put("Duna House", 0);
        portalCounts.put("Otthon Centrum", 0);
        portalCounts.put("Jófogás Ingatlan", 0);
        portalCounts.put("Ingatlan.com", 0);

        Map<String, Integer> cityCounts = new LinkedHashMap<>();

        int cityIndex = 0;
        for (String currentCity : citiesToSearch) {
            cityIndex++;
            if (cityIndex > 1) {
                // Rövid szünet a városok közötti kérésekben a kíméletes portálhívásért
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

            // 1. Duna House
            if (cityCriteria.isPortalEnabled("dh")) {
                try {
                    System.out.println("[" + cityIndex + "/" + citiesToSearch.size() + "] Duna House lekérdezése (" + currentCity + ")...");
                    List<IngatlanItem> dhItems = new DunaHouseScraper().search(cityCriteria);
                    cityFoundItems.addAll(dhItems);
                    portalCounts.put("Duna House", portalCounts.get("Duna House") + dhItems.size());
                } catch (Exception e) {
                    System.err.println("Hiba a Duna House keresésekor (" + currentCity + "): " + e.getMessage());
                }
            }

            // 2. Otthon Centrum
            if (cityCriteria.isPortalEnabled("oc")) {
                try {
                    System.out.println("[" + cityIndex + "/" + citiesToSearch.size() + "] Otthon Centrum lekérdezése (" + currentCity + ")...");
                    List<IngatlanItem> ocItems = new OtthonCentrumScraper().search(cityCriteria);
                    cityFoundItems.addAll(ocItems);
                    portalCounts.put("Otthon Centrum", portalCounts.get("Otthon Centrum") + ocItems.size());
                } catch (Exception e) {
                    System.err.println("Hiba az Otthon Centrum keresésekor (" + currentCity + "): " + e.getMessage());
                }
            }

            // 3. Jófogás Ingatlan
            if (cityCriteria.isPortalEnabled("jofogas")) {
                try {
                    System.out.println("[" + cityIndex + "/" + citiesToSearch.size() + "] Jófogás Ingatlan lekérdezése (" + currentCity + ")...");
                    List<IngatlanItem> jfItems = new JofogasScraper().search(cityCriteria);
                    cityFoundItems.addAll(jfItems);
                    portalCounts.put("Jófogás Ingatlan", portalCounts.get("Jófogás Ingatlan") + jfItems.size());
                } catch (Exception e) {
                    System.err.println("Hiba a Jófogás keresésekor (" + currentCity + "): " + e.getMessage());
                }
            }

            // 4. Ingatlan.com
            if (cityCriteria.isPortalEnabled("ingatlancom")) {
                try {
                    System.out.println("[" + cityIndex + "/" + citiesToSearch.size() + "] Ingatlan.com lekérdezése (" + currentCity + ")...");
                    List<IngatlanItem> icItems = new IngatlanComScraper().search(cityCriteria);
                    cityFoundItems.addAll(icItems);
                    portalCounts.put("Ingatlan.com", portalCounts.get("Ingatlan.com") + icItems.size());
                } catch (Exception e) {
                    System.err.println("Hiba az Ingatlan.com keresésekor (" + currentCity + "): " + e.getMessage());
                }
            }

            // Duplikációk szűrése a városon belüli találati listán URL alapján és isNew kalkuláció
            Map<String, IngatlanItem> uniqueCityMap = new LinkedHashMap<>();
            for (IngatlanItem item : cityFoundItems) {
                item.setCity(currentCity);
                item.setLocation(currentCity);

                if (isFirstSearch) {
                    // Első futás új városra: alapállapot, minden elem nem új (isNew = false)
                    item.setNew(false);
                } else {
                    // Ismert város: valóban új-e az adatbázishoz képest
                    boolean isNew = repository.isNewUrl(currentCity, item.getUrl());
                    item.setNew(isNew);
                }

                uniqueCityMap.putIfAbsent(item.getUrl(), item);
            }

            List<IngatlanItem> finalCityItems = new ArrayList<>(uniqueCityMap.values());

            // Város adatbázisának azonnali mentése / frissítése
            repository.saveCityItems(currentCity, finalCityItems);

            cityCounts.put(currentCity, finalCityItems.size());
            allAggregatedItems.addAll(finalCityItems);
        }

        // Duplikátumok szűrése a teljes aggregált listán
        Map<String, IngatlanItem> uniqueAggregatedMap = new LinkedHashMap<>();
        for (IngatlanItem item : allAggregatedItems) {
            uniqueAggregatedMap.putIfAbsent(item.getUrl(), item);
        }
        List<IngatlanItem> uniqueAggregatedList = new ArrayList<>(uniqueAggregatedMap.values());

        // Új elemek összesítése
        List<IngatlanItem> newItems = uniqueAggregatedList.stream()
                .filter(IngatlanItem::isNew)
                .collect(Collectors.toList());

        // Mentjük az utolsó keresési konfigurációt
        repository.saveLastCriteria(criteria);

        // Opcionális automatikus böngészőfül nyitás ha kérte a felhasználó
        // Kizárólag a ténylegesen új hirdetéseket nyitja meg (így új város első futásakor semmit)
        if (criteria.isAutoOpenBrowser() && !newItems.isEmpty()) {
            Set<String> urlsToOpen = newItems.stream().map(IngatlanItem::getUrl).collect(Collectors.toCollection(LinkedHashSet::new));
            BrowserUtils.openUrls(urlsToOpen);
        }

        // Rendezés: új elemek legelöl, majd dátum szerint
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
