package com.consultorio.service;

import com.consultorio.domain.Doctor;
import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.repository.DoctorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificacionProgramadaService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionProgramadaService.class);

    private final DoctorRepository doctorRepository;
    private final TurnoService turnoService;
    private final EmailService emailService;

    @Autowired
    public NotificacionProgramadaService(DoctorRepository doctorRepository,
                                         TurnoService turnoService,
                                         EmailService emailService) {
        this.doctorRepository = doctorRepository;
        this.turnoService = turnoService;
        this.emailService = emailService;
    }

    // Tarea programada: Todos los días a las 20:00 hs (cron = "0 0 20 * * ?")
    @Scheduled(cron = "0 0 20 * * ?")
    public void enviarResumenDiarioADoctores() {
        log.info("⏰ Ejecutando tarea programada: Procesando resúmenes diarios para doctores...");
        LocalDate mañana = LocalDate.now().plusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String fechaFormateada = mañana.format(formatter);

        List<Doctor> doctores = doctorRepository.findAll();

        for (Doctor doctor : doctores) {
            List<TurnoResponseDTO> turnosMañana = turnoService.obtenerAgendaDoctor(doctor.getId(), mañana);

            // Solo enviamos email si el doctor tiene turnos asignados para mañana
            if (!turnosMañana.isEmpty()) {
                emailService.enviarResumenDiarioDoctor(
                        doctor.getUsuario().getEmail(),
                        doctor.getNombre() + " " + doctor.getApellido(),
                        fechaFormateada,
                        turnosMañana
                );
            }
        }
    }
}
