package ingatlanok.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GraalVmRuntimeHintsTest {

    @Test
    @DisplayName("AppRuntimeHintsRegistrar sikeresen regisztrálja a reflexiós és erőforrás hint-eket")
    void testRegisterHints() {
        GraalVmRuntimeHints.AppRuntimeHintsRegistrar registrar = new GraalVmRuntimeHints.AppRuntimeHintsRegistrar();
        RuntimeHints hints = new RuntimeHints();
        registrar.registerHints(hints, getClass().getClassLoader());
        assertNotNull(hints.reflection());
        assertNotNull(hints.resources());
    }

    @Test
    @DisplayName("GraalVmRuntimeHints konstruktor példányosítható")
    void testConstructor() {
        GraalVmRuntimeHints config = new GraalVmRuntimeHints();
        assertNotNull(config);
    }
}
