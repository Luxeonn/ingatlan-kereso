package ingatlanok;

import ingatlanok.web.WebDashboardHtml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebDashboardHtmlSortTest {

    @Test
    void testHtmlContainsPriceSortControlsAndStyles() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains(".sort-box"), "HTML-nek tartalmaznia kell a .sort-box stílust");
        assertTrue(html.contains(".sort-select"), "HTML-nek tartalmaznia kell a .sort-select stílust");

        assertTrue(html.contains("id=\"sort-select\""), "HTML-nek tartalmaznia kell a sort-select elemet");
        assertTrue(html.contains("onchange=\"changeSort(this.value)\""), "sort-select-nek hívnia kell a changeSort függvényt");
        assertTrue(html.contains("value=\"default\""), "Tartalmaznia kell az alapértelmezett rendezési opciót");
        assertTrue(html.contains("value=\"price-asc\""), "Tartalmaznia kell az ár szerint növekvő rendezési opciót");
        assertTrue(html.contains("value=\"price-desc\""), "Tartalmaznia kell az ár szerint csökkenő rendezési opciót");

        assertTrue(html.contains("let currentSort = 'default';"), "JavaScriptben szerepelnie kell a currentSort változónak");
        assertTrue(html.contains("function changeSort("), "JavaScriptben szerepelnie kell a changeSort függvénynek");
        assertTrue(html.contains("function parsePrice("), "JavaScriptben szerepelnie kell a parsePrice függvénynek");
        assertTrue(html.contains("currentSort === 'price-asc'"), "renderListings-ben szerepelnie kell a növekvő rendezésnek");
        assertTrue(html.contains("currentSort === 'price-desc'"), "renderListings-ben szerepelnie kell a csökkenő rendezésnek");
    }

    @Test
    void testSortResetAndNullPriceHandlingInScript() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains("if (pA === null && pB === null) return 0;"), "Null árak kezelése: ha mindkettő null, egyenlő");
        assertTrue(html.contains("if (pA === null) return 1;"), "Null árak kezelése: ha A null, a végére kerül");
        assertTrue(html.contains("if (pB === null) return -1;"), "Null árak kezelése: ha B null, a végére kerül");

        assertTrue(html.contains("currentSort = 'default';"), "Keresés indításakor és törléskor a rendezés visszaáll alapértelmezettre");
        assertTrue(html.contains("sortSelect.value = 'default';"), "A select elem értéke is visszaáll alapértelmezettre");
    }
}
