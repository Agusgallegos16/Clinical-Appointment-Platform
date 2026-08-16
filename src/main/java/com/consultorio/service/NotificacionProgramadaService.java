package com.consultorio.service;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.EstadoTurno;
import com.consultorio.domain.Turno;
import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.repository.DoctorRepository;
import com.consultorio.repository.TurnoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificacionProgramadaService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionProgramadaService.class);

    private final DoctorRepository doctorRepository;
    private final TurnoRepository turnoRepository;
    private final TurnoService turnoService;
    private final EmailService emailService;

    @Autowired
    public NotificacionProgramadaService(DoctorRepository doctorRepository,
                                         TurnoRepository turnoRepository,
                                         TurnoService turnoService,
                                         EmailService emailService) {
        this.doctorRepository = doctorRepository;
        this.turnoRepository = turnoRepository;
        this.turnoService = turnoService;
        this.emailService = emailService;
    }

    // Tarea programada diaria: Todos los días a las 09:00 hs (cron = "0 0 9 * * ?")
    // Envía recordatorios a pacientes con citas en 48 horas
    @Scheduled(cron = "0 0 9 * * ?")
    public void enviarRecordatorios48hsAPacientes() {
        LocalDateTime desde = LocalDateTime.now().plusHours(47);
        LocalDateTime hasta = LocalDateTime.now().plusHours(49);

        List<Turno> turnosAVisar = turnoRepository.findByFechaHoraBetweenAndEstadoInAndRecordatorio48hsEnviadoFalse(
                desde, hasta, List.of(EstadoTurno.CONFIRMADO, EstadoTurno.PENDIENTE)
        );

        log.info("⏰ Procesando recordatorios de 48hs pre-turno. Cantidad a enviar: {}", turnosAVisar.size());

        for (Turno turno : turnosAVisar) {
            try {
                TurnoResponseDTO dto = TurnoResponseDTO.builder()
                        .id(turno.getId())
                        .pacienteId(turno.getPaciente().getId())
                        .pacienteNombre(turno.getPaciente().getNombre() + " " + turno.getPaciente().getApellido())
                        .doctorId(turno.getDoctor().getId())
                        .doctorNombre(turno.getDoctor().getNombre() + " " + turno.getDoctor().getApellido())
                        .especialidadNombre(turno.getEspecialidad().getNombre())
                        .fechaHora(turno.getFechaHora())
                        .estado(turno.getEstado())
                        .motivoConsulta(turno.getMotivoConsulta())
                        .build();

                String emailDestino = turno.getPaciente().getUsuario() != null
                        ? turno.getPaciente().getUsuario().getEmail()
                        : (turno.getPaciente().getTutor() != null && turno.getPaciente().getTutor().getUsuario() != null
                        ? turno.getPaciente().getTutor().getUsuario().getEmail()
                        : null);

                if (emailDestino != null) {
                    emailService.enviarRecordatorioTurno48hs(emailDestino, dto);
                }

                turno.setRecordatorio48hsEnviado(true);
                turnoRepository.save(turno);
            } catch (Exception e) {
                log.error("Error al enviar recordatorio 48hs para turno ID {}: {}", turno.getId(), e.getMessage());
            }
        }
    }

    // Tarea programada diaria: Todos los días a las 20:00 hs (cron = "0 0 20 * * ?")
    @Scheduled(cron = "0 0 20 * * ?")
    public void enviarResumenDiarioADoctores() {
        enviarResumenDiarioADoctoresParaFecha(LocalDate.now().plusDays(1));
    }

    // Tarea programada semanal: Todos los domingos a las 20:00 hs (cron = "0 0 20 * * SUN")
    @Scheduled(cron = "0 0 20 * * SUN")
    public void enviarResumenSemanalADoctores() {
        LocalDate hoy = LocalDate.now();
        LocalDate lunesSemana = hoy.with(DayOfWeek.MONDAY);
        LocalDate domingoSemana = hoy.with(DayOfWeek.SUNDAY);
        enviarResumenSemanalADoctoresParaRango(lunesSemana, domingoSemana);
    }

    // Procesa el envío de resúmenes diarios para una fecha determinada
    public void enviarResumenDiarioADoctoresParaFecha(LocalDate fechaTarget) {
        log.info("⏰ Procesando resúmenes diarios de agenda para la fecha: {}", fechaTarget);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String fechaFormateada = fechaTarget.format(formatter);

        List<Doctor> doctores = doctorRepository.findAll();

        if (doctores.isEmpty()) {
            log.info("ℹ️ No hay doctores registrados en el sistema.");
            return;
        }

        for (Doctor doctor : doctores) {
            List<TurnoResponseDTO> turnosDia = turnoService.obtenerAgendaDoctor(doctor.getId(), fechaTarget);

            if (!turnosDia.isEmpty()) {
                emailService.enviarResumenDiarioDoctor(
                        doctor.getUsuario().getEmail(),
                        doctor.getNombre() + " " + doctor.getApellido(),
                        fechaFormateada,
                        turnosDia
                );
            } else {
                log.info("ℹ️ El Dr/a. {} {} no posee turnos confirmados para el día {}. Se omite notificación.",
                        doctor.getNombre(), doctor.getApellido(), fechaFormateada);
            }
        }
    }

    // Procesa el envío de reportes semanales de actividad para un rango de fechas
    public void enviarResumenSemanalADoctoresParaRango(LocalDate desde, LocalDate hasta) {
        log.info("⏰ Procesando reportes semanales de actividad desde {} hasta {}", desde, hasta);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String periodoFormateado = String.format("%s al %s", desde.format(formatter), hasta.format(formatter));

        List<Doctor> doctores = doctorRepository.findAll();

        if (doctores.isEmpty()) {
            log.info("ℹ️ No hay doctores registrados en el sistema.");
            return;
        }

        for (Doctor doctor : doctores) {
            List<TurnoResponseDTO> turnosSemana = turnoService.obtenerTurnosRangoDoctor(doctor.getId(), desde, hasta);

            if (!turnosSemana.isEmpty()) {
                emailService.enviarResumenSemanalDoctor(
                        doctor.getUsuario().getEmail(),
                        doctor.getNombre() + " " + doctor.getApellido(),
                        periodoFormateado,
                        turnosSemana
                );
            } else {
                log.info("ℹ️ El Dr/a. {} {} no registró actividad de turnos en el período {}.",
                        doctor.getNombre(), doctor.getApellido(), periodoFormateado);
            }
        }
    }
}
