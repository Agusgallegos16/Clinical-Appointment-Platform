package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.HorarioAtencionDTO;
import com.consultorio.dto.RegistroDoctorDTO;
import com.consultorio.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final EspecialidadRepository especialidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final HorarioAtencionRepository horarioAtencionRepository;
    private final SlotHorarioRepository slotHorarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DoctorService(DoctorRepository doctorRepository,
                         EspecialidadRepository especialidadRepository,
                         UsuarioRepository usuarioRepository,
                         HorarioAtencionRepository horarioAtencionRepository,
                         SlotHorarioRepository slotHorarioRepository,
                         EmailService emailService,
                         PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.especialidadRepository = especialidadRepository;
        this.usuarioRepository = usuarioRepository;
        this.horarioAtencionRepository = horarioAtencionRepository;
        this.slotHorarioRepository = slotHorarioRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Doctor registrarDoctor(RegistroDoctorDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El email ya se encuentra registrado: " + dto.getEmail());
        }

        String rawPassword = (dto.getPassword() != null && !dto.getPassword().isBlank()) ? dto.getPassword() : "123456";

        Usuario usuario = Usuario.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(rawPassword))
                .rol(Rol.DOCTOR)
                .activo(true)
                .build();

        var especialidades = (dto.getEspecialidadIds() != null && !dto.getEspecialidadIds().isEmpty())
                ? especialidadRepository.findAllById(dto.getEspecialidadIds())
                : List.<Especialidad>of();

        Doctor doctor = Doctor.builder()
                .usuario(usuario)
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .especialidades(especialidades)
                .build();

        Doctor guardado = doctorRepository.save(doctor);
        emailService.enviarEmailBienvenida(guardado.getUsuario().getEmail(), guardado.getNombre());
        return guardado;
    }

    @Transactional
    public Doctor actualizarDoctor(Long id, RegistroDoctorDTO dto) {
        Doctor doctor = obtenerPorId(id);

        doctor.setNombre(dto.getNombre());
        doctor.setApellido(dto.getApellido());

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            if (!doctor.getUsuario().getEmail().equalsIgnoreCase(dto.getEmail()) &&
                    usuarioRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("El email ya pertenece a otro usuario: " + dto.getEmail());
            }
            doctor.getUsuario().setEmail(dto.getEmail());
        }

        if (dto.getEspecialidadIds() != null) {
            var especialidades = especialidadRepository.findAllById(dto.getEspecialidadIds());
            doctor.setEspecialidades(especialidades);
        }

        return doctorRepository.save(doctor);
    }

    @Transactional
    public void eliminarDoctor(Long id) {
        Doctor doctor = obtenerPorId(id);
        doctorRepository.delete(doctor);
    }

    public List<Doctor> listarTodos() {
        return doctorRepository.findAll();
    }

    public List<Doctor> listarPorEspecialidad(Long especialidadId) {
        return doctorRepository.findByEspecialidadesId(especialidadId);
    }

    public Doctor obtenerPorId(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado con ID: " + id));
    }

    @Transactional
    public HorarioAtencion agregarHorarioAtencion(Long doctorId, HorarioAtencionDTO dto) {
        Doctor doctor = obtenerPorId(doctorId);
        LocalDate hoy = LocalDate.now();

        if (dto.getFecha() != null && dto.getFecha().isBefore(hoy)) {
            throw new IllegalArgumentException("No se pueden configurar horarios para fechas pasadas.");
        }

        if (dto.getFechaHasta() != null && dto.getFechaHasta().isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha límite de vigencia no puede ser anterior a la fecha actual.");
        }

        if (dto.getDiaSemana() == null && dto.getFecha() == null) {
            throw new IllegalArgumentException("Debe especificar un día de la semana o una fecha puntual para el horario.");
        }

        if (dto.getHoraInicio().isAfter(dto.getHoraFin()) || dto.getHoraInicio().equals(dto.getHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }

        Especialidad especialidad = null;
        if (dto.getEspecialidadId() != null) {
            especialidad = especialidadRepository.findById(dto.getEspecialidadId()).orElse(null);
        }

        int duracion = dto.getDuracionTurnoMinutos() > 0 ? dto.getDuracionTurnoMinutos() : 30;

        HorarioAtencion horario = HorarioAtencion.builder()
                .doctor(doctor)
                .especialidad(especialidad)
                .diaSemana(dto.getDiaSemana())
                .fecha(dto.getFecha())
                .fechaDesde(dto.getFechaDesde())
                .fechaHasta(dto.getFechaHasta())
                .horaInicio(dto.getHoraInicio())
                .horaFin(dto.getHoraFin())
                .duracionTurnoMinutos(duracion)
                .build();

        HorarioAtencion guardado = horarioAtencionRepository.save(horario);

        // Generar slots instanciados concretos
        if (dto.getFecha() != null) {
            generarSlotsParaFecha(doctor, dto.getFecha(), dto.getHoraInicio(), dto.getHoraFin(), duracion, especialidad, true);
        } else if (dto.getDiaSemana() != null) {
            instanciarSlotsSemanales(doctor, dto.getDiaSemana(), dto.getHoraInicio(), dto.getHoraFin(), duracion, especialidad, dto.getFechaDesde(), dto.getFechaHasta());
        }

        return guardado;
    }

    @Transactional
    public HorarioAtencion actualizarHorarioAtencion(Long horarioId, HorarioAtencionDTO dto) {
        HorarioAtencion horario = horarioAtencionRepository.findById(horarioId)
                .orElseThrow(() -> new IllegalArgumentException("Horario no encontrado con ID: " + horarioId));

        LocalDate hoy = LocalDate.now();

        if (dto.getFecha() != null && dto.getFecha().isBefore(hoy)) {
            throw new IllegalArgumentException("No se pueden configurar horarios para fechas pasadas.");
        }

        if (dto.getHoraInicio().isAfter(dto.getHoraFin()) || dto.getHoraInicio().equals(dto.getHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }

        Especialidad especialidad = null;
        if (dto.getEspecialidadId() != null) {
            especialidad = especialidadRepository.findById(dto.getEspecialidadId()).orElse(null);
        }

        int duracion = dto.getDuracionTurnoMinutos() > 0 ? dto.getDuracionTurnoMinutos() : 30;

        horario.setEspecialidad(especialidad);
        horario.setDiaSemana(dto.getDiaSemana());
        horario.setFecha(dto.getFecha());
        horario.setFechaDesde(dto.getFechaDesde());
        horario.setFechaHasta(dto.getFechaHasta());
        horario.setHoraInicio(dto.getHoraInicio());
        horario.setHoraFin(dto.getHoraFin());
        horario.setDuracionTurnoMinutos(duracion);

        HorarioAtencion guardado = horarioAtencionRepository.save(horario);

        // Regenerar los slots correspondientes
        if (dto.getFecha() != null) {
            generarSlotsParaFecha(horario.getDoctor(), dto.getFecha(), dto.getHoraInicio(), dto.getHoraFin(), duracion, especialidad, true);
        } else if (dto.getDiaSemana() != null) {
            instanciarSlotsSemanales(horario.getDoctor(), dto.getDiaSemana(), dto.getHoraInicio(), dto.getHoraFin(), duracion, especialidad, dto.getFechaDesde(), dto.getFechaHasta());
        }

        return guardado;
    }

    private void instanciarSlotsSemanales(Doctor doctor, DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin, int duracionMinutos, Especialidad especialidad, LocalDate fechaDesde, LocalDate fechaHasta) {
        LocalDate inicio = (fechaDesde != null && !fechaDesde.isBefore(LocalDate.now())) ? fechaDesde : LocalDate.now();
        LocalDate fin = (fechaHasta != null) ? fechaHasta : inicio.plusWeeks(8);

        for (LocalDate date = inicio; !date.isAfter(fin); date = date.plusDays(1)) {
            if (mapearDiaSemana(date.getDayOfWeek()) == diaSemana) {
                generarSlotsParaFecha(doctor, date, horaInicio, horaFin, duracionMinutos, especialidad, false);
            }
        }
    }

    private void generarSlotsParaFecha(Doctor doctor, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, int duracionMinutos, Especialidad especialidad, boolean esPuntual) {
        List<SlotHorario> existentes = slotHorarioRepository.findByDoctorIdAndFecha(doctor.getId(), fecha);

        if (esPuntual) {
            // Horario puntual sobreescribe cualquier slot existente en ese rango
            List<SlotHorario> colisionantes = existentes.stream()
                    .filter(s -> horaInicio.isBefore(s.getHoraFin()) && horaFin.isAfter(s.getHoraInicio()))
                    .collect(Collectors.toList());
            if (!colisionantes.isEmpty()) {
                slotHorarioRepository.deleteAll(colisionantes);
            }
        } else {
            // Horario semanal borra solo los slots semanales previos colisionantes
            List<SlotHorario> semanalesColisionantes = existentes.stream()
                    .filter(s -> !s.isEsPuntual() && horaInicio.isBefore(s.getHoraFin()) && horaFin.isAfter(s.getHoraInicio()))
                    .collect(Collectors.toList());
            if (!semanalesColisionantes.isEmpty()) {
                slotHorarioRepository.deleteAll(semanalesColisionantes);
            }
        }

        // Obtener remanentes actualizados
        List<SlotHorario> remanentes = slotHorarioRepository.findByDoctorIdAndFecha(doctor.getId(), fecha);

        LocalTime actual = horaInicio;
        int duracion = duracionMinutos > 0 ? duracionMinutos : 30;

        List<SlotHorario> nuevosSlots = new ArrayList<>();
        while (actual.plusMinutes(duracion).isBefore(horaFin) || actual.plusMinutes(duracion).equals(horaFin)) {
            LocalTime slotStart = actual;
            LocalTime slotEnd = actual.plusMinutes(duracion);

            // Si es semanal, no chocar con un slot puntual existente
            boolean chocaConPuntual = !esPuntual && remanentes.stream()
                    .anyMatch(s -> s.isEsPuntual() && slotStart.isBefore(s.getHoraFin()) && slotEnd.isAfter(s.getHoraInicio()));

            if (!chocaConPuntual) {
                SlotHorario slot = SlotHorario.builder()
                        .doctor(doctor)
                        .especialidad(especialidad)
                        .fecha(fecha)
                        .horaInicio(slotStart)
                        .horaFin(slotEnd)
                        .duracionMinutos(duracion)
                        .esPuntual(esPuntual)
                        .build();
                nuevosSlots.add(slot);
            }

            actual = actual.plusMinutes(duracion);
        }

        if (!nuevosSlots.isEmpty()) {
            slotHorarioRepository.saveAll(nuevosSlots);
        }
    }

    @Transactional
    public void limpiarHorariosSemana(Long doctorId, LocalDate desde, LocalDate hasta) {
        slotHorarioRepository.deleteByDoctorIdAndFechaBetween(doctorId, desde, hasta);

        List<HorarioAtencion> horarios = horarioAtencionRepository.findByDoctorId(doctorId);
        for (HorarioAtencion h : horarios) {
            if (h.getFecha() != null && !h.getFecha().isBefore(desde) && !h.getFecha().isAfter(hasta)) {
                horarioAtencionRepository.delete(h);
            }
        }
    }

    public List<SlotHorario> obtenerSlotsDoctor(Long doctorId, LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null) {
            return slotHorarioRepository.findByDoctorIdAndFechaBetween(doctorId, desde, hasta);
        }
        return slotHorarioRepository.findByDoctorId(doctorId);
    }

    @Transactional
    public void eliminarSlotIndividual(Long slotId) {
        slotHorarioRepository.deleteById(slotId);
    }

    public List<HorarioAtencion> obtenerHorariosDoctor(Long doctorId) {
        return horarioAtencionRepository.findByDoctorId(doctorId);
    }

    @Transactional
    public void eliminarHorarioAtencion(Long horarioId) {
        horarioAtencionRepository.deleteById(horarioId);
    }

    private DiaSemana mapearDiaSemana(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return DiaSemana.LUNES;
            case TUESDAY: return DiaSemana.MARTES;
            case WEDNESDAY: return DiaSemana.MIERCOLES;
            case THURSDAY: return DiaSemana.JUEVES;
            case FRIDAY: return DiaSemana.VIERNES;
            case SATURDAY: return DiaSemana.SABADO;
            case SUNDAY: return DiaSemana.DOMINGO;
            default: throw new IllegalArgumentException("Día de la semana no soportado: " + dayOfWeek);
        }
    }
}
