package com.consultorio.service;

import com.consultorio.config.GoogleCalendarConfig;
import com.consultorio.domain.Turno;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.UsuarioRepository;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;

@Service
public class GoogleCalendarOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarOAuthService.class);

    private final GoogleCalendarConfig config;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public GoogleCalendarOAuthService(GoogleCalendarConfig config, UsuarioRepository usuarioRepository) {
        this.config = config;
        this.usuarioRepository = usuarioRepository;
    }

    public String generarUrlAutorizacion(Long usuarioId) {
        if (config.getClientId() == null || config.getClientId().isBlank()) {
            throw new IllegalStateException("Falta configurar GOOGLE_CLIENT_ID en las variables de entorno o en application.properties.");
        }
        if (config.getClientSecret() == null || config.getClientSecret().isBlank()) {
            throw new IllegalStateException("Falta configurar GOOGLE_CLIENT_SECRET en las variables de entorno o en application.properties.");
        }

        try {
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    config.getClientId(),
                    config.getClientSecret(),
                    Collections.singleton(CalendarScopes.CALENDAR))
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .build();

            return flow.newAuthorizationUrl()
                    .setRedirectUri(config.getRedirectUri())
                    .setState(String.valueOf(usuarioId))
                    .build();
        } catch (Exception e) {
            log.error("Error al generar URL de autorización OAuth2: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo iniciar el proceso de vincular Google Calendar.", e);
        }
    }

    public void procesarCallback(String code, String state) {
        try {
            Long usuarioId = Long.parseLong(state);
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    config.getClientId(),
                    config.getClientSecret(),
                    Collections.singleton(CalendarScopes.CALENDAR))
                    .setAccessType("offline")
                    .build();

            TokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri(config.getRedirectUri())
                    .execute();

            usuario.setGoogleAccessToken(tokenResponse.getAccessToken());
            if (tokenResponse.getRefreshToken() != null) {
                usuario.setGoogleRefreshToken(tokenResponse.getRefreshToken());
            }
            if (tokenResponse.getExpiresInSeconds() != null) {
                usuario.setGoogleTokenExpiry(System.currentTimeMillis() + (tokenResponse.getExpiresInSeconds() * 1000));
            }
            usuario.setGoogleCalendarConnected(true);
            usuarioRepository.save(usuario);

            log.info("✅ Google Calendar vinculado exitosamente para el usuario: {}", usuario.getEmail());

        } catch (Exception e) {
            log.error("Error al procesar callback OAuth2 de Google Calendar: {}", e.getMessage(), e);
            throw new RuntimeException("Error en la vinculación con Google Calendar.", e);
        }
    }

    public Calendar obtenerCalendarClientParaUsuario(Usuario usuario) {
        if (usuario == null || !usuario.isGoogleCalendarConnected() || usuario.getGoogleRefreshToken() == null) {
            return null;
        }

        try {
            // Verificar si el token venció y refrescarlo
            if (usuario.getGoogleTokenExpiry() != null && System.currentTimeMillis() > (usuario.getGoogleTokenExpiry() - 60000)) {
                refrescarAccessToken(usuario);
            }

            Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                    .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                    .setJsonFactory(GsonFactory.getDefaultInstance())
                    .build();

            credential.setAccessToken(usuario.getGoogleAccessToken());

            return new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName("SistemaDeGestionDeTurnos")
                    .build();

        } catch (Exception e) {
            log.error("Error al instanciar cliente de Google Calendar para usuario {}: {}", usuario.getEmail(), e.getMessage());
            return null;
        }
    }

    private void refrescarAccessToken(Usuario usuario) {
        try {
            TokenResponse response = new GoogleRefreshTokenRequest(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    usuario.getGoogleRefreshToken(),
                    config.getClientId(),
                    config.getClientSecret())
                    .execute();

            usuario.setGoogleAccessToken(response.getAccessToken());
            if (response.getExpiresInSeconds() != null) {
                usuario.setGoogleTokenExpiry(System.currentTimeMillis() + (response.getExpiresInSeconds() * 1000));
            }
            usuarioRepository.save(usuario);
            log.info("🔄 Access token de Google Calendar refrescado para usuario: {}", usuario.getEmail());

        } catch (Exception e) {
            log.error("No se pudo refrescar el token de Google para usuario {}: {}", usuario.getEmail(), e.getMessage());
        }
    }

    public String crearEventoParaUsuario(Turno turno, Usuario usuario) {
        Calendar calendar = obtenerCalendarClientParaUsuario(usuario);
        if (calendar == null) return null;

        try {
            Event event = new Event();
            String titulo;
            if (usuario.getRol() == com.consultorio.domain.Rol.DOCTOR) {
                titulo = String.format("Turno Médico (%s) - Paciente: %s %s",
                        turno.getEspecialidad().getNombre(),
                        turno.getPaciente().getNombre(),
                        turno.getPaciente().getApellido());
            } else {
                titulo = String.format("Turno Médico (%s) - Dr/a. %s %s",
                        turno.getEspecialidad().getNombre(),
                        turno.getDoctor().getNombre(),
                        turno.getDoctor().getApellido());
            }

            String descripcion = String.format(
                    "📌 Turno Médico Confirmado\n\n" +
                    " - Médico: Dr/a. %s %s\n" +
                    " - Paciente: %s %s\n" +
                    " - Especialidad: %s\n" +
                    " - Motivo: %s\n",
                    turno.getDoctor().getNombre(), turno.getDoctor().getApellido(),
                    turno.getPaciente().getNombre(), turno.getPaciente().getApellido(),
                    turno.getEspecialidad().getNombre(),
                    turno.getMotivoConsulta() != null ? turno.getMotivoConsulta() : "Consulta General"
            );

            event.setSummary(titulo);
            event.setDescription(descripcion);

            ZoneId zoneId = ZoneId.systemDefault();
            ZonedDateTime inicioZoned = turno.getFechaHora().atZone(zoneId);
            ZonedDateTime finZoned = inicioZoned.plusMinutes(30);

            DateTime startDateTime = new DateTime(inicioZoned.toInstant().toEpochMilli());
            DateTime endDateTime = new DateTime(finZoned.toInstant().toEpochMilli());

            event.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone(zoneId.getId()));
            event.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone(zoneId.getId()));

            Event createdEvent = calendar.events()
                    .insert("primary", event)
                    .execute();

            log.info("📅 Evento agendado exitosamente en Google Calendar personal de [{}] con ID: {}", usuario.getEmail(), createdEvent.getId());
            return createdEvent.getId();

        } catch (Exception e) {
            log.error("Error al crear evento en Google Calendar personal de {}: {}", usuario.getEmail(), e.getMessage());
            return null;
        }
    }

    public void eliminarEventoParaUsuario(String googleEventId, Usuario usuario) {
        Calendar calendar = obtenerCalendarClientParaUsuario(usuario);
        if (calendar == null || googleEventId == null) return;

        try {
            calendar.events()
                    .delete("primary", googleEventId)
                    .execute();

            log.info("📅 Evento {} eliminado del Google Calendar personal de [{}]", googleEventId, usuario.getEmail());
        } catch (Exception e) {
            log.error("Error al eliminar evento {} del Google Calendar de {}: {}", googleEventId, usuario.getEmail(), e.getMessage());
        }
    }

    public void desconectar(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));

        usuario.setGoogleAccessToken(null);
        usuario.setGoogleRefreshToken(null);
        usuario.setGoogleTokenExpiry(null);
        usuario.setGoogleCalendarConnected(false);
        usuarioRepository.save(usuario);

        log.info("🔌 Google Calendar desconectado para usuario: {}", usuario.getEmail());
    }
}
