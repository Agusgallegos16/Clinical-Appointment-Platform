package com.consultorio.controller;

import com.consultorio.dto.UsuarioAdminDTO;
import com.consultorio.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Gestión de Usuarios (Admin)", description = "Endpoints para búsqueda, bloqueo y eliminación de usuarios (Exclusivo ADMIN).")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    @Autowired
    public AdminUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Buscar usuarios por email o listar todos (Exclusivo ADMIN)")
    public ResponseEntity<List<UsuarioAdminDTO>> buscarUsuarios(@RequestParam(value = "email", required = false) String email) {
        List<UsuarioAdminDTO> resultados = usuarioService.buscarUsuariosPorEmail(email);
        return ResponseEntity.ok(resultados);
    }

    @PatchMapping("/{id}/bloquear")
    @Operation(summary = "Bloquear o desbloquear a un usuario por ID (Exclusivo ADMIN)")
    public ResponseEntity<UsuarioAdminDTO> cambiarEstadoBloqueo(@PathVariable Long id,
                                                                 @RequestParam boolean bloquear,
                                                                 Authentication authentication) {
        String emailAdminActual = authentication.getName();
        UsuarioAdminDTO usuarioActualizado = usuarioService.cambiarEstadoBloqueo(id, bloquear, emailAdminActual);
        return ResponseEntity.ok(usuarioActualizado);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar definitivamente a un usuario de la base de datos (Exclusivo ADMIN)")
    public ResponseEntity<Map<String, String>> eliminarUsuario(@PathVariable Long id,
                                                                Authentication authentication) {
        String emailAdminActual = authentication.getName();
        usuarioService.eliminarUsuario(id, emailAdminActual);
        return ResponseEntity.ok(Map.of("message", "Usuario eliminado exitosamente de la base de datos."));
    }

    @PostMapping("/registro")
    @Operation(summary = "Registrar un nuevo usuario (Doctor, Secretaria o Paciente) sin contraseña (Exclusivo ADMIN)")
    public ResponseEntity<UsuarioAdminDTO> registrarUsuario(@jakarta.validation.Valid @RequestBody com.consultorio.dto.RegistroUsuarioAdminDTO dto) {
        UsuarioAdminDTO creado = usuarioService.registrarUsuarioPorAdmin(dto);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(creado);
    }
}
