package ingatlanok;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MainTest {

  @Test
  @DisplayName("isCliRequested detects --cli in arguments")
  void testIsCliRequestedArgs() {
    assertTrue(Main.isCliRequested(new String[] {"--cli"}));
    assertTrue(Main.isCliRequested(new String[] {"--web", "--cli"}));
    assertFalse(Main.isCliRequested(new String[] {"--web"}));
    assertFalse(Main.isCliRequested(new String[] {}));
    assertFalse(Main.isCliRequested(null));
  }

  @Test
  @DisplayName("isCliRequested respects system property")
  void testIsCliRequestedSystemProperty() {
    try {
      System.setProperty("cli", "true");
      assertTrue(Main.isCliRequested(new String[] {}));
    } finally {
      System.clearProperty("cli");
    }
  }

  @Test
  @DisplayName("filterLauncherArgs strips launcher flags and retains spring args")
  void testFilterLauncherArgs() {
    assertArrayEquals(new String[0], Main.filterLauncherArgs(null));
    assertArrayEquals(new String[0], Main.filterLauncherArgs(new String[] {"--web"}));
    assertArrayEquals(new String[0], Main.filterLauncherArgs(new String[] {"--cli"}));
    assertArrayEquals(
        new String[] {"--server.port=9090"},
        Main.filterLauncherArgs(new String[] {"--web", "--server.port=9090", "--cli"}));
  }
}
