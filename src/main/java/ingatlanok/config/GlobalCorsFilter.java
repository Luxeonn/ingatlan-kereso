package ingatlanok.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Globális CORS és biztonsági szűrő, amely minden kérésre (beleértve a közvetlen OPTIONS preflight hívásokat is)
 * biztonságos keretek között garantálja az Access-Control és biztonsági fejlécek jelenlétét.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalCorsFilter implements Filter {

    private static final Pattern LOCALHOST_ORIGIN_PATTERN =
            Pattern.compile("^https?://(localhost|127\\.0\\.0\\.1)(:[0-9]+)?$");

    private boolean isAllowedOrigin(String origin) {
        if (origin == null) {
            return false;
        }
        return LOCALHOST_ORIGIN_PATTERN.matcher(origin.trim()).matches();
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        String origin = request.getHeader("Origin");
        if (origin != null && isAllowedOrigin(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin.trim());
            response.setHeader("Vary", "Origin");
        } else if (origin == null) {
            response.setHeader("Access-Control-Allow-Origin", "http://127.0.0.1");
            response.setHeader("Vary", "Origin");
        }

        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE, HEAD");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept, Origin, X-Requested-With");
        response.setHeader("Access-Control-Max-Age", "3600");

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; font-src 'self' data:; img-src 'self' data: https:; connect-src 'self' http://localhost:* http://127.0.0.1:*;");

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            if (origin != null && !isAllowedOrigin(origin)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }
}
