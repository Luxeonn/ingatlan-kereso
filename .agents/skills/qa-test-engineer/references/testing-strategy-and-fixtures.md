# Tesztelési Stratégia és Fixture Referencia

Ez a dokumentum az egység-, integrációs és scraper tesztek szabványos felépítését és fixture kezelését mutatja be.

---

## 1. Tesztkönyvtár Struktúra

```text
src/test/
├── java/
│   └── ingatlanok/
│       ├── common/                     # Utility egységtesztek (PropertyParserTest, CityUtilsTest, stb.)
│       ├── core/                       # Feldolgozó motor tesztek (PropertyFilterEngineTest, stb.)
│       ├── persistence/                # SQLite és JSON perzisztencia tesztek
│       ├── portal/                     # Portál scraper tesztek
│       └── web/                        # WebServer és WebDashboardHtml tesztek
└── resources/
    └── fixtures/                       # Statikus offline HTML/JSON válaszok
        ├── dh_sample.html
        ├── oc_sample.html
        ├── jofogas_sample.html
        └── ingatlanbazar_sample.json
```

---

## 2. Mintakód: Portál Parser Tesztelése Offline Fixture-rel

```java
package ingatlanok.portal.dh;

import ingatlanok.model.IngatlanItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DunaHouseParserTest {

    @Test
    @DisplayName("Duna House keresési oldal sikeres parsingja offline HTML fixture-ből")
    void testParseSearchPage() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/dh_sample.html")) {
            assertNotNull(is, "A teszt fixture nem található!");
            Document doc = Jsoup.parse(is, StandardCharsets.UTF_8.name(), "https://dh.hu");
            
            DunaHouseParser parser = new DunaHouseParser();
            List<IngatlanItem> items = parser.parseListingCards(doc);
            
            assertFalse(items.isEmpty(), "Legalább egy hirdetést találnia kell a mintában");
            IngatlanItem first = items.get(0);
            assertNotNull(first.getUrl());
            assertTrue(first.getUrl().startsWith("https://dh.hu/"));
            assertTrue(first.getPriceNumeric() > 0);
            assertTrue(first.getFloorArea() > 0);
        }
    }
}
```

---

## 3. Mintakód: SQLite Perzisztencia Tesztelése `@TempDir`-rel

```java
package ingatlanok.persistence;

import ingatlanok.model.IngatlanItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqliteListingStorageTest {

    @TempDir
    Path tempDir;

    private SqliteListingStorage storage;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test_ingatlanok.db");
        SqliteDatabaseManager dbManager = new SqliteDatabaseManager(dbPath.toString());
        storage = new SqliteListingStorage(dbManager);
    }

    @Test
    void testSaveAndRetrieveItems() {
        IngatlanItem item = new IngatlanItem(
            "Debrecen", "55 M Ft", "Eladó tégla lakás",
            "https://portal.hu/hirdetes/123", "PORTAL"
        );
        item.setPriceNumeric(55_000_000L);
        item.setFloorArea(65.0);

        storage.saveCityItems("debrecen", List.of(item));

        List<IngatlanItem> retrieved = storage.getItemsByCity("debrecen");
        assertEquals(1, retrieved.size());
        assertEquals("https://portal.hu/hirdetes/123", retrieved.get(0).getUrl());
    }
}
```

---

## 4. JaCoCo Lefedettségi Riport Generálása

```bash
# Tesztek futtatása és kódlefedettségi riport generálása
mvn clean test jacoco:report

# Riport megtekintése
# target/site/jacoco/index.html
```
