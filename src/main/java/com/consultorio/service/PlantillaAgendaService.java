package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.AplicarPlantillaDTO;
import com.consultorio.dto.CrearPlantillaDTO;
import com.consultorio.dto.DetallePlantillaDTO;
import com.consultorio.repository.DoctorRepository;
import com.consultorio.repository.EspecialidadRepository;
import com.consultorio.repository.HorarioAtencionRepository;
import com.consultorio.repository.PlantillaAgendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlantillaAgendaService {

    private final PlantillaAgendaRepository plantillaAgendaRepository;
    private final DoctorRepository doctorRepository;
    private final EspecialidadRepository especialidadRepository;
    private final HorarioAtencionRepository horarioAtencionRepository;

    @Autowired
    public PlantillaAgendaService(PlantillaAgendaRepository plantillaAgendaRepository,
                                  DoctorRepository doctorRepository,
                                  EspecialidadRepository especialidadRepository,
                                  HorarioAtencionRepository horarioAtencionRepository) {
        this.plantillaAgendaRepository = plantillaAgendaRepository;
        this.doctorRepository = doctorRepository;
        this.especialidadRepository = especialidadRepository;
        this.horarioAtencionRepository = horarioAtencionRepository;
    }

    @Transactional
    public PlantillaAgenda crearPlantilla(Long doctorId, CrearPlantillaDTO dto) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado con ID: " + doctorId));

        PlantillaAgenda plantilla = PlantillaAgenda.builder()
                .doctor(doctor)
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .build();

        List<DetallePlantilla> detalles = new ArrayList<>();
        for (DetallePlantillaDTO dDto : dto.getDetalles()) {
            Especialidad especialidad = null;
            if (dDto.getEspecialidadId() != null) {
                especialidad = especialidadRepository.findById(dDto.getEspecialidadId()).orElse(null);
            }

            detalles.add(DetallePlantilla.builder()
                    .plantilla(plantilla)
                    .especialidad(especialidad)
                    .horaInicio(dDto.getHoraInicio())
                    .horaFin(dDto.getHoraFin())
                    .duracionTurnoMinutos(dDto.getDuracionTurnoMinutos() > 0 ? dDto.getDuracionTurnoMinutos() : 30)
                    .build());
        }

        plantilla.setDetalles(detalles);
        return plantillaAgendaRepository.save(plantilla);
    }

    public List<PlantillaAgenda> listarPlantillasDoctor(Long doctorId) {
        return plantillaAgendaRepository.findByDoctorId(doctorId);
    }

    @Transactional
    public List<HorarioAtencion> aplicarPlantilla(Long doctorId, AplicarPlantillaDTO dto) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado con ID: " + doctorId));

        PlantillaAgenda plantilla = plantillaAgendaRepository.findById(dto.getPlantillaId())
                .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada con ID: " + dto.getPlantillaId()));

        if (dto.getDiaSemana() == null && dto.getFecha() == null) {
            throw new IllegalArgumentException("Debe indicar un día de la semana o una fecha puntual para aplicar la plantilla.");
        }

        // Limpiar horarios existentes previamente para evitar conflictos
        if (dto.getFecha() != null) {
            horarioAtencionRepository.deleteByDoctorIdAndFecha(doctorId, dto.getFecha());
        } else {
            horarioAtencionRepository.deleteByDoctorIdAndDiaSemanaAndFechaIsNull(doctorId, dto.getDiaSemana());
        }

        List<HorarioAtencion> nuevosHorarios = new ArrayList<>();
        for (DetallePlantilla detalle : plantilla.getDetalles()) {
            HorarioAtencion h = HorarioAtencion.builder()
                    .doctor(doctor)
                    .especialidad(detalle.getEspecialidad())
                    .diaSemana(dto.getDiaSemana())
                    .fecha(dto.getFecha())
                    .horaInicio(detalle.getHoraInicio())
                    .horaFin(detalle.getHoraFin())
                    .duracionTurnoMinutos(detalle.getDuracionTurnoMinutos())
                    .build();
            nuevosHorarios.add(horarioAtencionRepository.save(h));
        }

        return nuevosHorarios;
    }

    @Transactional
    public void eliminarPlantilla(Long plantillaId) {
        plantillaAgendaRepository.deleteById(plantillaId);
    }
}
