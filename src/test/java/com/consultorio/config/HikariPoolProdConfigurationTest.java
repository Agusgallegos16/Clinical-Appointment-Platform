package com.consultorio.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "app.admin.password=SecretProdPassword123!",
        "spring.datasource.url=jdbc:h2:mem:hikaritestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=secret-key-for-prod-profile-testing-purpose-only-256-bits-long",
        "app.cors.allowed-origins=http://localhost:3000",
        "app.google.calendar.redirect-uri=http://localhost:8080/callback"
})
class HikariPoolProdConfigurationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Perfil prod: El DataSource debe ser HikariCP con pool calibrado para producción")
    void testHikariPoolConfiguracionProduccion() {
        assertNotNull(dataSource, "El DataSource debe estar configurado.");
        assertTrue(dataSource instanceof HikariDataSource, "El DataSource debe ser una instancia de HikariDataSource.");

        HikariDataSource hikari = (HikariDataSource) dataSource;

        assertEquals(20, hikari.getMaximumPoolSize(), "MaximumPoolSize debe estar configurado en 20.");
        assertEquals(10, hikari.getMinimumIdle(), "MinimumIdle debe estar configurado en 10.");
        assertEquals(15000, hikari.getConnectionTimeout(), "ConnectionTimeout debe estar calibrado en 15000 ms.");
        assertEquals(30000, hikari.getLeakDetectionThreshold(), "LeakDetectionThreshold debe estar calibrado en 30000 ms.");
    }
}
