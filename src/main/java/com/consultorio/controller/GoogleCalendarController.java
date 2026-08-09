package com.consultorio.controller;

import com.consultorio.domain.Usuario;
import com.consultorio.repository.UsuarioRepository;
import com.consultorio.security.SecurityUtils;
import com.consultorio.service.GoogleCalendarOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/api/google-calendar")
@Tag(name = "Google Calendar OAuth2", description = "Endpoints para vincular y sincronizar Google Calendar personal por usuario.")
public class GoogleCalendarController {

    private final GoogleCalendarOAuthService oauthService;
    private final SecurityUtils securityUtils;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public GoogleCalendarController(GoogleCalendarOAuthService oauthService,
                                    SecurityUtils securityUtils,
                                    UsuarioRepository usuarioRepository) {
        this.oauthService = oauthService;
        this.securityUtils = securityUtils;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/auth-url")
    @Operation(summary = "Obtener URL de autorización de Google OAuth2 para el usuario autenticado")
    public ResponseEntity<Map<String, String>> obtenerAuthUrl() {
        String email = securityUtils.obtenerEmailUsuarioAutenticado();
        if (email == null) throw new AccessDeniedException("No autenticado");

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String authUrl = oauthService.generarUrlAutorizacion(usuario.getId());
        return ResponseEntity.ok(Map.of("url", authUrl));
    }

    @GetMapping("/callback")
    @Operation(summary = "Callback público de Google OAuth2 al otorgar consentimiento")
    public RedirectView callback(@RequestParam("code") String code, @RequestParam("state") String state) {
        oauthService.procesarCallback(code, state);
        return new RedirectView("http://localhost:5173/google-calendar/success");
    }

    @GetMapping("/status")
    @Operation(summary = "Consultar si el usuario autenticado tiene Google Calendar conectado")
    public ResponseEntity<Map<String, Object>> obtenerEstado() {
        String email = securityUtils.obtenerEmailUsuarioAutenticado();
        if (email == null) throw new AccessDeniedException("No autenticado");

        Usuario usuario = usuarioRepository.findByEmail(email)
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

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        oauthService.desconectar(usuario.getId());
        return ResponseEntity.ok(Map.of("message", "Google Calendar desconectado exitosamente."));
    }
}
