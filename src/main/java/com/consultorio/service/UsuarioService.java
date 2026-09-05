package com.consultorio.service;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.Especialidad;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.dto.EstablecerPasswordDoctorDTO;
import com.consultorio.dto.RegistroUsuarioAdminDTO;
import com.consultorio.dto.SolicitudRestablecerPasswordDTO;
import com.consultorio.dto.UsuarioAdminDTO;
import com.consultorio.repository.DoctorRepository;
import com.consultorio.repository.EspecialidadRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final DoctorRepository doctorRepository;
    private final PacienteRepository pacienteRepository;
    private final EspecialidadRepository especialidadRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository,
                          DoctorRepository doctorRepository,
                          PacienteRepository pacienteRepository,
                          EspecialidadRepository especialidadRepository,
                          EmailService emailService,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.doctorRepository = doctorRepository;
        this.pacienteRepository = pacienteRepository;
        this.especialidadRepository = especialidadRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void solicitarRestablecimientoPassword(SolicitudRestablecerPasswordDTO dto) {
        if (dto.getConfirmarNuevaPassword() != null && !dto.getNuevaPassword().equals(dto.getConfirmarNuevaPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden. Por favor verifíquelas.");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(dto.getEmail());
        if (usuarioOpt.isEmpty()) {
            throw new IllegalArgumentException("No existe ninguna cuenta registrada con el correo electrónico ingresado.");
        }

        Usuario usuario = usuarioOpt.get();
        String token = UUID.randomUUID().toString();

        usuario.setNuevaPasswordPendiente(passwordEncoder.encode(dto.getNuevaPassword()));
        usuario.setTokenRestablecimientoPassword(token);
        usuario.setTokenRestablecimientoExpiracion(LocalDateTime.now().plusHours(1));

        usuarioRepository.save(usuario);

        // Notificación por email con enlace de confirmación
        emailService.enviarEmailRestablecerPassword(usuario.getEmail(), token);
    }

    @Transactional
    public boolean confirmarRestablecimientoPassword(String token) {
        Usuario usuario = usuarioRepository.findByTokenRestablecimientoPassword(token)
                .orElseThrow(() -> new IllegalArgumentException("El token de confirmación es inválido o no existe."));

        if (usuario.getTokenRestablecimientoExpiracion() != null && usuario.getTokenRestablecimientoExpiracion().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El token de confirmación ha expirado. Por favor solicite nuevamente el cambio de contraseña.");
        }

        if (usuario.getNuevaPasswordPendiente() == null) {
            throw new IllegalStateException("No hay ninguna contraseña nueva pendiente de activación.");
        }

        // Actualizar la contraseña real en la base de datos recién tras la confirmación vía mail
        usuario.setPassword(usuario.getNuevaPasswordPendiente());
        usuario.setNuevaPasswordPendiente(null);
        usuario.setTokenRestablecimientoPassword(null);
        usuario.setTokenRestablecimientoExpiracion(null);

        usuarioRepository.save(usuario);
        log.info("✅ Contraseña actualizada con éxito para el usuario: {}", usuario.getEmail());

        return true;
    }

    @Transactional
    public boolean establecerPasswordDoctor(EstablecerPasswordDoctorDTO dto) {
        if (dto.getConfirmarPassword() != null && !dto.getPassword().equals(dto.getConfirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden. Por favor verifíquelas.");
        }

        Usuario usuario = usuarioRepository.findByTokenVerificacionEmail(dto.getToken())
                .orElseThrow(() -> new IllegalArgumentException("El token de activación es inválido o no existe."));

        if (usuario.getTokenVerificacionExpiracion() != null && usuario.getTokenVerificacionExpiracion().isBefore(LocalDateTime.now())) {
            // El token de 24hs ha expirado: descartar el registro para permitir un nuevo alta
            Doctor doctorObj = doctorRepository.findByUsuarioId(usuario.getId()).orElse(null);
            if (doctorObj != null) {
                doctorRepository.delete(doctorObj);
            }
            usuarioRepository.delete(usuario);
            throw new IllegalArgumentException("El enlace de activación ha expirado (límite 24hs). La solicitud de alta fue descartada. Solicite al Administrador que vuelva a registrar su perfil.");
        }

        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacionEmail(null);
        usuario.setTokenVerificacionExpiracion(null);
        usuarioRepository.save(usuario);

        log.info("✅ Cuenta de médico activada con éxito para: {}", usuario.getEmail());
        return true;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioAdminDTO> buscarUsuariosPaginados(String query, Pageable pageable) {
        Page<Usuario> usuariosPage = (query == null || query.trim().isEmpty())
                ? usuarioRepository.findAll(pageable)
                : usuarioRepository.buscarPorNombreApellidoOEmail(query.trim(), pageable);

        return usuariosPage.map(this::mapToUsuarioAdminDTO);
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdminDTO> buscarUsuariosPorEmail(String queryEmail) {
        List<Usuario> usuarios = (queryEmail == null || queryEmail.trim().isEmpty())
                ? usuarioRepository.findAll()
                : usuarioRepository.findByEmailContainingIgnoreCase(queryEmail.trim());

        return usuarios.stream().map(this::mapToUsuarioAdminDTO).collect(Collectors.toList());
    }

    private UsuarioAdminDTO mapToUsuarioAdminDTO(Usuario u) {
        String nombre = null;
        String apellido = null;

        if (u.getRol() == Rol.PACIENTE || u.getRol() == Rol.SECRETARIA) {
            Optional<Paciente> p = pacienteRepository.findByUsuarioId(u.getId());
            if (p.isPresent()) {
                nombre = p.get().getNombre();
                apellido = p.get().getApellido();
            }
        } else if (u.getRol() == Rol.DOCTOR) {
            Optional<Doctor> d = doctorRepository.findByUsuarioId(u.getId());
            if (d.isPresent()) {
                nombre = d.get().getNombre();
                apellido = d.get().getApellido();
            }
        }

        return UsuarioAdminDTO.builder()
                .id(u.getId())
                .email(u.getEmail())
                .rol(u.getRol())
                .activo(u.isActivo())
                .bloqueado(u.isBloqueado())
                .emailVerificado(u.isEmailVerificado())
                .nombre(nombre)
                .apellido(apellido)
                .build();
    }

    @Transactional
    public UsuarioAdminDTO cambiarEstadoBloqueo(Long usuarioId, boolean bloquear, String emailAdminActual) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));

        if (usuario.getEmail().equalsIgnoreCase(emailAdminActual)) {
            throw new IllegalArgumentException("Un administrador no puede cambiar su propio estado de bloqueo.");
        }

        usuario.setBloqueado(bloquear);
        usuarioRepository.save(usuario);

        log.info("🛡️ Estado de bloqueo de usuario {} cambiado a: {}", usuario.getEmail(), bloquear);

        return mapToUsuarioAdminDTO(usuario);
    }

    @Transactional
    public void eliminarUsuario(Long usuarioId, String emailAdminActual) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));

        if (usuario.getEmail().equalsIgnoreCase(emailAdminActual)) {
            throw new IllegalArgumentException("Un administrador no puede auto-eliminarse del sistema.");
        }

        // Si es paciente, eliminar o desvincular perfil Paciente
        Optional<Paciente> paciente = pacienteRepository.findByUsuarioId(usuarioId);
        paciente.ifPresent(pacienteRepository::delete);

        // Si es doctor, eliminar o desvincular perfil Doctor
        Optional<Doctor> doctor = doctorRepository.findByUsuarioId(usuarioId);
        doctor.ifPresent(doctorRepository::delete);

        usuarioRepository.delete(usuario);
        log.info("🗑️ Usuario {} eliminado del sistema por el admin {}", usuario.getEmail(), emailAdminActual);
    }

    @Transactional
    public UsuarioAdminDTO registrarUsuarioPorAdmin(RegistroUsuarioAdminDTO dto) {
        if (dto.getRol() == null) {
            throw new IllegalArgumentException("El rol es obligatorio.");
        }

        String emailLimpio = dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : "";
        if (emailLimpio.isBlank()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio.");
        }

        var usuarioExistenteOpt = usuarioRepository.findByEmailIgnoreCase(emailLimpio);
        if (usuarioExistenteOpt.isPresent()) {
            Usuario exist = usuarioExistenteOpt.get();
            if (!exist.isActivo() && !exist.isEmailVerificado() && exist.getTokenVerificacionExpiracion() != null && exist.getTokenVerificacionExpiracion().isBefore(LocalDateTime.now())) {
                Doctor doc = doctorRepository.findByUsuarioId(exist.getId()).orElse(null);
                if (doc != null) doctorRepository.delete(doc);
                Paciente pac = pacienteRepository.findByUsuarioId(exist.getId()).orElse(null);
                if (pac != null) pacienteRepository.delete(pac);
                usuarioRepository.delete(exist);
                usuarioRepository.flush();
            } else {
                throw new IllegalArgumentException("El correo electrónico " + dto.getEmail() + " ya se encuentra registrado por otro usuario.");
            }
        }

        // Si se especificó DNI, verificar que no pertenezca a otra cuenta activa
        if (dto.getDni() != null) {
            Optional<Paciente> pacienteExistenteDni = pacienteRepository.findByDni(dto.getDni());
            if (pacienteExistenteDni.isPresent() && pacienteExistenteDni.get().getUsuario() != null) {
                Usuario userDni = pacienteExistenteDni.get().getUsuario();
                if (!userDni.getEmail().equalsIgnoreCase(emailLimpio)) {
                    throw new IllegalArgumentException("El DNI " + dto.getDni() + " ya pertenece a otra cuenta registrada (" + userDni.getEmail() + ").");
                }
            }
        }

        String tokenActivacion = UUID.randomUUID().toString();

        Usuario usuario = Usuario.builder()
                .email(emailLimpio)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .rol(dto.getRol())
                .activo(false)
                .emailVerificado(false)
                .tokenVerificacionEmail(tokenActivacion)
                .tokenVerificacionExpiracion(LocalDateTime.now().plusHours(24))
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        if (dto.getRol() == Rol.DOCTOR) {
            List<Especialidad> especialidades = new ArrayList<>();
            if (dto.getEspecialidadIds() != null && !dto.getEspecialidadIds().isEmpty()) {
                especialidades = especialidadRepository.findAllById(dto.getEspecialidadIds());
            }
            Doctor doctor = Doctor.builder()
                    .usuario(usuarioGuardado)
                    .nombre(dto.getNombre().trim())
                    .apellido(dto.getApellido().trim())
                    .fotoUrl(dto.getFotoUrl() != null ? dto.getFotoUrl().trim() : null)
                    .especialidades(especialidades)
                    .build();
            doctorRepository.save(doctor);
        } else if (dto.getRol() == Rol.PACIENTE || dto.getRol() == Rol.SECRETARIA) {
            Paciente pacienteExistente = null;
            if (dto.getDni() != null) {
                pacienteExistente = pacienteRepository.findByDni(dto.getDni()).orElse(null);
            }
            if (pacienteExistente != null) {
                pacienteExistente.setUsuario(usuarioGuardado);
                pacienteExistente.setNombre(dto.getNombre().trim());
                pacienteExistente.setApellido(dto.getApellido().trim());
                if (dto.getTelefono() != null) pacienteExistente.setTelefono(dto.getTelefono().trim());
                if (dto.getFechaNacimiento() != null) pacienteExistente.setFechaNacimiento(dto.getFechaNacimiento());
                pacienteRepository.save(pacienteExistente);
            } else {
                Paciente nuevoPaciente = Paciente.builder()
                        .usuario(usuarioGuardado)
                        .dni(dto.getDni())
                        .nombre(dto.getNombre().trim())
                        .apellido(dto.getApellido().trim())
                        .telefono(dto.getTelefono() != null ? dto.getTelefono().trim() : null)
                        .fechaNacimiento(dto.getFechaNacimiento())
                        .build();
                pacienteRepository.save(nuevoPaciente);
            }
        }

        emailService.enviarEmailActivacionDoctor(usuarioGuardado.getEmail(), dto.getNombre(), tokenActivacion);

        return UsuarioAdminDTO.builder()
                .id(usuarioGuardado.getId())
                .email(usuarioGuardado.getEmail())
                .rol(usuarioGuardado.getRol())
                .activo(usuarioGuardado.isActivo())
                .bloqueado(usuarioGuardado.isBloqueado())
                .emailVerificado(usuarioGuardado.isEmailVerificado())
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .build();
    }


}
