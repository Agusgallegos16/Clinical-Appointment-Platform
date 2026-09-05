package com.consultorio.controller;

import com.consultorio.domain.Usuario;
import com.consultorio.security.SecurityUtils;
import com.consultorio.service.GoogleCalendarOAuthService;
import com.consultorio.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/api/google-calendar")
@Tag(name = "Google Calendar OAuth2", description = "Endpoints para vincular y sincronizar Google Calendar personal por usuario.")
public class GoogleCalendarController {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarController.class);

    private final GoogleCalendarOAuthService oauthService;
    private final SecurityUtils securityUtils;
    private final UsuarioService usuarioService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:${APP_FRONTEND_URL:http://localhost:5173}}")
    private String frontendUrl;

    @Autowired
    public GoogleCalendarController(GoogleCalendarOAuthService oauthService,
                                    SecurityUtils securityUtils,
                                    UsuarioService usuarioService) {
        this.oauthService = oauthService;
        this.securityUtils = securityUtils;
        this.usuarioService = usuarioService;
    }

    private String getCleanFrontendUrl() {
        if (frontendUrl == null || frontendUrl.isBlank()) return "http://localhost:5173";
        String clean = frontendUrl.trim();
        return clean.endsWith("/") ? clean.substring(0, clean.length() - 1) : clean;
    }

    @GetMapping("/auth-url")
    @Operation(summary = "Obtener URL de autorización de Google OAuth2 para el usuario autenticado")
    public ResponseEntity<?> obtenerAuthUrl() {
        String email = securityUtils.obtenerEmailUsuarioAutenticado();
        if (email == null) throw new AccessDeniedException("No autenticado");

        Usuario usuario = usuarioService.obtenerPorEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        try {
            String authUrl = oauthService.generarUrlAutorizacion(usuario.getId());
            return ResponseEntity.ok(Map.of("url", authUrl));
        } catch (Exception e) {
            log.error("Error al generar URL de autenticación Google OAuth2: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("message", "No se pudo obtener la URL de conexión con Google. Verifique credenciales."));
        }
    }

    @GetMapping("/callback")
    @Operation(summary = "Callback público de Google OAuth2 al otorgar consentimiento")
    public RedirectView callback(@RequestParam("code") String code, @RequestParam("state") String state) {
        String targetBaseUrl = getCleanFrontendUrl();
        try {
            oauthService.procesarCallback(code, state);
            return new RedirectView(targetBaseUrl + "/google-calendar/success");
        } catch (Exception e) {
            log.error("Error en callback de Google OAuth: {}", e.getMessage());
            return new RedirectView(targetBaseUrl + "/doctor?calendar_error=true");
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Consultar si el usuario autenticado tiene Google Calendar conectado")
    public ResponseEntity<Map<String, Object>> obtenerEstado() {
        String email = securityUtils.obtenerEmailUsuarioAutenticado();
        if (email == null) throw new AccessDeniedException("No autenticado");

        Usuario usuario = usuarioService.obtenerPorEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return ResponseEntity.ok(Map.of(
                "connected", usuario.isGoogleCalendarConnected(),
                "email", usuario.getEmail()
        ));
    }

    @PostMapping("/disconnect")
    @Operation(summary = "Desconectar Google Calendar del usuario autenticado")
    public ResponseEntity<Map<String, String>> desconectar() {
        String email = securityUtils.obtenerEmailUsuarioAutenticado();
        if (email == null) throw new AccessDeniedException("No autenticado");

        Usuario usuario = usuarioService.obtenerPorEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        oauthService.desconectar(usuario.getId());
        return ResponseEntity.ok(Map.of("message", "Google Calendar desconectado exitosamente."));
    }
}
