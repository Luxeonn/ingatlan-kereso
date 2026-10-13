package ingatlanok;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    @DisplayName("Main instance creation")
    void testMainInstance() {
        Main main = new Main();
        assertNotNull(main);
    }
}
