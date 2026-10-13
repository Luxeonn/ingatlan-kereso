package ingatlanok;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchCriteriaFilterTest {

    @Test
    void testPriceRangeFilteringRejectsOutOfRange() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Hajdúszoboszló");
        criteria.setMinPriceMillion(100);
        criteria.setMaxPriceMillion(120);

        IngatlanItem itemTooLow = createItem("1", "https://dh.hu/ingatlan/1", "Ház", "8 900 000 Ft", 8.9, "Hajdúszoboszló");
        assertFalse(criteria.matches(itemTooLow), "8.9 M Ft árú ingatlant ki kell szűrni, ha a min ár 100 M Ft");

        IngatlanItem item37M = createItem("2", "https://dh.hu/ingatlan/2", "Ház", "37 000 000 Ft", 37.0, "Hajdúszoboszló");
        assertFalse(criteria.matches(item37M), "37 M Ft árú ingatlant ki kell szűrni a 100-120 M Ft sávból");

        IngatlanItem itemTooHigh = createItem("3", "https://dh.hu/ingatlan/3", "Ház", "134 000 000 Ft", 134.0, "Hajdúszoboszló");
        assertFalse(criteria.matches(itemTooHigh), "134 M Ft árú ingatlant ki kell szűrni, ha a max ár 120 M Ft");

        IngatlanItem item195M = createItem("4", "https://dh.hu/ingatlan/4", "Ház", "195 000 000 Ft", 195.0, "Hajdúszoboszló");
        assertFalse(criteria.matches(item195M), "195 M Ft árú ingatlant ki kell szűrni a 100-120 M Ft sávból");

        IngatlanItem item105M = createItem("5", "https://dh.hu/ingatlan/5", "Ház", "105 000 000 Ft", 105.0, "Hajdúszoboszló");
        assertTrue(criteria.matches(item105M), "105 M Ft árú ingatlannak át kell mennie a szűrésen");

        IngatlanItem item104_9M = createItem("6", "https://www.oc.hu/ingatlanok/U0050375", "Ház", "104,9M Ft", 104.9, "Hajdúszoboszló");
        assertTrue(criteria.matches(item104_9M), "104.9 M Ft árú ingatlannak át kell mennie a szűrésen");

        IngatlanItem item112_5M = createItem("7", "https://www.oc.hu/ingatlanok/H520605", "Ház", "112,5M Ft", 112.5, "Hajdúszoboszló");
        assertTrue(criteria.matches(item112_5M), "112.5 M Ft árú ingatlannak át kell mennie a szűrésen");
    }

    @Test
    void testPriceRangeFilteringWithoutPrice() {
        SearchCriteria criteriaWithRange = new SearchCriteria();
        criteriaWithRange.setMinPriceMillion(50);
        criteriaWithRange.setMaxPriceMillion(100);

        IngatlanItem itemNoPrice = createItem("8", "https://dh.hu/ingatlan/8", "Ház", "Ár a hirdetésben", null, "Debrecen");
        assertFalse(criteriaWithRange.matches(itemNoPrice), "Explicit ársáv megadásakor az ismeretlen árú hirdetést ki kell szűrni");

        SearchCriteria criteriaNoRange = new SearchCriteria();
        criteriaNoRange.setCity("Debrecen");
        criteriaNoRange.setMinPriceMillion(null);
        criteriaNoRange.setMaxPriceMillion(null);
        criteriaNoRange.setMinArea(null);
        criteriaNoRange.setMaxArea(null);
        assertTrue(criteriaNoRange.matches(itemNoPrice), "Ársáv hiányában az ár nélküli hirdetés is megengedett");
    }

    @Test
    void testFloorAreaFiltering() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setMinPriceMillion(10);
        criteria.setMaxPriceMillion(200);
        criteria.setMinArea(90);
        criteria.setMaxArea(200);

        IngatlanItem itemSmall = createItemWithArea("1", "https://dh.hu/1", "Kicsi", 100.0, 60.0);
        assertFalse(criteria.matches(itemSmall));

        IngatlanItem itemLarge = createItemWithArea("2", "https://dh.hu/2", "Nagy", 100.0, 250.0);
        assertFalse(criteria.matches(itemLarge));

        IngatlanItem itemMatch = createItemWithArea("3", "https://dh.hu/3", "Megfelelő", 100.0, 120.0);
        assertTrue(criteria.matches(itemMatch));
    }

    @Test
    void testExcludeViewedFiltering() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setMinPriceMillion(10);
        criteria.setMaxPriceMillion(200);

        IngatlanItem itemNotViewed = createItem("10", "https://dh.hu/10", "Ház", "50 000 000 Ft", 50.0, "Debrecen");
        itemNotViewed.setViewed(false);

        IngatlanItem itemViewed = createItem("11", "https://dh.hu/11", "Ház", "60 000 000 Ft", 60.0, "Debrecen");
        itemViewed.setViewed(true);

        assertTrue(criteria.matches(itemNotViewed));
        assertTrue(criteria.matches(itemViewed));

        criteria.setExcludeViewed(true);
        assertTrue(criteria.matches(itemNotViewed));
        assertFalse(criteria.matches(itemViewed));

        SearchCriteria cloned = criteria.cloneForCity("Debrecen");
        assertTrue(cloned.isExcludeViewed());
        assertFalse(cloned.matches(itemViewed));
    }

    @Test
    void testLocationFilteringRejectsWrongCities() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Hajdúszoboszló");
        criteria.setMinPriceMillion(100);
        criteria.setMaxPriceMillion(120);

        IngatlanItem itemKisnana = createItem(
                "HZ070659",
                "https://dh.hu/ingatlan/HZ070659/elado-haz-heves-megye-kisnana-falu",
                "3264 Kisnána HZ070659",
                "105 000 000 Ft",
                105.0,
                "Kisnána"
        );
        assertFalse(criteria.matches(itemKisnana), "Kisnánai hirdetést ki kell szűrni Hajdúszoboszló keresésnél");

        IngatlanItem itemCegled = createItem(
                "HZ069153",
                "https://dh.hu/ingatlan/HZ069153/elado-haz-pest-megye-cegled-oroszlan-utca",
                "2700 Cegléd HZ069153",
                "105 000 000 Ft",
                105.0,
                "Cegléd"
        );
        assertFalse(criteria.matches(itemCegled), "Ceglédi hirdetést ki kell szűrni Hajdúszoboszló keresésnél");

        IngatlanItem itemPecs = createItem(
                "HZ071728",
                "https://dh.hu/ingatlan/HZ071728/elado-haz-baranya-megye-pecs-patacs",
                "7634 Pécs HZ071728",
                "105 000 000 Ft",
                105.0,
                "Pécs"
        );
        assertFalse(criteria.matches(itemPecs), "Pécsi hirdetést ki kell szűrni Hajdúszoboszló keresésnél");

        IngatlanItem itemBudapest = createItem(
                "H525239",
                "https://www.oc.hu/ingatlanok/H525239",
                "Budapest XII. kerületi eladó lakás",
                "105 000 000 Ft",
                105.0,
                "Budapest XII. kerület"
        );
        assertFalse(criteria.matches(itemBudapest), "Budapesti hirdetést ki kell szűrni Hajdúszoboszló keresésnél");

        IngatlanItem itemSzoboszlo = createItem(
                "M339414",
                "https://dh.hu/ingatlan/M339414/elado-haz-hajdu-bihar-megye-hajduszoboszlo-4200-hajduszoboszlo,-kertvaros",
                "4200 Hajdúszoboszló Kertváros",
                "105 000 000 Ft",
                105.0,
                "Hajdúszoboszló"
        );
        assertTrue(criteria.matches(itemSzoboszlo), "Valódi hajdúszoboszlói ingatlannak át kell mennie a szűrésen");

        IngatlanItem itemSzoboszloKertvaros = createItem(
                "U0050375",
                "https://www.oc.hu/ingatlanok/U0050375",
                "Eladó ház Hajdúszoboszló Kertváros",
                "104,9M Ft",
                104.9,
                "Hajdúszoboszló, Kertváros"
        );
        assertTrue(criteria.matches(itemSzoboszloKertvaros), "Hajdúszoboszló városrészi ingatlannak át kell mennie a szűrésen");
    }

    private IngatlanItem createItem(String id, String url, String title, String priceRaw, Double priceMillion, String location) {
        Long priceHuf = priceMillion != null ? Math.round(priceMillion * 1_000_000.0) : null;
        PriceInfo priceInfo = new PriceInfo(priceRaw, priceHuf, priceMillion);
        PropertyDetails detailsInfo = new PropertyDetails("", 100.0, 3.0, null);
        IngatlanItem item = new IngatlanItem(
                id,
                PortalSource.of("Duna House"),
                url,
                title,
                priceInfo,
                location,
                detailsInfo,
                "2026-10-13 17:00:00",
                true
        );
        item.setLocation(location);
        return item;
    }

    private IngatlanItem createItemWithArea(String id, String url, String title, Double priceMillion, Double area) {
        Long priceHuf = priceMillion != null ? Math.round(priceMillion * 1_000_000.0) : null;
        PriceInfo priceInfo = new PriceInfo(priceMillion + " M Ft", priceHuf, priceMillion);
        PropertyDetails detailsInfo = new PropertyDetails(area + " m²", area, 3.0, null);
        return new IngatlanItem(
                id,
                PortalSource.of("Duna House"),
                url,
                title,
                priceInfo,
                "Debrecen",
                detailsInfo,
                "2026-10-13 17:00:00",
                true
        );
    }
}
