package ingatlanok.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring WebMvc konfiguráció a headless REST API számára: 1. CORS szabályok biztosítása a kliens és
 * fejlesztői szerverek számára.
 */
@Configuration
public class SpaWebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOriginPatterns(
            "http://localhost*",
            "https://localhost*",
            "http://127.0.0.1*",
            "https://127.0.0.1*",
            "http://0.0.0.0*",
            "https://0.0.0.0*",
            "http://[::1]*",
            "https://[::1]*")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
        .allowedHeaders("Content-Type", "Authorization", "Accept", "Origin", "X-Requested-With")
        .allowCredentials(false)
        .maxAge(3600);
  }

  // Statikus SPA fallback és resource handler eltávolítva a headless REST API működéshez.
}
