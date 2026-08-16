package com.consultorio.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("API - Sistema de Gestión de Turnos Médicos")
                        .description("Documentación interactiva oficial de la API RESTful para la gestión del Consultorio Médico.\n\n"
                                + "### 🚀 Funcionalidades Principales:\n"
                                + "- **Autenticación y Roles**: Seguridad basada en JWT Bearer Token (ADMIN, DOCTOR, PACIENTE).\n"
                                + "- **Gestión de Pacientes**: Perfil de paciente, historial de turnos y registro/desvinculación de menores a cargo.\n"
                                + "- **Configuración de Médicos**: Control de visibilidad pública de perfil (`disponibleParaTurnos`) y configuración de doble sistema de advertencias (Bloqueante e Informativa).\n"
                                + "- **Agenda y Disponibilidad**: Definición de horarios semanales, plantillas avanzadas de atención y consulta dinámica de slots libres.\n"
                                + "- **Sincronización con Google Calendar**: Conexión OAuth2 por usuario para sincronización automática de turnos en calendarios personales.\n"
                                + "- **Notificaciones Automáticas**: Recordatorios por correo electrónico y resúmenes de agenda diarios y semanales.")
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Consultorio Médico - Equipo de Desarrollo")
                                .email("soporte@consultorio.com")
                        )
                )
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Servidor Local de Desarrollo")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingrese el Token JWT obtenido al autenticarse en `/api/auth/login`.")
                        )
                );
    }
}
