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

    @Value("${app.admin.email:admin@adminconsultorio.com}")
    private String adminEmail;

    @Value("${app.admin.password:82HgaPoa9Aq}")
    private String adminPassword;

    @Autowired
    public DataInitializer(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
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
