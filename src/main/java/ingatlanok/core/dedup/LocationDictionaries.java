package ingatlanok.core.dedup;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Static district, quarter, and street lookup dictionaries for {@link LocationConflictDetector}.
 */
final class LocationDictionaries {

  static final Set<String> STREET_LEADING_PREFIXES = Set.of(
      "miskolc", "miskolcon", "debrecen", "debreceni", "debrecenben", "budapest", "szeged", "pecs",
      "gyor", "avas", "avason", "jozsa", "jozsan", "alsojozsa", "alsojozsan", "felsojozsa",
      "felsojozsan", "homokkert", "kertvaros", "belvaros", "elado", "kiado", "ingatlan", "lakas",
      "haz", "csaladi", "ikerhaz", "sorhaz", "telek", "panel", "panellakas", "tegla",
      "teglalakas", "csendes", "nyugodt", "napfenyes", "uj", "felujitott", "kis", "nagy");

  static final Set<String> INVALID_STREET_STEMS = Set.of(
      "az", "egy", "ez", "fo", "fold", "zsak", "csendes", "egyiranyu",
      "aszfaltozott", "aszfalt", "sajat", "magan", "kertes", "szeles",
      "mellek", "kozponti", "elado", "ingatlan", "lakas", "haz",
      "panel", "tegla", "csaladi", "sarok", "kivalo", "szep");

  static final List<Map.Entry<String, String>> QUARTER_SYNONYMS = List.of(
      Map.entry("varoskozpont", "belvaros"), Map.entry("julia-telep", "juliatelep"),
      Map.entry("juliatelep", "juliatelep"), Map.entry("juliakert", "juliatelep"),
      Map.entry("lenc-telep", "lencztelep"), Map.entry("lenctelep", "lencztelep"),
      Map.entry("lencztelep", "lencztelep"), Map.entry("also-jozsa", "also-jozsa"),
      Map.entry("alsojozsa", "also-jozsa"), Map.entry("felso-jozsa", "felso-jozsa"),
      Map.entry("felsojozsa", "felso-jozsa"), Map.entry("veres-peter", "veres-peter"),
      Map.entry("verespeter", "veres-peter"), Map.entry("boldogfalvi", "boldogfalvikert"),
      Map.entry("szabadsag-telep", "szabadsagtelep"), Map.entry("szabadsagtelep", "szabadsagtelep"),
      Map.entry("gereby-telep", "gerebytelep"), Map.entry("gerebytelep", "gerebytelep"),
      Map.entry("biczo-kert", "biczokert"), Map.entry("biczokert", "biczokert"),
      Map.entry("kerekes-telep", "kerekestelep"), Map.entry("kerekestelep", "kerekestelep"),
      Map.entry("bartok-telep", "bartoktelep"), Map.entry("bartoktelep", "bartoktelep"));

  static final String[] COMPOUND_QUARTERS = {
      "julia-telep", "lenc-telep", "also-jozsa", "felso-jozsa", "veres-peter",
      "szabadsag-telep", "gereby-telep", "biczo-kert", "kerekes-telep", "bartok-telep"
  };

  static final String[] QUARTERS = {
      "banomkert", "kertvaros", "nagyerd", "nagyerdot", "tocoskert", "csapokert", "jozsa", "venkert",
      "homokkert", "juliatelep", "juliakert", "lencztelep", "lenctelep", "alsojozsa", "felsojozsa",
      "verespeter", "boldogfalvi", "szabadsagtelep", "gerebytelep", "halap", "kondoros", "biczokert",
      "kerekestelep", "bartoktelep", "furdonegyed", "vargakert", "epreskert", "dobozikert",
      "kismacs", "nagymacs", "pallag", "ujszeged", "felsovaros", "tarjan", "rokus", "moravaros",
      "szentmihaly", "marosto", "avas", "diosgyor", "szirma", "goromboly", "tapolca", "komlosteto",
      "uranvaros", "mecsekoldal", "meszes", "patacs", "gyarvaros", "nadorvaros", "szabadhegy",
      "marcalvaros", "revfalu", "furdovezet", "belvaros", "varoskozpont"
  };

  static final Map<String, Integer> BP_DISTRICT_NAMES = Map.ofEntries(
      Map.entry("varkerulet", 1), Map.entry("krisztinavaros", 1), Map.entry("taban", 1),
      Map.entry("rozsadomb", 2), Map.entry("pasaret", 2), Map.entry("pesthidegkut", 2),
      Map.entry("obuda", 3), Map.entry("bekasmegyer", 3), Map.entry("csillaghegy", 3),
      Map.entry("ujpest", 4), Map.entry("kaposztasmegyer", 4),
      Map.entry("belvaros", 5), Map.entry("lipotvaros", 5),
      Map.entry("terezvaros", 6), Map.entry("erzsebetvaros", 7),
      Map.entry("jozsefvaros", 8), Map.entry("corvin", 8), Map.entry("palotanegyed", 8),
      Map.entry("ferencvaros", 9),
      Map.entry("kobanya", 10), Map.entry("ohegy", 10), Map.entry("ujhegy", 10),
      Map.entry("ujbuda", 11), Map.entry("gazdagret", 11), Map.entry("kelenfold", 11),
      Map.entry("gellerthegy", 11), Map.entry("sasad", 11), Map.entry("madarhegy", 11),
      Map.entry("hegyvidek", 12), Map.entry("zugliget", 12), Map.entry("farkasret", 12),
      Map.entry("angyalfold", 13), Map.entry("ujlipotvaros", 13), Map.entry("vizafogo", 13),
      Map.entry("zuglo", 14), Map.entry("alsorakos", 14), Map.entry("herminamezo", 14),
      Map.entry("rakospalota", 15), Map.entry("pestujhely", 15), Map.entry("ujpalota", 15),
      Map.entry("matyasfold", 16), Map.entry("sashalom", 16), Map.entry("cinkota", 16),
      Map.entry("rakosmente", 17), Map.entry("rakoskeresztur", 17), Map.entry("rakoscsaba", 17),
      Map.entry("pestszentlorinc", 18), Map.entry("pestszentimre", 18), Map.entry("havanna", 18),
      Map.entry("kispest", 19), Map.entry("wekerle", 19),
      Map.entry("pesterzsebet", 20), Map.entry("csepel", 21),
      Map.entry("budafok", 22), Map.entry("nagyteteny", 22), Map.entry("budateteny", 22),
      Map.entry("soroksar", 23));

  static final List<String> ROMAN_DISTRICTS = List.of(
      "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
      "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX",
      "XXI", "XXII", "XXIII");

  static final List<String> QUARTER_NOISE_TOKENS = List.of(
      "emelet", "foldszint", "fszt", "tetoter", "zaroszint",
      "lakoovezet", "kisterseg", "agglomeracio");

  static final Set<String> TITLE_COMMA_NON_QUARTER_TOKENS = Set.of(
      "elado", "kiado", "szoba", "szobas", "lakas", "panel", "panellakas",
      "tegla", "teglalakas", "haz", "ikerhaz", "sorhaz", "telek", "garazs", "felujitott");

  private LocationDictionaries() {}
}
