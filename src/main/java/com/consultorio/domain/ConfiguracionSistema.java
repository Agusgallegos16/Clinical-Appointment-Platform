package com.consultorio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_sistema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionSistema {

    public static final Long ID_SINGLETON = 1L;

    @Id
    private Long id;

    @Builder.Default
    @Column(name = "resumen_diario_medicos_habilitado", nullable = false)
    private Boolean resumenDiarioMedicosHabilitado = true;

    @Builder.Default
    @Column(name = "resumen_semanal_medicos_habilitado", nullable = false)
    private Boolean resumenSemanalMedicosHabilitado = true;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "modificado_por", length = 100)
    private String modificadoPor;

    public boolean isResumenDiarioMedicosHabilitado() {
        return Boolean.TRUE.equals(this.resumenDiarioMedicosHabilitado);
    }

    public boolean isResumenSemanalMedicosHabilitado() {
        return Boolean.TRUE.equals(this.resumenSemanalMedicosHabilitado);
    }
}
