package com.consultorio.adapter;

import com.consultorio.domain.Turno;
import com.consultorio.domain.Usuario;
import com.consultorio.service.GoogleCalendarOAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.google.calendar.enabled", havingValue = "true")
public class GoogleCalendarAdapter implements CalendarioAdapter {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarAdapter.class);

    private final GoogleCalendarOAuthService oauthService;

    @Autowired
    public GoogleCalendarAdapter(GoogleCalendarOAuthService oauthService) {
        this.oauthService = oauthService;
    }

    @Override
    public String agendarEventoParaUsuario(Turno turno, Usuario usuario) {
        if (usuario == null || !usuario.isGoogleCalendarConnected()) {
            log.info("ℹ️ El usuario {} no tiene Google Calendar vinculado. Se omite la sincronización.", usuario != null ? usuario.getEmail() : "null");
            return null;
        }

        return oauthService.crearEventoParaUsuario(turno, usuario);
    }

    @Override
    public void cancelarEventoParaUsuario(String googleEventId, Usuario usuario) {
        if (usuario == null || !usuario.isGoogleCalendarConnected() || googleEventId == null) {
            return;
        }

        oauthService.eliminarEventoParaUsuario(googleEventId, usuario);
    }
}
