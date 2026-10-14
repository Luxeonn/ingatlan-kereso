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
        assertTrue(html.contains("value=\"default\""), "Tartalmaznia kell a legújabb elöl rendezési opciót");
        assertTrue(html.contains("value=\"price-asc\" selected"), "Tartalmaznia kell az ár szerint növekvő alapértelmezett opciót");
        assertTrue(html.contains("value=\"price-desc\""), "Tartalmaznia kell az ár szerint csökkenő rendezési opciót");
        assertTrue(html.contains("value=\"area-asc\""), "Tartalmaznia kell az alapterület szerint növekvő opciót");
        assertTrue(html.contains("value=\"area-desc\""), "Tartalmaznia kell az alapterület szerint csökkenő opciót");
        assertTrue(html.contains("value=\"sqm-price-asc\""), "Tartalmaznia kell a fajlagos négyzetméterár növekvő opciót");
        assertTrue(html.contains("value=\"sqm-price-desc\""), "Tartalmaznia kell a fajlagos négyzetméterár csökkenő opciót");

        assertTrue(html.contains("let currentSort = 'price-asc';"), "JavaScriptben szerepelnie kell a currentSort változónak");
        assertTrue(html.contains("function changeSort("), "JavaScriptben szerepelnie kell a changeSort függvénynek");
        assertTrue(html.contains("function parsePrice("), "JavaScriptben szerepelnie kell a parsePrice függvénynek");
        assertTrue(html.contains("function getItemArea("), "JavaScriptben szerepelnie kell a getItemArea függvénynek");
        assertTrue(html.contains("function getItemSqmPrice("), "JavaScriptben szerepelnie kell a getItemSqmPrice függvénynek");
        assertTrue(html.contains("currentSort === 'price-asc'"), "renderListings-ben szerepelnie kell a növekvő rendezésnek");
        assertTrue(html.contains("currentSort === 'price-desc'"), "renderListings-ben szerepelnie kell a csökkenő rendezésnek");
        assertTrue(html.contains("currentSort === 'area-asc'"), "renderListings-ben szerepelnie kell az alapterület növekvő rendezésnek");
        assertTrue(html.contains("currentSort === 'sqm-price-asc'"), "renderListings-ben szerepelnie kell a fajlagos ár növekvő rendezésnek");
    }

    @Test
    void testSortResetAndNullPriceHandlingInScript() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains("if (pA === null && pB === null) return 0;"), "Null árak kezelése: ha mindkettő null, egyenlő");
        assertTrue(html.contains("if (pA === null) return 1;"), "Null árak kezelése: ha A null, a végére kerül");
        assertTrue(html.contains("if (pB === null) return -1;"), "Null árak kezelése: ha B null, a végére kerül");

        assertTrue(html.contains("currentSort = 'price-asc';"), "Keresés indításakor és törléskor a rendezés visszaáll növekvő árra");
        assertTrue(html.contains("sortSelect.value = 'price-asc';"), "A select elem értéke is visszaáll növekvő árra");
    }

    @Test
    void testPortalBulkSelectButtonsAndFunctions() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains("selectAllPortals()"), "Tartalmaznia kell a selectAllPortals hívást");
        assertTrue(html.contains("deselectAllPortals()"), "Tartalmaznia kell a deselectAllPortals hívást");
        assertTrue(html.contains("Mindet kijelöl"), "Tartalmaznia kell a Mindet kijelöl gombot");
        assertTrue(html.contains("Mindet töröl"), "Tartalmaznia kell a Mindet töröl gombot");
        assertTrue(html.contains(".btn-text-link"), "Tartalmaznia kell a .btn-text-link stílust");
    }
}
