package ingatlanok.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ingatlanok.persistence.entity.ConfigEntity;
import ingatlanok.persistence.entity.ListingObservationEntity;
import ingatlanok.persistence.entity.PriceHistoryEntity;
import ingatlanok.persistence.entity.PropertyEntity;
import ingatlanok.persistence.entity.StatusLogEntity;

@SpringBootTest
class JpaSchemaIntegrityTest {

  private static final List<Class<?>> EXPECTED_ENTITIES =
      List.of(
          PropertyEntity.class,
          ListingObservationEntity.class,
          PriceHistoryEntity.class,
          StatusLogEntity.class,
          ConfigEntity.class);

  @Autowired
  private EntityManager entityManager;

  @Test
  @DisplayName("Minden JPA entitás rendelkezik @Entity annotációval és egyértelmű táblanévvel")
  void testEntityAnnotationsAndTableNames() {
    for (Class<?> entityClass : EXPECTED_ENTITIES) {
      assertTrue(
          entityClass.isAnnotationPresent(Entity.class),
          entityClass.getSimpleName() + " osztálynak @Entity-nek kell lennie");

      Table table = entityClass.getAnnotation(Table.class);
      if (table != null) {
        assertFalse(table.name().isBlank(), entityClass.getSimpleName() + " táblanév nem lehet üres");
      }
    }
  }

  @Test
  @DisplayName("Hibernate metamodell sikeresen betölti az összes elvárt entitást sémaházirend-hiba nélkül")
  void testMetamodelIntegrity() {
    assertNotNull(entityManager, "EntityManager nem lehet null");
    Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();
    assertNotNull(entities);

    for (Class<?> expected : EXPECTED_ENTITIES) {
      boolean found = entities.stream().anyMatch(e -> e.getJavaType().equals(expected));
      assertTrue(
          found,
          "A Hibernate metamodell nem tartalmazza a(z) " + expected.getSimpleName() + " entitást!");
    }
  }
}
