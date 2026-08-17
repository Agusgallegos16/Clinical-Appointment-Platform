package com.consultorio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Monitoreo Público", description = "Endpoints de verificación de estado y keep-alive")
public class PingController {

    @GetMapping("/ping")
    @Operation(summary = "Verificación de estado (keep-alive)", description = "Devuelve 200 OK y estado UP sin requerir autenticación ni consultar la base de datos.")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
