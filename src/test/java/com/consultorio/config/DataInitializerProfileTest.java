package com.consultorio.config;

import com.consultorio.domain.Usuario;
import com.consultorio.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "app.dev.seed-test-users=true",
        "app.admin.password=SecretProdPassword123!",
        "spring.datasource.url=jdbc:h2:mem:prodtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=secret-key-for-prod-profile-testing-purpose-only-256-bits-long",
        "app.cors.allowed-origins=http://localhost:3000",
        "app.google.calendar.redirect-uri=http://localhost:8080/callback"
})
class DataInitializerProfileTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Perfil prod: Aunque seed-test-users sea true, el seeder no debe poblar usuarios de prueba")
    void testSeederNoEjecutaBajoPerfilProduccion() {
        List<Usuario> usuarios = usuarioRepository.findAll();

        // En producción solo debe existir el administrador inicial, no las 40 cuentas de prueba
        boolean existenUsuariosDePrueba = usuarios.stream()
                .anyMatch(u -> u.getEmail().endsWith("@ejemplo.com") || u.getEmail().contains("testuser"));

        assertFalse(existenUsuariosDePrueba, "No deben crearse usuarios de prueba en el perfil de producción.");
        assertTrue(usuarios.size() <= 1, "Solo debe registrarse el administrador en la base de datos.");
    }
}
