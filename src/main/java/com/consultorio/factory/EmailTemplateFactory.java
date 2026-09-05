package com.consultorio.factory;

import com.consultorio.domain.EstadoTurno;
import com.consultorio.dto.TurnoResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Factoría (Factory Pattern) para la generación de plantillas HTML de correo electrónico.
 * Garantiza un diseño estético moderno, sobrio y responsive unificado para todas las notificaciones del sistema.
 */
@Component
public class EmailTemplateFactory {

    @Value("${app.clinic.name:Instituto Médico Consultorios}")
    private String clinicName;

    @Value("${app.clinic.address:Av. Principal 1234, CABA}")
    private String clinicAddress;

    @Value("${app.clinic.phone:+54 11 1234-5678}")
    private String clinicPhone;

    @Value("${app.frontend.url:${APP_FRONTEND_URL:http://localhost:5173}}")
    private String frontendUrl;

    private String getCleanFrontendUrl() {
        if (frontendUrl == null || frontendUrl.isBlank()) return "http://localhost:5173";
        String clean = frontendUrl.trim();
        return clean.endsWith("/") ? clean.substring(0, clean.length() - 1) : clean;
    }

    // 1. Email de Verificación de Correo Electrónico
    public String crearEmailVerificacion(String nombreUsuario, String urlVerificacion) {
        String contenido = String.format(
                "<p style='color: #334155; font-size: 16px; margin: 0 0 16px 0; line-height: 1.5;'>Hola <strong style='color: #0f172a;'>%s</strong>,</p>" +
                "<p style='color: #475569; font-size: 15px; margin: 0 0 24px 0; line-height: 1.6;'>Gracias por registrarte en el sistema de gestión de turnos de <strong>%s</strong>. Para activar tu cuenta y comenzar a utilizar la plataforma, confirma tu correo electrónico ingresando al siguiente botón:</p>" +
                "<div style='text-align: center; margin: 28px 0;'>" +
                "  <a href='%s' style='display: inline-block; background-color: #0284c7; color: #ffffff; text-decoration: none; font-size: 15px; font-weight: 700; padding: 14px 28px; border-radius: 8px; box-shadow: 0 2px 8px rgba(2, 132, 199, 0.3);'>Confirmar Mi Correo Electrónico</a>" +
                "</div>" +
                "<div style='background-color: #fffbeb; border: 1px solid #fef3c7; border-radius: 8px; padding: 14px 18px; margin-top: 20px;'>" +
                "  <p style='color: #92400e; font-size: 13px; margin: 0; line-height: 1.5;'>⏱️ <strong>Nota de Seguridad:</strong> Este enlace expira en 24 horas.</p>" +
                "</div>",
                nombreUsuario, clinicName, urlVerificacion
        );
        return construirBaseTemplate("Confirmación de Cuenta", "Activación de Usuario", contenido);
    }

    // 2. Email de Activación para Doctores
    public String crearEmailActivacionDoctor(String nombreDoctor, String urlActivacion) {
        String contenido = String.format(
                "<p style='color: #334155; font-size: 16px; margin: 0 0 16px 0; line-height: 1.5;'>Estimado/a <strong style='color: #0f172a;'>Dr/a. %s</strong>,</p>" +
                "<p style='color: #475569; font-size: 15px; margin: 0 0 24px 0; line-height: 1.6;'>Has sido dado/a de alta como profesional médico en <strong>%s</strong>. Para configurar tu contraseña e ingresar por primera vez, por favor haz clic en el siguiente botón:</p>" +
                "<div style='text-align: center; margin: 28px 0;'>" +
                "  <a href='%s' style='display: inline-block; background-color: #0284c7; color: #ffffff; text-decoration: none; font-size: 15px; font-weight: 700; padding: 14px 28px; border-radius: 8px; box-shadow: 0 2px 8px rgba(2, 132, 199, 0.3);'>Establecer Mi Contraseña</a>" +
                "</div>" +
                "<div style='background-color: #fffbeb; border: 1px solid #fef3c7; border-radius: 8px; padding: 14px 18px; margin-top: 20px;'>" +
                "  <p style='color: #92400e; font-size: 13px; margin: 0; line-height: 1.5;'>⚠️ <strong>Importante:</strong> Este enlace expira en 24 horas. Transcurrido dicho plazo, el registro se descartará automáticamente.</p>" +
                "</div>",
                nombreDoctor, clinicName, urlActivacion
        );
        return construirBaseTemplate("Bienvenido/a a " + clinicName, "Configuración de Contraseña Médica", contenido);
    }

    // 3. Email de Restablecimiento de Contraseña
    public String crearEmailRestablecerPassword(String urlConfirmacion) {
        String contenido = String.format(
                "<p style='color: #334155; font-size: 16px; margin: 0 0 16px 0; line-height: 1.5;'>Hola,</p>" +
                "<p style='color: #475569; font-size: 15px; margin: 0 0 24px 0; line-height: 1.6;'>Hemos recibido una solicitud para restablecer la contraseña de tu cuenta en <strong>%s</strong>. Haz clic en el botón a continuación para autorizar el cambio:</p>" +
                "<div style='text-align: center; margin: 28px 0;'>" +
                "  <a href='%s' style='display: inline-block; background-color: #0284c7; color: #ffffff; text-decoration: none; font-size: 15px; font-weight: 700; padding: 14px 28px; border-radius: 8px; box-shadow: 0 2px 8px rgba(2, 132, 199, 0.3);'>Restablecer Mi Contraseña</a>" +
                "</div>" +
                "<p style='color: #64748b; font-size: 13px; line-height: 1.5;'>Si no solicitaste este cambio, puedes ignorar este mensaje de forma segura y tu contraseña actual no será modificada. Este enlace vence en 1 hora.</p>",
                clinicName, urlConfirmacion
        );
        return construirBaseTemplate("Seguridad de la Cuenta", "Solicitud de Cambio de Contraseña", contenido);
    }

    // 4. Email de Bienvenida al Paciente
    public String crearEmailBienvenida(String nombreUsuario) {
        String contenido = String.format(
                "<p style='color: #334155; font-size: 16px; margin: 0 0 16px 0; line-height: 1.5;'>Hola <strong style='color: #0f172a;'>%s</strong>,</p>" +
                "<p style='color: #475569; font-size: 15px; margin: 0 0 24px 0; line-height: 1.6;'>¡Tu cuenta ha sido creada exitosamente! Ya puedes ingresar al portal web para consultar profesionales, horarios disponibles y reservar tus turnos médicos.</p>" +
                "<table role='presentation' width='100%%' cellspacing='0' cellpadding='0' border='0' style='margin-bottom: 24px; border: 1px solid #e2e8f0; border-radius: 8px;'>" +
                "  <tr><td style='padding: 12px 16px; border-bottom: 1px solid #f1f5f9; color: #64748b; font-size: 14px;'>Dirección:</td><td style='padding: 12px 16px; border-bottom: 1px solid #f1f5f9; color: #0f172a; font-weight: 600; font-size: 14px;'>%s</td></tr>" +
                "  <tr><td style='padding: 12px 16px; color: #64748b; font-size: 14px;'>Teléfono:</td><td style='padding: 12px 16px; color: #0f172a; font-weight: 600; font-size: 14px;'>%s</td></tr>" +
                "</table>" +
                "<div style='text-align: center; margin: 28px 0;'>" +
                "  <a href='%s/login' style='display: inline-block; background-color: #0284c7; color: #ffffff; text-decoration: none; font-size: 15px; font-weight: 700; padding: 14px 28px; border-radius: 8px;'>Ingresar a Mi Cuenta</a>" +
                "</div>",
                nombreUsuario, clinicAddress, clinicPhone, getCleanFrontendUrl()
        );
        return construirBaseTemplate("¡Bienvenido/a a " + clinicName + "!", "Registro Completado Exitosamente", contenido);
    }

    // 5. Email de Confirmación de Turno
    public String crearEmailConfirmacionTurno(TurnoResponseDTO turno) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");
        String fechaFormateada = turno.getFechaHora().format(formatter);
        String osInfo = (turno.getObraSocial() != null && !turno.getObraSocial().trim().isEmpty()) ? turno.getObraSocial() : "Particular / Sin Obra Social";

        String contenido = String.format(
                "<p style='color: #334155; font-size: 16px; margin: 0 0 16px 0; line-height: 1.5;'>Hola <strong style='color: #0f172a;'>%s</strong>,</p>" +
                "<p style='color: #475569; font-size: 15px; margin: 0 0 24px 0; line-height: 1.6;'>Tu turno médico ha sido <strong>CONFIRMADO</strong> con éxito. A continuación te presentamos el detalle completo de tu cita:</p>" +
                "<div style='background-color: #f0f9ff; border-left: 4px solid #0284c7; border-radius: 8px; padding: 16px; margin-bottom: 24px; text-align: center;'>" +
                "  <span style='display: block; color: #0369a1; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.8px; margin-bottom: 4px;'>FECHA Y HORA PROGRAMADA</span>" +
                "  <span style='display: block; color: #0284c7; font-size: 22px; font-weight: 800;'>%s hs</span>" +
                "</div>" +
                "<table role='presentation' width='100%%' cellspacing='0' cellpadding='0' border='0' style='margin-bottom: 24px;'>" +
                "  <tr><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #64748b; font-size: 14px; width: 38%%;'>Profesional Médico:</td><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #0f172a; font-size: 15px; font-weight: 700;'>Dr/a. %s</td></tr>" +
                "  <tr><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #64748b; font-size: 14px;'>Especialidad:</td><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #0f172a; font-size: 15px; font-weight: 600;'>%s</td></tr>" +
                "  <tr><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #64748b; font-size: 14px;'>Cobertura Médica:</td><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #0f172a; font-size: 15px;'>%s</td></tr>" +
                "  <tr><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #64748b; font-size: 14px;'>Motivo de Consulta:</td><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #0f172a; font-size: 15px;'>%s</td></tr>" +
                "  <tr><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #64748b; font-size: 14px;'>Dirección:</td><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #0f172a; font-size: 15px; font-weight: 600;'>%s</td></tr>" +
                "  <tr><td style='padding: 10px 0; color: #64748b; font-size: 14px;'>Teléfono:</td><td style='padding: 10px 0; color: #0f172a; font-size: 15px; font-weight: 600;'>%s</td></tr>" +
                "</table>" +
                "<div style='background-color: #fffbeb; border: 1px solid #fef3c7; border-radius: 8px; padding: 14px 18px; margin-bottom: 24px;'>" +
                "  <p style='color: #92400e; font-size: 13px; margin: 0; line-height: 1.5;'>📌 <strong>Indicación:</strong> Por favor, concurrir 15 minutos antes con DNI y credencial médica vigente.</p>" +
                "</div>" +
                "<div style='text-align: center; margin: 24px 0 12px 0;'>" +
                "  <a href='%s/paciente/turnos' style='display: inline-block; background-color: #0284c7; color: #ffffff; text-decoration: none; font-size: 15px; font-weight: 700; padding: 14px 28px; border-radius: 8px;'>Ver Mi Turno en el Portal</a>" +
                "</div>",
                turno.getPacienteNombre(), fechaFormateada, turno.getDoctorNombre(), turno.getEspecialidadNombre(),
                osInfo, turno.getMotivoConsulta() != null ? turno.getMotivoConsulta() : "Consulta General",
                clinicAddress, clinicPhone, getCleanFrontendUrl()
        );
        return construirBaseTemplate("Confirmación de Turno", "Cita Médica Reservada", contenido);
    }

    // 6. Email de Recordatorio 48hs
    public String crearEmailRecordatorioTurno48hs(TurnoResponseDTO turno) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");
        String fechaFormateada = turno.getFechaHora().format(formatter);

        String contenido = String.format(
                "<p style='color: #334155; font-size: 16px; margin: 0 0 16px 0; line-height: 1.5;'>Hola <strong style='color: #0f172a;'>%s</strong>,</p>" +
                "<p style='color: #475569; font-size: 15px; margin: 0 0 24px 0; line-height: 1.6;'>Te recordamos que tienes una cita médica programada en las próximas <strong>48 horas</strong>:</p>" +
                "<div style='background-color: #f0f9ff; border-left: 4px solid #0284c7; border-radius: 8px; padding: 16px; margin-bottom: 24px; text-align: center;'>" +
                "  <span style='display: block; color: #0369a1; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.8px; margin-bottom: 4px;'>FECHA Y HORA PRÓXIMA</span>" +
                "  <span style='display: block; color: #0284c7; font-size: 22px; font-weight: 800;'>%s hs</span>" +
                "</div>" +
                "<table role='presentation' width='100%%' cellspacing='0' cellpadding='0' border='0' style='margin-bottom: 24px;'>" +
                "  <tr><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #64748b; font-size: 14px; width: 38%%;'>Profesional Médico:</td><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #0f172a; font-size: 15px; font-weight: 700;'>Dr/a. %s</td></tr>" +
                "  <tr><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #64748b; font-size: 14px;'>Especialidad:</td><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #0f172a; font-size: 15px; font-weight: 600;'>%s</td></tr>" +
                "  <tr><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #64748b; font-size: 14px;'>Dirección:</td><td style='padding: 10px 0; border-bottom: 1px solid #f1f5f9; color: #0f172a; font-size: 15px;'>%s</td></tr>" +
                "  <tr><td style='padding: 10px 0; color: #64748b; font-size: 14px;'>Teléfono:</td><td style='padding: 10px 0; color: #0f172a; font-size: 15px;'>%s</td></tr>" +
                "</table>" +
                "<p style='color: #64748b; font-size: 13px; text-align: center; margin-top: 20px;'>En caso de no poder asistir, por favor cancela tu turno desde la plataforma para liberar la vacante a otro paciente.</p>",
                turno.getPacienteNombre(), fechaFormateada, turno.getDoctorNombre(), turno.getEspecialidadNombre(), clinicAddress, clinicPhone
        );
        return construirBaseTemplate("Recordatorio de Turno", "Cita en 48 Horas", contenido);
    }

    // 7. Email de Cancelación de Turno por parte del Doctor
    public String crearEmailCancelacionDoctor(TurnoResponseDTO turno, String motivoCancelacion) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");
        String fechaFormateada = turno.getFechaHora().format(formatter);

        String contenido = String.format(
                "<p style='color: #334155; font-size: 16px; margin: 0 0 16px 0; line-height: 1.5;'>Estimado/a <strong style='color: #0f172a;'>%s</strong>,</p>" +
                "<p style='color: #475569; font-size: 15px; margin: 0 0 20px 0; line-height: 1.6;'>Lamentamos informarte que la cita médica programada para el día <strong>%s hs</strong> con el/la Dr/a. <strong>%s</strong> (%s) ha debido ser cancelada.</p>" +
                "<div style='background-color: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; padding: 16px; margin-bottom: 24px;'>" +
                "  <span style='display: block; color: #991b1b; font-size: 13px; font-weight: 700; margin-bottom: 4px;'>📝 Motivo del Profesional:</span>" +
                "  <span style='display: block; color: #7f1d1d; font-size: 14px; font-style: italic;'>\"%s\"</span>" +
                "</div>" +
                "<p style='color: #475569; font-size: 14px; line-height: 1.6;'>Te pedimos disculpas por los inconvenientes generados. Te invitamos a ingresar al portal para agendar una nueva cita en el horario de tu preferencia.</p>" +
                "<div style='text-align: center; margin: 24px 0;'>" +
                "  <a href='%s/paciente/reservar' style='display: inline-block; background-color: #0284c7; color: #ffffff; text-decoration: none; font-size: 15px; font-weight: 700; padding: 14px 28px; border-radius: 8px;'>Reprogramar Nuevo Turno</a>" +
                "</div>",
                turno.getPacienteNombre(), fechaFormateada, turno.getDoctorNombre(), turno.getEspecialidadNombre(), motivoCancelacion, getCleanFrontendUrl()
        );
        return construirBaseTemplate("Aviso de Cancelación", "Modificación de Agenda Médica", contenido);
    }

    // 8. Email de Resumen Diario para el Doctor
    public String crearEmailResumenDiarioDoctor(String doctorNombre, String fechaMañana, List<TurnoResponseDTO> turnos) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<p style='color: #334155; font-size: 16px; margin: 0 0 16px 0;'>Estimado/a <strong style='color: #0f172a;'>Dr/a. %s</strong>,</p>", doctorNombre));
        sb.append(String.format("<p style='color: #475569; font-size: 15px; margin: 0 0 20px 0;'>Le presentamos la nómina de turnos médica agendados en <strong>%s</strong> para el día de mañana (<strong>%s</strong>):</p>", clinicName, fechaMañana));

        DateTimeFormatter horaFormatter = DateTimeFormatter.ofPattern("HH:mm");
        sb.append("<table role='presentation' width='100%' cellspacing='0' cellpadding='0' border='0' style='border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; margin-bottom: 24px;'>");
        sb.append("  <tr style='background-color: #f8fafc;'><th style='padding: 10px 12px; text-align: left; color: #64748b; font-size: 13px;'>Hora</th><th style='padding: 10px 12px; text-align: left; color: #64748b; font-size: 13px;'>Paciente</th><th style='padding: 10px 12px; text-align: left; color: #64748b; font-size: 13px;'>Especialidad</th><th style='padding: 10px 12px; text-align: left; color: #64748b; font-size: 13px;'>Motivo</th></tr>");

        for (TurnoResponseDTO t : turnos) {
            sb.append("  <tr>");
            sb.append(String.format("    <td style='padding: 10px 12px; border-top: 1px solid #f1f5f9; color: #0284c7; font-weight: 700; font-size: 14px;'>%s hs</td>", t.getFechaHora().format(horaFormatter)));
            sb.append(String.format("    <td style='padding: 10px 12px; border-top: 1px solid #f1f5f9; color: #0f172a; font-weight: 600; font-size: 14px;'>%s</td>", t.getPacienteNombre()));
            sb.append(String.format("    <td style='padding: 10px 12px; border-top: 1px solid #f1f5f9; color: #475569; font-size: 13px;'>%s</td>", t.getEspecialidadNombre()));
            sb.append(String.format("    <td style='padding: 10px 12px; border-top: 1px solid #f1f5f9; color: #64748b; font-size: 13px;'>%s</td>", t.getMotivoConsulta() != null ? t.getMotivoConsulta() : "Consulta General"));
            sb.append("  </tr>");
        }
        sb.append("</table>");

        return construirBaseTemplate("Agenda Médica Diaria", "Turnos de Mañana (" + fechaMañana + ")", sb.toString());
    }

    // 9. Email de Resumen Semanal para el Doctor
    public String crearEmailResumenSemanalDoctor(String doctorNombre, String periodoFormateado, List<TurnoResponseDTO> turnos) {
        long completados = turnos.stream().filter(t -> t.getEstado() == EstadoTurno.COMPLETADO).count();
        long cancelados = turnos.stream().filter(t -> t.getEstado() == EstadoTurno.CANCELADO).count();
        long confirmados = turnos.stream().filter(t -> t.getEstado() == EstadoTurno.CONFIRMADO || t.getEstado() == EstadoTurno.PENDIENTE).count();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<p style='color: #334155; font-size: 16px; margin: 0 0 16px 0;'>Estimado/a <strong style='color: #0f172a;'>Dr/a. %s</strong>,</p>", doctorNombre));
        sb.append(String.format("<p style='color: #475569; font-size: 15px; margin: 0 0 20px 0;'>A continuación se detalla el informe semanal de actividad (%s):</p>", periodoFormateado));

        sb.append("<table role='presentation' width='100%' cellspacing='0' cellpadding='0' border='0' style='margin-bottom: 24px; background-color: #f8fafc; border-radius: 8px; padding: 16px;'>");
        sb.append("  <tr>");
        sb.append(String.format("    <td style='text-align: center;'><span style='display:block; color: #64748b; font-size: 12px;'>TOTAL</span><span style='font-size: 20px; font-weight: 800; color: #0f172a;'>%d</span></td>", turnos.size()));
        sb.append(String.format("    <td style='text-align: center;'><span style='display:block; color: #16a34a; font-size: 12px;'>COMPLETADOS</span><span style='font-size: 20px; font-weight: 800; color: #16a34a;'>%d</span></td>", completados));
        sb.append(String.format("    <td style='text-align: center;'><span style='display:block; color: #0284c7; font-size: 12px;'>CONFIRMADOS</span><span style='font-size: 20px; font-weight: 800; color: #0284c7;'>%d</span></td>", confirmados));
        sb.append(String.format("    <td style='text-align: center;'><span style='display:block; color: #dc2626; font-size: 12px;'>CANCELADOS</span><span style='font-size: 20px; font-weight: 800; color: #dc2626;'>%d</span></td>", cancelados));
        sb.append("  </tr>");
        sb.append("</table>");

        return construirBaseTemplate("Informe Semanal de Actividad", periodoFormateado, sb.toString());
    }

    /**
     * Plantilla base universal estilo Card (Clean UI) que envuelve a todas las notificaciones.
     */
    private String construirBaseTemplate(String tituloBanner, String subtituloBanner, String contenidoHtml) {
        return String.format(
                "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>" +
                "<body style='margin: 0; padding: 0; background-color: #f4f6f8; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif;'>" +
                "  <table role='presentation' width='100%%' cellspacing='0' cellpadding='0' border='0' style='background-color: #f4f6f8; padding: 30px 15px;'>" +
                "    <tr>" +
                "      <td align='center'>" +
                "        <table role='presentation' width='100%%' cellspacing='0' cellpadding='0' border='0' style='max-width: 600px; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06); border: 1px solid #e2e8f0;'>" +
                "          <tr>" +
                "            <td style='background-color: #0284c7; padding: 26px 30px; text-align: center;'>" +
                "              <h1 style='color: #ffffff; margin: 0; font-size: 22px; font-weight: 800; letter-spacing: -0.5px;'>%s</h1>" +
                "              <p style='color: #e0f2fe; margin: 4px 0 0 0; font-size: 13px; font-weight: 500;'>%s | %s</p>" +
                "            </td>" +
                "          </tr>" +
                "          <tr>" +
                "            <td style='padding: 32px 30px;'>" +
                "              %s" +
                "            </td>" +
                "          </tr>" +
                "          <tr>" +
                "            <td style='background-color: #f8fafc; padding: 18px 30px; text-align: center; border-top: 1px solid #f1f5f9;'>" +
                "              <p style='color: #94a3b8; font-size: 12px; margin: 0; line-height: 1.5;'>© 2026 %s. Todos los derechos reservados.</p>" +
                "            </td>" +
                "          </tr>" +
                "        </table>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>",
                clinicName, tituloBanner, subtituloBanner, contenidoHtml, clinicName
        );
    }
}
