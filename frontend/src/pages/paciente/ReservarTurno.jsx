import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Stepper,
  Step,
  StepLabel,
  Button,
  Grid,
  Chip,
  TextField,
  Alert,
  CircularProgress,
  Divider,
  Paper,
  Avatar,
  Radio,
} from '@mui/material';
import {
  MedicalServices as SpecialtyIcon,
  Person as DoctorIcon,
  Event as DateIcon,
  AccessTime as TimeIcon,
  CheckCircle as CheckIcon,
  CalendarMonth as CalendarIcon,
  ChildCare as ChildIcon,
  Person as PersonIcon,
  ArrowForward as ArrowForwardIcon,
} from '@mui/icons-material';
import { especialidadService } from '../../api/especialidadService';
import { doctorService } from '../../api/doctorService';
import { turnoService } from '../../api/turnoService';
import { pacienteService } from '../../api/pacienteService';
import { pacienteMenorService } from '../../api/pacienteMenorService';
import { useAuth } from '../../context/AuthContext';
import dayjs from 'dayjs';

const ReservarTurno = () => {
  const { entidadId } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const queryPacienteId = searchParams.get('pacienteId') ? Number(searchParams.get('pacienteId')) : null;
  const queryNombre = searchParams.get('nombre');

  const todayStr = dayjs().format('YYYY-MM-DD');

  // Pacientes (Titular + Menores)
  const [titular, setTitular] = useState(null);
  const [menores, setMenores] = useState([]);
  const [selectedPaciente, setSelectedPaciente] = useState(null);
  const [loadingPacientes, setLoadingPacientes] = useState(true);

  // Pasos de navegación
  const [activeStep, setActiveStep] = useState(0);

  // Estados de datos médicos
  const [especialidades, setEspecialidades] = useState([]);
  const [doctores, setDoctores] = useState([]);
  const [slotsDisponibles, setSlotsDisponibles] = useState([]);

  // Selecciones del usuario
  const [selectedEspecialidad, setSelectedEspecialidad] = useState(null);
  const [selectedDoctor, setSelectedDoctor] = useState(null);
  const [selectedFecha, setSelectedFecha] = useState(todayStr);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [motivoConsulta, setMotivoConsulta] = useState('');

  // Estados de interfaz
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (entidadId) {
      cargarDatosIniciales();
    }
  }, [entidadId]);

  const cargarDatosIniciales = async () => {
    setLoadingPacientes(true);
    try {
      const [titularData, menoresData, especialidadesData] = await Promise.all([
        pacienteService.obtenerPorId(entidadId).catch(() => null),
        pacienteMenorService.listarMenores().catch(() => []),
        especialidadService.listarTodas().catch(() => []),
      ]);

      const titularObj = titularData
        ? { id: titularData.id, nombre: `${titularData.nombre} ${titularData.apellido}`, esMenor: false }
        : { id: entidadId, nombre: 'Titular', esMenor: false };

      setTitular(titularObj);
      setMenores(menoresData);
      setEspecialidades(especialidadesData);

      // Si viene query parameter o si no hay menores, seleccionar paciente por defecto
      if (queryPacienteId) {
        const menorMatch = menoresData.find((m) => m.id === queryPacienteId);
        if (menorMatch) {
          setSelectedPaciente({ id: menorMatch.id, nombre: `${menorMatch.nombre} ${menorMatch.apellido}`, esMenor: true });
        } else {
          setSelectedPaciente(titularObj);
        }
      } else {
        setSelectedPaciente(titularObj);
      }
    } catch (err) {
      setError('Error al cargar la información inicial.');
    } finally {
      setLoadingPacientes(false);
    }
  };

  const tieneMenores = menores.length > 0;
  const steps = tieneMenores
    ? ['¿Para quién?', 'Especialidad', 'Doctor', 'Fecha y Horario Libre', 'Confirmar Reserva']
    : ['Especialidad', 'Doctor', 'Fecha y Horario Libre', 'Confirmar Reserva'];

  const pasoEspecialidadIndex = tieneMenores ? 1 : 0;
  const pasoDoctorIndex = tieneMenores ? 2 : 1;
  const pasoHorarioIndex = tieneMenores ? 3 : 2;
  const pasoConfirmarIndex = tieneMenores ? 4 : 3;

  const handleSelectEspecialidad = async (especialidad) => {
    setSelectedEspecialidad(especialidad);
    setLoading(true);
    setError('');
    try {
      const data = await doctorService.listarDoctores(especialidad.id);
      setDoctores(data);
      setActiveStep(pasoDoctorIndex);
    } catch (err) {
      setError('Error al obtener la lista de médicos para la especialidad seleccionada.');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectDoctor = async (doctor) => {
    setSelectedDoctor(doctor);
    setActiveStep(pasoHorarioIndex);
    cargarDisponibilidad(doctor.id, selectedFecha, selectedEspecialidad?.id);
  };

  const cargarDisponibilidad = async (doctorId, fechaStr, especialidadId) => {
    setLoading(true);
    setError('');
    setSelectedSlot(null);
    try {
      const targetEspId = especialidadId || selectedEspecialidad?.id;
      const data = await doctorService.obtenerDisponibilidad(doctorId, fechaStr, targetEspId);

      const isToday = fechaStr === todayStr;

      const slotsConFechaHora = data
        .filter((s) => s.disponible)
        .map((s) => {
          const horaFormateada = typeof s.hora === 'string'
            ? (s.hora.length === 5 ? `${s.hora}:00` : s.hora)
            : `${String(s.hora[0]).padStart(2, '0')}:${String(s.hora[1]).padStart(2, '0')}:00`;

          const slotDateTime = dayjs(`${fechaStr}T${horaFormateada}`);

          return {
            ...s,
            horaTexto: typeof s.hora === 'string' ? s.hora.substring(0, 5) : `${String(s.hora[0]).padStart(2, '0')}:${String(s.hora[1]).padStart(2, '0')}`,
            fechaHoraIso: `${fechaStr}T${horaFormateada}`,
            pasado: isToday && slotDateTime.isBefore(dayjs()),
          };
        })
        .filter((s) => !s.pasado);

      setSlotsDisponibles(slotsConFechaHora);
    } catch (err) {
      setError('Error al consultar horarios disponibles para la fecha elegida.');
    } finally {
      setLoading(false);
    }
  };

  const handleFechaChange = (e) => {
    const nuevaFecha = e.target.value;
    setSelectedFecha(nuevaFecha);
    if (selectedDoctor) {
      cargarDisponibilidad(selectedDoctor.id, nuevaFecha, selectedEspecialidad?.id);
    }
  };

  const handleConfirmarReserva = async () => {
    if (!selectedSlot || !selectedPaciente) return;
    setLoading(true);
    setError('');

    try {
      await turnoService.reservar({
        pacienteId: selectedPaciente.id,
        doctorId: selectedDoctor.id,
        especialidadId: selectedEspecialidad.id,
        fechaHora: selectedSlot.fechaHoraIso,
        motivoConsulta: motivoConsulta || 'Consulta General',
      });

      setSuccess(true);
      setTimeout(() => {
        if (selectedPaciente.esMenor) {
          navigate(`/paciente/turnos?pacienteId=${selectedPaciente.id}&nombre=${encodeURIComponent(selectedPaciente.nombre)}`);
        } else {
          navigate('/paciente/turnos');
        }
      }, 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'Error al reservar el turno seleccionado.');
    } finally {
      setLoading(false);
    }
  };

  if (loadingPacientes) {
    return (
      <Box display="flex" justifyContent="center" py={6}>
        <CircularProgress size={44} />
      </Box>
    );
  }

  return (
    <Box sx={{ width: { xs: '100%', md: '900px' }, mx: 'auto', boxSizing: 'border-box' }}>
      <Typography variant="h4" fontWeight={700} color="primary" mb={1}>
        Reservar Turno Médico
      </Typography>
      <Typography variant="body1" color="text.secondary" mb={3}>
        Completá los pasos para agendar la cita médica.
      </Typography>

      <Stepper activeStep={activeStep} alternativeLabel sx={{ mb: 4, width: '100%' }}>
        {steps.map((label) => (
          <Step key={label}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }}>¡Turno reservado exitosamente! Redirigiendo a tus turnos...</Alert>}

      {/* PASO 0 (Opcional): ¿Para quién es el turno? (Solo si posee menores a cargo) */}
      {tieneMenores && activeStep === 0 && (
        <Box sx={{ width: '100%', minHeight: '380px' }}>
          <Typography variant="h6" fontWeight={700} mb={1}>
            ¿Para quién es el turno que estás agendando?
          </Typography>
          <Typography variant="body2" color="text.secondary" mb={3}>
            Seleccioná si el turno es para vos o para algún menor.
          </Typography>

          <Grid container spacing={2.5} mb={4}>
            {/* Opción 1: Paciente Titular */}
            <Grid item xs={12} sm={6}>
              <Card
                onClick={() => setSelectedPaciente(titular)}
                sx={{
                  cursor: 'pointer',
                  border: selectedPaciente?.id === titular?.id ? '2px solid #0284c7' : '1px solid #e2e8f0',
                  bgcolor: selectedPaciente?.id === titular?.id ? 'action.selected' : 'background.paper',
                  borderRadius: 3,
                  transition: '0.2s',
                  '&:hover': { borderColor: 'primary.main', transform: 'translateY(-2px)' },
                }}
              >
                <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Radio checked={selectedPaciente?.id === titular?.id} color="primary" />
                  <Avatar sx={{ bgcolor: 'primary.main', width: 48, height: 48 }}>
                    <PersonIcon />
                  </Avatar>
                  <Box>
                    <Typography variant="subtitle1" fontWeight={700}>
                      Para mí
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {titular?.nombre} (Titular)
                    </Typography>
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            {/* Opciones Menores a Cargo */}
            {menores.map((menor) => {
              const menorObj = { id: menor.id, nombre: `${menor.nombre} ${menor.apellido}`, esMenor: true };
              const isSelected = selectedPaciente?.id === menor.id;
              return (
                <Grid item xs={12} sm={6} key={menor.id}>
                  <Card
                    onClick={() => setSelectedPaciente(menorObj)}
                    sx={{
                      cursor: 'pointer',
                      border: isSelected ? '2px solid #0284c7' : '1px solid #e2e8f0',
                      bgcolor: isSelected ? 'action.selected' : 'background.paper',
                      borderRadius: 3,
                      transition: '0.2s',
                      '&:hover': { borderColor: 'primary.main', transform: 'translateY(-2px)' },
                    }}
                  >
                    <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                      <Radio checked={isSelected} color="primary" />
                      <Avatar sx={{ bgcolor: 'info.main', width: 48, height: 48 }}>
                        <ChildIcon />
                      </Avatar>
                      <Box>
                        <Typography variant="subtitle1" fontWeight={700}>
                          Para {menor.nombre} {menor.apellido}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          {menor.edad} años — DNI: {menor.dni}
                        </Typography>
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              );
            })}
          </Grid>

          <Box display="flex" justifyContent="flex-end">
            <Button
              variant="contained"
              size="large"
              disabled={!selectedPaciente}
              onClick={() => setActiveStep(pasoEspecialidadIndex)}
              endIcon={<ArrowForwardIcon />}
              sx={{ borderRadius: 3, px: 4, fontWeight: 700 }}
            >
              Continuar a Especialidades
            </Button>
          </Box>
        </Box>
      )}

      {/* PASO ESPECIALIDAD */}
      {activeStep === pasoEspecialidadIndex && (
        <Box sx={{ width: '100%', minHeight: '380px' }}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6" fontWeight={600}>
              Seleccioná una Especialidad Médica
            </Typography>
            {tieneMenores && (
              <Chip
                label={`Cita para: ${selectedPaciente?.nombre}`}
                color={selectedPaciente?.esMenor ? 'info' : 'primary'}
                variant="outlined"
                sx={{ fontWeight: 600 }}
              />
            )}
          </Box>
          <Grid container spacing={2}>
            {especialidades.map((esp) => (
              <Grid item xs={12} sm={6} md={4} key={esp.id}>
                <Card
                  onClick={() => handleSelectEspecialidad(esp)}
                  sx={{
                    width: '100%',
                    height: '100%',
                    cursor: 'pointer',
                    transition: '0.2s',
                    '&:hover': { borderColor: 'primary.main', transform: 'translateY(-2px)' },
                  }}
                >
                  <CardContent>
                    <Box display="flex" alignItems="center" gap={1.5} mb={1}>
                      <SpecialtyIcon color="primary" />
                      <Typography variant="subtitle1" fontWeight={700}>
                        {esp.nombre}
                      </Typography>
                    </Box>
                    <Typography variant="body2" color="text.secondary">
                      {esp.descripcion}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>

          {tieneMenores && (
            <Box mt={3}>
              <Button variant="outlined" onClick={() => setActiveStep(0)}>
                Volver a Elección de Paciente
              </Button>
            </Box>
          )}
        </Box>
      )}

      {/* PASO DOCTOR */}
      {activeStep === pasoDoctorIndex && (
        <Box sx={{ width: '100%', minHeight: '380px', boxSizing: 'border-box' }}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6" fontWeight={600}>
              Seleccioná un Médico en {selectedEspecialidad?.nombre}
            </Typography>
            <Button variant="outlined" size="small" onClick={() => setActiveStep(pasoEspecialidadIndex)}>
              Cambiar Especialidad
            </Button>
          </Box>

          {loading ? (
            <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
          ) : doctores.length === 0 ? (
            <Alert severity="info">No hay médicos registrados actualmente para esta especialidad.</Alert>
          ) : (
            <Grid container spacing={2}>
              {doctores.map((doc) => (
                <Grid item xs={12} sm={6} md={4} key={doc.id}>
                  <Card
                    onClick={() => handleSelectDoctor(doc)}
                    sx={{
                      width: '100%',
                      height: '100%',
                      cursor: 'pointer',
                      transition: '0.2s',
                      '&:hover': { borderColor: 'primary.main', transform: 'translateY(-2px)' },
                    }}
                  >
                    <CardContent>
                      <Box display="flex" alignItems="center" gap={2} mb={1.5}>
                        <Avatar
                          src={doc.fotoUrl}
                          alt={doc.nombre}
                          sx={{ width: 54, height: 54 }}
                          imgProps={{ style: { objectFit: 'cover', imageRendering: '-webkit-optimize-contrast' } }}
                        >
                          {doc.nombre?.charAt(0)}
                        </Avatar>
                        <Box>
                          <Typography variant="subtitle1" fontWeight={700}>
                            Dr/a. {doc.nombre} {doc.apellido}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {selectedEspecialidad?.nombre}
                          </Typography>
                        </Box>
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          )}
        </Box>
      )}

      {/* PASO HORARIO */}
      {activeStep === pasoHorarioIndex && (
        <Box sx={{ width: '100%', minHeight: '380px', boxSizing: 'border-box' }}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6" fontWeight={600}>
              Dr/a. {selectedDoctor?.nombre} {selectedDoctor?.apellido}
            </Typography>
            <Button variant="outlined" size="small" onClick={() => setActiveStep(pasoDoctorIndex)}>
              Cambiar Médico
            </Button>
          </Box>

          <Grid container spacing={2} mb={3}>
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                fullWidth
                label="Seleccionar Fecha"
                type="date"
                value={selectedFecha}
                onChange={handleFechaChange}
                InputLabelProps={{ shrink: true }}
                inputProps={{ min: todayStr }}
              />
            </Grid>
          </Grid>

          {loading ? (
            <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
          ) : slotsDisponibles.length === 0 ? (
            <Alert severity="warning">No hay horarios libres disponibles para la fecha seleccionada.</Alert>
          ) : (
            <Box mb={3}>
              <Typography variant="subtitle2" color="text.secondary" mb={1}>
                Horarios Libres Disponibles:
              </Typography>
              <Grid container spacing={1.5}>
                {slotsDisponibles.map((slot) => (
                  <Grid item key={slot.horaTexto}>
                    <Chip
                      icon={<TimeIcon />}
                      label={`${slot.horaTexto} hs`}
                      clickable
                      color={selectedSlot?.horaTexto === slot.horaTexto ? 'primary' : 'default'}
                      variant={selectedSlot?.horaTexto === slot.horaTexto ? 'filled' : 'outlined'}
                      onClick={() => setSelectedSlot(slot)}
                      sx={{ fontSize: '0.95rem', py: 2, px: 1 }}
                    />
                  </Grid>
                ))}
              </Grid>
            </Box>
          )}

          <Box display="flex" gap={2}>
            <Button variant="outlined" onClick={() => setActiveStep(pasoDoctorIndex)}>
              Volver
            </Button>
            <Button
              variant="contained"
              color="primary"
              disabled={!selectedSlot}
              onClick={() => setActiveStep(pasoConfirmarIndex)}
            >
              Continuar a Confirmación
            </Button>
          </Box>
        </Box>
      )}

      {/* PASO CONFIRMACIÓN */}
      {activeStep === pasoConfirmarIndex && (
        <Box sx={{ width: '100%', minHeight: '380px', boxSizing: 'border-box' }}>
          <Typography variant="h6" fontWeight={600} mb={2}>
            Resumen y Confirmación de Cita
          </Typography>

          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="subtitle1" fontWeight={700} color="primary" gutterBottom>
              Especialidad: {selectedEspecialidad?.nombre}
            </Typography>
            <Divider sx={{ my: 1.5 }} />
            <Typography variant="body1" mb={1}>
              👤 <strong>Paciente:</strong> {selectedPaciente?.nombre}{' '}
              {selectedPaciente?.esMenor && <Chip label="Menor a cargo" size="small" color="info" sx={{ ml: 1, fontWeight: 600 }} />}
            </Typography>
            <Typography variant="body1" mb={1}>
              👨‍⚕️ <strong>Profesional:</strong> Dr/a. {selectedDoctor?.nombre} {selectedDoctor?.apellido}
            </Typography>
            <Typography variant="body1" mb={1}>
              📅 <strong>Fecha:</strong> {dayjs(selectedFecha).format('DD/MM/YYYY')}
            </Typography>
            <Typography variant="body1" mb={2}>
              ⏰ <strong>Hora:</strong> {selectedSlot?.horaTexto} hs
            </Typography>

            <TextField
              fullWidth
              multiline
              rows={3}
              label="Motivo de la Consulta / Comentarios para el médico (Opcional)"
              value={motivoConsulta}
              onChange={(e) => setMotivoConsulta(e.target.value)}
              placeholder="Ej: Chequeo de rutina, el turno es para un menor..."
            />
          </Paper>

          <Box display="flex" gap={2}>
            <Button variant="outlined" onClick={() => setActiveStep(pasoHorarioIndex)}>
              Volver
            </Button>
            <Button
              variant="contained"
              color="success"
              size="large"
              disabled={loading || success}
              onClick={handleConfirmarReserva}
              startIcon={<CheckIcon />}
            >
              {loading ? <CircularProgress size={24} color="inherit" /> : 'Confirmar Reserva'}
            </Button>
          </Box>
        </Box>
      )}
    </Box>
  );
};

export default ReservarTurno;
