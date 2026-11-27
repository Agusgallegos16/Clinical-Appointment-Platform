package com.consultorio.controller;

import com.consultorio.service.SupabaseStorageService;
import com.consultorio.service.SupabaseStorageService.StorageUploadResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/storage")
@Tag(name = "Almacenamiento de Imágenes", description = "Endpoints autenticados para subida y borrado seguro de imágenes raster en Supabase Storage.")
public class StorageController {

    private final SupabaseStorageService storageService;

    @Autowired
    public StorageController(SupabaseStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(value = "/doctores/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Subir avatar de un médico validando magic bytes raster (DOCTOR / ADMIN)")
    public ResponseEntity<StorageUploadResult> subirAvatarDoctor(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "doctorId", required = false) String doctorId) {
        StorageUploadResult result = storageService.uploadDoctorAvatar(file, doctorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping(value = "/institucion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Subir imagen de la galería institucional validando magic bytes raster (Exclusivo ADMIN)")
    public ResponseEntity<StorageUploadResult> subirImagenInstitucion(
            @RequestPart("file") MultipartFile file) {
        StorageUploadResult result = storageService.uploadInstitucionImage(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/institucion/{fileName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar una imagen de la galería institucional (Exclusivo ADMIN)")
    public ResponseEntity<Void> eliminarImagenInstitucion(@PathVariable String fileName) {
        storageService.deleteInstitucionImage(fileName);
        return ResponseEntity.noContent().build();
    }
}
