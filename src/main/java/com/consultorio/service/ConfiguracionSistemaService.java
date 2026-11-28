package com.consultorio.service;

import com.consultorio.domain.ConfiguracionSistema;
import com.consultorio.dto.ActualizarNotificacionesConfigDTO;
import com.consultorio.dto.NotificacionesConfigDTO;
import com.consultorio.repository.ConfiguracionSistemaRepository;
import com.consultorio.security.SecurityUtils;
import com.consultorio.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class ConfiguracionSistemaService {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionSistemaService.class);

    private final ConfiguracionSistemaRepository configuracionSistemaRepository;
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;
    private final Clock clock;

    @Autowired
    public ConfiguracionSistemaService(ConfiguracionSistemaRepository configuracionSistemaRepository,
                                       AuditLogService auditLogService,
                                       SecurityUtils securityUtils,
                                       Clock clock) {
        this.configuracionSistemaRepository = configuracionSistemaRepository;
        this.auditLogService = auditLogService;
        this.securityUtils = securityUtils;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ConfiguracionSistema obtenerConfiguracion() {
        return configuracionSistemaRepository.findById(ConfiguracionSistema.ID_SINGLETON)
                .orElseGet(() -> ConfiguracionSistema.builder()
                        .id(ConfiguracionSistema.ID_SINGLETON)
                        .resumenDiarioMedicosHabilitado(true)
                        .resumenSemanalMedicosHabilitado(true)
                        .build());
    }

    @Transactional(readOnly = true)
    public NotificacionesConfigDTO obtenerNotificacionesConfigDTO() {
        ConfiguracionSistema config = obtenerConfiguracion();
        return NotificacionesConfigDTO.builder()
                .resumenDiarioMedicosHabilitado(config.isResumenDiarioMedicosHabilitado())
                .resumenSemanalMedicosHabilitado(config.isResumenSemanalMedicosHabilitado())
                .fechaModificacion(config.getFechaModificacion())
                .modificadoPor(config.getModificadoPor())
                .build();
    }

    @Transactional
    public NotificacionesConfigDTO actualizarNotificacionesConfig(ActualizarNotificacionesConfigDTO dto, String usuarioEmail) {
        if (dto == null || (dto.getResumenDiarioMedicosHabilitado() == null
                && dto.getResumenSemanalMedicosHabilitado() == null)) {
            throw new IllegalArgumentException("Debe indicar al menos una configuración para actualizar.");
        }

        ConfiguracionSistema config = configuracionSistemaRepository.findById(ConfiguracionSistema.ID_SINGLETON)
                .orElseGet(() -> ConfiguracionSistema.builder()
                        .id(ConfiguracionSistema.ID_SINGLETON)
                        .resumenDiarioMedicosHabilitado(true)
                        .resumenSemanalMedicosHabilitado(true)
                        .build());

        if (dto.getResumenDiarioMedicosHabilitado() != null) {
            config.setResumenDiarioMedicosHabilitado(dto.getResumenDiarioMedicosHabilitado());
        }

        if (dto.getResumenSemanalMedicosHabilitado() != null) {
            config.setResumenSemanalMedicosHabilitado(dto.getResumenSemanalMedicosHabilitado());
        }

        config.setFechaModificacion(LocalDateTime.now(clock));
        config.setModificadoPor(usuarioEmail != null ? usuarioEmail : "ADMIN");

        ConfiguracionSistema guardada = configuracionSistemaRepository.save(config);
        auditLogService.record(
                "NOTIFICATION_CONFIG_UPDATED",
                "SUCCESS",
                securityUtils.obtenerIdUsuarioAutenticado(),
                "ADMIN",
                "CONFIG",
                String.valueOf(ConfiguracionSistema.ID_SINGLETON),
                String.format("diario=%s semanal=%s",
                        guardada.isResumenDiarioMedicosHabilitado(),
                        guardada.isResumenSemanalMedicosHabilitado())
        );
        log.info("Configuración global de resúmenes actualizada por {}: diario={}, semanal={}",
                LogSanitizer.maskEmail(usuarioEmail), guardada.isResumenDiarioMedicosHabilitado(), guardada.isResumenSemanalMedicosHabilitado());

        return NotificacionesConfigDTO.builder()
                .resumenDiarioMedicosHabilitado(guardada.isResumenDiarioMedicosHabilitado())
                .resumenSemanalMedicosHabilitado(guardada.isResumenSemanalMedicosHabilitado())
                .fechaModificacion(guardada.getFechaModificacion())
                .modificadoPor(guardada.getModificadoPor())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean isResumenDiarioMedicosHabilitado() {
        return obtenerConfiguracion().isResumenDiarioMedicosHabilitado();
    }

    @Transactional(readOnly = true)
    public boolean isResumenSemanalMedicosHabilitado() {
        return obtenerConfiguracion().isResumenSemanalMedicosHabilitado();
    }
}
