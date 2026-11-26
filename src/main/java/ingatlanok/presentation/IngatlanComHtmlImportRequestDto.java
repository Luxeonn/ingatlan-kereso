package ingatlanok.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import ingatlanok.model.PropertyType;

/**
 * Request payload for importing user-supplied Ingatlan.com search result HTML (#455).
 */
public record IngatlanComHtmlImportRequestDto(
    @NotBlank(message = "Az importálandó HTML tartalom nem lehet üres")
        @Size(max = 5_242_880, message = "A HTML tartalom mérete meghaladja a 5 MB korlátot")
        String html,
    @Size(max = 2000, message = "A forrás URL túl hosszú") String sourceUrl,
    @Size(max = 200, message = "A település neve túl hosszú") String city,
    PropertyType propertyType) {}
