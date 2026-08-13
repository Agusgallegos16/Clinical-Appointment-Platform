package com.consultorio.service;

import com.consultorio.domain.EstadoTurno;
import com.consultorio.dto.TurnoResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    // Email de Verificación de Correo Electrónico (Enlace de activación)
    public void enviarEmailVerificacion(String emailDestino, String nombreUsuario, String token) {
        String urlVerificacion = "http://localhost:5173/confirmar-email?token=" + token;
        String asunto = "Confirma tu correo electrónico - Consultorio Médico";
        String cuerpo = String.format(
                "Hola %s,\n\n" +
                "Gracias por registrarte en nuestro sistema de gestión de turnos.\n" +
                "Para activar tu cuenta y poder iniciar sesión, por favor confirma tu correo electrónico ingresando al siguiente enlace:\n\n" +
                "%s\n\n" +
                "Este enlace expira en 24 horas.\n\n" +
                "Saludos cordiales,\nEquipo del Consultorio Médico.",
                nombreUsuario, urlVerificacion
        );

        enviarCorreo(emailDestino, asunto, cuerpo);
    }

    // Email de Activación de Cuenta y Configuración de Contraseña para Doctores
    public void enviarEmailActivacionDoctor(String emailDestino, String nombreDoctor, String token) {
        String urlActivacion = "http://localhost:5173/establecer-password-doctor?token=" + token;
        String asunto = "Bienvenido/a al Consultorio Médico - Configuración de Contraseña";
        String cuerpo = String.format(
                "Estimado/a Dr/a. %s,\n\n" +
                "Has sido dado/a de alta como profesional médico en nuestro sistema de gestión de turnos.\n" +
                "Para activar tu cuenta de usuario y configurar tu contraseña de acceso por primera vez, ingresa al siguiente enlace:\n\n" +
                "%s\n\n" +
                "⚠️ Este enlace de activación expira en 24 horas. En caso de no responder en dicho plazo, la solicitud de alta será descartada automáticamente.\n\n" +
                "Saludos cordiales,\nEquipo de Administración del Consultorio Médico.",
                nombreDoctor, urlActivacion
        );

        enviarCorreo(emailDestino, asunto, cuerpo);
    }

    // Email de Confirmación de Restablecimiento de Contraseña
    public void enviarEmailRestablecerPassword(String emailDestino, String token) {
        String urlConfirmacion = "http://localhost:5173/confirmar-restablecimiento?token=" + token;
        String asunto = "Confirmación de cambio de contraseña - Consultorio Médico";
        String cuerpo = String.format(
                "Hola,\n\n" +
                "Hemos recibido una solicitud para cambiar la contraseña de tu cuenta.\n" +
                "Para confirmar el cambio e ingresar con tu nueva contraseña, por favor accede al siguiente enlace:\n\n" +
                "%s\n\n" +
                "Si no realizaste esta solicitud, puedes ignorar este correo y tu contraseña actual seguirá siendo la misma.\n" +
                "Este enlace expira en 1 hora.\n\n" +
                "Saludos cordiales,\nEquipo del Consultorio Médico.",
                urlConfirmacion
        );

        enviarCorreo(emailDestino, asunto, cuerpo);
    }

    // 1. Email de Bienvenida al crearse la cuenta
    public void enviarEmailBienvenida(String emailDestino, String nombreUsuario) {
        String asunto = "¡Bienvenido al Sistema de Gestión de Turnos!";
        String cuerpo = String.format(
                "Hola %s,\n\n" +
                "Tu cuenta ha sido creada exitosamente en nuestro sistema de salud.\n" +
                "Ya puedes acceder para consultar disponibilidad y agendar tus turnos médicos.\n\n" +
                "Saludos cordiales,\nEquipo del Consultorio Médico.",
                nombreUsuario
        );

        enviarCorreo(emailDestino, asunto, cuerpo);
    }

    // 2. Email de Confirmación de Reserva de Turno
    public void enviarConfirmacionTurno(String emailDestino, TurnoResponseDTO turno) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fechaFormateada = turno.getFechaHora().format(formatter);

        String osInfo = (turno.getObraSocial() != null && !turno.getObraSocial().trim().isEmpty()) ? turno.getObraSocial() : "Particular / Sin Obra Social";

        String asunto = "Confirmación de Turno Médico - " + fechaFormateada;
        String cuerpo = String.format(
                "Hola %s,\n\n" +
                "Tu turno médico ha sido CONFIRMADO con éxito.\n\n" +
                "📌 Detalle de la Cita:\n" +
                " - Profesional: Dr. %s\n" +
                " - Especialidad: %s\n" +
                " - Fecha y Hora: %s hs\n" +
                " - Cobertura: %s\n" +
                " - Motivo: %s\n\n" +
                "En caso de no poder asistir, por favor cancela tu turno desde la plataforma con al menos 24 hs de anticipación.\n\n" +
                "Saludos cordiales,\nEquipo del Consultorio Médico.",
                turno.getPacienteNombre(),
                turno.getDoctorNombre(),
                turno.getEspecialidadNombre(),
                fechaFormateada,
                osInfo,
                turno.getMotivoConsulta() != null ? turno.getMotivoConsulta() : "Consulta General"
        );

        enviarCorreo(emailDestino, asunto, cuerpo);
    }

    // Recordatorio de Turno 48hs antes (Para Pacientes)
    public void enviarRecordatorioTurno48hs(String emailDestino, TurnoResponseDTO turno) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fechaFormateada = turno.getFechaHora().format(formatter);

        String asunto = "Recordatorio: Tu turno médico es en 48 hs - " + fechaFormateada;
        String cuerpo = String.format(
                "Hola %s,\n\n" +
                "Te recordamos que tienes una cita médica programada en las próximas 48 horas:\n\n" +
                "📌 Detalle de la Cita:\n" +
                " - Profesional: Dr/a. %s\n" +
                " - Especialidad: %s\n" +
                " - Fecha y Hora: %s hs\n\n" +
                "En caso de que no puedas asistir, recuerda que puedes cancelar tu turno desde la plataforma para liberar el horario para otro paciente.\n\n" +
                "Saludos cordiales,\nEquipo del Consultorio Médico.",
                turno.getPacienteNombre(),
                turno.getDoctorNombre(),
                turno.getEspecialidadNombre(),
                fechaFormateada
        );

        enviarCorreo(emailDestino, asunto, cuerpo);
    }

    // Cancelación de Turno por parte del Doctor con Justificación Obligatoria y Disculpas
    public void enviarEmailCancelacionDoctor(String emailDestino, TurnoResponseDTO turno, String motivoCancelacion) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fechaFormateada = turno.getFechaHora().format(formatter);

        String asunto = "Aviso importante: Cancelación de Turno Médico - Dr/a. " + turno.getDoctorNombre();
        String cuerpo = String.format(
                "Estimado/a %s,\n\n" +
                "Lamentamos informarte que tu cita médica programada para el día %s hs con el/la Dr/a. %s (%s) ha debido ser cancelada.\n\n" +
                "📝 Motivo / Justificación del Profesional:\n" +
                "\"%s\"\n\n" +
                "Te ofrecemos nuestras más sinceras disculpas por los inconvenientes que esto pueda ocasionarte. Te invitamos a ingresar al sistema para agendar un nuevo turno en la fecha y horario que te resulte más conveniente.\n\n" +
                "Atentamente,\nEquipo del Consultorio Médico.",
                turno.getPacienteNombre(),
                fechaFormateada,
                turno.getDoctorNombre(),
                turno.getEspecialidadNombre(),
                motivoCancelacion
        );

        enviarCorreo(emailDestino, asunto, cuerpo);
    }

    // 3. Email de Resumen Diario para el Doctor (Enviado al final del día)
    public void enviarResumenDiarioDoctor(String emailDestino, String doctorNombre, String fechaMañana, List<TurnoResponseDTO> turnos) {
        StringBuilder cuerpoBuilder = new StringBuilder();
        cuerpoBuilder.append(String.format("Estimado/a Dr/a. %s,\n\n", doctorNombre));
        cuerpoBuilder.append(String.format("Le enviamos el resumen de turnos programados para el día de mañana (%s):\n\n", fechaMañana));

        DateTimeFormatter horaFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (int i = 0; i < turnos.size(); i++) {
            TurnoResponseDTO t = turnos.get(i);
            cuerpoBuilder.append(String.format("%d. [%s hs] Paciente: %s | Especialidad: %s | Motivo: %s\n",
                    i + 1,
                    t.getFechaHora().format(horaFormatter),
                    t.getPacienteNombre(),
                    t.getEspecialidadNombre(),
                    t.getMotivoConsulta() != null ? t.getMotivoConsulta() : "Sin especificar"
            ));
        }

        cuerpoBuilder.append("\nQue tenga una excelente jornada.\nSaludos,\nEquipo del Consultorio.");

        String asunto = String.format("Resumen de Turnos para Mañana (%s) - Dr/a. %s", fechaMañana, doctorNombre);
        enviarCorreo(emailDestino, asunto, cuerpoBuilder.toString());
    }

    // 4. Email de Resumen Semanal para el Doctor (Completados, Confirmados y Cancelados)
    public void enviarResumenSemanalDoctor(String emailDestino, String doctorNombre, String periodoFormateado, List<TurnoResponseDTO> turnos) {
        long completados = turnos.stream().filter(t -> t.getEstado() == EstadoTurno.COMPLETADO).count();
        long cancelados = turnos.stream().filter(t -> t.getEstado() == EstadoTurno.CANCELADO).count();
        long confirmados = turnos.stream().filter(t -> t.getEstado() == EstadoTurno.CONFIRMADO || t.getEstado() == EstadoTurno.PENDIENTE).count();

        StringBuilder cuerpoBuilder = new StringBuilder();
        cuerpoBuilder.append(String.format("Estimado/a Dr/a. %s,\n\n", doctorNombre));
        cuerpoBuilder.append(String.format("Le enviamos el informe semanal de actividad del consultorio (%s):\n\n", periodoFormateado));

        cuerpoBuilder.append("📊 Resumen de Métrica Semanal:\n");
        cuerpoBuilder.append(String.format(" - Total de Turnos: %d\n", turnos.size()));
        cuerpoBuilder.append(String.format(" - ✅ Completados: %d\n", completados));
        cuerpoBuilder.append(String.format(" - 📌 Confirmados / Pendientes: %d\n", confirmados));
        cuerpoBuilder.append(String.format(" - ❌ Cancelados: %d\n\n", cancelados));

        cuerpoBuilder.append("📋 Detalle Completo de Citas Médicas:\n");

        DateTimeFormatter fechaHoraFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (int i = 0; i < turnos.size(); i++) {
            TurnoResponseDTO t = turnos.get(i);
            cuerpoBuilder.append(String.format("%d. [%s hs] [%s] Paciente: %s | Especialidad: %s | Motivo: %s\n",
                    i + 1,
                    t.getFechaHora().format(fechaHoraFormatter),
                    t.getEstado(),
                    t.getPacienteNombre(),
                    t.getEspecialidadNombre(),
                    t.getMotivoConsulta() != null ? t.getMotivoConsulta() : "Sin especificar"
            ));
        }

        cuerpoBuilder.append("\nSaludos cordiales,\nEquipo del Consultorio Médico.");

        String asunto = String.format("Reporte Semanal de Actividad (%s) - Dr/a. %s", periodoFormateado, doctorNombre);
        enviarCorreo(emailDestino, asunto, cuerpoBuilder.toString());
    }

    private void enviarCorreo(String destino, String asunto, String cuerpo) {
        String logMessage = String.format("\n📧 ==================== [NOTIFICACIÓN POR EMAIL] ====================\nPara: %s\nAsunto: %s\nContenido:\n%s\n====================================================================",
                destino, asunto, cuerpo);

        log.info(logMessage);

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(destino);
                message.setSubject(asunto);
                message.setText(cuerpo);
                mailSender.send(message);
            } catch (Exception e) {
                log.warn("No se pudo enviar el correo vía SMTP (modo simulación activo): {}", e.getMessage());
            }
        }
    }
}
