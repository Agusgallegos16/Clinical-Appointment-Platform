package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.HorarioAtencionDTO;
import com.consultorio.dto.RegistroDoctorDTO;
import com.consultorio.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorAgendaService doctorAgendaService;
    private final UsuarioService usuarioService;
    private final EspecialidadService especialidadService;
    private final EmailService emailService;

    @Autowired
    public DoctorService(DoctorRepository doctorRepository,
                         DoctorAgendaService doctorAgendaService,
                         UsuarioService usuarioService,
                         EspecialidadService especialidadService,
                         EmailService emailService) {
        this.doctorRepository = doctorRepository;
        this.doctorAgendaService = doctorAgendaService;
        this.usuarioService = usuarioService;
        this.especialidadService = especialidadService;
        this.emailService = emailService;
    }

    @Transactional
    public Doctor registrarDoctor(RegistroDoctorDTO dto) {
        var usuarioExistenteOpt = usuarioService.obtenerPorEmail(dto.getEmail());
        if (usuarioExistenteOpt.isPresent()) {
            Usuario exist = usuarioExistenteOpt.get();
            // Si el usuario no activó su cuenta y expiro su token de 24hs, descartarlo para permitir nuevo alta
            if (!exist.isActivo() && !exist.isEmailVerificado() && exist.getTokenVerificacionExpiracion() != null && exist.getTokenVerificacionExpiracion().isBefore(java.time.LocalDateTime.now())) {
                doctorRepository.findByUsuarioId(exist.getId()).ifPresent(doctorRepository::delete);
                usuarioService.eliminarEntidadDirecta(exist);
            } else {
                throw new IllegalArgumentException("El email ya se encuentra registrado: " + dto.getEmail());
            }
        }

        String tokenActivacion = UUID.randomUUID().toString();
        Usuario usuarioGuardado = usuarioService.crearUsuarioParaAdmin(dto.getEmail(), Rol.DOCTOR, tokenActivacion);

        Set<Especialidad> especialidades = new HashSet<>();
        if (dto.getEspecialidadIds() != null && !dto.getEspecialidadIds().isEmpty()) {
            especialidades = new HashSet<>(especialidadService.obtenerTodasPorIds(dto.getEspecialidadIds()));
        }

        Doctor doctor = Doctor.builder()
                .usuario(usuarioGuardado)
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .fotoUrl(dto.getFotoUrl())
                .especialidades(especialidades)
                .build();

        Doctor guardado = doctorRepository.save(doctor);
        emailService.enviarEmailActivacionDoctor(guardado.getUsuario().getEmail(), guardado.getNombre(), tokenActivacion);
        return guardado;
    }

    @Transactional
    public Doctor actualizarDoctor(UUID id, RegistroDoctorDTO dto) {
        Doctor doctor = obtenerPorId(id);

        doctor.setNombre(dto.getNombre());
        doctor.setApellido(dto.getApellido());
        doctor.setFotoUrl(dto.getFotoUrl());

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            if (!doctor.getUsuario().getEmail().equalsIgnoreCase(dto.getEmail()) &&
                    usuarioService.existePorEmail(dto.getEmail())) {
                throw new IllegalArgumentException("El email ya pertenece a otro usuario: " + dto.getEmail());
            }
            doctor.getUsuario().setEmail(dto.getEmail());
        }

        if (dto.getEspecialidadIds() != null) {
            var especialidades = especialidadService.obtenerTodasPorIds(dto.getEspecialidadIds());
            doctor.setEspecialidades(new HashSet<>(especialidades));
        }

        return doctorRepository.save(doctor);
    }

    @Transactional
    public void eliminarDoctor(UUID id) {
        Doctor doctor = obtenerPorId(id);
        doctorRepository.delete(doctor);
    }

    @Transactional
    public Doctor cambiarDisponibilidadTurnos(UUID id, boolean disponible) {
        Doctor doctor = obtenerPorId(id);
        doctor.setDisponibleParaTurnos(disponible);
        return doctorRepository.save(doctor);
    }

    @Transactional
    public Doctor configurarAdvertenciaBloqueante(UUID id, boolean activa, String mensaje) {
        Doctor doctor = obtenerPorId(id);
        doctor.setTieneAdvertenciaBloqueante(activa);
        doctor.setMensajeAdvertenciaBloqueante(mensaje);
        return doctorRepository.save(doctor);
    }

    @Transactional
    public Doctor configurarAdvertenciaInformativa(UUID id, boolean activa, String mensaje) {
        Doctor doctor = obtenerPorId(id);
        doctor.setTieneAdvertenciaInformativa(activa);
        doctor.setMensajeAdvertenciaInformativa(mensaje);
        return doctorRepository.save(doctor);
    }

    public List<Doctor> listarTodos() {
        return listarTodos(false);
    }

    public List<Doctor> listarTodos(boolean soloVisibles) {
        if (soloVisibles) {
            return doctorRepository.findByDisponibleParaTurnosTrue();
        }
        return doctorRepository.findAll();
    }

    public List<Doctor> listarPorEspecialidad(Long especialidadId, boolean soloVisibles) {
        if (soloVisibles) {
            return doctorRepository.findByEspecialidadesIdAndDisponibleParaTurnosTrue(especialidadId);
        }
        return doctorRepository.findByEspecialidadesId(especialidadId);
    }

    public Doctor obtenerPorId(UUID id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado con ID: " + id));
    }

    @Transactional
    public Doctor obtenerPorIdConBloqueo(UUID id) {
        return doctorRepository.findByIdWithLock(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado con ID: " + id));
    }

    public Optional<Doctor> obtenerPorUsuarioId(Long usuarioId) {
        return doctorRepository.findByUsuarioId(usuarioId);
    }

    @org.springframework.context.event.EventListener
    @Transactional
    public void alEliminarUsuario(com.consultorio.event.UsuarioEliminadoEvent event) {
        doctorRepository.findByUsuarioId(event.usuarioId())
                .ifPresent(doctorRepository::delete);
    }

    @Transactional
    public void eliminarDoctor(Doctor doctor) {
        if (doctor != null) {
            doctorRepository.delete(doctor);
        }
    }

    @Transactional
    public Doctor crearDoctorParaUsuario(Usuario usuario, String nombre, String apellido, String fotoUrl, List<Long> especialidadIds) {
        Set<Especialidad> especialidades = new HashSet<>();
        if (especialidadIds != null && !especialidadIds.isEmpty()) {
            especialidades = new HashSet<>(especialidadService.obtenerTodasPorIds(especialidadIds));
        }
        Doctor doctor = Doctor.builder()
                .usuario(usuario)
                .nombre(nombre != null ? nombre.trim() : "")
                .apellido(apellido != null ? apellido.trim() : "")
                .fotoUrl(fotoUrl != null ? fotoUrl.trim() : null)
                .especialidades(especialidades)
                .build();
        return doctorRepository.save(doctor);
    }

    public boolean estaDisponibleParaTurnos(UUID doctorId) {
        return doctorRepository.findById(doctorId)
                .map(Doctor::isDisponibleParaTurnos)
                .orElse(false);
    }

    // Delegación hacia DoctorAgendaService para backward-compatibility
    @Transactional
    public HorarioAtencion agregarHorarioAtencion(UUID doctorId, HorarioAtencionDTO dto) {
        return doctorAgendaService.agregarHorarioAtencion(doctorId, dto);
    }

    @Transactional
    public HorarioAtencion actualizarHorarioAtencion(Long horarioId, HorarioAtencionDTO dto) {
        return doctorAgendaService.actualizarHorarioAtencion(horarioId, dto);
    }

    @Transactional(readOnly = true)
    public List<HorarioAtencion> obtenerHorariosDoctor(UUID doctorId) {
        return doctorAgendaService.obtenerHorariosDoctor(doctorId);
    }

    @Transactional
    public void eliminarHorarioAtencion(Long horarioId) {
        doctorAgendaService.eliminarHorarioAtencion(horarioId);
    }

    @Transactional
    public void limpiarHorariosSemana(UUID doctorId, LocalDate desde, LocalDate hasta) {
        doctorAgendaService.limpiarHorariosSemana(doctorId, desde, hasta);
    }

    @Transactional
    public void limpiarHorariosPorFecha(UUID doctorId, LocalDate fecha) {
        doctorAgendaService.limpiarHorariosPorFecha(doctorId, fecha);
    }

    @Transactional
    public void limpiarHorariosPorDiaSemana(UUID doctorId, DiaSemana diaSemana) {
        doctorAgendaService.limpiarHorariosPorDiaSemana(doctorId, diaSemana);
    }

    @Transactional(readOnly = true)
    public List<SlotHorario> obtenerSlotsDoctor(UUID doctorId, LocalDate desde, LocalDate hasta) {
        return doctorAgendaService.obtenerSlotsDoctor(doctorId, desde, hasta);
    }

    @Transactional(readOnly = true)
    public List<SlotHorario> obtenerSlotsDoctorPorFecha(UUID doctorId, LocalDate fecha) {
        return doctorAgendaService.obtenerSlotsDoctorPorFecha(doctorId, fecha);
    }

    @Transactional
    public void eliminarSlotIndividual(Long slotId) {
        doctorAgendaService.eliminarSlotIndividual(slotId);
    }

    @Transactional(readOnly = true)
    public boolean existeSlotActivo(UUID doctorId, LocalDate fecha, LocalTime horaInicio) {
        return doctorAgendaService.existeSlotActivo(doctorId, fecha, horaInicio);
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void limpiarDoctoresExpirados() {
        List<Doctor> todos = doctorRepository.findAll();
        for (Doctor doc : todos) {
            Usuario u = doc.getUsuario();
            if (u != null && !u.isActivo() && !u.isEmailVerificado()
                    && u.getTokenVerificacionExpiracion() != null
                    && u.getTokenVerificacionExpiracion().isBefore(java.time.LocalDateTime.now())) {
                doctorRepository.delete(doc);
                usuarioService.eliminarEntidadDirecta(u);
            }
        }
    }

    @Transactional(readOnly = true)
    public Doctor obtenerPorUsuarioEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email del usuario autenticado no puede ser nulo o vacío.");
        }
        String cleanEmail = email.trim();
        return doctorRepository.findByUsuarioEmailIgnoreCase(cleanEmail)
                .orElseGet(() -> doctorRepository.findByUsuarioEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado para el usuario con email: " + email)));
    }

    @Transactional
    public Doctor actualizarPerfilDoctor(UUID id, com.consultorio.dto.ActualizarPerfilDoctorDTO dto) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado con id: " + id));

        doctor.setNombre(dto.getNombre().trim());
        doctor.setApellido(dto.getApellido().trim());

        if (dto.getFotoUrl() != null) {
            doctor.setFotoUrl(dto.getFotoUrl().trim());
        }

        if (dto.getEspecialidadIds() != null) {
            List<Especialidad> nuevasEspecialidades = especialidadService.obtenerTodasPorIds(dto.getEspecialidadIds());
            doctor.setEspecialidades(new HashSet<>(nuevasEspecialidades));
        }

        Usuario usuario = doctor.getUsuario();
        if (usuario != null) {
            usuarioService.actualizarCredenciales(usuario.getId(), dto.getEmail(), dto.getPassword());
        }

        return doctorRepository.save(doctor);
    }
}
