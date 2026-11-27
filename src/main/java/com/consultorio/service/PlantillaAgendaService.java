package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.AplicarPlantillaDTO;
import com.consultorio.dto.CrearPlantillaDTO;
import com.consultorio.dto.DetallePlantillaDTO;
import com.consultorio.dto.HorarioAtencionDTO;
import com.consultorio.repository.PlantillaAgendaRepository;
import com.consultorio.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PlantillaAgendaService {

    private final PlantillaAgendaRepository plantillaAgendaRepository;
    private final DoctorService doctorService;
    private final EspecialidadService especialidadService;
    private final SecurityUtils securityUtils;

    @Autowired
    public PlantillaAgendaService(PlantillaAgendaRepository plantillaAgendaRepository,
                                  DoctorService doctorService,
                                  EspecialidadService especialidadService,
                                  SecurityUtils securityUtils) {
        this.plantillaAgendaRepository = plantillaAgendaRepository;
        this.doctorService = doctorService;
        this.especialidadService = especialidadService;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public PlantillaAgenda crearPlantilla(UUID doctorId, CrearPlantillaDTO dto) {
        Doctor doctor = doctorService.obtenerPorId(doctorId);
        securityUtils.verificarAccesoDoctor(doctor, false);

        PlantillaAgenda plantilla = PlantillaAgenda.builder()
                .doctor(doctor)
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .build();

        List<DetallePlantilla> detalles = new ArrayList<>();
        for (DetallePlantillaDTO dDto : dto.getDetalles()) {
            Especialidad especialidad = null;
            if (dDto.getEspecialidadId() != null) {
                especialidad = especialidadService.buscarPorId(dDto.getEspecialidadId()).orElse(null);
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

    @Transactional
    public PlantillaAgenda actualizarPlantilla(Long plantillaId, CrearPlantillaDTO dto) {
        PlantillaAgenda plantilla = plantillaAgendaRepository.findById(plantillaId)
                .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada con ID: " + plantillaId));
        securityUtils.verificarAccesoDoctor(plantilla.getDoctor(), false);

        plantilla.setNombre(dto.getNombre());
        plantilla.setDescripcion(dto.getDescripcion());
        plantilla.getDetalles().clear();

        for (DetallePlantillaDTO dDto : dto.getDetalles()) {
            Especialidad especialidad = null;
            if (dDto.getEspecialidadId() != null) {
                especialidad = especialidadService.buscarPorId(dDto.getEspecialidadId()).orElse(null);
            }

            plantilla.getDetalles().add(DetallePlantilla.builder()
                    .plantilla(plantilla)
                    .especialidad(especialidad)
                    .horaInicio(dDto.getHoraInicio())
                    .horaFin(dDto.getHoraFin())
                    .duracionTurnoMinutos(dDto.getDuracionTurnoMinutos() > 0 ? dDto.getDuracionTurnoMinutos() : 30)
                    .build());
        }

        return plantillaAgendaRepository.save(plantilla);
    }

    @Transactional(readOnly = true)
    public List<PlantillaAgenda> listarPlantillasDoctor(UUID doctorId) {
        Doctor doctor = doctorService.buscarPorId(doctorId).orElse(null);
        securityUtils.verificarAccesoDoctor(doctor, false);
        return plantillaAgendaRepository.findByDoctorId(doctorId);
    }

    @Transactional
    public List<HorarioAtencion> aplicarPlantilla(UUID doctorId, AplicarPlantillaDTO dto) {
        Doctor doctor = doctorService.obtenerPorId(doctorId);
        securityUtils.verificarAccesoDoctor(doctor, false);

        PlantillaAgenda plantilla = plantillaAgendaRepository.findById(dto.getPlantillaId())
                .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada con ID: " + dto.getPlantillaId()));
        if (plantilla.getDoctor() == null || !plantilla.getDoctor().getId().equals(doctor.getId())) {
            throw new AccessDeniedException("Access denied: template does not belong to the target doctor.");
        }

        if (dto.getDiaSemana() == null && dto.getFecha() == null) {
            throw new IllegalArgumentException("Debe indicar un día de la semana o una fecha puntual para aplicar la plantilla.");
        }

        if (dto.getFecha() != null) {
            doctorService.limpiarHorariosPorFecha(doctorId, dto.getFecha());
        } else {
            doctorService.limpiarHorariosPorDiaSemana(doctorId, dto.getDiaSemana());
        }

        List<HorarioAtencion> nuevosHorarios = new ArrayList<>();
        for (DetallePlantilla detalle : plantilla.getDetalles()) {
            HorarioAtencionDTO hDto = new HorarioAtencionDTO();
            hDto.setDiaSemana(dto.getDiaSemana());
            hDto.setFecha(dto.getFecha());
            hDto.setFechaDesde(dto.getFechaDesde());
            hDto.setFechaHasta(dto.getFechaHasta());
            hDto.setHoraInicio(detalle.getHoraInicio());
            hDto.setHoraFin(detalle.getHoraFin());
            hDto.setDuracionTurnoMinutos(detalle.getDuracionTurnoMinutos());
            if (detalle.getEspecialidad() != null) {
                hDto.setEspecialidadId(detalle.getEspecialidad().getId());
            }

            HorarioAtencion h = doctorService.agregarHorarioAtencion(doctorId, hDto);
            nuevosHorarios.add(h);
        }

        return nuevosHorarios;
    }

    @Transactional
    public void eliminarPlantilla(Long plantillaId) {
        PlantillaAgenda plantilla = plantillaAgendaRepository.findById(plantillaId).orElse(null);
        securityUtils.verificarAccesoDoctor(plantilla != null ? plantilla.getDoctor() : null, false);
        if (plantilla != null) {
            plantillaAgendaRepository.delete(plantilla);
        }
    }
}
