import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
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
} from '@mui/material';
import {
  MedicalServices as SpecialtyIcon,
  Person as DoctorIcon,
  Event as DateIcon,
  AccessTime as TimeIcon,
  CheckCircle as CheckIcon,
  CalendarMonth as CalendarIcon,
} from '@mui/icons-material';
import { especialidadService } from '../../api/especialidadService';
import { doctorService } from '../../api/doctorService';
import { turnoService } from '../../api/turnoService';
import { useAuth } from '../../context/AuthContext';
import dayjs from 'dayjs';

const steps = ['Especialidad', 'Doctor', 'Fecha y Horario Libre', 'Confirmar Reserva'];

const ReservarTurno = () => {
  const [activeStep, setActiveStep] = useState(0);
  const { entidadId } = useAuth();
  const navigate = useNavigate();

  const todayStr = dayjs().format('YYYY-MM-DD');

  // Estados de datos
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
    cargarEspecialidades();
  }, []);

  const cargarEspecialidades = async () => {
    try {
      const data = await especialidadService.listarTodas();
      setEspecialidades(data);
    } catch (err) {
      setError('Error al cargar especialidades médicas.');
    }
  };

  const handleSelectEspecialidad = async (especialidad) => {
    setSelectedEspecialidad(especialidad);
    setLoading(true);
    setError('');
    try {
      const data = await doctorService.listarDoctores(especialidad.id);
      setDoctores(data);
      setActiveStep(1);
    } catch (err) {
      setError('Error al obtener la lista de médicos para la especialidad seleccionada.');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectDoctor = async (doctor) => {
    setSelectedDoctor(doctor);
    setActiveStep(2);
    cargarDisponibilidad(doctor.id, selectedFecha, selectedEspecialidad?.id);
  };

  const cargarDisponibilidad = async (doctorId, fechaStr, especialidadId) => {
    setLoading(true);
    setError('');
    setSelectedSlot(null);
    try {
      const targetEspId = especialidadId || selectedEspecialidad?.id;
      const data = await doctorService.obtenerDisponibilidad(doctorId, fechaStr, targetEspId);

      const now = dayjs();
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
            horaTexto: horaFormateada.substring(0, 5),
            fechaHoraIso: `${fechaStr}T${horaFormateada}`,
            slotDateTime,
          };
        })
        .filter((s) => {
          // Si la fecha elegida es el día de hoy, filtrar los slots cuyas horas ya transcurrieron
          if (isToday) {
            return s.slotDateTime.isAfter(now);
          }
          return true;
        });

      setSlotsDisponibles(slotsConFechaHora);
    } catch (err) {
      setError('Error al consultar los horarios disponibles.');
    } finally {
      setLoading(false);
    }
  };

  const handleFechaChange = (e) => {
    const nuevaFecha = e.target.value;
    // Impedir seleccionar fechas pasadas
    if (dayjs(nuevaFecha).isBefore(dayjs(todayStr), 'day')) {
      setError('No podés seleccionar una fecha anterior a la actual.');
      return;
    }
    setError('');
    setSelectedFecha(nuevaFecha);
    if (selectedDoctor) {
      cargarDisponibilidad(selectedDoctor.id, nuevaFecha, selectedEspecialidad?.id);
    }
  };

  const handleConfirmarReserva = async () => {
    if (!selectedSlot || !entidadId) return;
    setLoading(true);
    setError('');

    try {
      await turnoService.reservar({
        pacienteId: entidadId,
        doctorId: selectedDoctor.id,
        especialidadId: selectedEspecialidad.id,
        fechaHora: selectedSlot.fechaHoraIso,
        motivoConsulta: motivoConsulta || 'Consulta General',
      });

      setSuccess(true);
      setTimeout(() => {
        navigate('/paciente/turnos');
      }, 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'Error al reservar el turno seleccionado.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box maxWidth="900px" mx="auto">
      <Typography variant="h4" fontWeight={700} color="primary" mb={1}>
        Reservar Turno Médico
      </Typography>
      <Typography variant="body1" color="text.secondary" mb={4}>
        Elegí el profesional y el horario disponible para agendar tu cita.
      </Typography>

      <Stepper activeStep={activeStep} alternativeLabel sx={{ mb: 4 }}>
        {steps.map((label) => (
          <Step key={label}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }}>¡Turno reservado exitosamente! Redirigiendo a tus turnos...</Alert>}

      {/* PASO 0: Seleccionar Especialidad */}
      {activeStep === 0 && (
        <Box>
          <Typography variant="h6" fontWeight={600} mb={2}>
            Seleccioná una Especialidad Médica
          </Typography>
          <Grid container spacing={2}>
            {especialidades.map((esp) => (
              <Grid item xs={12} sm={6} md={4} key={esp.id}>
                <Card
                  onClick={() => handleSelectEspecialidad(esp)}
                  sx={{
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
        </Box>
      )}

      {/* PASO 1: Seleccionar Doctor */}
      {activeStep === 1 && (
        <Box>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6" fontWeight={600}>
              Seleccioná un Profesional ({selectedEspecialidad?.nombre})
            </Typography>
            <Button size="small" onClick={() => setActiveStep(0)}>
              Cambiar Especialidad
            </Button>
          </Box>

          {loading ? (
            <CircularProgress />
          ) : doctores.length === 0 ? (
            <Alert severity="warning">No hay doctores registrados para esta especialidad por el momento.</Alert>
          ) : (
            <Grid container spacing={2}>
              {doctores.map((doc) => (
                <Grid item xs={12} sm={6} key={doc.id}>
                  <Card
                    onClick={() => handleSelectDoctor(doc)}
                    sx={{
                      cursor: 'pointer',
                      transition: '0.2s',
                      '&:hover': { borderColor: 'primary.main', transform: 'translateY(-2px)' },
                    }}
                  >
                    <CardContent>
                      <Box display="flex" alignItems="center" gap={2}>
                        <DoctorIcon color="primary" sx={{ fontSize: 40 }} />
                        <Box>
                          <Typography variant="subtitle1" fontWeight={700}>
                            Dr/a. {doc.nombre} {doc.apellido}
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

      {/* PASO 2: Calendario Interactivo y Selección de Horario Libre por Especialidad */}
      {activeStep === 2 && (
        <Box>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6" fontWeight={600}>
              Elegí Fecha y Horario Disponible (Dr/a. {selectedDoctor?.nombre} {selectedDoctor?.apellido} — {selectedEspecialidad?.nombre})
            </Typography>
            <Button size="small" onClick={() => setActiveStep(1)}>
              Cambiar Doctor
            </Button>
          </Box>

          <Paper sx={{ p: 3, mb: 3 }}>
            <Grid container spacing={3} alignItems="center">
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Seleccionar Fecha de Atención"
                  type="date"
                  value={selectedFecha}
                  onChange={handleFechaChange}
                  inputProps={{ min: todayStr }}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  📅 Mostrando disponibilidades para <strong>{selectedEspecialidad?.nombre}</strong> el <strong>{dayjs(selectedFecha).format('DD/MM/YYYY')}</strong>
                </Typography>
              </Grid>
            </Grid>
          </Paper>

          {/* Matriz Visual de Slots Disponibles */}
          <Paper sx={{ p: 3, mb: 3 }}>
            <Box display="flex" alignItems="center" gap={1} mb={2}>
              <TimeIcon color="primary" />
              <Typography variant="subtitle1" fontWeight={700}>
                Horarios Libres Confirmados para {selectedEspecialidad?.nombre}
              </Typography>
            </Box>

            {loading ? (
              <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
            ) : slotsDisponibles.length === 0 ? (
              <Alert severity="info">El profesional no posee turnos libres de {selectedEspecialidad?.nombre} configurados para la fecha seleccionada. Probá con otra fecha.</Alert>
            ) : (
              <Grid container spacing={1.5} mb={2}>
                {slotsDisponibles.map((slot, index) => {
                  const isSelected = selectedSlot?.fechaHoraIso === slot.fechaHoraIso;
                  return (
                    <Grid item key={index}>
                      <Chip
                        label={`⏰ ${slot.horaTexto} hs`}
                        color={isSelected ? 'primary' : 'default'}
                        variant={isSelected ? 'filled' : 'outlined'}
                        onClick={() => setSelectedSlot(slot)}
                        clickable
                        sx={{
                          fontSize: '1rem',
                          py: 2.2,
                          px: 1.5,
                          fontWeight: isSelected ? 700 : 500,
                          borderWidth: isSelected ? 2 : 1,
                        }}
                      />
                    </Grid>
                  );
                })}
              </Grid>
            )}
          </Paper>

          <Button
            variant="contained"
            size="large"
            disabled={!selectedSlot}
            onClick={() => setActiveStep(3)}
          >
            Continuar a Confirmación
          </Button>
        </Box>
      )}

      {/* PASO 3: Confirmación y Motivo */}
      {activeStep === 3 && (
        <Box>
          <Typography variant="h6" fontWeight={600} mb={2}>
            Resumen y Confirmación de Cita
          </Typography>

          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="subtitle1" fontWeight={700} color="primary" gutterBottom>
              Especialidad: {selectedEspecialidad?.nombre}
            </Typography>
            <Divider sx={{ my: 1.5 }} />
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
              label="Motivo de la Consulta (Opcional)"
              value={motivoConsulta}
              onChange={(e) => setMotivoConsulta(e.target.value)}
              placeholder="Ej: Chequeo de rutina, consulta médica..."
            />
          </Paper>

          <Box display="flex" gap={2}>
            <Button variant="outlined" onClick={() => setActiveStep(2)}>
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
