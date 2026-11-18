package ingatlanok.config;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Single centralized CORS policy restricting cross-origin access to local loopback interfaces
 * (exact hostnames with optional ports, rejecting subdomain spoofing like localhost.attacker.com).
 */
@Configuration
public class WebCorsConfig {

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE + 2)
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(
        List.of(
            "http://localhost",
            "http://localhost:*",
            "https://localhost",
            "https://localhost:*",
            "http://127.0.0.1",
            "http://127.0.0.1:*",
            "https://127.0.0.1",
            "https://127.0.0.1:*",
            "http://0.0.0.0",
            "http://0.0.0.0:*",
            "https://0.0.0.0",
            "https://0.0.0.0:*",
            "http://[::1]",
            "http://[::1]:*",
            "https://[::1]",
            "https://[::1]:*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
    config.setAllowedHeaders(
        List.of("Content-Type", "Authorization", "Accept", "Origin", "X-Requested-With"));
    config.setAllowCredentials(false);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source) {
      @Override
      protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
            && request.getHeader("Origin") == null) {
          response.setHeader("Access-Control-Allow-Origin", "http://127.0.0.1");
          response.setHeader("Vary", "Origin");
          response.setStatus(HttpServletResponse.SC_OK);
          return;
        }
        super.doFilterInternal(request, response, filterChain);
      }
    };
  }
}
