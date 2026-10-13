package ingatlanok.common;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Szövegek ékezetmentesítését és URL-barát keresési slugok előállítását biztosító segédosztály.
 */
public final class SlugUtils {

    private SlugUtils() {
        // Segédosztály, nem példányosítható
    }

    /**
     * Eltávolítja a magyar és egyéb diakritikus ékezeteket a megadott szövegből.
     * Pl. "Hajdúszoboszló" -> "Hajduszoboszlo", "ár" -> "ar".
     */
    public static String stripAccents(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    /**
     * URL-barát, kisbetűs slugot képez a megadott szövegből.
     * Az ékezetek eltávolításra kerülnek, a nem alfanumerikus karakterek kötőjellé alakulnak.
     * Pl. "Debrecen - Józsa" -> "debrecen-jozsa", "Hajdú-Bihar" -> "hajdu-bihar".
     */
    public static String toSlug(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String noAccents = stripAccents(text.trim().toLowerCase(Locale.ROOT));
        String slug = noAccents.replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+|-+$", "");
        return slug;
    }
}
