package ingatlanok;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;
import ingatlanok.scraper.DunaHouseScraper;
import ingatlanok.scraper.IngatlanComScraper;
import ingatlanok.scraper.JofogasScraper;
import ingatlanok.scraper.OtthonCentrumScraper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PropertyTypeUrlAndParsingTest {

    private final DunaHouseScraper dhScraper = new DunaHouseScraper();
    private final OtthonCentrumScraper ocScraper = new OtthonCentrumScraper();
    private final JofogasScraper jfScraper = new JofogasScraper();
    private final IngatlanComScraper icScraper = new IngatlanComScraper();

    @ParameterizedTest
    @ValueSource(strings = {"lakas", "haz", "garazs", "telek", "nyaralo", "iroda", "osszes"})
    void testAllPropertyTypesUrlGenerationDunaHouse(String type) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Hajdúszoboszló");
        criteria.setPropertyType(type);
        criteria.setMinPriceMillion(100);
        criteria.setMaxPriceMillion(120);

        String url = dhScraper.buildSearchUrl(criteria);
        assertNotNull(url);
        assertTrue(url.startsWith("https://dh.hu/elado-ingatlan/"), "DH URL prefix hibás: " + url);
        assertTrue(url.contains("hajdu-bihar/hajduszoboszlo"), "DH URL-nek tartalmaznia kell a megye/város slugot: " + url);
        assertTrue(url.contains("/100-120-mFt"), "DH URL-nek tartalmaznia kell az ársávot: " + url);

        String expectedTypeSlug = switch (type) {
            case "haz" -> "haz";
            case "garazs" -> "garazs";
            case "telek" -> "telek";
            case "nyaralo" -> "nyaralo";
            case "iroda" -> "iroda";
            case "osszes" -> "lakas-haz";
            default -> "lakas";
        };
        assertTrue(url.contains("/" + expectedTypeSlug + "/"), "DH URL-nek tartalmaznia kell a típust (" + expectedTypeSlug + "): " + url);
    }

    @ParameterizedTest
    @ValueSource(strings = {"lakas", "haz", "garazs", "telek", "nyaralo", "iroda", "osszes"})
    void testAllPropertyTypesUrlGenerationOtthonCentrum(String type) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Hajdúszoboszló");
        criteria.setPropertyType(type);

        String url = ocScraper.buildSearchUrl(criteria);
        assertNotNull(url);
        assertTrue(url.startsWith("https://www.oc.hu/elado/"), "OC URL prefix hibás: " + url);
        assertTrue(url.contains("hajduszoboszlo"), "OC URL-nek tartalmaznia kell a város slugot: " + url);

        String expectedType = switch (type) {
            case "haz" -> "haz";
            case "nyaralo" -> "nyaralo";
            case "telek" -> "telek";
            case "garazs" -> "garazs";
            case "iroda" -> "uzleticelu";
            case "osszes" -> "lakas";
            default -> "lakas";
        };
        assertTrue(url.contains("/" + expectedType + "/"), "OC URL-nek tartalmaznia kell a típust (" + expectedType + "): " + url);
    }

    @ParameterizedTest
    @ValueSource(strings = {"lakas", "haz", "garazs", "telek", "nyaralo", "iroda", "osszes"})
    void testAllPropertyTypesUrlGenerationJofogas(String type) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Hajdúszoboszló");
        criteria.setPropertyType(type);
        criteria.setMinPriceMillion(100);
        criteria.setMaxPriceMillion(120);

        String url = jfScraper.buildSearchUrl(criteria);
        assertNotNull(url);
        assertTrue(url.startsWith("https://ingatlan.jofogas.hu/magyarorszag/ingatlan?q="), "Jófogás URL prefix hibás: " + url);
        assertTrue(url.contains("min_price=100000000"), "Jófogás URL-nek tartalmaznia kell a min árat: " + url);
        assertTrue(url.contains("max_price=120000000"), "Jófogás URL-nek tartalmaznia kell a max árat: " + url);

        String expectedTypeWord = switch (type) {
            case "haz" -> "haz";
            case "garazs" -> "garazs";
            case "telek" -> "telek";
            case "nyaralo" -> "nyaralo";
            case "iroda" -> "iroda";
            case "osszes" -> "ingatlan";
            default -> "lakas";
        };
        assertTrue(url.contains(expectedTypeWord), "Jófogás URL-nek tartalmaznia kell a típusszót (" + expectedTypeWord + "): " + url);
    }

    @ParameterizedTest
    @ValueSource(strings = {"lakas", "haz", "garazs", "telek", "nyaralo", "iroda", "osszes"})
    void testAllPropertyTypesUrlGenerationIngatlanCom(String type) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Hajdúszoboszló");
        criteria.setPropertyType(type);
        criteria.setMinPriceMillion(100);
        criteria.setMaxPriceMillion(120);

        String url = icScraper.buildSearchUrl(criteria);
        assertNotNull(url);
        assertTrue(url.startsWith("https://ingatlan.com/lista/elado+"), "Ingatlan.com URL prefix hibás: " + url);
        assertTrue(url.contains("+hajduszoboszlo"), "Ingatlan.com URL-nek tartalmaznia kell a várost: " + url);
        assertTrue(url.contains("+100-120-mFt"), "Ingatlan.com URL-nek tartalmaznia kell az ársávot: " + url);
    }

    @Test
    void testProcessingCardsForAllTypes() {

        String cardLakas = "4025 Debrecen Belváros | 2 szoba | 55 m² 45 000 000 Ft";
        PriceInfo priceLakas = PropertyParser.parsePrice(cardLakas);
        PropertyDetails detailsLakas = PropertyParser.parseDetails(cardLakas, priceLakas);
        assertEquals(45_000_000L, priceLakas.getAmountHuf());
        assertEquals(45.0, priceLakas.getAmountMillion());
        assertEquals(55.0, detailsLakas.getAreaSquareMeters());
        assertEquals(2.0, detailsLakas.getRoomCount());

        String cardHaz = "Prémium 4200 Hajdúszoboszló Kertváros | 4 szoba | 120 m² 105 000 000 Ft";
        PriceInfo priceHaz = PropertyParser.parsePrice(cardHaz);
        PropertyDetails detailsHaz = PropertyParser.parseDetails(cardHaz, priceHaz);
        assertEquals(105_000_000L, priceHaz.getAmountHuf());
        assertEquals(105.0, priceHaz.getAmountMillion());
        assertEquals(120.0, detailsHaz.getAreaSquareMeters());
        assertEquals(4.0, detailsHaz.getRoomCount());

        String cardGarazs = "Debrecen Egyetemváros eladó garázs 18 m² 6,5 M Ft";
        PriceInfo priceGarazs = PropertyParser.parsePrice(cardGarazs);
        PropertyDetails detailsGarazs = PropertyParser.parseDetails(cardGarazs, priceGarazs);
        assertEquals(6_500_000L, priceGarazs.getAmountHuf());
        assertEquals(6.5, priceGarazs.getAmountMillion());
        assertEquals(18.0, detailsGarazs.getAreaSquareMeters());

        String cardTelek = "Hajdúszoboszló belterületi építési telek 800 m² 22 000 000 Ft";
        PriceInfo priceTelek = PropertyParser.parsePrice(cardTelek);
        PropertyDetails detailsTelek = PropertyParser.parseDetails(cardTelek, priceTelek);
        assertEquals(22_000_000L, priceTelek.getAmountHuf());
        assertEquals(22.0, priceTelek.getAmountMillion());
        assertEquals(800.0, detailsTelek.getAreaSquareMeters());

        String cardNyaralo = "Balatonföldvár üdülőövezet 60 m² • 2 szoba 35,9M Ft";
        PriceInfo priceNyaralo = PropertyParser.parsePrice(cardNyaralo);
        PropertyDetails detailsNyaralo = PropertyParser.parseDetails(cardNyaralo, priceNyaralo);
        assertEquals(35_900_000L, priceNyaralo.getAmountHuf());
        assertEquals(35.9, priceNyaralo.getAmountMillion());
        assertEquals(60.0, detailsNyaralo.getAreaSquareMeters());
        assertEquals(2.0, detailsNyaralo.getRoomCount());

        String cardIroda = "Szeged Belvárosi Iroda 110 m² | 3 szoba 75 000 000 Ft";
        PriceInfo priceIroda = PropertyParser.parsePrice(cardIroda);
        PropertyDetails detailsIroda = PropertyParser.parseDetails(cardIroda, priceIroda);
        assertEquals(75_000_000L, priceIroda.getAmountHuf());
        assertEquals(75.0, priceIroda.getAmountMillion());
        assertEquals(110.0, detailsIroda.getAreaSquareMeters());
        assertEquals(3.0, detailsIroda.getRoomCount());
    }

    @Test
    void testBudapestSpecialCountyRouteHandling() {
        SearchCriteria criteriaBp = new SearchCriteria();
        criteriaBp.setCity("Budapest");
        criteriaBp.setPropertyType("lakas");

        String url = dhScraper.buildSearchUrl(criteriaBp);
        assertTrue(url.contains("/lakas/budapest"), "Budapest esetén a DH URL-nek közvetlenül /budapest formátumúnak kell lennie: " + url);
    }
}
