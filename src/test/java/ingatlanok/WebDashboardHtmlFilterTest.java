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

    @Test
    void testHtmlContainsCollapsibleSearchPanelAndStyles() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains("id=\"search-section\""));
        assertTrue(html.contains("id=\"search-form-container\""));
        assertTrue(html.contains("id=\"search-collapse-chevron\""));
        assertTrue(html.contains("id=\"search-summary-bar\""));
        assertTrue(html.contains("onclick=\"toggleSearchCollapse()\""));
        assertTrue(html.contains(".search-form-container.collapsed"));
        assertTrue(html.contains(".search-chevron.rotated"));
        assertTrue(html.contains(".search-summary-chips"));
        assertTrue(html.contains("function toggleSearchCollapse()"));
        assertTrue(html.contains("function setSearchCollapsed(collapsed)"));
        assertTrue(html.contains("function updateSearchSummaryChips()"));
        assertTrue(html.contains("function escapeHtml(text)"));
    }

    @Test
    void testHtmlContainsPropertyCardImagesAndSpaciousStyles() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains(".card-image-container"));
        assertTrue(html.contains(".card-image"));
        assertTrue(html.contains(".card-image-fallback"));
        assertTrue(html.contains("loading=\"lazy\""));
        assertTrue(html.contains("gap: 24px;"));
        assertTrue(html.contains("border-radius: 18px;"));
        assertTrue(html.contains("padding: 24px;"));
        assertTrue(html.contains("item.imageUrl"));
    }

    @Test
    void testHtmlContainsDiagnosticsModalAndErrorStatusIndicators() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains("id=\"btn-diagnostics\""), "HTML-nek tartalmaznia kell a diagnosztika gombot");
        assertTrue(html.contains("id=\"modal-diagnostics\""), "HTML-nek tartalmaznia kell a diagnosztika modális ablakot");
        assertTrue(html.contains("id=\"diagnostics-modal-body\""), "HTML-nek tartalmaznia kell a modális törzsét");
        assertTrue(html.contains("id=\"btn-copy-diagnostics\""), "HTML-nek tartalmaznia kell a hibajelentés másoló gombot");
        assertTrue(html.contains("onclick=\"openDiagnosticsModal()\""), "Meg kell hívnia az openDiagnosticsModal függvényt");
        assertTrue(html.contains("function updateDiagnosticsUi("), "JavaScriptben szerepelnie kell az updateDiagnosticsUi függvénynek");
        assertTrue(html.contains("function openDiagnosticsModal()"), "JavaScriptben szerepelnie kell az openDiagnosticsModal függvénynek");
        assertTrue(html.contains("function closeDiagnosticsModal()"), "JavaScriptben szerepelnie kell a closeDiagnosticsModal függvénynek");
        assertTrue(html.contains("function copyDiagnosticsReport()"), "JavaScriptben szerepelnie kell a copyDiagnosticsReport függvénynek");
        assertTrue(html.contains(".modal-overlay"), "CSS-nek tartalmaznia kell a .modal-overlay stílust");
        assertTrue(html.contains(".diag-card"), "CSS-nek tartalmaznia kell a .diag-card stílust");
    }

    @Test
    void testHtmlContainsKoltozzbePortalAndFilters() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains("id=\"portal-kb\""), "Tartalmaznia kell a Költözzbe checkboxot");
        assertTrue(html.contains("value=\"koltozzbe\""), "Tartalmaznia kell a koltozzbe értéket");
        assertTrue(html.contains("data-portal=\"kb\""), "Tartalmaznia kell a Költözzbe szűrőgombot");
        assertTrue(html.contains("data-portal=\"ib\""), "Tartalmaznia kell az Ingatlanbazár szűrőgombot");
        assertTrue(html.contains(".portal-badge.kb"), "Tartalmaznia kell a Költözzbe badge CSS osztályt");
    }

    @Test
    void testHtmlContainsLiveLogsModalAndControls() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains("id=\"btn-logs\""), "HTML-nek tartalmaznia kell a fejléc napló gombot");
        assertTrue(html.contains("id=\"btn-quick-logs\""), "HTML-nek tartalmaznia kell a gyorsművelet napló gombot");
        assertTrue(html.contains("id=\"modal-logs\""), "HTML-nek tartalmaznia kell a napló modális ablakot");
        assertTrue(html.contains("id=\"logs-terminal\""), "HTML-nek tartalmaznia kell a napló terminált");
        assertTrue(html.contains("id=\"btn-copy-logs\""), "HTML-nek tartalmaznia kell a napló másoló gombot");
        assertTrue(html.contains("onclick=\"openLogsModal()\""), "openLogsModal hívásnak szerepelnie kell");
        assertTrue(html.contains("onclick=\"closeLogsModal()\""), "closeLogsModal hívásnak szerepelnie kell");
        assertTrue(html.contains("onclick=\"refreshLogs()\""), "refreshLogs hívásnak szerepelnie kell");
        assertTrue(html.contains("onclick=\"clearLogsServer()\""), "clearLogsServer hívásnak szerepelnie kell");
        assertTrue(html.contains("function openLogsModal()"), "JS-ben léteznie kell az openLogsModal függvénynek");
        assertTrue(html.contains("function refreshLogs()"), "JS-ben léteznie kell a refreshLogs függvénynek");
        assertTrue(html.contains("function filterLogs("), "JS-ben léteznie kell a filterLogs függvénynek");
        assertTrue(html.contains("function renderLogs()"), "JS-ben léteznie kell a renderLogs függvénynek");
        assertTrue(html.contains("function copyLogsToClipboard()"), "JS-ben léteznie kell a copyLogsToClipboard függvénynek");
        assertTrue(html.contains("function clearLogsServer()"), "JS-ben léteznie kell a clearLogsServer függvénynek");
    }

    @Test
    void testHtmlContainsCityDropdownToggleButtonAndFallback() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains("id=\"city-dropdown-toggle-btn\""), "HTML-nek tartalmaznia kell a város legördülő gombot");
        assertTrue(html.contains("onclick=\"toggleCityDropdown(event)\""), "toggleCityDropdown eseménykezelőnek léteznie kell");
        assertTrue(html.contains("function toggleCityDropdown("), "JS-ben szerepelnie kell a toggleCityDropdown függvénynek");
        assertTrue(html.contains("function toggleCityTag("), "JS-ben szerepelnie kell a toggleCityTag függvénynek");
        assertTrue(html.contains("Debreceni agglomeráció"), "Alapértelmezett sablonnak szerepelnie kell a fallback adatokban");
    }

    @Test
    void testHtmlContainsEmptyStateSearchRecoveryButton() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains("function resetSearchBoundsAndRerun()"), "JS-ben szerepelnie kell a resetSearchBoundsAndRerun függvénynek");
        assertTrue(html.contains("resetSearchBoundsAndRerun()"), "Empty state-nek tartalmaznia kell a hívást");
    }

    @Test
    void testHtmlScriptHasNoUnescapedMultilineStrings() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        int scriptStart = html.indexOf("<script>");
        int scriptEnd = html.lastIndexOf("</script>");
        assertTrue(scriptStart >= 0 && scriptEnd > scriptStart, "HTML-nek tartalmaznia kell a script blokkot");

        String script = html.substring(scriptStart + 8, scriptEnd);

        // Ellenőrizzük, hogy nincs-e elrontott join('\n') ami tényleges sortörést tartalmaz idézőjelek között
        assertFalse(script.contains("join('\n')"), "Script nem tartalmazhat egyidézőjeles sortörést, join('\\n') szükséges");
        assertTrue(script.contains("join('\\n')"), "Scriptnek join('\\n') formátumot kell használnia a másoláshoz");
    }

    @Test
    void testHtmlContainsNormalizedSearchAndRobustPortalCheckboxRestoration() {
        String html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html);

        assertTrue(html.contains("function normalizeStr("), "JS-ben szerepelnie kell a normalizeStr ékezetmentesítő segédfüggvénynek");
        assertTrue(html.contains("termNorm = normalizeStr("), "Város keresőnek normalizált kifejezést kell használnia");
        assertTrue(html.contains("nameNorm = normalizeStr(c.name)"), "Város nevének normalizált összehasonlítása szükséges");
        assertTrue(html.contains("document.querySelectorAll('.portal-checkboxes input[type=\"checkbox\"]').forEach"),
                "Portálok visszaállításának robusztusan querySelectorAll-lal kell történnie");
    }
}

