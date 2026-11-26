package ingatlanok.config;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns a correlation id to every request (MDC key {@code cid}, echoed in {@code
 * X-Correlation-Id}) and logs one line when the request starts and one when it ends.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Correlation-Id";
  public static final String MDC_KEY = "cid";

  private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

  static String resolveId(String incoming) {
    if (incoming != null && VALID_ID.matcher(incoming.trim()).matches()) {
      return incoming.trim();
    }
    return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String cid = resolveId(request.getHeader(HEADER));
    MDC.put(MDC_KEY, cid);
    response.setHeader(HEADER, cid);
    long start = System.nanoTime();
    log.info("Kérés: {} {}", request.getMethod(), request.getRequestURI());
    try {
      filterChain.doFilter(request, response);
    } finally {
      log.info(
          "Válasz: {} {} -> {} ({} ms)",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          (System.nanoTime() - start) / 1_000_000);
      MDC.remove(MDC_KEY);
    }
  }
}
