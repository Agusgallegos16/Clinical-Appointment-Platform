package com.consultorio.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:prodprobe;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "app.jwt.secret=ProdProbeSuperSecretKeyWithAtLeast32BytesLength!",
        "app.cors.allowed-origins=https://app.consultorio.example",
        "spring.security.oauth2.client.registration.google.client-id=dummy-id",
        "spring.security.oauth2.client.registration.google.client-secret=dummy-secret",
        "app.admin.email=admin@prodprobe.test",
        "app.admin.password=ProdAdminStrongPass12!"
})
class Phase3ProdProfileSecurityProbesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S9-Probe (prod): Swagger/OpenAPI and H2 console are denied even for ADMIN; strict prod security headers emitted on HTTPS")
    void prodProfileBlocksSwaggerAndH2AndEmitsStrictHeaders() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/v3/api-docs").with(user("admin@prodprobe.test").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/swagger-ui.html").with(user("admin@prodprobe.test").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/swagger-ui/index.html").with(user("admin@prodprobe.test").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/h2-console/").with(user("admin@prodprobe.test").roles("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/public/ping").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=()"));
    }
}
