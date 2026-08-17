package com.consultorio.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    @Autowired
    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path != null && path.startsWith("/api/public/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = obtenerJwtDeHeader(request);
            if (jwt != null && jwtUtils.validarToken(jwt)) {
                String email = jwtUtils.obtenerEmailDelToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("🔓 Usuario autenticado correctamente con JWT: {} ({})", email, request.getRequestURI());
            }
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            log.warn("El token JWT presente en la petición pertenece a un usuario inexistente: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("No se pudo autenticar la petición con el token JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String obtenerJwtDeHeader(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth)) {
            String trimmed = headerAuth.trim();
            if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
                trimmed = trimmed.substring(7).trim();
            }
            if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
            return trimmed;
        }
        return null;
    }
}
