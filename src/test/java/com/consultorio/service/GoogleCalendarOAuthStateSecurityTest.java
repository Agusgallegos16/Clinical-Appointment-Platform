package com.consultorio.service;

import com.consultorio.config.GoogleCalendarConfig;
import com.consultorio.repository.UsuarioRepository;
import com.consultorio.security.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(classes = {GoogleCalendarOAuthService.class, GoogleCalendarConfig.class, JwtUtils.class})
@TestPropertySource(properties = {
        "app.jwt.secret=test-google-calendar-state-hmac-secret-key-at-least-256-bits",
        "app.jwt.expiration-ms=86400000",
        "app.google.calendar.enabled=true",
        "app.google.calendar.client-id=test-client-id.apps.googleusercontent.com",
        "app.google.calendar.client-secret=test-client-secret",
        "app.google.calendar.redirect-uri=http://localhost:8080/api/google-calendar/callback"
})
class GoogleCalendarOAuthStateSecurityTest {

    @Autowired
    private GoogleCalendarOAuthService oauthService;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("CRIT-03: generarUrlAutorizacion debe emitir state firmado y procesarCallback debe rechazar IDs planos, tokens alterados y JWTs de login")
    void debeFirmarStateYRechazarStatesNoFirmadosAlteradosODeOtroProposito() {
        String authUrl = oauthService.generarUrlAutorizacion(42L);
        String stateParam = UriComponentsBuilder.fromUriString(authUrl)
                .build()
                .getQueryParams()
                .getFirst("state");

        assertNotNull(stateParam);
        assertNotEquals("42", stateParam, "El parámetro state no debe exponer el ID numérico plano sin firmar");
        assertTrue(stateParam.contains("."), "El parámetro state debe ser un token firmado");
        assertFalse(jwtUtils.validarToken(stateParam), "El token state OAuth no debe ser aceptado como JWT de sesión");

        // 1. State numérico plano sin firma (ataque CSRF original)
        assertThrows(RuntimeException.class, () -> oauthService.procesarCallback("auth-code", "42"));

        // 2. State con firma alterada (en bytes efectivos del HMAC)
        int idx = stateParam.length() - 5;
        char targetChar = stateParam.charAt(idx);
        String tamperedState = stateParam.substring(0, idx) + (targetChar == 'A' ? 'B' : 'A') + stateParam.substring(idx + 1);
        assertThrows(RuntimeException.class, () -> oauthService.procesarCallback("auth-code", tamperedState));

        // 3. JWT ordinario de sesión (sin propósito gcal_oauth_state y distinta clave derivada)
        String loginJwt = jwtUtils.generarToken("doctor@test.com", "DOCTOR");
        assertThrows(RuntimeException.class, () -> oauthService.procesarCallback("auth-code", loginJwt));

        // 4. State expirado (TTL vencido)
        String expiredState = jwtUtils.generarTokenEstadoGoogleCalendar(42L, -5000L);
        assertThrows(RuntimeException.class, () -> oauthService.procesarCallback("auth-code", expiredState));

        verifyNoInteractions(usuarioRepository);

        // 4. State legítimo firmado sí pasa validación criptográfica y extrae usuarioId = 42L
        when(usuarioRepository.findById(42L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> oauthService.procesarCallback("auth-code", stateParam));
        verify(usuarioRepository, times(1)).findById(42L);
    }
}
