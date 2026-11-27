package com.consultorio.service;

import com.consultorio.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    public static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final Pattern SAFE_ID_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9_-]");

    private final SecurityUtils securityUtils;
    private final RestTemplate restTemplate;

    @Value("${app.supabase.url:}")
    private String supabaseUrl;

    @Value("${app.supabase.service-role-key:}")
    private String serviceRoleKey;

    @Value("${app.supabase.bucket.doctores:fotos-doctores}")
    private String doctoresBucket;

    @Value("${app.supabase.bucket.institucion:fotos-institucion}")
    private String institucionBucket;

    @Autowired
    public SupabaseStorageService(SecurityUtils securityUtils) {
        this(securityUtils, new RestTemplate());
    }

    SupabaseStorageService(SecurityUtils securityUtils, RestTemplate restTemplate) {
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public record StorageUploadResult(String url, String path) {}

    private record ValidatedImage(byte[] bytes, String extension, String mimeType) {}

    public StorageUploadResult uploadDoctorAvatar(MultipartFile file, String requestedDoctorId) {
        ValidatedImage image = validateAndDetectRasterImage(file);
        String ownerTag = resolveSafeDoctorTag(requestedDoctorId);
        String objectPath = String.format(
                "avatars/doctor_%s_%d_%s.%s",
                ownerTag,
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8),
                image.extension()
        );
        return uploadToSupabase(doctoresBucket, objectPath, image);
    }

    public StorageUploadResult uploadInstitucionImage(MultipartFile file) {
        ValidatedImage image = validateAndDetectRasterImage(file);
        String objectPath = String.format(
                "galeria_%d_%s.%s",
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8),
                image.extension()
        );
        return uploadToSupabase(institucionBucket, objectPath, image);
    }

    public void deleteInstitucionImage(String fileName) {
        validateSafeObjectName(fileName);
        String baseUrl = requireCleanSupabaseUrl();
        String key = requireServiceRoleKey();

        String deleteUrl = baseUrl + "/storage/v1/object/" + institucionBucket + "/" + fileName;
        HttpHeaders headers = buildAuthHeaders(key);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, String.class);
        } catch (RestClientResponseException ex) {
            log.warn("Supabase Storage delete failed with status {}", ex.getStatusCode().value());
            throw new IllegalStateException("Could not delete image from storage provider.");
        }
    }

    private StorageUploadResult uploadToSupabase(String bucket, String objectPath, ValidatedImage image) {
        String baseUrl = requireCleanSupabaseUrl();
        String key = requireServiceRoleKey();

        String uploadUrl = baseUrl + "/storage/v1/object/" + bucket + "/" + objectPath;
        HttpHeaders headers = buildAuthHeaders(key);
        headers.setContentType(MediaType.parseMediaType(image.mimeType()));
        headers.set("x-upsert", "true");

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(image.bytes(), headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Storage provider rejected the upload.");
            }
        } catch (RestClientResponseException ex) {
            log.warn("Supabase Storage upload failed with status {}", ex.getStatusCode().value());
            throw new IllegalStateException("Could not upload image to storage provider.");
        }

        String publicUrl = baseUrl + "/storage/v1/object/public/" + bucket + "/" + objectPath;
        return new StorageUploadResult(publicUrl, objectPath);
    }

    private ValidatedImage validateAndDetectRasterImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("An image file is required.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("The image exceeds the maximum allowed size of 5 MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String lowerName = originalFilename.toLowerCase(Locale.ROOT).trim();
            if (lowerName.endsWith(".svg") || lowerName.endsWith(".svgz")) {
                throw new IllegalArgumentException("SVG files are not allowed. Upload a valid JPEG, PNG, or WEBP image.");
            }
        }

        String declaredContentType = file.getContentType();
        if (declaredContentType != null && declaredContentType.toLowerCase(Locale.ROOT).contains("svg")) {
            throw new IllegalArgumentException("SVG files are not allowed. Upload a valid JPEG, PNG, or WEBP image.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not read uploaded image bytes.");
        }

        if (bytes.length == 0) {
            throw new IllegalArgumentException("An image file is required.");
        }
        if (bytes.length > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("The image exceeds the maximum allowed size of 5 MB.");
        }

        // Explicitly reject SVG/XML text payloads even with forged extensions or MIME headers
        String headAscii = new String(bytes, 0, Math.min(bytes.length, 64), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT)
                .trim();
        if (headAscii.contains("<svg") || headAscii.startsWith("<?xml")) {
            throw new IllegalArgumentException("SVG/XML payloads are not allowed. Upload a valid JPEG, PNG, or WEBP image.");
        }

        if (isJpeg(bytes)) {
            return new ValidatedImage(bytes, "jpg", MediaType.IMAGE_JPEG_VALUE);
        }
        if (isPng(bytes)) {
            return new ValidatedImage(bytes, "png", MediaType.IMAGE_PNG_VALUE);
        }
        if (isWebp(bytes)) {
            return new ValidatedImage(bytes, "webp", "image/webp");
        }

        throw new IllegalArgumentException("Unsupported image format. Only valid JPEG, PNG, or WEBP raster images are allowed.");
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private String resolveSafeDoctorTag(String requestedDoctorId) {
        if (!securityUtils.esAdmin()) {
            Long authUserId = securityUtils.obtenerIdUsuarioAutenticado();
            if (authUserId != null) {
                return "u" + authUserId;
            }
            return "self";
        }
        if (requestedDoctorId == null || requestedDoctorId.isBlank()) {
            return "temp";
        }
        String sanitized = SAFE_ID_CHARS_PATTERN.matcher(requestedDoctorId.trim()).replaceAll("");
        if (sanitized.isEmpty()) {
            return "temp";
        }
        return sanitized.length() > 64 ? sanitized.substring(0, 64) : sanitized;
    }

    private void validateSafeObjectName(String fileName) {
        if (fileName == null || fileName.isBlank()
                || fileName.startsWith(".")
                || fileName.contains("..")
                || !SAFE_FILENAME_PATTERN.matcher(fileName).matches()) {
            throw new IllegalArgumentException("Invalid file name.");
        }
    }

    private HttpHeaders buildAuthHeaders(String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(key);
        headers.set("apikey", key);
        return headers;
    }

    private String requireCleanSupabaseUrl() {
        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            throw new IllegalStateException("Supabase Storage URL is not configured on the server.");
        }
        String trimmed = supabaseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private String requireServiceRoleKey() {
        if (serviceRoleKey == null || serviceRoleKey.isBlank()) {
            throw new IllegalStateException("Supabase Storage service role key is not configured on the server.");
        }
        return serviceRoleKey.trim();
    }
}
