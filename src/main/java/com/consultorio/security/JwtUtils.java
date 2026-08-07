package com.consultorio.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private int jwtExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.trim().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generarToken(String email, String rol) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(this.key)
                .compact();
    }

    public String obtenerEmailDelToken(String token) {
        String tokenLimpio = limpiarToken(token);
        return Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(tokenLimpio)
                .getPayload()
                .getSubject();
    }

    public boolean validarToken(String authToken) {
        try {
            String tokenLimpio = limpiarToken(authToken);
            Jwts.parser().verifyWith(this.key).build().parseSignedClaims(tokenLimpio);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error al validar el token JWT: {}", e.getMessage());
        }
        return false;
    }

    private String limpiarToken(String token) {
        if (token == null) return null;
        String t = token.trim();
        if (t.regionMatches(true, 0, "Bearer ", 0, 7)) {
            t = t.substring(7).trim();
        }
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
            t = t.substring(1, t.length() - 1).trim();
        }
        return t;
    }
}
