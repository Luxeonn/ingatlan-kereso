package ingatlanok.model.raw;

import java.util.List;

import ingatlanok.model.PortalScrapeStatus;

public record RawExtractionResult(
    PortalScrapeStatus status,
    List<RawPropertyListing> listings,
    ExtractionMethod usedMethod,
    String fallbackUrl,
    String message,
    Integer httpStatusCode,
    String technicalDetails
) {

  public static RawExtractionResult success(
      List<RawPropertyListing> listings, ExtractionMethod usedMethod) {
    return new RawExtractionResult(
        PortalScrapeStatus.SUCCESS,
        listings != null ? List.copyOf(listings) : List.of(),
        usedMethod,
        null,
        null,
        200,
        null
    );
  }

  public static RawExtractionResult empty(
      ExtractionMethod usedMethod, String fallbackUrl) {
    return new RawExtractionResult(
        PortalScrapeStatus.EMPTY,
        List.of(),
        usedMethod,
        fallbackUrl,
        "Nem található találat",
        200,
        null
    );
  }

  public static RawExtractionResult botProtected(
      ExtractionMethod usedMethod, String fallbackUrl, String message) {
    return new RawExtractionResult(
        PortalScrapeStatus.BOT_PROTECTED,
        List.of(),
        usedMethod,
        fallbackUrl,
        message,
        403,
        "Bot protection triggered"
    );
  }

  public static RawExtractionResult failure(
      ExtractionMethod usedMethod, String fallbackUrl, String message, Integer httpStatusCode) {
    return new RawExtractionResult(
        PortalScrapeStatus.FAILED,
        List.of(),
        usedMethod,
        fallbackUrl,
        message,
        httpStatusCode != null ? httpStatusCode : 500,
        null
    );
  }
}
