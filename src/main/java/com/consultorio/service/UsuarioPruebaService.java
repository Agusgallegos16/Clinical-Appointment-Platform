package com.consultorio.service;

import com.consultorio.domain.AuthProvider;
import com.consultorio.domain.Doctor;
import com.consultorio.domain.Especialidad;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.DoctorRepository;
import com.consultorio.repository.EspecialidadRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class UsuarioPruebaService {

  private static final Logger log = LoggerFactory.getLogger(UsuarioPruebaService.class);

  private final UsuarioRepository usuarioRepository;
  private final DoctorRepository doctorRepository;
  private final PacienteRepository pacienteRepository;
  private final EspecialidadRepository especialidadRepository;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  public UsuarioPruebaService(
      UsuarioRepository usuarioRepository,
      DoctorRepository doctorRepository,
      PacienteRepository pacienteRepository,
      EspecialidadRepository especialidadRepository,
      PasswordEncoder passwordEncoder) {
    this.usuarioRepository = usuarioRepository;
    this.doctorRepository = doctorRepository;
    this.pacienteRepository = pacienteRepository;
    this.especialidadRepository = especialidadRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public int poblarUsuariosDePrueba() {
    String defaultPassword = passwordEncoder.encode("Password123!");

    // Inicializar catálogo de especialidades médicas básicas si no existen
    Especialidad espGeneral = especialidadRepository.findByNombreIgnoreCase("Medicina General")
        .orElseGet(() -> especialidadRepository.save(Especialidad.builder()
            .nombre("Medicina General")
            .descripcion("Atención médica primaria integral y preventiva para adultos y familias.")
            .build()));

    Especialidad espCardio = especialidadRepository.findByNombreIgnoreCase("Cardiología")
        .orElseGet(() -> especialidadRepository.save(Especialidad.builder()
            .nombre("Cardiología")
            .descripcion("Prevención, diagnóstico y tratamiento integral de patologías cardiovasculares.")
            .build()));

    Especialidad espPedia = especialidadRepository.findByNombreIgnoreCase("Pediatría")
        .orElseGet(() -> especialidadRepository.save(Especialidad.builder()
            .nombre("Pediatría")
            .descripcion("Atención integral del desarrollo, salud y patologías de niños y adolescentes.")
            .build()));

    Especialidad espDerma = especialidadRepository.findByNombreIgnoreCase("Dermatología")
        .orElseGet(() -> especialidadRepository.save(Especialidad.builder()
            .nombre("Dermatología")
            .descripcion("Diagnóstico y tratamiento de afecciones de la piel, pelo y uñas.")
            .build()));

    Especialidad espTrauma = especialidadRepository.findByNombreIgnoreCase("Traumatología")
        .orElseGet(() -> especialidadRepository.save(Especialidad.builder()
            .nombre("Traumatología")
            .descripcion("Especialidad médica dedicada al tratamiento de lesiones óseas y musculares.")
            .build()));

    String[][] datos = {
        {"Doctor", "Prueba", "doctor@test.com", "DOCTOR", "true", "false", "true"},
        {"Secretaría", "Prueba", "secretaria@test.com", "SECRETARIA", "true", "false", "true"},
        {"Paciente", "Prueba", "paciente@test.com", "PACIENTE", "true", "false", "true"},
        {"Agustín", "Benítez", "agustin.benitez@test.com", "PACIENTE", "true", "false", "true"},
        {"Alicia", "Gómez", "alicia.gomez@test.com", "PACIENTE", "true", "false", "true"},
        {"Beatriz", "Morales", "beatriz.morales@test.com", "DOCTOR", "true", "false", "true"},
        {"Bernardo", "Pérez", "bernardo.perez@test.com", "PACIENTE", "true", "false", "true"},
        {"Camilo", "Fuentes", "camilo.fuentes@test.com", "PACIENTE", "true", "false", "true"},
        {"Carlos", "López", "carlos.lopez@test.com", "SECRETARIA", "true", "false", "true"},
        {"Daniela", "Rossi", "daniela.rossi@test.com", "PACIENTE", "true", "false", "true"},
        {"Diana", "Rodríguez", "diana.rodriguez@test.com", "DOCTOR", "true", "false", "true"},
        {"Esteban", "Castro", "esteban.castro@test.com", "PACIENTE", "true", "false", "true"},
        {"Eugenia", "Blanco", "eugenia.blanco@test.com", "PACIENTE", "true", "false", "false"},
        {"Facundo", "Álvarez", "facundo.alvarez@test.com", "PACIENTE", "true", "false", "true"},
        {"Florencia", "Navarro", "florencia.navarro@test.com", "DOCTOR", "true", "false", "true"},
        {"Gabriel", "Santillán", "gabriel.santillan@test.com", "PACIENTE", "true", "false", "true"},
        {"Gonzalo", "Silva", "gonzalo.silva@test.com", "PACIENTE", "true", "true", "true"},
        {"Helena", "Domínguez", "helena.dominguez@test.com", "PACIENTE", "true", "false", "true"},
        {"Hugo", "Quiroga", "hugo.quiroga@test.com", "SECRETARIA", "true", "false", "true"},
        {"Ignacio", "Ortiz", "ignacio.ortiz@test.com", "PACIENTE", "true", "false", "true"},
        {"Irene", "Barrientos", "irene.barrientos@test.com", "PACIENTE", "true", "false", "true"},
        {"Javier", "Cárdenas", "javier.cardenas@test.com", "DOCTOR", "true", "false", "true"},
        {"Julieta", "Romero", "julieta.romero@test.com", "PACIENTE", "true", "false", "true"},
        {"Karina", "Varela", "karina.varela@test.com", "PACIENTE", "true", "false", "true"},
        {"Kevin", "Rivas", "kevin.rivas@test.com", "PACIENTE", "true", "false", "true"},
        {"Lautaro", "Ferreyra", "lautaro.ferreyra@test.com", "PACIENTE", "true", "false", "false"},
        {"Lucía", "Medina", "lucia.medina@test.com", "DOCTOR", "true", "false", "true"},
        {"Manuel", "Torres", "manuel.torres@test.com", "PACIENTE", "true", "false", "true"},
        {"Mara", "Solís", "mara.solis@test.com", "PACIENTE", "true", "false", "true"},
        {"Martín", "Delgado", "martin.delgado@test.com", "PACIENTE", "true", "false", "true"},
        {"Natalia", "Herrera", "natalia.herrera@test.com", "PACIENTE", "true", "false", "true"},
        {"Nicolás", "Duarte", "nicolas.duarte@test.com", "DOCTOR", "true", "false", "true"},
        {"Octavio", "Ramos", "octavio.ramos@test.com", "PACIENTE", "true", "false", "true"},
        {"Paula", "Vargas", "paula.vargas@test.com", "PACIENTE", "true", "false", "true"},
        {"Ramiro", "Quintana", "ramiro.quintana@test.com", "PACIENTE", "true", "false", "true"},
        {"Romina", "Paz", "romina.paz@test.com", "PACIENTE", "true", "false", "true"},
        {"Santiago", "Méndez", "santiago.mendez@test.com", "DOCTOR", "true", "false", "true"},
        {"Tomás", "Aguirre", "tomas.aguirre@test.com", "PACIENTE", "true", "false", "true"},
        {"Úrsula", "Blanco", "ursula.blanco@test.com", "PACIENTE", "true", "false", "true"},
        {"Valentín", "Ríos", "valentin.rios@test.com", "PACIENTE", "true", "false", "true"},
        {"Walter", "Cáceres", "walter.caceres@test.com", "PACIENTE", "true", "true", "true"},
        {"Ximena", "Soto", "ximena.soto@test.com", "SECRETARIA", "true", "false", "true"},
        {"Zoe", "Godoy", "zoe.godoy@test.com", "PACIENTE", "true", "false", "true"}
    };

    int creados = 0;
    long dniBase = 35000100L;

    for (String[] fila : datos) {
      String nombre = fila[0];
      String apellido = fila[1];
      String email = fila[2];
      Rol rol = Rol.valueOf(fila[3]);
      boolean activo = Boolean.parseBoolean(fila[4]);
      boolean bloqueado = Boolean.parseBoolean(fila[5]);
      boolean emailVerificado = Boolean.parseBoolean(fila[6]);

      if (usuarioRepository.existsByEmail(email)) {
        continue;
      }

      Usuario usuario = Usuario.builder()
          .email(email)
          .password(defaultPassword)
          .rol(rol)
          .activo(activo)
          .bloqueado(bloqueado)
          .emailVerificado(emailVerificado)
          .authProvider(AuthProvider.LOCAL)
          .build();

      Usuario savedUser = usuarioRepository.save(usuario);

      if (rol == Rol.PACIENTE || rol == Rol.SECRETARIA) {
        while (pacienteRepository.existsByDni(dniBase)) {
          dniBase++;
        }
        Paciente paciente = Paciente.builder()
            .usuario(savedUser)
            .nombre(nombre)
            .apellido(apellido)
            .dni(dniBase++)
            .telefono("+54 11 4455-66" + String.format("%02d", (creados % 100)))
            .email(email)
            .fechaNacimiento(java.time.LocalDate.of(1990 + (creados % 20), (creados % 12) + 1, (creados % 25) + 1))
            .build();
        pacienteRepository.save(paciente);
      } else if (rol == Rol.DOCTOR) {
        Set<Especialidad> especialidadesDoctor = new HashSet<>();
        String emailLower = email.toLowerCase();
        if (emailLower.contains("diana") || emailLower.contains("cardio")) {
          especialidadesDoctor.add(espCardio);
        } else if (emailLower.contains("florencia") || emailLower.contains("santiago") || emailLower.contains("pedia")) {
          especialidadesDoctor.add(espPedia);
        } else if (emailLower.contains("javier") || emailLower.contains("derma")) {
          especialidadesDoctor.add(espDerma);
        } else if (emailLower.contains("lucia") || emailLower.contains("trauma")) {
          especialidadesDoctor.add(espTrauma);
        } else if (emailLower.contains("nicolas") || emailLower.contains("doctor@test.com")) {
          especialidadesDoctor.add(espCardio);
          especialidadesDoctor.add(espGeneral);
        } else {
          especialidadesDoctor.add(espGeneral);
        }

        Doctor doctor = Doctor.builder()
            .usuario(savedUser)
            .nombre(nombre)
            .apellido(apellido)
            .disponibleParaTurnos(true)
            .especialidades(especialidadesDoctor)
            .build();
        doctorRepository.save(doctor);
      }

      creados++;
    }

    log.info("Se han creado {} usuarios de prueba en la base de datos.", creados);
    return creados;
  }
}
