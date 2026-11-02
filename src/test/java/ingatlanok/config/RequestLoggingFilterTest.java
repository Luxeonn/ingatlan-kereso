package ingatlanok.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {

  private final RequestLoggingFilter filter = new RequestLoggingFilter();

  @Test
  void setsIdInMdcAndHeaderAndClearsAfterwards() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> inside = new AtomicReference<>();
    filter.doFilter(
        new MockHttpServletRequest("POST", "/api/search"),
        response,
        (req, res) -> inside.set(MDC.get(RequestLoggingFilter.MDC_KEY)));
    assertEquals(inside.get(), response.getHeader(RequestLoggingFilter.HEADER));
    assertEquals(16, inside.get().length());
    assertNull(MDC.get(RequestLoggingFilter.MDC_KEY));
  }

  @Test
  void acceptsSafeInboundIdAndReplacesUnsafeOnes() {
    assertEquals("trace-123.AB_cd", RequestLoggingFilter.resolveId(" trace-123.AB_cd "));
    assertEquals(16, RequestLoggingFilter.resolveId("bad id\r\nX: 1").length());
    assertEquals(16, RequestLoggingFilter.resolveId(null).length());
  }

  @Test
  void clearsMdcWhenChainFails() {
    try {
      filter.doFilter(
          new MockHttpServletRequest("GET", "/api/x"),
          new MockHttpServletResponse(),
          (req, res) -> {
            throw new jakarta.servlet.ServletException("fail");
          });
    } catch (Exception expected) {
      assertEquals("fail", expected.getMessage());
    }
    assertNull(MDC.get(RequestLoggingFilter.MDC_KEY));
  }
}
