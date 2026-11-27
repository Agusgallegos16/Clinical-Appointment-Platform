package com.consultorio.config;

import com.consultorio.domain.AuthProvider;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.UsuarioRepository;
import com.consultorio.service.UsuarioPruebaService;
import com.consultorio.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final UsuarioPruebaService usuarioPruebaService;
    private final Environment environment;

    @Value("${app.admin.email:admin@adminconsultorio.com}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.dev.seed-test-users:false}")
    private boolean seedTestUsers;

    @Autowired
    public DataInitializer(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           JdbcTemplate jdbcTemplate,
                           UsuarioPruebaService usuarioPruebaService,
                           Environment environment) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.usuarioPruebaService = usuarioPruebaService;
        this.environment = environment;
    }

    @Override
    public void run(String... args) throws Exception {
        // Actualizaciones automáticas de DDL para bases de datos preexistentes (PostgreSQL / H2)
        ejecutarDdlSeguro("ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check", "Eliminar check constraint rol anterior");
        ejecutarDdlSeguro("ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check CHECK (rol IN ('ADMIN', 'DOCTOR', 'PACIENTE', 'SECRETARIA'))", "Check constraint roles");
        ejecutarDdlSeguro("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS google_id VARCHAR(255)", "Columna google_id en usuarios");
        ejecutarDdlSeguro("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(50) DEFAULT 'LOCAL'", "Columna auth_provider en usuarios");
        ejecutarDdlSeguro("UPDATE usuarios SET auth_provider = 'LOCAL' WHERE auth_provider IS NULL", "Poblar auth_provider por defecto en usuarios existentes");
        ejecutarDdlSeguro("ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS email VARCHAR(100)", "Columna email en pacientes");
        ejecutarDdlSeguro("ALTER TABLE pacientes ALTER COLUMN dni DROP NOT NULL", "Hacer DNI nullable en pacientes para Google OAuth JIT");
        ejecutarDdlSeguro("ALTER TABLE turnos ADD COLUMN IF NOT EXISTS google_event_id_secretaria VARCHAR(255)", "Columna google_event_id_secretaria en turnos");
        ejecutarDdlSeguro("CREATE UNIQUE INDEX IF NOT EXISTS uk_turnos_doctor_fecha_activos ON turnos (doctor_id, fecha_hora) WHERE estado != 'CANCELADO'", "Índice único de turnos activos");
        ejecutarDdlSeguro("CREATE TABLE IF NOT EXISTS configuracion_sistema (" +
                "id BIGINT PRIMARY KEY, " +
                "resumen_diario_medicos_habilitado BOOLEAN NOT NULL DEFAULT TRUE, " +
                "resumen_semanal_medicos_habilitado BOOLEAN NOT NULL DEFAULT TRUE, " +
                "fecha_modificacion TIMESTAMP, " +
                "modificado_por VARCHAR(100))", "Tabla de configuración global del sistema");
        ejecutarDdlSeguro("INSERT INTO configuracion_sistema (id, resumen_diario_medicos_habilitado, resumen_semanal_medicos_habilitado) " +
                "SELECT 1, TRUE, TRUE WHERE NOT EXISTS (SELECT 1 FROM configuracion_sistema WHERE id = 1)", "Inicializar fila singleton de configuración");
        log.info("✅ DDL y migraciones de tablas usuarios, pacientes, turnos y configuración aplicadas correctamente.");
        boolean esProduccion = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));

        if (!usuarioRepository.existsByEmail(adminEmail) && !usuarioRepository.existsByRol(Rol.ADMIN)) {
            String passAdmin = adminPassword;
            if ((passAdmin == null || passAdmin.isBlank()) && !esProduccion) {
                passAdmin = "AdminLocal123!";
            }

            if (passAdmin == null || passAdmin.isBlank()) {
                log.warn("⚠️ No se pudo inicializar el Administrador: se requiere configurar 'app.admin.password' o 'APP_ADMIN_PASSWORD'.");
            } else {
                Usuario admin = Usuario.builder()
                        .email(adminEmail.trim())
                        .password(passwordEncoder.encode(passAdmin.trim()))
                        .rol(Rol.ADMIN)
                        .activo(true)
                        .emailVerificado(true)
                        .authProvider(AuthProvider.LOCAL)
                        .build();

                usuarioRepository.save(admin);
                log.info("✅ Usuario Administrador inicial ({}) creado correctamente en la primera ejecución.", adminEmail);
            }
        } else {
            log.info("ℹ️ Inicialización omitida: Ya existe al menos un usuario Administrador en la base de datos.");
        }

        boolean esPerfilLocalODev = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("local") || p.equalsIgnoreCase("dev"));

        if (!esProduccion && (seedTestUsers || esPerfilLocalODev)) {
            log.info("🌱 Seeding activo para entorno de desarrollo local: sembrando usuarios y especialidades de prueba...");
            int creados = usuarioPruebaService.poblarUsuariosDePrueba();
            log.info("✅ Seeding finalizado con éxito: {} usuarios de prueba creados o confirmados.", creados);
        } else if (seedTestUsers && esProduccion) {
            log.warn("⚠️ Seeding de usuarios de prueba omitido: perfil de producción activo.");
        }
    }

    private void ejecutarDdlSeguro(String sql, String descripcion) {
        try {
            jdbcTemplate.execute(sql);
            log.debug("DDL aplicado: {}", descripcion);
        } catch (Exception e) {
            log.debug("DDL ya aplicado o omitido ({}) - Info: {}", descripcion, e.getMessage());
        }
    }
}
