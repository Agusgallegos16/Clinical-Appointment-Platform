package com.consultorio.config;

import com.consultorio.domain.*;
import com.consultorio.dto.*;
import com.consultorio.repository.*;
import com.consultorio.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final EspecialidadRepository especialidadRepository;
    private final DoctorService doctorService;
    private final PacienteService pacienteService;
    private final PlantillaAgendaService plantillaAgendaService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(EspecialidadRepository especialidadRepository,
                           DoctorService doctorService,
                           PacienteService pacienteService,
                           PlantillaAgendaService plantillaAgendaService,
                           UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder) {
        this.especialidadRepository = especialidadRepository;
        this.doctorService = doctorService;
        this.pacienteService = pacienteService;
        this.plantillaAgendaService = plantillaAgendaService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 0. Crear Usuario Administrador de Prueba
        if (!usuarioRepository.existsByEmail("admin@consultorio.com")) {
            Usuario admin = Usuario.builder()
                    .email("admin@consultorio.com")
                    .password(passwordEncoder.encode("123456"))
                    .rol(Rol.ADMIN)
                    .activo(true)
                    .build();
            usuarioRepository.save(admin);
        }

        // 1. Crear Especialidades iniciales
        Especialidad cardiologia = especialidadRepository.save(Especialidad.builder()
                .nombre("Cardiología")
                .descripcion("Atención y diagnóstico del sistema cardiovascular")
                .build());

        Especialidad pediatria = especialidadRepository.save(Especialidad.builder()
                .nombre("Pediatría")
                .descripcion("Atención médica para niños y adolescentes")
                .build());

        Especialidad dermatologia = especialidadRepository.save(Especialidad.builder()
                .nombre("Dermatología")
                .descripcion("Cuidado y tratamiento de la piel")
                .build());

        // 2. Registrar Doctor de Prueba
        RegistroDoctorDTO doctorDTO = new RegistroDoctorDTO();
        doctorDTO.setEmail("doctor.perez@consultorio.com");
        doctorDTO.setPassword("123456");
        doctorDTO.setNombre("Juan");
        doctorDTO.setApellido("Pérez");
        doctorDTO.setMatricula("MP-99481");
        doctorDTO.setEspecialidadIds(List.of(cardiologia.getId(), pediatria.getId()));

        Doctor doctor = doctorService.registrarDoctor(doctorDTO);

        // 3. Configurar Horarios Estándar Semanales del Doctor (Lunes de 09:00 a 13:00)
        HorarioAtencionDTO horarioLunes = new HorarioAtencionDTO();
        horarioLunes.setDiaSemana(DiaSemana.LUNES);
        horarioLunes.setHoraInicio(LocalTime.of(9, 0));
        horarioLunes.setHoraFin(LocalTime.of(13, 0));
        horarioLunes.setDuracionTurnoMinutos(30);

        doctorService.agregarHorarioAtencion(doctor.getId(), horarioLunes);

        // 4. Crear una Plantilla reusable para el Doctor ("Día de Prácticas")
        CrearPlantillaDTO plantillaDTO = new CrearPlantillaDTO();
        plantillaDTO.setNombre("Jornada de Prácticas");
        plantillaDTO.setDescripcion("Turnos cortos a la mañana y extendidos a la tarde");

        DetallePlantillaDTO franjaMañana = new DetallePlantillaDTO();
        franjaMañana.setHoraInicio(LocalTime.of(8, 0));
        franjaMañana.setHoraFin(LocalTime.of(12, 0));
        franjaMañana.setDuracionTurnoMinutos(15);

        DetallePlantillaDTO franjaTarde = new DetallePlantillaDTO();
        franjaTarde.setHoraInicio(LocalTime.of(14, 0));
        franjaTarde.setHoraFin(LocalTime.of(18, 0));
        franjaTarde.setDuracionTurnoMinutos(45);

        plantillaDTO.setDetalles(List.of(franjaMañana, franjaTarde));
        plantillaAgendaService.crearPlantilla(doctor.getId(), plantillaDTO);

        // 5. Registrar Paciente de Prueba
        RegistroPacienteDTO pacienteDTO = new RegistroPacienteDTO();
        pacienteDTO.setEmail("paciente.gomez@gmail.com");
        pacienteDTO.setPassword("123456");
        pacienteDTO.setNombre("María");
        pacienteDTO.setApellido("Gómez");
        pacienteDTO.setDni(38491029L);
        pacienteDTO.setTelefono("+5491122334455");

        pacienteService.registrarPaciente(pacienteDTO);

        System.out.println("✅ Datos de prueba iniciales (Admin, Doctor y Paciente) cargados correctamente en H2 Database.");
    }
}
