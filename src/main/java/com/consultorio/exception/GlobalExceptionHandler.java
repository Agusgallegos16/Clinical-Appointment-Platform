package com.consultorio.exception;

import com.consultorio.dto.ApiErrorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage())
        );

        ApiErrorDTO errorDTO = ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Error de validación")
                .mensaje("La solicitud contiene campos inválidos.")
                .detalles(errores)
                .build();

        return ResponseEntity.badRequest().body(errorDTO);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorDTO> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Petición inválida: {}", ex.getMessage());

        ApiErrorDTO errorDTO = ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Petición inválida")
                .mensaje(ex.getMessage())
                .build();

        return ResponseEntity.badRequest().body(errorDTO);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorDTO> handleIllegalState(IllegalStateException ex) {
        log.warn("Conflicto en solicitud: {}", ex.getMessage());

        ApiErrorDTO errorDTO = ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflicto en la solicitud")
                .mensaje(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDTO);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDTO> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String errorMsg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        String mensaje = "Error de integridad de datos. Verifique que los campos ingresados sean válidos y no dupliquen datos existentes.";
        if (errorMsg.contains("delete") || errorMsg.contains("foreign key")) {
            mensaje = "No se puede eliminar el registro porque posee elementos asociados (doctores o turnos activos).";
        }

        log.warn("Restricción de integridad: {}", mensaje);

        ApiErrorDTO errorDTO = ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Restricción de integridad")
                .mensaje(mensaje)
                .build();

        return ResponseEntity.badRequest().body(errorDTO);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorDTO> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Intento de autenticación con credenciales inválidas.");

        ApiErrorDTO errorDTO = ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("No autorizado")
                .mensaje("Credenciales inválidas. Email o contraseña incorrectos.")
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorDTO);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDTO> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acceso denegado a recurso restringido.");

        ApiErrorDTO errorDTO = ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Acceso denegado")
                .mensaje("No tiene los permisos suficientes para acceder a este recurso.")
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDTO);
    }

    @ExceptionHandler({
            com.google.api.client.googleapis.json.GoogleJsonResponseException.class,
            com.google.api.client.auth.oauth2.TokenResponseException.class
    })
    public ResponseEntity<ApiErrorDTO> handleGoogleApiExceptions(Exception ex) {
        log.warn("⚠️ Excepción de Google API capturada defensivamente: {}", ex.getMessage());

        ApiErrorDTO errorDTO = ApiErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error("Error de Integración externa (Google)")
                .mensaje("Ocurrió un inconveniente al comunicarse con Google Calendar. Su sesión en la aplicación permanece activa.")
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorDTO);
    }
}
