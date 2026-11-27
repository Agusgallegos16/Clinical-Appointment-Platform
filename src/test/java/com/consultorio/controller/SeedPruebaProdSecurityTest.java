package com.consultorio.controller;

import com.consultorio.repository.UsuarioRepository;
import com.consultorio.service.UsuarioPruebaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "app.dev.seed-test-users=true",
        "app.admin.password=SecretProdPassword123!",
        "spring.datasource.url=jdbc:h2:mem:seedprodsec;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=secret-key-for-prod-profile-testing-purpose-only-256-bits-long",
        "app.cors.allowed-origins=http://localhost:3000",
        "app.google.calendar.redirect-uri=http://localhost:8080/api/google-calendar/callback"
})
class SeedPruebaProdSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioPruebaService usuarioPruebaService;

    @Test
    @WithMockUser(username = "admin@adminconsultorio.com", roles = {"ADMIN"})
    @DisplayName("HIGH-06: POST /api/admin/usuarios/seed-prueba debe ser rechazado con 403 en perfil prod sin crear cuentas @test.com")
    void endpointSeedPruebaDebeEstarBloqueadoEnPerfilProduccion() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios/seed-prueba"))
                .andExpect(status().isForbidden());

        assertFalse(usuarioRepository.existsByEmail("doctor@test.com"),
                "No debe crearse doctor@test.com en producción");
        assertFalse(usuarioRepository.existsByEmail("secretaria@test.com"),
                "No debe crearse secretaria@test.com en producción");
    }

    @Test
    @DisplayName("HIGH-06: UsuarioPruebaService.poblarUsuariosDePrueba debe lanzar AccessDeniedException bajo perfil prod")
    void servicioSeedPruebaDebeRechazarEjecucionDirectaEnProduccion() {
        assertThrows(AccessDeniedException.class, () -> usuarioPruebaService.poblarUsuariosDePrueba());
        assertFalse(usuarioRepository.existsByEmail("paciente@test.com"));
    }
}
