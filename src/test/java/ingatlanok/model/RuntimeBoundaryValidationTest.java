package ingatlanok.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.core.normalization.ListingValidator;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.web.HistoryQueryDto;

@DisplayName("RuntimeBoundaryValidationTest - Domain és külső határ típusbiztonsági validációs egységtesztek (#403)")
class RuntimeBoundaryValidationTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  @DisplayName("SearchCriteria Bean Validation - Érvényes alapértelmezett feltételek hiba nélkül lefutnak")
  void testSearchCriteriaDefaultValid() {
    SearchCriteria criteria = new SearchCriteria();
    assertTrue(validator.validate(criteria).isEmpty());
  }

  @Test
  @DisplayName("SearchCriteria Bean Validation - Negatív értékek és invertált tartományok hibát adnak")
  void testSearchCriteriaInvalidNumbers() {
    SearchCriteria c1 = new SearchCriteria();
    c1.setMinPriceMillion(-1);
    assertFalse(validator.validate(c1).isEmpty());

    SearchCriteria c2 = new SearchCriteria();
    c2.setMaxPriceMillion(-1);
    assertFalse(validator.validate(c2).isEmpty());

    SearchCriteria c3 = new SearchCriteria();
    c3.setMinPriceMillion(100);
    c3.setMaxPriceMillion(50);
    assertFalse(validator.validate(c3).isEmpty());

    SearchCriteria c4 = new SearchCriteria();
    c4.setMinArea(-5);
    assertFalse(validator.validate(c4).isEmpty());

    SearchCriteria c5 = new SearchCriteria();
    c5.setMaxArea(-10);
    assertFalse(validator.validate(c5).isEmpty());

    SearchCriteria c6 = new SearchCriteria();
    c6.setMinArea(200);
    c6.setMaxArea(80);
    assertFalse(validator.validate(c6).isEmpty());

    SearchCriteria c7 = new SearchCriteria();
    c7.setMinRooms(-2);
    assertFalse(validator.validate(c7).isEmpty());
  }

  @Test
  @DisplayName("SearchCriteria Bean Validation - Ismeretlen portál és túl hosszú településnév hibát ad")
  void testSearchCriteriaInvalidPortalsAndCities() {
    SearchCriteria c1 = new SearchCriteria();
    c1.setPortals(List.of("dh", "fake_portal_xyz"));
    assertFalse(validator.validate(c1).isEmpty());

    SearchCriteria c2 = new SearchCriteria();
    c2.setCity("A".repeat(201));
    assertFalse(validator.validate(c2).isEmpty());

    SearchCriteria c3 = new SearchCriteria();
    List<String> tooManyCities = new ArrayList<>();
    for (int i = 0; i < 51; i++) {
      tooManyCities.add("Város" + i);
    }
    c3.setCities(tooManyCities);
    assertFalse(validator.validate(c3).isEmpty());
  }

  @Test
  @DisplayName("SearchPreset Bean Validation - OnCreate vs OnUpdate csoportok helyesen viselkednek")
  void testSearchPresetValidationGroups() {
    SearchPreset missingName = new SearchPreset(null, null, new SearchCriteria(), null, null, null);
    Set<ConstraintViolation<SearchPreset>> createViolations =
        validator.validate(missingName, ValidationGroups.OnCreate.class);
    assertFalse(createViolations.isEmpty());

    Set<ConstraintViolation<SearchPreset>> updateViolationsNullName =
        validator.validate(missingName, ValidationGroups.OnUpdate.class);
    assertTrue(updateViolationsNullName.isEmpty(), "PUT frissítésnél a null név megengedett");

    SearchPreset blankName = new SearchPreset(null, "   ", new SearchCriteria(), null, null, null);
    Set<ConstraintViolation<SearchPreset>> updateViolationsBlankName =
        validator.validate(blankName, ValidationGroups.OnUpdate.class);
    assertFalse(updateViolationsBlankName.isEmpty(), "PUT frissítésnél a blank név tilos");
  }

  @Test
  @DisplayName("HistoryQueryDto Bean Validation - Érvénytelen query paraméterek hibát adnak")
  void testHistoryQueryDtoValidation() {
    HistoryQueryDto q1 = new HistoryQueryDto(
        null, false, null, -1.0, 100.0, 50.0, 100.0, 1.0, 3.0, false, false, "DISCOVERED_DESC", 50, 0);
    assertFalse(validator.validate(q1).isEmpty());

    HistoryQueryDto q2 = new HistoryQueryDto(
        null, false, null, 120.0, 80.0, 50.0, 100.0, 1.0, 3.0, false, false, "DISCOVERED_DESC", 50, 0);
    assertFalse(validator.validate(q2).isEmpty());

    HistoryQueryDto q3 = new HistoryQueryDto(
        null, false, null, 50.0, 100.0, -10.0, 100.0, 1.0, 3.0, false, false, "DISCOVERED_DESC", 50, 0);
    assertFalse(validator.validate(q3).isEmpty());

    HistoryQueryDto q4 = new HistoryQueryDto(
        null, false, null, 50.0, 100.0, 150.0, 60.0, 1.0, 3.0, false, false, "DISCOVERED_DESC", 50, 0);
    assertFalse(validator.validate(q4).isEmpty());

    HistoryQueryDto q5 = new HistoryQueryDto(
        null, false, null, 50.0, 100.0, 50.0, 100.0, 4.0, 2.0, false, false, "DISCOVERED_DESC", 50, 0);
    assertFalse(validator.validate(q5).isEmpty());

    HistoryQueryDto q6 = new HistoryQueryDto(
        null, false, null, 50.0, 100.0, 50.0, 100.0, 1.0, 3.0, false, false, "DISCOVERED_DESC", 0, 0);
    assertFalse(validator.validate(q6).isEmpty());

    HistoryQueryDto q7 = new HistoryQueryDto(
        null, false, null, 50.0, 100.0, 50.0, 100.0, 1.0, 3.0, false, false, "DISCOVERED_DESC", 50, -1);
    assertFalse(validator.validate(q7).isEmpty());

    HistoryQueryDto q8 = new HistoryQueryDto(
        null, false, "ismeretlen_portal", 50.0, 100.0, 50.0, 100.0, 1.0, 3.0, false, false, "DISCOVERED_DESC", 50, 0);
    assertFalse(validator.validate(q8).isEmpty());
  }

  @Test
  @DisplayName("Money - Negatív érték nem engedélyezett a domain értékobjektumban")
  void testMoneyValidation() {
    assertTrue(Money.ofHuf(0).amountHuf() == 0);
    assertTrue(Money.ofHuf(50_000_000L).amountHuf() == 50_000_000L);
    assertTrue(Money.ofMillions(45.5).amountHuf() == 45_500_000L);

    assertThrows(IllegalArgumentException.class, () -> new Money(-1L));
    assertThrows(IllegalArgumentException.class, () -> Money.ofHuf(-100L));
    assertThrows(IllegalArgumentException.class, () -> Money.ofMillions(-5.0));
  }

  @Test
  @DisplayName("ListingSortOption.fromString() - Érvénytelen rendezési kulcs kivételt dob")
  void testListingSortOptionValidation() {
    assertEquals(ListingSortOption.DISCOVERED_DESC, ListingSortOption.fromString(null));
    assertEquals(ListingSortOption.DISCOVERED_DESC, ListingSortOption.fromString(""));
    assertEquals(ListingSortOption.PRICE_ASC, ListingSortOption.fromString("price_asc"));
    assertEquals(ListingSortOption.PRICE_DESC, ListingSortOption.fromString("PRICE_DESC"));

    assertThrows(IllegalArgumentException.class, () -> ListingSortOption.fromString("INVALID_SORT"));
    assertThrows(IllegalArgumentException.class, () -> ListingSortOption.fromString("DROP_TABLE"));
  }

  @Test
  @DisplayName("ListingValidator - Hibás szobaszám, null karakter vagy irreális adatok elutasítása")
  void testListingValidatorBoundaryChecks() {
    ListingValidator validator = new ListingValidator();
    PortalDescriptor desc = new PortalDescriptor(
        "dh", "Duna House", "#e4002b", "https://dh.hu", true, List.of(), true);

    RawPropertyListing validListing = RawPropertyListing.builder()
        .portalId("dh")
        .url("https://dh.hu/elado-haz/1234")
        .title("Szép családi ház")
        .priceHuf(60_000_000L)
        .areaSquareMeters(120.0)
        .roomCount(4.0)
        .locationText("Debrecen")
        .build();
    assertTrue(validator.isValid(validListing, desc));

    RawPropertyListing plotZeroRooms = RawPropertyListing.builder()
        .portalId("dh")
        .url("https://dh.hu/elado-telek/5678")
        .title("Építési telek")
        .priceHuf(25_000_000L)
        .areaSquareMeters(1000.0)
        .roomCount(0.0)
        .locationText("Debrecen")
        .build();
    assertTrue(validator.isValid(plotZeroRooms, desc));

    RawPropertyListing negativeRooms = RawPropertyListing.builder()
        .portalId("dh")
        .url("https://dh.hu/elado-haz/999")
        .title("Hibás szobaszám")
        .priceHuf(40_000_000L)
        .areaSquareMeters(100.0)
        .roomCount(-1.0)
        .locationText("Debrecen")
        .build();
    assertFalse(validator.isValid(negativeRooms, desc));

    RawPropertyListing excessiveRooms = RawPropertyListing.builder()
        .portalId("dh")
        .url("https://dh.hu/elado-haz/1000")
        .title("Túl sok szoba")
        .priceHuf(40_000_000L)
        .areaSquareMeters(100.0)
        .roomCount(150.0)
        .locationText("Debrecen")
        .build();
    assertFalse(validator.isValid(excessiveRooms, desc));

    RawPropertyListing nullByteText = RawPropertyListing.builder()
        .portalId("dh")
        .url("https://dh.hu/elado-haz/1001")
        .title("Injektált szöveg\u0000ártalmas")
        .priceHuf(40_000_000L)
        .areaSquareMeters(100.0)
        .roomCount(3.0)
        .locationText("Debrecen")
        .build();
    assertFalse(validator.isValid(nullByteText, desc));
  }
}
