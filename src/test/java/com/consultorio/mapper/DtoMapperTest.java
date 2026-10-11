package com.consultorio.mapper;

import com.consultorio.domain.*;
import com.consultorio.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DtoMapperTest {

    private DtoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DtoMapper();
    }

    @Test
    @DisplayName("Debe mapear Especialidad a EspecialidadResponseDTO")
    void testToDtoEspecialidad() {
        Especialidad esp = Especialidad.builder()
                .id(10L)
                .nombre("Cardiología")
                .descripcion("Atención del corazón")
                .build();

        EspecialidadResponseDTO dto = mapper.toDto(esp);

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("Cardiología", dto.getNombre());
        assertEquals("Atención del corazón", dto.getDescripcion());
    }

    @Test
    @DisplayName("Debe mapear Doctor a DoctorResponseDTO con relaciones planas y jerárquicas")
    void testToDtoDoctor() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .email("doctor@test.com")
                .rol(Rol.DOCTOR)
                .activo(true)
                .build();

        Especialidad esp = Especialidad.builder()
                .id(5L)
                .nombre("Pediatría")
                .build();

        UUID docId = UUID.randomUUID();
        Doctor doctor = Doctor.builder()
                .id(docId)
                .nombre("Gregory")
                .apellido("House")
                .fotoUrl("http://foto.jpg")
                .usuario(usuario)
                .especialidades(Set.of(esp))
                .disponibleParaTurnos(true)
                .tieneAdvertenciaBloqueante(false)
                .build();

        DoctorResponseDTO dto = mapper.toDto(doctor);

        assertNotNull(dto);
        assertEquals(docId, dto.getId());
        assertEquals("Gregory", dto.getNombre());
        assertEquals("House", dto.getApellido());
        assertEquals("doctor@test.com", dto.getEmail());
        assertNotNull(dto.getUsuario());
        assertEquals("doctor@test.com", dto.getUsuario().getEmail());
        assertNotNull(dto.getEspecialidades());
        assertEquals(1, dto.getEspecialidades().size());
        assertEquals("Pediatría", dto.getEspecialidades().get(0).getNombre());
    }

    @Test
    @DisplayName("Debe mapear Paciente a PacienteResponseDTO resolviendo teléfono y email fallback")
    void testToDtoPaciente() {
        Usuario usuario = Usuario.builder()
                .id(2L)
                .email("paciente@test.com")
                .rol(Rol.PACIENTE)
                .build();

        UUID pacId = UUID.randomUUID();
        Paciente paciente = Paciente.builder()
                .id(pacId)
                .nombre("Juan")
                .apellido("Pérez")
                .dni(30123456L)
                .usuario(usuario)
                .fechaNacimiento(LocalDate.of(1995, 5, 20))
                .telefono("1122334455")
                .build();

        PacienteResponseDTO dto = mapper.toDto(paciente);

        assertNotNull(dto);
        assertEquals(pacId, dto.getId());
        assertEquals("Juan", dto.getNombre());
        assertEquals("Pérez", dto.getApellido());
        assertEquals(30123456L, dto.getDni());
        assertEquals("1122334455", dto.getTelefono());
        assertEquals("paciente@test.com", dto.getEmail());
        assertNotNull(dto.getEdad());
        assertTrue(dto.getEdad() > 0);
    }

    @Test
    @DisplayName("Debe mapear PlantillaAgenda con Detalles a PlantillaAgendaResponseDTO")
    void testToDtoPlantillaAgenda() {
        Doctor doctor = Doctor.builder().id(UUID.randomUUID()).nombre("Dr").apellido("Test").build();

        PlantillaAgenda plantilla = PlantillaAgenda.builder()
                .id(1L)
                .doctor(doctor)
                .nombre("Mañana Lunes")
                .descripcion("Turnos de la mañana")
                .build();

        DetallePlantilla detalle = DetallePlantilla.builder()
                .id(100L)
                .plantilla(plantilla)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .duracionTurnoMinutos(20)
                .build();

        plantilla.setDetalles(List.of(detalle));

        PlantillaAgendaResponseDTO dto = mapper.toDto(plantilla);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Mañana Lunes", dto.getNombre());
        assertEquals(1, dto.getDetalles().size());
        assertEquals(LocalTime.of(8, 0), dto.getDetalles().get(0).getHoraInicio());
        assertEquals(20, dto.getDetalles().get(0).getDuracionTurnoMinutos());
    }
}
