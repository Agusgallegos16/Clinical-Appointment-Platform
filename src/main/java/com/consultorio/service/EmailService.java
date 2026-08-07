package com.consultorio.service;

import com.consultorio.dto.TurnoResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Autowired(required = false)
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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

        String asunto = "Confirmación de Turno Médico - " + fechaFormateada;
        String cuerpo = String.format(
                "Hola %s,\n\n" +
                "Tu turno médico ha sido CONFIRMADO con éxito.\n\n" +
                "📌 Detalle de la Cita:\n" +
                " - Profesional: Dr. %s\n" +
                " - Especialidad: %s\n" +
                " - Fecha y Hora: %s hs\n" +
                " - Motivo: %s\n\n" +
                "En caso de no poder asistir, por favor cancela tu turno desde la plataforma con al menos 24 hs de anticipación.\n\n" +
                "Saludos cordiales,\nEquipo del Consultorio Médico.",
                turno.getPacienteNombre(),
                turno.getDoctorNombre(),
                turno.getEspecialidadNombre(),
                fechaFormateada,
                turno.getMotivoConsulta() != null ? turno.getMotivoConsulta() : "Consulta General"
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

    private void enviarCorreo(String destino, String asunto, String cuerpo) {
        log.info("\n📧 === [NOTIFICACIÓN POR EMAIL] ===\nPara: {}\nAsunto: {}\nContenido:\n{}\n==================================",
                destino, asunto, cuerpo);

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
