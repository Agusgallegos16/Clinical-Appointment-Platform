package com.consultorio.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_TRACKED_KEYS = 10_000;
    private static final int SWEEP_INTERVAL_REQUESTS = 256;

    private static final Map<String, Integer> ENDPOINT_LIMITS = Map.of(
            "/api/auth/login", 10,
            "/api/auth/solicitar-restablecimiento-password", 5,
            "/api/auth/confirmar-restablecimiento-password", 10,
            "/api/auth/registro-paciente", 10,
            "/api/auth/oauth2/exchange", 20
    );

    private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    private final AtomicInteger requestSequence = new AtomicInteger(0);

    @Value("${app.security.trusted-proxies:}")
    private List<String> trustedProxies = Collections.emptyList();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        Integer maxRequests = "POST".equalsIgnoreCase(request.getMethod()) ? ENDPOINT_LIMITS.get(path) : null;

        if (maxRequests != null) {
            long ahora = System.currentTimeMillis();
            maybeEvict(ahora);

            String clientIp = obtenerClientIp(request);
            String key = clientIp + "|" + path;

            RequestCounter counter = requestCounts.compute(key, (k, c) -> {
                if (c == null || (ahora - c.startTime) > WINDOW_MS) {
                    return new RequestCounter(ahora, 1);
                }
                c.count++;
                return c;
            });

            if (counter.count > maxRequests) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"status\":429,\"error\":\"Demasiadas solicitudes\",\"mensaje\":\"Ha superado el límite de solicitudes permitidas. Por favor espere 1 minuto.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void maybeEvict(long ahora) {
        if (requestSequence.incrementAndGet() % SWEEP_INTERVAL_REQUESTS == 0 || requestCounts.size() >= MAX_TRACKED_KEYS) {
            requestCounts.entrySet().removeIf(entry -> (ahora - entry.getValue().startTime) > WINDOW_MS);
            if (requestCounts.size() >= MAX_TRACKED_KEYS) {
                Iterator<Map.Entry<String, RequestCounter>> it = requestCounts.entrySet().iterator();
                int removed = 0;
                while (it.hasNext() && removed < 1_000) {
                    it.next();
                    it.remove();
                    removed++;
                }
            }
        }
    }

    private String obtenerClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr() != null ? request.getRemoteAddr().trim() : "unknown";
        if (esProxyConfiable(remoteAddr)) {
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.isBlank()) {
                String[] hops = xfHeader.split(",");
                for (int i = hops.length - 1; i >= 0; i--) {
                    String candidate = hops[i].trim();
                    if (!candidate.isEmpty() && !esProxyExplicito(candidate)) {
                        return candidate;
                    }
                }
                return hops[0].trim();
            }
        }
        return remoteAddr;
    }

    private boolean esProxyExplicito(String ip) {
        if (trustedProxies == null) return false;
        for (String proxy : trustedProxies) {
            if (proxy != null && !proxy.isBlank() && !"*".equals(proxy.trim()) && proxy.trim().equalsIgnoreCase(ip)) {
                return true;
            }
        }
        return false;
    }

    private boolean esProxyConfiable(String remoteAddr) {
        if (trustedProxies == null || trustedProxies.isEmpty()) {
            return false;
        }
        for (String proxy : trustedProxies) {
            if (proxy == null || proxy.isBlank()) continue;
            String trimmed = proxy.trim();
            if ("*".equals(trimmed) || trimmed.equalsIgnoreCase(remoteAddr)) {
                return true;
            }
        }
        return false;
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
