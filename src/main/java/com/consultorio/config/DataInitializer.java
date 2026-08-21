package com.consultorio.config;

import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Value("${app.admin.email:admin@adminconsultorio.com}")
    private String adminEmail;

    @Value("${app.admin.password:82HgaPoa9Aq}")
    private String adminPassword;

    @Autowired
    public DataInitializer(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // Actualizar automáticamente la restricción de base de datos PostgreSQL para permitir el rol SECRETARIA
        try {
            jdbcTemplate.execute("ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check");
            jdbcTemplate.execute("ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check CHECK (rol IN ('ADMIN', 'DOCTOR', 'PACIENTE', 'SECRETARIA'))");
            jdbcTemplate.execute("ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS email VARCHAR(100)");
            jdbcTemplate.execute("ALTER TABLE turnos ADD COLUMN IF NOT EXISTS google_event_id_secretaria VARCHAR(255)");
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_turnos_doctor_fecha_activos ON turnos (doctor_id, fecha_hora) WHERE estado != 'CANCELADO'");
            log.info("✅ DDL de tablas usuarios, pacientes y restricciones únicas de turnos actualizados correctamente.");
        } catch (Exception e) {
            log.warn("ℹ️ Nota sobre DDL inicial: {}", e.getMessage());
        }
        if (!usuarioRepository.existsByEmail(adminEmail) && !usuarioRepository.existsByRol(Rol.ADMIN)) {
            Usuario admin = Usuario.builder()
                    .email(adminEmail.trim())
                    .password(passwordEncoder.encode(adminPassword.trim()))
                    .rol(Rol.ADMIN)
                    .activo(true)
                    .emailVerificado(true)
                    .build();

            usuarioRepository.save(admin);
            log.info("✅ Usuario Administrador inicial ({}) creado correctamente en la primera ejecución.", adminEmail);
        } else {
            log.info("ℹ️ Inicialización omitida: Ya existe al menos un usuario Administrador en la base de datos.");
        }
    }
}
