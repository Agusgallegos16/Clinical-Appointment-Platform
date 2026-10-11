package com.consultorio.controller;

import com.consultorio.dto.RegistroUsuarioAdminDTO;
import com.consultorio.dto.UsuarioAdminDTO;
import com.consultorio.service.AdminUsuarioFacade;
import com.consultorio.service.UsuarioPruebaService;
import com.consultorio.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Gestión de Usuarios (Admin)", description = "Endpoints para búsqueda, bloqueo y eliminación de usuarios.")
public class AdminUsuarioController {

    private final AdminUsuarioFacade adminUsuarioFacade;
    private final UsuarioService usuarioService;
    private final UsuarioPruebaService usuarioPruebaService;

    @Autowired
    public AdminUsuarioController(AdminUsuarioFacade adminUsuarioFacade,
                                  UsuarioService usuarioService,
                                  UsuarioPruebaService usuarioPruebaService) {
        this.adminUsuarioFacade = adminUsuarioFacade;
        this.usuarioService = usuarioService;
        this.usuarioPruebaService = usuarioPruebaService;
    }

    @GetMapping
    @Operation(summary = "Buscar usuarios paginados por nombre, apellido o email")
    public ResponseEntity<Page<UsuarioAdminDTO>> buscarUsuarios(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size) {
        String termino = (query != null && !query.isBlank()) ? query : email;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("email").ignoreCase()));
        Page<UsuarioAdminDTO> resultados = adminUsuarioFacade.buscarUsuariosPaginados(termino, pageable);
        return ResponseEntity.ok(resultados);
    }

    @PatchMapping("/{id}/bloquear")
    @Operation(summary = "Bloquear o desbloquear a un usuario por ID")
    public ResponseEntity<UsuarioAdminDTO> cambiarEstadoBloqueo(@PathVariable Long id,
                                                                 @RequestParam boolean bloquear,
                                                                 Authentication authentication) {
        String emailAdminActual = authentication.getName();
        UsuarioAdminDTO usuarioActualizado = usuarioService.cambiarEstadoBloqueo(id, bloquear, emailAdminActual);
        return ResponseEntity.ok(usuarioActualizado);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar definitivamente a un usuario de la base de datos")
    public ResponseEntity<Map<String, String>> eliminarUsuario(@PathVariable Long id,
                                                                Authentication authentication) {
        String emailAdminActual = authentication.getName();
        usuarioService.eliminarUsuario(id, emailAdminActual);
        return ResponseEntity.ok(Map.of("message", "Usuario eliminado exitosamente de la base de datos."));
    }

    @PostMapping("/registro")
    @Operation(summary = "Registrar un nuevo usuario sin contraseña")
    public ResponseEntity<UsuarioAdminDTO> registrarUsuario(@Valid @RequestBody RegistroUsuarioAdminDTO dto) {
        UsuarioAdminDTO creado = adminUsuarioFacade.registrarUsuarioPorAdmin(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PostMapping("/seed-prueba")
    @Operation(summary = "Poblar temporalmente la base de datos con 40 usuarios de prueba para verificar paginación")
    public ResponseEntity<Map<String, Object>> poblarUsuariosPrueba() {
        int creados = usuarioPruebaService.poblarUsuariosDePrueba();
        return ResponseEntity.ok(Map.of(
                "message", "Se han generado " + creados + " usuarios de prueba en la base de datos.",
                "totalCreados", creados
        ));
    }
}
