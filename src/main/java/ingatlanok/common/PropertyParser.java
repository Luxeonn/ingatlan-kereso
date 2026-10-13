package ingatlanok.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ingatlanhirdetések szöveges adatait (ár, alapterület, szobaszám, azonosítók)
 * kinyerő és normalizáló közös segédosztály.
 */
public final class PropertyParser {

    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+[,.]?\\d*\\s*M\\s*Ft|\\d[\\d\\s]*\\s*Ft)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DETAILS_PATTERN = Pattern.compile("(\\d+[,.]?\\d*\\s*m²[^\\n•|]*\\d+\\s*szoba|\\d+\\s*szoba[^\\d•|]*[•|]?\\s*\\d+[,.]?\\d*\\s*m²|\\d+[,.]?\\d*\\s*m²)", Pattern.CASE_INSENSITIVE);

    private PropertyParser() {
        // Segédosztály, nem példányosítható
    }

    /**
     * Kinyeri az ingatlan hirdetési árát a megadott kártyaszövegből.
     * Pl. "48.5 M Ft", "32 500 000 Ft".
     * Ha nincs találat, "Ár a hirdetésben" értékkel tér vissza.
     */
    public static String extractPrice(String text) {
        if (text == null || text.isBlank()) {
            return "Ár a hirdetésben";
        }
        Matcher matcher = PRICE_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "Ár a hirdetésben";
    }

    /**
     * Kinyeri az alapterület és szobaszám információkat a kártyaszövegből.
     * Pl. "2 szoba • 54 m²", "65 m²".
     */
    public static String extractDetails(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        Matcher matcher = DETAILS_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    /**
     * Kinyeri a megadott regex minta első egyező csoportját a szövegből.
     */
    public static String extractFirstGroup(String text, Pattern pattern, String fallback) {
        if (text == null || pattern == null) {
            return fallback;
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : fallback;
    }
}
