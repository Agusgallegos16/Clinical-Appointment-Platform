package com.consultorio.service;

import com.consultorio.domain.SlotHorario;
import com.consultorio.dto.SlotDisponibilidadDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DisponibilidadService {

    private final DoctorService doctorService;
    private final TurnoService turnoService;

    @Autowired
    public DisponibilidadService(DoctorService doctorService, TurnoService turnoService) {
        this.doctorService = doctorService;
        this.turnoService = turnoService;
    }

    public List<SlotDisponibilidadDTO> obtenerHorariosDisponibles(UUID doctorId, LocalDate fecha) {
        return obtenerHorariosDisponibles(doctorId, fecha, null);
    }

    public List<SlotDisponibilidadDTO> obtenerHorariosDisponibles(UUID doctorId, LocalDate fecha, Long especialidadId) {
        if (!doctorService.estaDisponibleParaTurnos(doctorId)) {
            return Collections.emptyList();
        }

        Set<LocalTime> horasOcupadas = turnoService.obtenerHorasOcupadasDoctor(doctorId, fecha);

        // Consultar slots concretos guardados para este doctor en esta fecha a través de DoctorService
        List<SlotHorario> slotsDoctor = doctorService.obtenerSlotsDoctorPorFecha(doctorId, fecha);

        // Filtrar por especialidad: coincide la especialidad requerida o es General (null)
        List<SlotHorario> slotsFiltrados = slotsDoctor.stream()
                .filter(s -> especialidadId == null || s.getEspecialidad() == null || s.getEspecialidad().getId().equals(especialidadId))
                .collect(Collectors.toList());

        List<SlotDisponibilidadDTO> resultado = new ArrayList<>();
        for (SlotHorario s : slotsFiltrados) {
            boolean disponible = !horasOcupadas.contains(s.getHoraInicio());
            if (disponible) {
                resultado.add(SlotDisponibilidadDTO.builder()
                        .id(s.getId())
                        .hora(s.getHoraInicio())
                        .disponible(true)
                        .especialidadId(s.getEspecialidad() != null ? s.getEspecialidad().getId() : null)
                        .especialidadNombre(s.getEspecialidad() != null ? s.getEspecialidad().getNombre() : null)
                        .build());
            }
        }

        resultado.sort(Comparator.comparing(SlotDisponibilidadDTO::getHora));
        return resultado;
    }
}
