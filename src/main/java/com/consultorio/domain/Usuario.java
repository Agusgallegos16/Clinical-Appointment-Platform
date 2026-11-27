package com.consultorio.domain;

import com.consultorio.security.EncryptedStringAttributeConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @NotBlank(message = "La contraseña es obligatoria")
    @Column(nullable = false)
    private String password;

    @NotNull(message = "El rol es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    @Builder.Default
    @Column(name = "bloqueado", columnDefinition = "boolean default false")
    private Boolean bloqueado = false;

    public boolean isBloqueado() {
        return Boolean.TRUE.equals(this.bloqueado);
    }

    @Builder.Default
    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado = false;

    @JsonIgnore
    @Column(name = "token_verificacion_email")
    private String tokenVerificacionEmail;

    @JsonIgnore
    @Column(name = "token_verificacion_expiracion")
    private LocalDateTime tokenVerificacionExpiracion;

    @JsonIgnore
    @Column(name = "token_restablecimiento_password")
    private String tokenRestablecimientoPassword;

    @JsonIgnore
    @Column(name = "token_restablecimiento_expiracion")
    private LocalDateTime tokenRestablecimientoExpiracion;

    @JsonIgnore
    @Column(name = "nueva_password_pendiente")
    private String nuevaPasswordPendiente;

    @JsonIgnore
    @Convert(converter = EncryptedStringAttributeConverter.class)
    @Column(name = "google_access_token", length = 2048)
    private String googleAccessToken;

    @JsonIgnore
    @Convert(converter = EncryptedStringAttributeConverter.class)
    @Column(name = "google_refresh_token", length = 2048)
    private String googleRefreshToken;

    @JsonIgnore
    @Column(name = "google_token_expiry")
    private Long googleTokenExpiry;

    @Builder.Default
    @Column(name = "google_calendar_connected", nullable = false)
    private boolean googleCalendarConnected = false;

    @JsonIgnore
    @Column(name = "google_id")
    private String googleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @JsonIgnore
    @Builder.Default
    @Column(name = "token_version", columnDefinition = "integer default 0")
    private Integer tokenVersion = 0;

    public int getTokenVersionOrZero() {
        return this.tokenVersion != null ? this.tokenVersion : 0;
    }

    public void incrementTokenVersion() {
        this.tokenVersion = getTokenVersionOrZero() + 1;
    }
}
