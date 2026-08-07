package com.consultorio.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("API - Sistema de Gestión de Turnos Médicos")
                        .description("Documentación interactiva de la API RESTful para la gestión de turnos médicos, agendas de doctores, plantillas avanzadas e integración con Google Calendar API v3.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Consultorio Médico - Equipo de Desarrollo")
                                .email("soporte@consultorio.com")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingrese el Token JWT obtenido al hacer Login en /api/auth/login")
                        )
                );
    }
}
