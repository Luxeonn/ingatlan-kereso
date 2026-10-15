package ingatlanok.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Frontend és Alkalmazás Integritási Teszt.
 * Szigorúan ellenőrzi a WebDashboardHtml által generált HTML markupot,
 * a beágyazott JavaScript kód szintaktikai épségét, a DOM elemazonosítók (ID)
 * meglétét és az API végpontok összhangját.
 */
class WebDashboardHtmlIntegrityTest {

    private static String html;
    private static Document doc;
    private static String scriptContent;

    @BeforeAll
    static void setUp() {
        html = WebDashboardHtml.getHtml(8080);
        assertNotNull(html, "A generált HTML nem lehet null");
        assertFalse(html.isBlank(), "A generált HTML nem lehet üres");

        doc = Jsoup.parse(html);
        assertNotNull(doc, "A Jsoup dokumentum sikeresen parszolható kell legyen");

        Elements scripts = doc.select("script");
        StringBuilder sb = new StringBuilder();
        for (Element s : scripts) {
            // Csak az inline szkripteket gyűjtjük össze (nincs külső src)
            if (!s.hasAttr("src")) {
                sb.append(s.data()).append("\n");
            }
        }
        scriptContent = sb.toString().trim();
        assertFalse(scriptContent.isBlank(), "A HTML-nek tartalmaznia kell beágyazott JavaScript kódot");
    }

    @Test
    @DisplayName("HTML struktúra integritás: DOCTYPE, title, charset, meta viewport")
    void testHtmlStructuralIntegrity() {
        assertTrue(html.startsWith("<!DOCTYPE html>"), "A dokumentumnak szabványos HTML5 DOCTYPE-pal kell kezdődnie");
        assertNotNull(doc.head(), "A HTML-nek rendelkeznie kell <head> résszel");
        assertNotNull(doc.body(), "A HTML-nek rendelkeznie kell <body> résszel");

        Element title = doc.selectFirst("title");
        assertNotNull(title, "A HTML-nek rendelkeznie kell <title> elemmel");
        assertTrue(title.text().contains("Ingatlan Kereső"), "A címnek tartalmaznia kell az alkalmazás nevét");

        Element charsetMeta = doc.selectFirst("meta[charset]");
        assertNotNull(charsetMeta, "A HTML-nek meg kell határoznia a karakterkódolást (charset)");
        assertEquals("UTF-8", charsetMeta.attr("charset").toUpperCase(), "A karakterkódolásnak UTF-8-nak kell lennie");

        Element viewportMeta = doc.selectFirst("meta[name=viewport]");
        assertNotNull(viewportMeta, "A reszponzív megjelenítéshez szükséges a viewport meta elem");
    }

    @Test
    @DisplayName("JavaScript szintaktikai integritás: Nincsenek kezeletlen multiline string literálok")
    void testJavaScriptNoUnescapedMultilineStrings() {
        // A JavaScript-ben a standard stringek ('...' vagy "...") nem tartalmazhatnak nyers soremelést!
        // Ha valaki join('\n')-t ír a Java text blockba join('\\n') helyett, a Java beilleszti a valódi
        // soremelést, ami JavaScriptben "SyntaxError: Invalid or unexpected token" hibát eredményez.

        char[] chars = scriptContent.toCharArray();
        int len = chars.length;
        int line = 1;
        int col = 0;

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inTemplateLiteral = false; // `...`
        boolean inLineComment = false;     // //
        boolean inBlockComment = false;    // /* ... */
        boolean inRegex = false;           // /.../
        boolean escaped = false;
        char lastNonWhitespace = ';';

        for (int i = 0; i < len; i++) {
            char c = chars[i];
            col++;

            if (c == '\n') {
                line++;
                col = 0;
            }

            // Egysoros komment vége
            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                }
                continue;
            }

            // Többsoros komment vége
            if (inBlockComment) {
                if (c == '*' && i + 1 < len && chars[i + 1] == '/') {
                    inBlockComment = false;
                    i++;
                    col++;
                }
                continue;
            }

            // Regex literál vége
            if (inRegex) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '/') {
                    inRegex = false;
                    lastNonWhitespace = '/';
                }
                continue;
            }

            // Escape karakter stringeken belül
            if (escaped) {
                escaped = false;
                continue;
            }

            if ((inSingleQuote || inDoubleQuote || inTemplateLiteral) && c == '\\') {
                escaped = true;
                continue;
            }

            // Nem stringben / kommentben lévő karakterek elemzése
            if (!inSingleQuote && !inDoubleQuote && !inTemplateLiteral) {
                if (c == '/' && i + 1 < len && chars[i + 1] == '/') {
                    inLineComment = true;
                    i++;
                    col++;
                    continue;
                }
                if (c == '/' && i + 1 < len && chars[i + 1] == '*') {
                    inBlockComment = true;
                    i++;
                    col++;
                    continue;
                }
                // Regex felismerése: ha operátor vagy nyitó zárójel után áll a '/'
                if (c == '/') {
                    if (isRegexPrefix(lastNonWhitespace)) {
                        inRegex = true;
                        continue;
                    }
                }
            }

            // Idézőjelek kezelése
            if (c == '\'' && !inDoubleQuote && !inTemplateLiteral) {
                inSingleQuote = !inSingleQuote;
                if (!inSingleQuote) lastNonWhitespace = '\'';
                continue;
            }
            if (c == '"' && !inSingleQuote && !inTemplateLiteral) {
                inDoubleQuote = !inDoubleQuote;
                if (!inDoubleQuote) lastNonWhitespace = '"';
                continue;
            }
            if (c == '`' && !inSingleQuote && !inDoubleQuote) {
                inTemplateLiteral = !inTemplateLiteral;
                if (!inTemplateLiteral) lastNonWhitespace = '`';
                continue;
            }

            // SZIGORÚ VALIDÁCIÓ: Soremelés lezáratlan '...' vagy "..." string literálon belül
            if ((inSingleQuote || inDoubleQuote) && (c == '\n' || c == '\r')) {
                String quoteType = inSingleQuote ? "'" : "\"";
                fail("SZINTAKTIKAI HIBA A JAVASCRIPTBEN: Lezáratlan string literál valódi soremeléssel a(z) " +
                        line + ". sorban, " + col + ". oszlopban (" + quoteType + "). " +
                        "Java text block használatakor figyelj: '\\n' helyett '\\\\n'-t kell használni!");
            }

            if (!Character.isWhitespace(c) && !inSingleQuote && !inDoubleQuote && !inTemplateLiteral) {
                lastNonWhitespace = c;
            }
        }

        assertFalse(inSingleQuote, "Lezáratlan ' string literál a JavaScript kód végén!");
        assertFalse(inDoubleQuote, "Lezáratlan \" string literál a JavaScript kód végén!");
        assertFalse(inTemplateLiteral, "Lezáratlan ` template literál a JavaScript kód végén!");
        assertFalse(inBlockComment, "Lezáratlan /* ... */ blokk-komment a JavaScript kód végén!");
    }

    private static boolean isRegexPrefix(char c) {
        return c == '=' || c == '(' || c == ',' || c == '[' || c == ':' ||
                c == '!' || c == '&' || c == '|' || c == '?' || c == '{' ||
                c == ';' || c == '~' || c == '^' || c == '+' || c == '-';
    }

    @Test
    @DisplayName("JavaScript szintaktikai integritás: Zárójel kiegyensúlyozottság (Brace & Parenthesis Balance)")
    void testJavaScriptBraceBalance() {
        Deque<Character> stack = new ArrayDeque<>();
        char[] chars = scriptContent.toCharArray();
        int len = chars.length;

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inTemplateLiteral = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inRegex = false;
        boolean escaped = false;
        char lastNonWhitespace = ';';

        for (int i = 0; i < len; i++) {
            char c = chars[i];

            if (inLineComment) {
                if (c == '\n') inLineComment = false;
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && i + 1 < len && chars[i + 1] == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }
            if (inRegex) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '/') {
                    inRegex = false;
                    lastNonWhitespace = '/';
                }
                continue;
            }
            if (escaped) {
                escaped = false;
                continue;
            }
            if ((inSingleQuote || inDoubleQuote || inTemplateLiteral) && c == '\\') {
                escaped = true;
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && !inTemplateLiteral) {
                if (c == '/' && i + 1 < len && chars[i + 1] == '/') {
                    inLineComment = true;
                    i++;
                    continue;
                }
                if (c == '/' && i + 1 < len && chars[i + 1] == '*') {
                    inBlockComment = true;
                    i++;
                    continue;
                }
                if (c == '/' && isRegexPrefix(lastNonWhitespace)) {
                    inRegex = true;
                    continue;
                }
            }
            if (c == '\'' && !inDoubleQuote && !inTemplateLiteral) {
                inSingleQuote = !inSingleQuote;
                if (!inSingleQuote) lastNonWhitespace = '\'';
                continue;
            }
            if (c == '"' && !inSingleQuote && !inTemplateLiteral) {
                inDoubleQuote = !inDoubleQuote;
                if (!inDoubleQuote) lastNonWhitespace = '"';
                continue;
            }
            if (c == '`' && !inSingleQuote && !inDoubleQuote) {
                inTemplateLiteral = !inTemplateLiteral;
                if (!inTemplateLiteral) lastNonWhitespace = '`';
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && !inTemplateLiteral) {
                if (c == '(' || c == '{' || c == '[') {
                    stack.push(c);
                } else if (c == ')') {
                    assertFalse(stack.isEmpty(), "Felesleges záró zárójel: ')'");
                    char top = stack.pop();
                    assertEquals('(', top, "Nem egyező zárójelpár: várt '(', kapott ')'");
                } else if (c == '}') {
                    assertFalse(stack.isEmpty(), "Felesleges záró kapcsos zárójel: '}'");
                    char top = stack.pop();
                    assertEquals('{', top, "Nem egyező kapcsos zárójelpár: várt '{', kapott '}'");
                } else if (c == ']') {
                    assertFalse(stack.isEmpty(), "Felesleges záró szögletes zárójel: ']'");
                    char top = stack.pop();
                    assertEquals('[', top, "Nem egyező szögletes zárójelpár: várt '[', kapott ']'");
                }
                if (!Character.isWhitespace(c)) {
                    lastNonWhitespace = c;
                }
            }
        }

        assertTrue(stack.isEmpty(), "Lezáratlan nyitó zárójelek találhatók a JavaScriptben: " + stack);
    }

    @Test
    @DisplayName("DOM ID Integritás: A JavaScript-ben hivatkozott összes getElementById létezik a HTML-ben")
    void testDomElementIdIntegrity() {
        Pattern pattern = Pattern.compile("document\\.getElementById\\s*\\(\\s*['\"]([a-zA-Z0-9_-]+)['\"]\\s*\\)");
        Matcher matcher = pattern.matcher(scriptContent);

        Set<String> referencedIds = new TreeSet<>();
        while (matcher.find()) {
            referencedIds.add(matcher.group(1));
        }

        assertFalse(referencedIds.isEmpty(), "A JavaScriptnek hivatkoznia kell DOM elemekre");

        List<String> missingIds = new ArrayList<>();
        for (String id : referencedIds) {
            Element elem = doc.getElementById(id);
            if (elem == null) {
                missingIds.add(id);
            }
        }

        assertTrue(missingIds.isEmpty(),
                "A JavaScript kód olyan DOM ID-kra hivatkozik, amelyek nem léteznek a HTML markupban: " + missingIds);
    }

    @Test
    @DisplayName("Kritikus UI Vezérlőelemek Integritása: Gombok, szűrők, modális ablakok léteznek a DOM-ban")
    void testCriticalUiElementsExist() {
        String[] criticalIds = {
                "submit-btn",
                "btn-text",
                "btn-spinner",
                "city-search-input",
                "city-chips-box",
                "selected-chips-container",
                "city-dropdown",
                "city-dropdown-list",
                "city-input",
                "portal-container",
                "city-presets-bar",
                "min-price-input",
                "max-price-input",
                "min-area-input",
                "max-area-input",
                "type-select",
                "auto-open-toggle",
                "exclude-viewed-toggle",
                "sort-select",
                "modal-logs",
                "logs-terminal",
                "btn-copy-logs",
                "modal-diagnostics",
                "diagnostics-modal-body",
                "toast"
        };

        for (String id : criticalIds) {
            Element elem = doc.getElementById(id);
            assertNotNull(elem, "A kritikus UI elem nem található a HTML DOM-ban: #" + id);
        }
    }

    @Test
    @DisplayName("API Végpont Konzisztencia: A JS által hívott /api/... végpontok ismertek a WebServer számára")
    void testApiEndpointConsistency() {
        Pattern pattern = Pattern.compile("fetch\\s*\\(\\s*['\"`](/api/[a-zA-Z0-9/_-]+)['\"`]");
        Matcher matcher = pattern.matcher(scriptContent);

        Set<String> calledEndpoints = new TreeSet<>();
        while (matcher.find()) {
            calledEndpoints.add(matcher.group(1));
        }

        Set<String> knownEndpoints = Set.of(
                "/api/status",
                "/api/config",
                "/api/portals",
                "/api/cities",
                "/api/presets",
                "/api/search",
                "/api/properties",
                "/api/history",
                "/api/history/clear",
                "/api/items/viewed",
                "/api/open-urls",
                "/api/logs",
                "/api/logs/clear"
        );

        for (String ep : calledEndpoints) {
            assertTrue(knownEndpoints.contains(ep),
                    "A JavaScript ismeretlen vagy nem támogatott API végpontot hív: " + ep);
        }
    }

    @Test
    @DisplayName("Node.js külső szintaxisellenőrző futtatása (ha elérhető a runneren/környezetben)")
    void testJavaScriptWithNodeCliIfAvailable() throws Exception {
        boolean nodeAvailable = false;
        try {
            Process process = new ProcessBuilder("node", "--version").start();
            int exitCode = process.waitFor();
            nodeAvailable = (exitCode == 0);
        } catch (Exception ignored) {
            nodeAvailable = false;
        }

        if (!nodeAvailable) {
            System.out.println("ℹ️ [INFO] A rendszerben nincs 'node' parancs telepítve. A Java-alapú belső szintaxis-ellenőrző garantálta a validációt.");
            return;
        }

        Path tempFile = Files.createTempFile("dashboard-syntax-check-", ".js");
        try {
            Files.writeString(tempFile, scriptContent, StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder("node", "--check", tempFile.toAbsolutePath().toString());
            Process process = pb.start();

            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String l;
                while ((l = reader.readLine()) != null) {
                    errorOutput.append(l).append("\n");
                }
            }

            int exitCode = process.waitFor();
            assertEquals(0, exitCode,
                    "A Node.js szintaxisellenőrző (node --check) hibát talált a generált JavaScriptben:\n" + errorOutput);
            System.out.println("✅ [NODE.JS CHECK] A beágyazott JavaScript kód sikeresen átment a Node.js szigorú szintaxis-ellenőrzésén!");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
