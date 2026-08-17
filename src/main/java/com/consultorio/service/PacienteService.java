package com.consultorio.service;

import com.consultorio.domain.EstadoTurno;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Turno;
import com.consultorio.domain.Usuario;
import com.consultorio.dto.PacienteResumenEstadisticasDTO;
import com.consultorio.dto.RegistroPacienteDTO;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.TurnoRepository;
import com.consultorio.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final TurnoRepository turnoRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PacienteService(PacienteRepository pacienteRepository,
                           UsuarioRepository usuarioRepository,
                           TurnoRepository turnoRepository,
                           EmailService emailService,
                           PasswordEncoder passwordEncoder) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.turnoRepository = turnoRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Paciente registrarPaciente(RegistroPacienteDTO dto) {
        if (dto.getConfirmarPassword() != null && !dto.getPassword().equals(dto.getConfirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden. Por favor verifíquelas.");
        }
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario registrado con el email: " + dto.getEmail());
        }

        java.util.Optional<Paciente> pacienteExistenteOpt = pacienteRepository.findByDni(dto.getDni());
        if (pacienteExistenteOpt.isPresent()) {
            Paciente existente = pacienteExistenteOpt.get();
            if (existente.getUsuario() != null) {
                throw new IllegalArgumentException("Ya existe un paciente registrado con el DNI: " + dto.getDni());
            }
        }

        String tokenVerificacion = UUID.randomUUID().toString();

        Usuario usuario = Usuario.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .rol(Rol.PACIENTE)
                .activo(false) // Inactivo hasta que confirme por email
                .emailVerificado(false)
                .tokenVerificacionEmail(tokenVerificacion)
                .tokenVerificacionExpiracion(LocalDateTime.now().plusHours(24))
                .build();

        Paciente paciente;
        if (pacienteExistenteOpt.isPresent()) {
            // El paciente ya existía previamente como menor a cargo (sin usuario propio).
            // Le vinculamos la nueva cuenta de Usuario sin perder su historial médico ni DNI.
            paciente = pacienteExistenteOpt.get();
            paciente.setUsuario(usuario);
            paciente.setNombre(dto.getNombre());
            paciente.setApellido(dto.getApellido());
            paciente.setTelefono(dto.getTelefono());
            if (dto.getFechaNacimiento() != null) {
                paciente.setFechaNacimiento(dto.getFechaNacimiento());
            }
            paciente.setTutor(null); // Al registrar su propia cuenta, pasa a ser un paciente independiente
        } else {
            paciente = Paciente.builder()
                    .usuario(usuario)
                    .nombre(dto.getNombre())
                    .apellido(dto.getApellido())
                    .dni(dto.getDni())
                    .telefono(dto.getTelefono())
                    .fechaNacimiento(dto.getFechaNacimiento())
                    .build();
        }

        Paciente guardado = pacienteRepository.save(paciente);

        // Enviar correo de verificación de cuenta (Double Opt-In)
        emailService.enviarEmailVerificacion(guardado.getUsuario().getEmail(), guardado.getNombre(), tokenVerificacion);

        return guardado;
    }

    @Transactional
    public boolean confirmarEmail(String token) {
        Usuario usuario = usuarioRepository.findByTokenVerificacionEmail(token)
                .orElseThrow(() -> new IllegalArgumentException("El token de activación es inválido o no existe."));

        if (usuario.getTokenVerificacionExpiracion() != null && usuario.getTokenVerificacionExpiracion().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El token de activación ha expirado. Por favor solicite un nuevo registro.");
        }

        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacionEmail(null);
        usuario.setTokenVerificacionExpiracion(null);
        usuarioRepository.save(usuario);

        // Enviar correo de bienvenida tras activar exitosamente
        pacienteRepository.findByUsuarioEmail(usuario.getEmail())
                .ifPresent(p -> emailService.enviarEmailBienvenida(usuario.getEmail(), p.getNombre()));

        return true;
    }

    public Paciente obtenerPorId(UUID id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado con id: " + id));
    }

    public Paciente obtenerPorUsuarioEmail(String email) {
        return pacienteRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado con email: " + email));
    }

    public List<Paciente> listarTodos() {
        return pacienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PacienteResumenEstadisticasDTO obtenerEstadisticasPaciente(UUID pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado con id: " + pacienteId));

        List<Turno> turnos = turnoRepository.findByPacienteIdOrderByFechaHoraDesc(pacienteId);

        int total = turnos.size();
        int completados = (int) turnos.stream().filter(t -> t.getEstado() == EstadoTurno.COMPLETADO).count();
        int ausentes = (int) turnos.stream().filter(t -> t.getEstado() == EstadoTurno.AUSENTE).count();
        int cancelados = (int) turnos.stream().filter(t -> t.getEstado() == EstadoTurno.CANCELADO).count();
        int pendientes = (int) turnos.stream().filter(t -> t.getEstado() == EstadoTurno.CONFIRMADO || t.getEstado() == EstadoTurno.PENDIENTE).count();

        double pctCompletados = total > 0 ? Math.round((completados * 100.0 / total) * 10.0) / 10.0 : 0.0;
        double pctAusentes = total > 0 ? Math.round((ausentes * 100.0 / total) * 10.0) / 10.0 : 0.0;
        double pctCancelados = total > 0 ? Math.round((cancelados * 100.0 / total) * 10.0) / 10.0 : 0.0;
        double pctPendientes = total > 0 ? Math.round((pendientes * 100.0 / total) * 10.0) / 10.0 : 0.0;

        return PacienteResumenEstadisticasDTO.builder()
                .id(paciente.getId())
                .nombre(paciente.getNombre())
                .apellido(paciente.getApellido())
                .dni(String.valueOf(paciente.getDni()))
                .telefono(paciente.getTelefono())
                .email(paciente.getUsuario() != null ? paciente.getUsuario().getEmail() : "")
                .fechaNacimiento(paciente.getFechaNacimiento())
                .edad(paciente.getEdad())
                .totalTurnos(total)
                .totalCompletados(completados)
                .totalAusentes(ausentes)
                .totalCancelados(cancelados)
                .totalPendientes(pendientes)
                .porcentajeCompletados(pctCompletados)
                .porcentajeAusentes(pctAusentes)
                .porcentajeCancelados(pctCancelados)
                .porcentajePendientes(pctPendientes)
                .build();
    }

    @Transactional
    public com.consultorio.dto.PacienteMenorResponseDTO registrarMenor(UUID tutorPacienteId, com.consultorio.dto.RegistroMenorDTO dto) {
        Paciente tutor = pacienteRepository.findById(tutorPacienteId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente tutor no encontrado con id: " + tutorPacienteId));

        if (dto.getFechaNacimiento() != null) {
            int edadCalculada = java.time.Period.between(dto.getFechaNacimiento(), java.time.LocalDate.now()).getYears();
            if (edadCalculada >= 18) {
                throw new IllegalArgumentException("No es posible vincular a una persona mayor o igual a 18 años como menor a cargo.");
            }
        }

        java.util.Optional<Paciente> pacienteExistenteOpt = pacienteRepository.findByDni(dto.getDni());
        Paciente menor;

        if (pacienteExistenteOpt.isPresent()) {
            Paciente existente = pacienteExistenteOpt.get();

            // Si ya posee una cuenta propia independiente con Usuario, no se puede vincular como menor sin cuenta
            if (existente.getUsuario() != null) {
                throw new IllegalArgumentException("El DNI ingresado pertenece a un paciente registrado con cuenta propia independiente.");
            }

            // Si ya está vinculado a este mismo tutor
            if (existente.getTutor() != null && existente.getTutor().getId().equals(tutorPacienteId)) {
                throw new IllegalArgumentException("El menor ya se encuentra vinculado a tu cuenta.");
            }

            // Si está vinculado a otro tutor activo (menor de 18 años)
            if (existente.getTutor() != null && (existente.getEdad() == null || existente.getEdad() < 18)) {
                throw new IllegalArgumentException("El menor ya se encuentra vinculado a otro tutor a cargo.");
            }

            // Revincular o actualizar los datos del menor existente que estaba desvinculado
            menor = existente;
            menor.setTutor(tutor);
            menor.setNombre(dto.getNombre());
            menor.setApellido(dto.getApellido());
            if (dto.getFechaNacimiento() != null) {
                menor.setFechaNacimiento(dto.getFechaNacimiento());
            }
        } else {
            menor = Paciente.builder()
                    .tutor(tutor)
                    .nombre(dto.getNombre())
                    .apellido(dto.getApellido())
                    .dni(dto.getDni())
                    .fechaNacimiento(dto.getFechaNacimiento())
                    .usuario(null) // Menor a cargo no posee credenciales de acceso
                    .build();
        }

        Paciente guardado = pacienteRepository.save(menor);
        return mapearMenorResponseDTO(guardado);
    }

    @Transactional
    public List<com.consultorio.dto.PacienteMenorResponseDTO> listarMenoresDeTutor(UUID tutorPacienteId) {
        List<Paciente> menores = pacienteRepository.findByTutorId(tutorPacienteId);

        List<Paciente> validos = new java.util.ArrayList<>();
        for (Paciente m : menores) {
            if (m.getEdad() != null && m.getEdad() >= 18) {
                // Desvinculación automática por mayoría de edad (>= 18 años)
                m.setTutor(null);
                pacienteRepository.save(m);
            } else {
                validos.add(m);
            }
        }

        return validos.stream()
                .map(this::mapearMenorResponseDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public boolean desvincularMenor(UUID tutorPacienteId, UUID menorId) {
        Paciente menor = pacienteRepository.findById(menorId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente menor no encontrado con id: " + menorId));

        if (menor.getTutor() == null || !menor.getTutor().getId().equals(tutorPacienteId)) {
            throw new IllegalArgumentException("El menor seleccionado no pertenece a la tutela de este paciente.");
        }

        menor.setTutor(null);
        pacienteRepository.save(menor);
        return true;
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void desvincularMenoresMayoresDeEdad() {
        List<Paciente> vinculados = pacienteRepository.findByTutorIsNotNull();
        for (Paciente p : vinculados) {
            if (p.getEdad() != null && p.getEdad() >= 18) {
                p.setTutor(null);
                pacienteRepository.save(p);
            }
        }
    }

    private com.consultorio.dto.PacienteMenorResponseDTO mapearMenorResponseDTO(Paciente menor) {
        return com.consultorio.dto.PacienteMenorResponseDTO.builder()
                .id(menor.getId())
                .nombre(menor.getNombre())
                .apellido(menor.getApellido())
                .dni(menor.getDni())
                .fechaNacimiento(menor.getFechaNacimiento())
                .edad(menor.getEdad())
                .telefono(menor.getTelefono()) // Hereda el teléfono del tutor
                .tutorId(menor.getTutor() != null ? menor.getTutor().getId() : null)
                .tutorNombre(menor.getTutor() != null ? menor.getTutor().getNombre() + " " + menor.getTutor().getApellido() : null)
                .build();
    }

    @Transactional
    public Paciente actualizarPerfilPaciente(UUID id, com.consultorio.dto.ActualizarPerfilPacienteDTO dto) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado con id: " + id));

        paciente.setNombre(dto.getNombre().trim());
        paciente.setApellido(dto.getApellido().trim());
        if (dto.getTelefono() != null) {
            paciente.setTelefono(dto.getTelefono().trim());
        }

        Usuario usuario = paciente.getUsuario();
        if (usuario != null) {
            String nuevoEmail = dto.getEmail().trim();
            if (!usuario.getEmail().equalsIgnoreCase(nuevoEmail)) {
                if (usuarioRepository.existsByEmail(nuevoEmail)) {
                    throw new IllegalArgumentException("Ya existe una cuenta registrada con el email: " + nuevoEmail);
                }
                usuario.setEmail(nuevoEmail);
            }

            if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
                if (dto.getPassword().trim().length() < 6) {
                    throw new IllegalArgumentException("La nueva contraseña debe tener al menos 6 caracteres.");
                }
                usuario.setPassword(passwordEncoder.encode(dto.getPassword().trim()));
            }
            usuarioRepository.save(usuario);
        }

        return pacienteRepository.save(paciente);
    }
}
