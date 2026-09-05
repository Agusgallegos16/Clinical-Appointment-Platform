package com.consultorio.listener;

import com.consultorio.adapter.CalendarioAdapter;
import com.consultorio.domain.Doctor;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Turno;
import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.event.TurnoCanceladoEvent;
import com.consultorio.event.TurnoReservadoEvent;
import com.consultorio.repository.TurnoRepository;
import com.consultorio.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TurnoNotificacionesListener {

    private static final Logger log = LoggerFactory.getLogger(TurnoNotificacionesListener.class);

    private final TurnoRepository turnoRepository;
    private final CalendarioAdapter calendarioAdapter;
    private final EmailService emailService;

    @Autowired
    public TurnoNotificacionesListener(TurnoRepository turnoRepository,
                                       CalendarioAdapter calendarioAdapter,
                                       EmailService emailService) {
        this.turnoRepository = turnoRepository;
        this.calendarioAdapter = calendarioAdapter;
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alReservarTurno(TurnoReservadoEvent event) {
        Turno turno = turnoRepository.findById(event.turnoId()).orElse(null);
        if (turno == null) {
            return;
        }

        Paciente paciente = turno.getPaciente();
        Doctor doctor = turno.getDoctor();

        // Sincronizar en Google Calendar del Paciente
        try {
            if (paciente != null && paciente.getUsuario() != null && paciente.getUsuario().isGoogleCalendarConnected()) {
                String eventIdPaciente = calendarioAdapter.agendarEventoParaUsuario(turno, paciente.getUsuario());
                if (eventIdPaciente != null) {
                    turno.setGoogleEventId(eventIdPaciente);
                }
            }
        } catch (Exception e) {
            log.error("No se pudo agendar en el Google Calendar del paciente: {}", e.getMessage());
        }

        // Sincronizar en Google Calendar del Doctor
        try {
            if (doctor != null && doctor.getUsuario() != null && doctor.getUsuario().isGoogleCalendarConnected()) {
                String eventIdDoctor = calendarioAdapter.agendarEventoParaUsuario(turno, doctor.getUsuario());
                if (eventIdDoctor != null) {
                    turno.setGoogleEventIdDoctor(eventIdDoctor);
                }
            }
        } catch (Exception e) {
            log.error("No se pudo agendar en el Google Calendar del doctor: {}", e.getMessage());
        }

        if (turno.getGoogleEventId() != null || turno.getGoogleEventIdDoctor() != null) {
            turnoRepository.save(turno);
        }

        // Enviar email de confirmación
        String emailDestino = event.emailDestino();
        if (emailDestino != null && !emailDestino.isBlank()) {
            try {
                TurnoResponseDTO responseDTO = TurnoResponseDTO.builder()
                        .id(turno.getId())
                        .pacienteId(turno.getPaciente().getId())
                        .pacienteNombre(turno.getPaciente().getNombre() + " " + turno.getPaciente().getApellido())
                        .doctorId(turno.getDoctor().getId())
                        .doctorNombre(turno.getDoctor().getNombre() + " " + turno.getDoctor().getApellido())
                        .especialidadId(turno.getEspecialidad() != null ? turno.getEspecialidad().getId() : null)
                        .especialidadNombre(turno.getEspecialidad() != null ? turno.getEspecialidad().getNombre() : null)
                        .fechaHora(turno.getFechaHora())
                        .estado(turno.getEstado())
                        .motivoConsulta(turno.getMotivoConsulta())
                        .motivoCancelacion(turno.getMotivoCancelacion())
                        .tieneObraSocial(turno.isTieneObraSocial())
                        .obraSocial(turno.getObraSocial())
                        .googleEventId(turno.getGoogleEventId())
                        .build();
                emailService.enviarConfirmacionTurno(emailDestino, responseDTO);
            } catch (Exception e) {
                log.error("Error enviando email de confirmación: {}", e.getMessage());
            }
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCancelarTurno(TurnoCanceladoEvent event) {
        Turno turno = turnoRepository.findById(event.turnoId()).orElse(null);
        if (turno == null) {
            return;
        }

        // Cancelar evento en el Google Calendar del Paciente
        if (turno.getGoogleEventId() != null && turno.getPaciente() != null && turno.getPaciente().getUsuario() != null) {
            try {
                calendarioAdapter.cancelarEventoParaUsuario(turno.getGoogleEventId(), turno.getPaciente().getUsuario());
            } catch (Exception e) {
                log.error("No se pudo cancelar el evento en el calendario del paciente: {}", e.getMessage());
            }
        }

        // Cancelar evento en el Google Calendar del Doctor
        if (turno.getGoogleEventIdDoctor() != null && turno.getDoctor() != null && turno.getDoctor().getUsuario() != null) {
            try {
                calendarioAdapter.cancelarEventoParaUsuario(turno.getGoogleEventIdDoctor(), turno.getDoctor().getUsuario());
            } catch (Exception e) {
                log.error("No se pudo cancelar el evento en el calendario del doctor: {}", e.getMessage());
            }
        }

        // Si fue cancelado por el doctor, enviar email de disculpas / justificación
        if (event.canceladoPorDoctor() && event.motivo() != null) {
            Paciente paciente = turno.getPaciente();
            String emailDestino = null;
            if (paciente != null) {
                if (paciente.getUsuario() != null && paciente.getUsuario().getEmail() != null) {
                    emailDestino = paciente.getUsuario().getEmail();
                } else if (paciente.getTutor() != null && paciente.getTutor().getUsuario() != null) {
                    emailDestino = paciente.getTutor().getUsuario().getEmail();
                } else if (paciente.getEmail() != null) {
                    emailDestino = paciente.getEmail();
                }
            }

            if (emailDestino != null && !emailDestino.isBlank()) {
                try {
                    TurnoResponseDTO dto = TurnoResponseDTO.builder()
                            .id(turno.getId())
                            .pacienteId(turno.getPaciente().getId())
                            .pacienteNombre(turno.getPaciente().getNombre() + " " + turno.getPaciente().getApellido())
                            .doctorId(turno.getDoctor().getId())
                            .doctorNombre(turno.getDoctor().getNombre() + " " + turno.getDoctor().getApellido())
                            .especialidadId(turno.getEspecialidad() != null ? turno.getEspecialidad().getId() : null)
                            .especialidadNombre(turno.getEspecialidad() != null ? turno.getEspecialidad().getNombre() : null)
                            .fechaHora(turno.getFechaHora())
                            .estado(turno.getEstado())
                            .motivoConsulta(turno.getMotivoConsulta())
                            .motivoCancelacion(turno.getMotivoCancelacion())
                            .tieneObraSocial(turno.isTieneObraSocial())
                            .obraSocial(turno.getObraSocial())
                            .googleEventId(turno.getGoogleEventId())
                            .build();
                    emailService.enviarEmailCancelacionDoctor(emailDestino, dto, event.motivo().trim());
                } catch (Exception e) {
                    log.error("Error al enviar email de cancelación por médico: {}", e.getMessage());
                }
            }
        }
    }
}
