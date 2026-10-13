package ingatlanok;

import ingatlanok.web.WebDashboardHtml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebDashboardHtmlFilterTest {

    @Test
    void testHtmlContainsOnlyNewFilterButtonAndStyles() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains(".filter-only-new-btn"), "HTML-nek tartalmaznia kell a .filter-only-new-btn stílust");
        assertTrue(html.contains(".filter-only-new-btn.active"), "HTML-nek tartalmaznia kell az aktív állapot stílusát");

        assertTrue(html.contains("id=\"filter-only-new-btn\""), "HTML-nek tartalmaznia kell a filter-only-new-btn gombot");
        assertTrue(html.contains("id=\"only-new-count\""), "HTML-nek tartalmaznia kell az only-new-count azonosítójú számlálót");
        assertTrue(html.contains("onclick=\"toggleOnlyNewFilter()\""), "HTML gombnak meg kell hívnia a toggleOnlyNewFilter függvényt");

        assertTrue(html.contains("let onlyNewFilter = false;"), "JavaScriptben szerepelnie kell az onlyNewFilter változónak");
        assertTrue(html.contains("function toggleOnlyNewFilter()"), "JavaScriptben szerepelnie kell a toggleOnlyNewFilter függvénynek");
        assertTrue(html.contains("if (onlyNewFilter && !item.isNew)"), "renderListings-ben szerepelnie kell az isNew szűrési feltételnek");
        assertTrue(html.contains("Nincs új találat ebben a nézetben"), "Empty-state üzenetnek szerepelnie kell");
    }

    @Test
    void testHtmlContainsFloorAreaAndPriceStepperInputs() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains("id=\"min-area-input\""), "HTML-nek tartalmaznia kell a min-area-input mezőt");
        assertTrue(html.contains("id=\"max-area-input\""), "HTML-nek tartalmaznia kell a max-area-input mezőt");
        assertTrue(html.contains("value=\"90\""), "Min area alapértelmezett értéke 90 m²");
        assertTrue(html.contains("value=\"200\""), "Max area alapértelmezett értéke 200 m²");
        assertTrue(html.contains("value=\"70\""), "Min ár alapértelmezett értéke 70 M Ft");
        assertTrue(html.contains("value=\"120\""), "Max ár alapértelmezett értéke 120 M Ft");
        assertTrue(html.contains("minArea: minAreaVal ? parseInt(minAreaVal, 10) : null"), "Payload-ba be kell épülnie a minArea értéknek");
        assertTrue(html.contains("maxArea: maxAreaVal ? parseInt(maxAreaVal, 10) : null"), "Payload-ba be kell épülnie a maxArea értéknek");
    }

    @Test
    void testHtmlContainsViewedTrackingAndStyles() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains(".badge-viewed"));
        assertTrue(html.contains(".listing-card.is-viewed"));
        assertTrue(html.contains("id=\"exclude-viewed-toggle\""));
        assertTrue(html.contains("id=\"filter-not-viewed-btn\""));
        assertTrue(html.contains("id=\"not-viewed-count\""));
        assertTrue(html.contains("onclick=\"toggleNotViewedFilter()\""));
        assertTrue(html.contains("let notViewedFilter = false;"));
        assertTrue(html.contains("function toggleNotViewedFilter()"));
        assertTrue(html.contains("function markItemViewed("));
        assertTrue(html.contains("markItemViewed("));
    }

    @Test
    void testHtmlContainsDistinctCardVisualStates() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains(".listing-card.is-new"));
        assertTrue(html.contains(".listing-card.is-viewed"));
        assertTrue(html.contains(".listing-card.is-archive"));
        assertTrue(html.contains(".card-title.pulse-title"));
        assertTrue(html.contains("@keyframes titlePulse"));
        assertTrue(html.contains("const stateClass = isViewed ? 'is-viewed' : (isNew ? 'is-new' : 'is-archive');"));
        assertTrue(html.contains("const titlePulseClass = (isNew && !isViewed) ? 'pulse-title' : '';"));
    }

    @Test
    void testHtmlContainsExpandedQuickSearchLogic() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains("function buildSearchableString(item)"));
        assertTrue(html.contains("function itemMatchesQuickSearch(item, filterTerm)"));
        assertTrue(html.contains("item.priceInfo.amountMillion != null"));
        assertTrue(html.contains("item.detailsInfo.areaSquareMeters != null"));
        assertTrue(html.contains("item.detailsInfo.roomCount != null"));
        assertTrue(html.contains("tokens.every(token => {"));
        assertTrue(html.contains("itemMatchesQuickSearch(item, filterTerm)"));
        assertTrue(html.contains("updatePortalFilterCounts(filterTerm ? filtered : listToRender);"));
    }
}
