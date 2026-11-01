package ingatlanok.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.core.DataQualityEngine;
import ingatlanok.core.DataQualityEngine.QualityEvaluation;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;

class TestFixturesTest {

  private final DataQualityEngine qualityEngine = new DataQualityEngine();

  @Test
  @DisplayName("TestFixtures produces DataQualityEngine-compliant residential and plot listings")
  void testValidItemsPassDataQualityEngine() {
    IngatlanItem residential = TestFixtures.validResidentialItem("res-1");
    QualityEvaluation resEval = qualityEngine.evaluate(residential);
    assertTrue(resEval.score() >= DataQualityEngine.PASSING_THRESHOLD);
    assertEquals(PropertyType.LAKAS, residential.getPropertyType());

    IngatlanItem customRes =
        TestFixtures.validResidentialItem("res-2", "Szeged", 60_000_000L, 80.0, 3.0);
    customRes.setPriceDelta(-3_000_000L);
    assertEquals(-3_000_000L, customRes.getPriceDelta());
    assertEquals(750_000L, customRes.getDetailsInfo().getPricePerSquareMeter());

    IngatlanItem plot = TestFixtures.validPlotItem("plot-1", "Debrecen", 25_000_000L, 1000.0);
    QualityEvaluation plotEval = qualityEngine.evaluate(plot);
    assertTrue(plotEval.score() >= DataQualityEngine.PASSING_THRESHOLD);
    assertEquals(PropertyType.TELEK, plot.getPropertyType());

    SearchCriteria criteria = TestFixtures.validSearchCriteria("Budapest");
    assertNotNull(criteria);
    assertEquals("Budapest", criteria.getCity());
  }
}
