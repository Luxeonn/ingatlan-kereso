package ingatlanok.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Spring WebMvc konfiguráció:
 * 1. CORS szabályok biztosítása a kliens és fejlesztői szerverek számára.
 * 2. Statikus SPA assetek kiszolgálása és kliensoldali routing (SPA Fallback az index.html-re).
 */
@Configuration
public class SpaWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Statikus eszközök (JS, CSS, képek) kiszolgálása
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource resource = super.getResource(resourcePath, location);
                        if (resource != null && resource.isReadable()) {
                            return resource;
                        }
                        // API kéréseket soha ne irányítson az index.html-re (hagyni kell a RestControllernek)
                        if (resourcePath.startsWith("api") || resourcePath.startsWith("actuator")) {
                            return null;
                        }
                        // SPA fallback: minden egyéb kliens útvonal esetén index.html-t adunk vissza
                        Resource indexHtml = super.getResource("index.html", location);
                        if (indexHtml != null && indexHtml.isReadable()) {
                            return indexHtml;
                        }
                        return null;
                    }
                });
    }
}
