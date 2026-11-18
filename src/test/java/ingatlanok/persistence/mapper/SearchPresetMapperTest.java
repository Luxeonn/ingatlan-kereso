package ingatlanok.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import ingatlanok.common.JsonSupport;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchPreset;
import ingatlanok.persistence.entity.SearchPresetEntity;

@DisplayName("SearchPresetMapper Unit Tests")
class SearchPresetMapperTest {

  private final SearchPresetMapper mapper = Mappers.getMapper(SearchPresetMapper.class);

  @Test
  @DisplayName("toModel handles null, blank and invalid JSON fallbacks safely")
  void testToModelNullAndMalformedJsonFallbacks() {
    assertNull(mapper.toModel(null, JsonSupport.mapper()));

    SearchPresetEntity blankEntity =
        SearchPresetEntity.builder()
            .id(1L)
            .name("Üres sablon")
            .criteriaJson("   ")
            .createdAt("2026-11-17T10:00:00Z")
            .updatedAt("2026-11-17T10:00:00Z")
            .build();
    SearchPreset blankModel = mapper.toModel(blankEntity, JsonSupport.mapper());
    assertNotNull(blankModel);
    assertNotNull(blankModel.criteria());
    assertEquals("Hajdúszoboszló", blankModel.criteria().getCity());

    SearchPresetEntity corruptEntity =
        SearchPresetEntity.builder()
            .id(2L)
            .name("Hibás JSON")
            .criteriaJson("{broken-json")
            .build();
    SearchPreset corruptModel = mapper.toModel(corruptEntity, JsonSupport.mapper());
    assertNotNull(corruptModel);
    assertNotNull(corruptModel.criteria());
  }

  @Test
  @DisplayName("toNewEntity and updateEntityFromPreset handle nulls and defaults cleanly")
  void testToNewEntityAndUpdateFallbacks() {
    assertNull(mapper.toNewEntity(null, JsonSupport.mapper()));

    SearchPreset blankNamePreset = new SearchPreset(null, "  ", null, null, null, null);
    SearchPresetEntity entity = mapper.toNewEntity(blankNamePreset, JsonSupport.mapper());
    assertNotNull(entity);
    assertEquals("Névtelen sablon", entity.getName());
    assertEquals("{}", entity.getCriteriaJson());

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Szeged");
    SearchPreset partialUpdate = new SearchPreset(null, "   ", criteria, null, null, null);
    mapper.updateEntityFromPreset(partialUpdate, entity, JsonSupport.mapper());
    assertEquals("Névtelen sablon", entity.getName());
    assertNotNull(entity.getCriteriaJson());
  }
}
