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
}
