package com.consultorio.service;

import com.consultorio.domain.ConfiguracionSistema;
import com.consultorio.dto.ActualizarNotificacionesConfigDTO;
import com.consultorio.dto.NotificacionesConfigDTO;
import com.consultorio.repository.ConfiguracionSistemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ConfiguracionSistemaService {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionSistemaService.class);

    private final ConfiguracionSistemaRepository configuracionSistemaRepository;

    @Autowired
    public ConfiguracionSistemaService(ConfiguracionSistemaRepository configuracionSistemaRepository) {
        this.configuracionSistemaRepository = configuracionSistemaRepository;
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

        config.setFechaModificacion(LocalDateTime.now());
        config.setModificadoPor(usuarioEmail != null ? usuarioEmail : "ADMIN");

        ConfiguracionSistema guardada = configuracionSistemaRepository.save(config);
        log.info("⚙️ Configuración global de resúmenes actualizada por {}: diario={}, semanal={}",
                usuarioEmail, guardada.isResumenDiarioMedicosHabilitado(), guardada.isResumenSemanalMedicosHabilitado());

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
