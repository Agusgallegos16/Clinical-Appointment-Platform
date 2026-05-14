package com.serviciodegesrtiondepacientes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ServicioDeGestionDePacientesApplication {

  public static void main(String[] args) {
    SpringApplication.run(ServicioDeGestionDePacientesApplication.class, args);
  }

}
