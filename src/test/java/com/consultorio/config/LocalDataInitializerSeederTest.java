package com.consultorio.config;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.DoctorRepository;
import com.consultorio.repository.EspecialidadRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:localtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=local-dev-jwt-secret-key-must-be-at-least-256-bits-long-384729",
        "app.admin.password=AdminLocal123!"
})
class LocalDataInitializerSeederTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Test
    @DisplayName("Perfil local: DataInitializer debe crear admin, usuarios de prueba y especialidades")
    void testSeederEjecutaEnPerfilLocal() {
        // 1. Verificar administrador inicial
        assertTrue(usuarioRepository.existsByEmail("admin@adminconsultorio.com"),
                "Debe existir el usuario administrador inicial");

        // 2. Verificar cuentas genéricas de testing rápido
        Optional<Usuario> docOpt = usuarioRepository.findByEmail("doctor@test.com");
        assertTrue(docOpt.isPresent(), "Debe existir doctor@test.com");
        assertEquals(Rol.DOCTOR, docOpt.get().getRol());

        Optional<Usuario> secOpt = usuarioRepository.findByEmail("secretaria@test.com");
        assertTrue(secOpt.isPresent(), "Debe existir secretaria@test.com");
        assertEquals(Rol.SECRETARIA, secOpt.get().getRol());

        Optional<Usuario> pacOpt = usuarioRepository.findByEmail("paciente@test.com");
        assertTrue(pacOpt.isPresent(), "Debe existir paciente@test.com");
        assertEquals(Rol.PACIENTE, pacOpt.get().getRol());

        // 3. Verificar que se sembraron las especialidades médicas
        assertTrue(especialidadRepository.count() >= 5, "Deben existir las especialidades básicas");
        assertTrue(especialidadRepository.existsByNombreIgnoreCase("Medicina General"));
        assertTrue(especialidadRepository.existsByNombreIgnoreCase("Cardiología"));

        // 4. Verificar que los doctores tienen especialidades vinculadas
        Optional<Doctor> doctorPrueba = doctorRepository.findByUsuarioId(docOpt.get().getId());
        assertTrue(doctorPrueba.isPresent(), "Debe existir el perfil Doctor para doctor@test.com");
        assertFalse(doctorPrueba.get().getEspecialidades().isEmpty(),
                "El médico debe tener especialidades asignadas para poder agendar turnos");

        // 5. Verificar que el paciente tiene perfil completo
        Optional<Paciente> pacientePrueba = pacienteRepository.findByUsuarioId(pacOpt.get().getId());
        assertTrue(pacientePrueba.isPresent(), "Debe existir el perfil Paciente para paciente@test.com");
        assertTrue(pacientePrueba.get().isPerfilCompleto(), "El paciente de prueba debe tener perfil completo");
    }
}
