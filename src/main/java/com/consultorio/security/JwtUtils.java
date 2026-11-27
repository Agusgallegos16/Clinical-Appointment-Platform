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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);
    private static final String GCAL_STATE_PURPOSE = "gcal_oauth_state";
    private static final long GCAL_STATE_EXPIRATION_MS = 5 * 60 * 1000L;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private int jwtExpirationMs;

    private SecretKey key;
    private SecretKey oauthStateKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.trim().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] derivedStateKey = sha256.digest(("gcal-oauth-state-v1:" + jwtSecret.trim()).getBytes(StandardCharsets.UTF_8));
            this.oauthStateKey = Keys.hmacShaKeyFor(derivedStateKey);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible para derivar clave OAuth state", e);
        }
    }

    public String generarTokenEstadoGoogleCalendar(Long usuarioId) {
        return generarTokenEstadoGoogleCalendar(usuarioId, GCAL_STATE_EXPIRATION_MS);
    }

    public String generarTokenEstadoGoogleCalendar(Long usuarioId, long ttlMs) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("El ID de usuario es obligatorio para firmar state OAuth");
        }
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + ttlMs);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claim("purpose", GCAL_STATE_PURPOSE)
                .claim("uid", usuarioId)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(this.oauthStateKey)
                .compact();
    }

    public Long validarYObtenerUsuarioIdEstadoGoogleCalendar(String stateToken) {
        if (stateToken == null || stateToken.isBlank()) {
            throw new IllegalArgumentException("Parámetro state OAuth vacío o ausente");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(this.oauthStateKey)
                    .build()
                    .parseSignedClaims(stateToken.trim())
                    .getPayload();

            if (!GCAL_STATE_PURPOSE.equals(claims.get("purpose", String.class))) {
                throw new IllegalArgumentException("Propósito de token state OAuth inválido");
            }
            Number uid = claims.get("uid", Number.class);
            if (uid == null) {
                throw new IllegalArgumentException("Token state OAuth sin identificador de usuario");
            }
            return uid.longValue();
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Token state de Google Calendar inválido o expirado", e);
        }
    }

    public String generarToken(String email, String rol) {
        return generarToken(email, rol, 0);
    }

    public String generarToken(String email, String rol, int tokenVersion) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("rol", rol)
                .claim("tv", tokenVersion)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(this.key)
                .compact();
    }

    public Integer obtenerTokenVersionDelToken(String token) {
        String tokenLimpio = limpiarToken(token);
        Number tv = Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(tokenLimpio)
                .getPayload()
                .get("tv", Number.class);
        return tv != null ? tv.intValue() : null;
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
