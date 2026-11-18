package ingatlanok.persistence.mapper;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import tools.jackson.databind.ObjectMapper;

import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchPreset;
import ingatlanok.persistence.entity.SearchPresetEntity;

/**
 * MapStruct mapper converting between {@link SearchPresetEntity} and {@link SearchPreset} using an
 * injected Jackson {@link ObjectMapper} via {@code @Context}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SearchPresetMapper {

  @Mapping(target = "criteria", source = "criteriaJson", qualifiedByName = "jsonToCriteria")
  SearchPreset toModel(SearchPresetEntity entity, @Context ObjectMapper objectMapper);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "name", source = "name", qualifiedByName = "normalizePresetName")
  @Mapping(target = "criteriaJson", source = "criteria", qualifiedByName = "criteriaToJson")
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  SearchPresetEntity toNewEntity(SearchPreset preset, @Context ObjectMapper objectMapper);

  default void updateEntityFromPreset(
      SearchPreset preset, @MappingTarget SearchPresetEntity entity, ObjectMapper objectMapper) {
    if (preset == null || entity == null) {
      return;
    }
    if (preset.name() != null && !preset.name().isBlank()) {
      entity.setName(preset.name().trim());
    }
    if (preset.criteria() != null) {
      entity.setCriteriaJson(criteriaToJson(preset.criteria(), objectMapper));
    }
  }

  @Named("normalizePresetName")
  default String normalizePresetName(String name) {
    return name != null && !name.isBlank() ? name.trim() : "Névtelen sablon";
  }

  @Named("jsonToCriteria")
  default SearchCriteria jsonToCriteria(String criteriaJson, @Context ObjectMapper objectMapper) {
    if (criteriaJson == null || criteriaJson.isBlank()) {
      return new SearchCriteria();
    }
    try {
      SearchCriteria parsed = objectMapper.readValue(criteriaJson, SearchCriteria.class);
      return parsed != null ? parsed : new SearchCriteria();
    } catch (Exception e) {
      return new SearchCriteria();
    }
  }

  @Named("criteriaToJson")
  default String criteriaToJson(SearchCriteria criteria, @Context ObjectMapper objectMapper) {
    if (criteria == null) {
      return "{}";
    }
    try {
      return objectMapper.writeValueAsString(criteria);
    } catch (Exception e) {
      return "{}";
    }
  }
}
