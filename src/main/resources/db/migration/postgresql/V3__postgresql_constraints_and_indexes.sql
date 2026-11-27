-- PostgreSQL-specific constraints and partial unique index previously executed imperatively in DataInitializer

ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check;
ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check CHECK (rol IN ('ADMIN', 'DOCTOR', 'PACIENTE', 'SECRETARIA'));

ALTER TABLE pacientes ALTER COLUMN dni DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_turnos_doctor_fecha_activos
    ON turnos (doctor_id, fecha_hora)
    WHERE estado != 'CANCELADO';
