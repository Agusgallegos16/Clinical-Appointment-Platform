package com.consultorio.service;

import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.email.EmailSender;
import com.consultorio.factory.EmailTemplateFactory;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final EmailSender emailSender;
    private final JavaMailSender mailSender;
    private final EmailTemplateFactory emailTemplateFactory;

    @Value("${app.mail.from:notificaciones@consultorio.com}")
    private String mailFrom;

    @Value("${app.clinic.name:Instituto Médico Consultorios}")
    private String clinicName;

    @Autowired
    public EmailService(EmailSender emailSender,
                        ObjectProvider<JavaMailSender> mailSenderProvider,
                        EmailTemplateFactory emailTemplateFactory) {
        this.emailSender = emailSender;
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.emailTemplateFactory = emailTemplateFactory;
    }

    // 1. Email de Verificación de Correo Electrónico
    public void enviarEmailVerificacion(String emailDestino, String nombreUsuario, String token) {
        String urlVerificacion = "http://localhost:5173/confirmar-email?token=" + token;
        String asunto = "Confirma tu correo electrónico - " + clinicName;
        String htmlContent = emailTemplateFactory.crearEmailVerificacion(nombreUsuario, urlVerificacion);
        enviarCorreoHtml(emailDestino, asunto, htmlContent);
    }

    // 2. Email de Activación de Cuenta para Doctores
    public void enviarEmailActivacionDoctor(String emailDestino, String nombreDoctor, String token) {
        String urlActivacion = "http://localhost:5173/establecer-password-doctor?token=" + token;
        String asunto = "Bienvenido/a a " + clinicName + " - Configuración de Contraseña";
        String htmlContent = emailTemplateFactory.crearEmailActivacionDoctor(nombreDoctor, urlActivacion);
        enviarCorreoHtml(emailDestino, asunto, htmlContent);
    }

    // 3. Email de Confirmación de Restablecimiento de Contraseña
    public void enviarEmailRestablecerPassword(String emailDestino, String token) {
        String urlConfirmacion = "http://localhost:5173/confirmar-restablecimiento?token=" + token;
        String asunto = "Confirmación de cambio de contraseña - " + clinicName;
        String htmlContent = emailTemplateFactory.crearEmailRestablecerPassword(urlConfirmacion);
        enviarCorreoHtml(emailDestino, asunto, htmlContent);
    }

    // 4. Email de Bienvenida al Paciente
    public void enviarEmailBienvenida(String emailDestino, String nombreUsuario) {
        String asunto = "¡Bienvenido/a a " + clinicName + "!";
        String htmlContent = emailTemplateFactory.crearEmailBienvenida(nombreUsuario);
        enviarCorreoHtml(emailDestino, asunto, htmlContent);
    }

    // 5. Email de Confirmación de Reserva de Turno
    public void enviarConfirmacionTurno(String emailDestino, TurnoResponseDTO turno) {
        String asunto = "Confirmación de Turno Médico - " + clinicName;
        String htmlContent = emailTemplateFactory.crearEmailConfirmacionTurno(turno);
        enviarCorreoHtml(emailDestino, asunto, htmlContent);
    }

    // 6. Email de Recordatorio de Turno (48 hs antes)
    public void enviarRecordatorioTurno48hs(String emailDestino, TurnoResponseDTO turno) {
        String asunto = "Recordatorio: Tu turno médico es en 48 hs - " + clinicName;
        String htmlContent = emailTemplateFactory.crearEmailRecordatorioTurno48hs(turno);
        enviarCorreoHtml(emailDestino, asunto, htmlContent);
    }

    // 7. Email de Cancelación de Turno por parte del Doctor
    public void enviarEmailCancelacionDoctor(String emailDestino, TurnoResponseDTO turno, String motivoCancelacion) {
        String asunto = "Aviso importante: Cancelación de Turno Médico - Dr/a. " + turno.getDoctorNombre();
        String htmlContent = emailTemplateFactory.crearEmailCancelacionDoctor(turno, motivoCancelacion);
        enviarCorreoHtml(emailDestino, asunto, htmlContent);
    }

    // 8. Email de Resumen Diario para el Doctor
    public void enviarResumenDiarioDoctor(String emailDestino, String doctorNombre, String fechaMañana, List<TurnoResponseDTO> turnos) {
        String asunto = String.format("Resumen de Turnos para Mañana (%s) - Dr/a. %s", fechaMañana, doctorNombre);
        String htmlContent = emailTemplateFactory.crearEmailResumenDiarioDoctor(doctorNombre, fechaMañana, turnos);
        enviarCorreoHtml(emailDestino, asunto, htmlContent);
    }

    // 9. Email de Resumen Semanal para el Doctor
    public void enviarResumenSemanalDoctor(String emailDestino, String doctorNombre, String periodoFormateado, List<TurnoResponseDTO> turnos) {
        String asunto = String.format("Reporte Semanal de Actividad (%s) - Dr/a. %s", periodoFormateado, doctorNombre);
        String htmlContent = emailTemplateFactory.crearEmailResumenSemanalDoctor(doctorNombre, periodoFormateado, turnos);
        enviarCorreoHtml(emailDestino, asunto, htmlContent);
    }

    private void enviarCorreoHtml(String destino, String asunto, String cuerpoHtml) {
        String logMessage = String.format("\n📧 ==================== [NOTIFICACIÓN POR EMAIL HTML] ====================\nRemitente (From): %s\nPara: %s\nAsunto: %s\n====================================================================",
                mailFrom, destino, asunto);

        log.info(logMessage);

        try {
            emailSender.sendHtmlEmail(destino, asunto, cuerpoHtml);
        } catch (Exception e) {
            log.warn("⚠️ No se pudo enviar el correo vía proveedores API (Brevo/Resend): {}. Intentando vía SMTP...", e.getMessage());
            if (mailSender != null) {
                try {
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                    if (mailFrom != null && !mailFrom.isBlank()) {
                        helper.setFrom(mailFrom);
                    }
                    helper.setTo(destino);
                    helper.setSubject(asunto);
                    helper.setText(cuerpoHtml, true);
                    mailSender.send(message);
                    log.info("✅ Correo HTML enviado exitosamente vía SMTP de respaldo a: {}", destino);
                } catch (Exception smtpEx) {
                    log.warn("⚠️ Tampoco se pudo enviar el correo vía SMTP: {}", smtpEx.getMessage());
                }
            }
        }
    }
}
