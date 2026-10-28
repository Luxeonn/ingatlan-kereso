package ingatlanok.config;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Assigns a correlation id to every request (MDC key {@code cid}, echoed in {@code
 * X-Correlation-Id}) and logs one line when the request starts and one when it ends.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter implements Filter {

  public static final String HEADER = "X-Correlation-Id";
  public static final String MDC_KEY = "cid";

  private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");
  private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);

  static String resolveId(String incoming) {
    if (incoming != null && VALID_ID.matcher(incoming.trim()).matches()) {
      return incoming.trim();
    }
    return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;
    String cid = resolveId(request.getHeader(HEADER));
    MDC.put(MDC_KEY, cid);
    response.setHeader(HEADER, cid);
    long start = System.nanoTime();
    LOGGER.info("Kérés: {} {}", request.getMethod(), request.getRequestURI());
    try {
      chain.doFilter(req, res);
    } finally {
      LOGGER.info(
          "Válasz: {} {} -> {} ({} ms)",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          (System.nanoTime() - start) / 1_000_000);
      MDC.remove(MDC_KEY);
    }
  }
}
