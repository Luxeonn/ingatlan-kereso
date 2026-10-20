package ingatlanok.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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

}
