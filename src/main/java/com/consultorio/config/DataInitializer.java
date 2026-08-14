package com.consultorio.config;

import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@adminconsultorio.com";

        if (!usuarioRepository.existsByEmail(adminEmail)) {
            Usuario admin = Usuario.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode("82HgaPoa9Aq"))
                    .rol(Rol.ADMIN)
                    .activo(true)
                    .emailVerificado(true)
                    .build();

            usuarioRepository.save(admin);
            log.info("✅ Usuario Administrador inicial ({}) creado correctamente.", adminEmail);
        }
    }
}
