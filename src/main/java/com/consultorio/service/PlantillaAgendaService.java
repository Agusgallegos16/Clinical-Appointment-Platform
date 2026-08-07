package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.AplicarPlantillaDTO;
import com.consultorio.dto.CrearPlantillaDTO;
import com.consultorio.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlantillaAgendaService {

    private final PlantillaAgendaRepository plantillaRepository;
    private final HorarioAtencionRepository horarioAtencionRepository;
    private final DoctorRepository doctorRepository;

    @Autowired
    public PlantillaAgendaService(PlantillaAgendaRepository plantillaRepository,
                                  HorarioAtencionRepository horarioAtencionRepository,
                                  DoctorRepository doctorRepository) {
        this.plantillaRepository = plantillaRepository;
        this.horarioAtencionRepository = horarioAtencionRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public PlantillaAgenda crearPlantilla(Long doctorId, CrearPlantillaDTO dto) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado con id: " + doctorId));

        PlantillaAgenda plantilla = PlantillaAgenda.builder()
                .doctor(doctor)
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .build();

        List<DetallePlantilla> detalles = dto.getDetalles().stream().map(d ->
                DetallePlantilla.builder()
                        .plantilla(plantilla)
                        .horaInicio(d.getHoraInicio())
                        .horaFin(d.getHoraFin())
                        .duracionTurnoMinutos(d.getDuracionTurnoMinutos() > 0 ? d.getDuracionTurnoMinutos() : 30)
                        .build()
        ).collect(Collectors.toList());

        plantilla.setDetalles(detalles);
        return plantillaRepository.save(plantilla);
    }

    @Transactional
    public List<HorarioAtencion> aplicarPlantilla(Long doctorId, AplicarPlantillaDTO dto) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado con id: " + doctorId));

        PlantillaAgenda plantilla = plantillaRepository.findById(dto.getPlantillaId())
                .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada con id: " + dto.getPlantillaId()));

        if (!plantilla.getDoctor().getId().equals(doctorId)) {
            throw new IllegalArgumentException("La plantilla no pertenece al doctor especificado.");
        }

        if (dto.getFecha() == null && dto.getDiaSemana() == null) {
            throw new IllegalArgumentException("Debe especificar una fecha o un día de la semana para aplicar la plantilla.");
        }

        // Limpiar horarios existentes para la fecha o día especifico
        if (dto.getFecha() != null) {
            horarioAtencionRepository.deleteByDoctorIdAndFecha(doctorId, dto.getFecha());
        } else {
            horarioAtencionRepository.deleteByDoctorIdAndDiaSemanaAndFechaIsNull(doctorId, dto.getDiaSemana());
        }

        // Generar los nuevos HorarioAtencion basados en el molde de la plantilla
        List<HorarioAtencion> nuevosHorarios = plantilla.getDetalles().stream().map(d ->
                HorarioAtencion.builder()
                        .doctor(doctor)
                        .diaSemana(dto.getDiaSemana())
                        .fecha(dto.getFecha())
                        .horaInicio(d.getHoraInicio())
                        .horaFin(d.getHoraFin())
                        .duracionTurnoMinutos(d.getDuracionTurnoMinutos())
                        .build()
        ).collect(Collectors.toList());

        return horarioAtencionRepository.saveAll(nuevosHorarios);
    }

    public List<PlantillaAgenda> listarPlantillasDoctor(Long doctorId) {
        return plantillaRepository.findByDoctorId(doctorId);
    }
}
