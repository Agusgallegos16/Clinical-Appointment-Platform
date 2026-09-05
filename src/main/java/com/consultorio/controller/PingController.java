package com.consultorio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Monitoreo Público", description = "Endpoints de verificación de estado y keep-alive")
public class PingController {

    @RequestMapping(value = "/ping", method = {RequestMethod.GET, RequestMethod.HEAD})
    @Operation(summary = "Verificación de estado (keep-alive)", description = "Devuelve 200 OK sin requerir autenticación ni consultar la base de datos.")
    public ResponseEntity<Void> ping() {
        return ResponseEntity.ok().build();
    }
}
