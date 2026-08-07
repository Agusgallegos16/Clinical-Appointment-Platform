package com.consultorio.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_INTENTOS_POR_MINUTO = 10;
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Aplicar Rate Limiting específicamente al endpoint de login
        if ("/api/auth/login".equals(path) && "POST".equalsIgnoreCase(request.getMethod())) {
            String clientIp = obtenerClientIp(request);
            long ahora = System.currentTimeMillis();

            RequestCounter counter = requestCounts.compute(clientIp, (ip, c) -> {
                if (c == null || (ahora - c.startTime) > 60000) { // Reiniciar cada 60 segundos
                    return new RequestCounter(ahora, 1);
                }
                c.count++;
                return c;
            });

            if (counter.count > MAX_INTENTOS_POR_MINUTO) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"status\":429,\"error\":\"Demasiadas solicitudes\",\"mensaje\":\"Ha superado el límite de intentos de inicio de sesión. Por favor espere 1 minuto.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String obtenerClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private static class RequestCounter {
        final long startTime;
        int count;

        RequestCounter(long startTime, int count) {
            this.startTime = startTime;
            this.count = count;
        }
    }
}
