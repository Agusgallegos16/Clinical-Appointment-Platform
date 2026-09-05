package com.consultorio.service;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.DoctorRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioPruebaService {

  private static final Logger log = LoggerFactory.getLogger(UsuarioPruebaService.class);

  private final UsuarioRepository usuarioRepository;
  private final DoctorRepository doctorRepository;
  private final PacienteRepository pacienteRepository;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  public UsuarioPruebaService(
      UsuarioRepository usuarioRepository,
      DoctorRepository doctorRepository,
      PacienteRepository pacienteRepository,
      PasswordEncoder passwordEncoder, UsuarioRepository usuarioRepository1, DoctorRepository doctorRepository1, PacienteRepository pacienteRepository1, PasswordEncoder passwordEncoder1){
    this.usuarioRepository = usuarioRepository1;
    this.doctorRepository = doctorRepository1;
    this.pacienteRepository = pacienteRepository1;
    this.passwordEncoder = passwordEncoder1;
  }

  @Transactional
  public int poblarUsuariosDePrueba() {
    String defaultPassword = passwordEncoder.encode("Password123!");

    String[][] datos = {
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
        Doctor doctor = Doctor.builder()
            .usuario(savedUser)
            .nombre(nombre)
            .apellido(apellido)
            .disponibleParaTurnos(true)
            .build();
        doctorRepository.save(doctor);
      }

      creados++;
    }

    log.info("Se han creado {} usuarios de prueba en la base de datos.", creados);
    return creados;
  }
}
