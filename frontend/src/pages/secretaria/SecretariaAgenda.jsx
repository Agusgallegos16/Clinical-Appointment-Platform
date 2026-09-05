import React, { useEffect, useState, useMemo } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  TextField,
  Chip,
  Button,
  CircularProgress,
  Alert,
  Paper,
  Tabs,
  Tab,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  Avatar,
  Stepper,
  Step,
  StepLabel,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  InputAdornment,
} from '@mui/material';
import {
  PersonAdd as PersonAddIcon,
  Cancel as CancelIcon,
  Search as SearchIcon,
  CalendarMonth as CalendarIcon,
  MedicalServices as MedicalIcon,
  ArrowBack as ArrowBackIcon,
  RestartAlt as RestartIcon,
  CheckCircle as CheckIcon,
  FilterList as FilterListIcon,
  Block as BlockIcon,
} from '@mui/icons-material';
import dayjs from 'dayjs';
import axiosClient from '../../api/axiosClient';
import { doctorService } from '../../api/doctorService';
import ModalAgendarSecretaria from '../../components/secretaria/ModalAgendarSecretaria';

const SecretariaAgenda = () => {

  const [activeStep, setActiveStep] = useState(0);

  const [doctores, setDoctores] = useState([]);
  const [especialidades, setEspecialidades] = useState([]);
  const [searchDoctorQuery, setSearchDoctorQuery] = useState('');
  const [filterEspecialidadId, setFilterEspecialidadId] = useState('');

  const [selectedDoctor, setSelectedDoctor] = useState(null);
  const [fecha, setFecha] = useState(dayjs().format('YYYY-MM-DD'));
  const [selectedEspecialidadFiltroAgenda, setSelectedEspecialidadFiltroAgenda] = useState('');

  const [tabFiltro, setTabFiltro] = useState(0); // 0: Todos, 1: Reservados, 2: Libres
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [agendaTurnos, setAgendaTurnos] = useState([]);
  const [slotsDisponibles, setSlotsDisponibles] = useState([]);

  const [modalAgendarOpen, setModalAgendarOpen] = useState(false);
  const [selectedSlotParaAgendar, setSelectedSlotParaAgendar] = useState(null);

  const [cancelDialogOpen, setCancelDialogOpen] = useState(false);
  const [turnoACancelar, setTurnoACancelar] = useState(null);
  const [motivoCancelacion, setMotivoCancelacion] = useState('');
  const [submittingCancel, setSubmittingCancel] = useState(false);

  useEffect(() => {
    cargarDoctores();
    cargarEspecialidades();
  }, []);

  const cargarDoctores = async () => {
    setLoading(true);
    try {
      const res = await axiosClient.get('/doctores');
      setDoctores(res.data || []);
    } catch (err) {
      console.error('Error al cargar médicos:', err);
      setError('No se pudo cargar la nómina de médicos.');
    } finally {
      setLoading(false);
    }
  };

  const cargarEspecialidades = async () => {
    try {
      const res = await axiosClient.get('/especialidades');
      setEspecialidades(res.data || []);
    } catch (err) {
      console.error('Error al cargar especialidades:', err);
    }
  };

  const handleSelectDoctor = (doc) => {
    setSelectedDoctor(doc);
    setSelectedEspecialidadFiltroAgenda('');
    setActiveStep(1);
  };

  const handleConfirmarFecha = () => {
    if (!fecha) {
      setError('Por favor seleccioná una fecha válida.');
      return;
    }
    setActiveStep(2);
    cargarAgendaYDisponibilidad(selectedDoctor.id, fecha);
  };

  const cargarAgendaYDisponibilidad = async (doctorId, fechaSel, espFiltro = '') => {
    setLoading(true);
    setError('');
    try {
      const dispUrl = `/doctores/${doctorId}/disponibilidad?fecha=${fechaSel}${espFiltro ? `&especialidadId=${espFiltro}` : ''}`;
      const [agendaRes, dispRes] = await Promise.all([
        axiosClient.get(`/doctores/${doctorId}/agenda?fecha=${fechaSel}`),
        axiosClient.get(dispUrl),
      ]);

      setAgendaTurnos(agendaRes.data || []);
      setSlotsDisponibles(dispRes.data || []);
    } catch (err) {
      console.error('Error al cargar agenda y disponibilidad:', err);
      setError('Error al consultar la agenda del médico.');
    } finally {
      setLoading(false);
    }
  };

  const handleCambiarFiltroEspecialidadAgenda = (espId) => {
    setSelectedEspecialidadFiltroAgenda(espId);
    if (selectedDoctor && fecha) {
      cargarAgendaYDisponibilidad(selectedDoctor.id, fecha, espId);
    }
  };

  const handleDeshabilitarSlot = async (slotId) => {
    if (!slotId) return;
    try {
      await doctorService.eliminarSlot(slotId);
      cargarAgendaYDisponibilidad(selectedDoctor.id, fecha, selectedEspecialidadFiltroAgenda);
    } catch (err) {
      console.error('Error al deshabilitar slot:', err);
      setError('No se pudo deshabilitar el horario libre.');
    }
  };

  const handleOpenAgendarModal = (item) => {
    const espId = item.especialidadId || (selectedEspecialidadFiltroAgenda ? Number(selectedEspecialidadFiltroAgenda) : selectedDoctor?.especialidades?.[0]?.id);
    const espObj = selectedDoctor?.especialidades?.find((e) => e.id === Number(espId)) || selectedDoctor?.especialidades?.[0];

    setSelectedSlotParaAgendar({
      fechaHora: item.fechaHora,
      especialidadId: espObj ? espObj.id : espId,
      especialidadNombre: espObj ? espObj.nombre : (item.especialidadNombre || 'Consulta General'),
    });
    setModalAgendarOpen(true);
  };

  const handleCloseAgendarModal = () => {
    setModalAgendarOpen(false);
    setSelectedSlotParaAgendar(null);
  };

  const handleOpenCancelDialog = (turno) => {
    setTurnoACancelar(turno);
    setMotivoCancelacion('');
    setCancelDialogOpen(true);
  };

  const handleCloseCancelDialog = () => {
    setCancelDialogOpen(false);
    setTurnoACancelar(null);
    setMotivoCancelacion('');
  };

  const handleConfirmarCancelacion = async () => {
    if (!turnoACancelar) return;
    if (!motivoCancelacion.trim()) {
      setError('Debe ingresar un motivo obligatorio para cancelar el turno.');
      return;
    }
    setSubmittingCancel(true);
    try {
      await axiosClient.put(
        `/turnos/${turnoACancelar.id}/cancelar-doctor?motivo=${encodeURIComponent(motivoCancelacion.trim())}`
      );
      handleCloseCancelDialog();
      cargarAgendaYDisponibilidad(selectedDoctor.id, fecha, selectedEspecialidadFiltroAgenda);
    } catch (err) {
      console.error('Error al cancelar turno:', err);
      setError(err.response?.data?.message || 'No se pudo cancelar el turno.');
    } finally {
      setSubmittingCancel(false);
    }
  };

  const getChipColor = (estado) => {
    switch (estado) {
      case 'COMPLETADO': return 'success';
      case 'AUSENTE': return 'warning';
      case 'CANCELADO': return 'error';
      default: return 'primary';
    }
  };

  const getBorderColor = (item) => {
    if (item.tipo === 'LIBRE') return '#22c55e';
    const estado = item.datos?.estado;
    switch (estado) {
      case 'COMPLETADO': return '#22c55e';
      case 'AUSENTE': return '#f59e0b';
      case 'CANCELADO': return '#ef4444';
      default: return '#0284c7';
    }
  };

  // Filtrado de médicos para el Paso 1
  const doctoresFiltrados = useMemo(() => {
    return doctores.filter((doc) => {
      const nombreCompleto = `${doc.nombre} ${doc.apellido}`.toLowerCase();
      const coincideNombre = nombreCompleto.includes(searchDoctorQuery.toLowerCase());
      const coincideEspecialidad = !filterEspecialidadId || doc.especialidades?.some((e) => e.id === Number(filterEspecialidadId));
      return coincideNombre && coincideEspecialidad;
    });
  }, [doctores, searchDoctorQuery, filterEspecialidadId]);

  const turnosReservadosActivos = useMemo(() => {
    return Array.isArray(agendaTurnos) ? agendaTurnos.filter((t) => t && t.estado !== 'CANCELADO') : [];
  }, [agendaTurnos]);

  const slotsLibresActivos = useMemo(() => {
    return Array.isArray(slotsDisponibles) ? slotsDisponibles.filter((s) => s && s.disponible !== false) : [];
  }, [slotsDisponibles]);

  // Combinar todos los turnos reservados y slots libres en una lista ordenada por hora
  const listaTodos = useMemo(() => {
    const lista = [];

    // Añadir turnos reservados
    if (Array.isArray(agendaTurnos)) {
      agendaTurnos.forEach((t) => {
        if (t && t.estado !== 'CANCELADO') {
          lista.push({
            tipo: 'RESERVADO',
            hora: dayjs(t.fechaHora).format('HH:mm'),
            fechaHora: t.fechaHora,
            especialidadId: t.especialidadId,
            especialidadNombre: t.especialidadNombre,
            datos: t,
          });
        }
      });
    }

    // Añadir slots libres que verdaderamente estén disponibles
    if (Array.isArray(slotsDisponibles)) {
      slotsDisponibles.forEach((s) => {
        if (!s || s.disponible === false) return;

        const horaStr = typeof s.hora === 'string' ? s.hora : String(s.hora);
        const horaCorta = horaStr.substring(0, 5);
        const fullIsoFechaHora = `${fecha}T${horaStr.length === 5 ? horaStr + ':00' : horaStr}`;

        lista.push({
          tipo: 'LIBRE',
          hora: horaCorta,
          fechaHora: fullIsoFechaHora,
          especialidadId: s.especialidadId,
          especialidadNombre: s.especialidadNombre,
          datos: s,
        });
      });
    }

    // Ordenar cronológicamente
    lista.sort((a, b) => dayjs(a.fechaHora).valueOf() - dayjs(b.fechaHora).valueOf());
    return lista;
  }, [agendaTurnos, slotsDisponibles, fecha]);

  const listaFinal = useMemo(() => {
    let listaFiltrada = listaTodos;

    // Filtrar por especialidad en el cliente en caso de que esté seleccionada
    if (selectedEspecialidadFiltroAgenda) {
      const espFiltroId = Number(selectedEspecialidadFiltroAgenda);
      listaFiltrada = listaFiltrada.filter((item) => {
        return !item.especialidadId || item.especialidadId === espFiltroId;
      });
    }

    // Filtrar según la pestaña de estado seleccionada
    if (tabFiltro === 1) {
      return listaFiltrada.filter((item) => item.tipo === 'RESERVADO');
    }
    if (tabFiltro === 2) {
      return listaFiltrada.filter((item) => item.tipo === 'LIBRE');
    }

    return listaFiltrada;
  }, [listaTodos, selectedEspecialidadFiltroAgenda, tabFiltro]);
  const especialidadPrincipal = selectedDoctor?.especialidades?.[0];

  const steps = ['Seleccionar Médico', 'Seleccionar Día', 'Agendar'];

  return (
    <Box maxWidth="1000px" mx="auto">
      {/* Header */}
      <Box mb={3}>
        <Typography variant="h4" fontWeight={700} color="primary" mb={0.5}>
          Revisar Agenda Médica 🗓️
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Consultar disponibilidad y reservar turnos a pacientes.
        </Typography>
      </Box>

      {/* Indicador Stepper de Pasos */}
      <Paper sx={{ p: 2.5, mb: 4, borderRadius: 3 }}>
        <Stepper activeStep={activeStep} alternativeLabel>
          {steps.map((label, index) => (
            <Step key={label} completed={activeStep > index}>
              <StepLabel
                onClick={() => {
                  if (index < activeStep) setActiveStep(index);
                }}
                sx={{ cursor: index < activeStep ? 'pointer' : 'default' }}
              >
                <Typography fontWeight={activeStep === index ? 700 : 500}>
                  {label}
                </Typography>
              </StepLabel>
            </Step>
          ))}
        </Stepper>
      </Paper>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {/* =================================================================== */}
      {/* PASO 0: SELECCIONAR MÉDICO DE LA NÓMINA                             */}
      {/* =================================================================== */}
      {activeStep === 0 && (
        <Box>
          <Paper sx={{ p: 2.5, mb: 3, borderRadius: 3 }}>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  placeholder="Buscar médico por nombre o apellido..."
                  value={searchDoctorQuery}
                  onChange={(e) => setSearchDoctorQuery(e.target.value)}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <SearchIcon color="action" />
                      </InputAdornment>
                    ),
                  }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControl fullWidth>
                  <InputLabel id="filter-esp-label">Filtrar por Especialidad</InputLabel>
                  <Select
                    labelId="filter-esp-label"
                    value={filterEspecialidadId}
                    label="Filtrar por Especialidad"
                    onChange={(e) => setFilterEspecialidadId(e.target.value)}
                  >
                    <MenuItem value="">Todas las Especialidades</MenuItem>
                    {especialidades.map((esp) => (
                      <MenuItem key={esp.id} value={esp.id}>
                        {esp.nombre}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
            </Grid>
          </Paper>

          {loading ? (
            <Box display="flex" justifyContent="center" py={6}><CircularProgress /></Box>
          ) : doctoresFiltrados.length === 0 ? (
            <Alert severity="info">No se encontraron médicos que coincidan con la búsqueda.</Alert>
          ) : (
            <Grid container spacing={2.5}>
              {doctoresFiltrados.map((doc) => (
                <Grid item xs={12} sm={6} md={4} key={doc.id}>
                  <Card
                    sx={{
                      height: '100%',
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'space-between',
                      borderRadius: 3,
                      transition: '0.2s',
                      '&:hover': {
                        transform: 'translateY(-3px)',
                        boxShadow: '0 6px 18px rgba(2, 132, 199, 0.2)',
                      },
                    }}
                  >
                    <CardContent>
                      <Box display="flex" alignItems="center" gap={2} mb={2}>
                        <Avatar
                          src={doc.fotoUrl}
                          alt={`${doc.nombre} ${doc.apellido}`}
                          sx={{ width: 56, height: 56, bgcolor: 'primary.main', fontWeight: 700, fontSize: '1.4rem' }}
                        >
                          {doc.nombre?.charAt(0).toUpperCase()}
                        </Avatar>
                        <Box>
                          <Typography variant="h6" fontWeight={700}>
                            Dr. {doc.nombre} {doc.apellido}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            📧 {doc.usuario?.email || 'Medicina General'}
                          </Typography>
                        </Box>
                      </Box>

                      <Typography variant="caption" fontWeight={600} display="block" mb={0.5}>
                        Especialidades:
                      </Typography>
                      <Box display="flex" flexWrap="wrap" gap={0.5}>
                        {doc.especialidades?.map((esp) => (
                          <Chip key={esp.id} label={esp.nombre} size="small" color="primary" variant="outlined" />
                        ))}
                      </Box>
                    </CardContent>

                    <Box p={2} pt={0}>
                      <Button
                        variant="contained"
                        fullWidth
                        size="medium"
                        startIcon={<CheckIcon />}
                        onClick={() => handleSelectDoctor(doc)}
                        sx={{ borderRadius: 2, fontWeight: 700 }}
                      >
                        Seleccionar Profesional
                      </Button>
                    </Box>
                  </Card>
                </Grid>
              ))}
            </Grid>
          )}
        </Box>
      )}

      {/* =================================================================== */}
      {/* PASO 1: SELECCIONAR DÍA / FECHA                                     */}
      {/* =================================================================== */}
      {activeStep === 1 && selectedDoctor && (
        <Box maxWidth="600px" mx="auto">
          <Button
            startIcon={<ArrowBackIcon />}
            onClick={() => setActiveStep(0)}
            sx={{ mb: 2, fontWeight: 600 }}
          >
            Volver a Selección de Médico
          </Button>

          <Paper sx={{ p: 4, borderRadius: 3, textAlign: 'center' }}>
            <Box display="flex" flexDirection="column" alignItems="center" gap={1.5} mb={3}>
              <Avatar
                src={selectedDoctor.fotoUrl}
                alt={`${selectedDoctor.nombre} ${selectedDoctor.apellido}`}
                sx={{ width: 80, height: 80, bgcolor: 'primary.main', fontWeight: 700, fontSize: '1.8rem' }}
              >
                {selectedDoctor.nombre?.charAt(0).toUpperCase()}
              </Avatar>
              <Box>
                <Typography variant="h5" fontWeight={700} color="primary">
                  Dr. {selectedDoctor.nombre} {selectedDoctor.apellido}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {selectedDoctor.especialidades?.map(e => e.nombre).join(', ') || 'Medicina General'}
                </Typography>
              </Box>
            </Box>

            <Typography variant="subtitle1" fontWeight={700} mb={2}>
              Seleccioná la Fecha para consultar la Agenda:
            </Typography>

            <TextField
              fullWidth
              type="date"
              label="Fecha Seleccionada"
              value={fecha}
              onChange={(e) => setFecha(e.target.value)}
              InputLabelProps={{ shrink: true }}
              sx={{ mb: 4 }}
            />

            <Button
              variant="contained"
              size="large"
              fullWidth
              startIcon={<CalendarIcon />}
              onClick={handleConfirmarFecha}
              sx={{ py: 1.8, fontWeight: 700, borderRadius: 2 }}
            >
              Ver Agenda & Turnos
            </Button>
          </Paper>
        </Box>
      )}

      {/* =================================================================== */}
      {/* PASO 2: VER AGENDA Y FILTRAR / AGENDAR                             */}
      {/* =================================================================== */}
      {activeStep === 2 && selectedDoctor && (
        <Box>
          {/* Banner Resumen del Profesional y Fecha Elegidos */}
          <Paper
            sx={{
              p: 2.5,
              mb: 3,
              borderRadius: 3,
              background: 'linear-gradient(135deg, rgba(2, 132, 199, 0.06) 0%, rgba(14, 165, 233, 0.02) 100%)',
              border: '1px solid',
              borderColor: 'primary.light',
            }}
          >
            <Grid container spacing={2} alignItems="center" justifyContent="space-between">
              <Grid item xs={12} sm={8}>
                <Box display="flex" alignItems="center" gap={2}>
                  <Avatar
                    src={selectedDoctor.fotoUrl}
                    alt={`${selectedDoctor.nombre} ${selectedDoctor.apellido}`}
                    sx={{ width: 56, height: 56, bgcolor: 'primary.main', fontWeight: 700, fontSize: '1.3rem' }}
                  >
                    {selectedDoctor.nombre?.charAt(0).toUpperCase()}
                  </Avatar>
                  <Box>
                    <Typography variant="h6" fontWeight={700} color="primary">
                      Dr. {selectedDoctor.nombre} {selectedDoctor.apellido}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      📅 Fecha: <strong>{dayjs(fecha).format('DD/MM/YYYY')}</strong> | Especialidades: {selectedDoctor.especialidades?.map(e => e.nombre).join(', ') || 'General'}
                    </Typography>
                  </Box>
                </Box>
              </Grid>

              <Grid item xs={12} sm={4} textAlign={{ xs: 'left', sm: 'right' }}>
                <Button
                  variant="outlined"
                  color="primary"
                  startIcon={<RestartIcon />}
                  onClick={() => setActiveStep(0)}
                  sx={{ borderRadius: 2, textTransform: 'none', fontWeight: 600 }}
                >
                  Regresar
                </Button>
              </Grid>
            </Grid>
          </Paper>

          {/* Barra de Filtros: Por Especialidad (si aplica) + Tabs de Estado */}
          <Paper sx={{ mb: 3, p: 1.5, borderRadius: 3 }}>
            <Grid container spacing={2} alignItems="center">
              {selectedDoctor.especialidades && selectedDoctor.especialidades.length > 1 && (
                <Grid item xs={12} sm={4}>
                  <FormControl fullWidth size="small">
                    <InputLabel id="filtro-esp-agenda-label">Filtrar por Especialidad</InputLabel>
                    <Select
                      labelId="filtro-esp-agenda-label"
                      value={selectedEspecialidadFiltroAgenda}
                      label="Filtrar por Especialidad"
                      onChange={(e) => handleCambiarFiltroEspecialidadAgenda(e.target.value)}
                    >
                      <MenuItem value="">Todas las Especialidades</MenuItem>
                      {selectedDoctor.especialidades.map((esp) => (
                        <MenuItem key={esp.id} value={esp.id}>
                          {esp.nombre}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
              )}

              <Grid item xs={12} sm={selectedDoctor.especialidades?.length > 1 ? 8 : 12}>
                <Tabs
                  value={tabFiltro}
                  onChange={(e, val) => setTabFiltro(val)}
                  indicatorColor="primary"
                  textColor="primary"
                  variant="fullWidth"
                >
                  <Tab label={`Todos (${listaTodos.length})`} />
                  <Tab label={`Reservados (${turnosReservadosActivos.length})`} />
                  <Tab label={`Libres (${slotsLibresActivos.length})`} />
                </Tabs>
              </Grid>
            </Grid>
          </Paper>

          {/* Grilla de Citas y Slots Libres */}
          {loading ? (
            <Box display="flex" justifyContent="center" py={6}><CircularProgress /></Box>
          ) : listaFinal.length === 0 ? (
            <Alert severity="info">No se encontraron turnos u horarios libres configurados para la fecha y filtros seleccionados.</Alert>
          ) : (
            <Grid container spacing={2}>
              {listaFinal.map((item, index) => {
                const esReservado = item.tipo === 'RESERVADO';
                const turno = esReservado ? item.datos : null;
                const slot = !esReservado ? item.datos : null;

                return (
                  <Grid item xs={12} key={index}>
                    <Card
                      sx={{
                        borderLeft: `6px solid ${getBorderColor(item)}`,
                        transition: '0.2s',
                        borderRadius: 2,
                        '&:hover': { boxShadow: '0 4px 14px rgba(0,0,0,0.08)' },
                      }}
                    >
                      <CardContent sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, justifyContent: 'space-between', alignItems: { sm: 'center' }, gap: 2 }}>
                        <Box>
                          <Box display="flex" alignItems="center" gap={1.5} mb={0.5}>
                            <Typography variant="h6" fontWeight={700} color="primary">
                              {item.hora} hs
                            </Typography>
                            <Chip
                              label={esReservado ? `RESERVADO: ${turno?.estado || 'CONFIRMADO'}` : 'LIBRE'}
                              color={esReservado ? getChipColor(turno?.estado) : 'success'}
                              size="small"
                              sx={{ fontWeight: 700 }}
                            />
                          </Box>

                          {esReservado ? (
                            <Box>
                              <Typography variant="subtitle1" fontWeight={700} color="text.primary">
                                👤 Paciente: {turno.pacienteNombre}
                              </Typography>
                              <Typography variant="body2" color="text.secondary">
                                🏥 Especialidad: {turno.especialidadNombre || 'General'} | 💳 Cobertura: {turno.obraSocial || (turno.tieneObraSocial ? 'Obra Social' : 'Particular')}
                              </Typography>
                              {turno.motivoConsulta && (
                                <Typography variant="body2" color="text.secondary">
                                  💬 Motivo: {turno.motivoConsulta}
                                </Typography>
                              )}
                            </Box>
                          ) : (
                            <Box>
                              <Typography variant="body2" fontWeight={600} color="text.primary">
                                🏥 Especialidad: {item.especialidadNombre || 'Consulta General (Cualquier Especialidad)'}
                              </Typography>
                            </Box>
                          )}
                        </Box>

                        <Box>
                          {esReservado ? (
                            turno?.estado !== 'COMPLETADO' && turno?.estado !== 'CANCELADO' && turno?.estado !== 'AUSENTE' && (
                              <Button
                                variant="outlined"
                                color="error"
                                size="small"
                                startIcon={<CancelIcon />}
                                onClick={() => handleOpenCancelDialog(turno)}
                              >
                                Cancelar Turno
                              </Button>
                            )
                          ) : (
                            <Box display="flex" gap={1} flexWrap="wrap">
                              <Button
                                variant="contained"
                                color="success"
                                size="small"
                                startIcon={<PersonAddIcon />}
                                onClick={() => handleOpenAgendarModal(item)}
                              >
                                Agendar
                              </Button>
                              {slot?.id && (
                                <Button
                                  variant="outlined"
                                  color="error"
                                  size="small"
                                  startIcon={<BlockIcon />}
                                  onClick={() => handleDeshabilitarSlot(slot.id)}
                                >
                                  Deshabilitar
                                </Button>
                              )}
                            </Box>
                          )}
                        </Box>
                      </CardContent>
                    </Card>
                  </Grid>
                );
              })}
            </Grid>
          )}

          {/* Modal de Agendado por Secretaría */}
          {modalAgendarOpen && selectedSlotParaAgendar && (
            <ModalAgendarSecretaria
              open={modalAgendarOpen}
              onClose={handleCloseAgendarModal}
              onSuccess={() => cargarAgendaYDisponibilidad(selectedDoctor.id, fecha, selectedEspecialidadFiltroAgenda)}
              doctor={selectedDoctor}
              especialidadId={selectedSlotParaAgendar.especialidadId}
              especialidadNombre={selectedSlotParaAgendar.especialidadNombre}
              fechaHora={selectedSlotParaAgendar.fechaHora}
            />
          )}

          {/* Dialog de Confirmación de Cancelación */}
          <Dialog open={cancelDialogOpen} onClose={handleCloseCancelDialog} maxWidth="sm" fullWidth>
            <DialogTitle fontWeight={700} color="error.main">
              Confirmar Cancelación de Turno
            </DialogTitle>
            <DialogContent dividers>
              <Typography variant="body1" mb={2}>
                ¿Estás segura/o de cancelar el turno de <strong>{turnoACancelar?.pacienteNombre}</strong> agendado para las <strong>{dayjs(turnoACancelar?.fechaHora).format('HH:mm')} hs</strong>?
              </Typography>
              <Typography variant="body2" color="text.secondary" mb={2}>
                📧 Se le enviará un correo electrónico automático de notificación al paciente indicando la cancelación y la justificación.
              </Typography>
              <TextField
                fullWidth
                multiline
                rows={3}
                label="Motivo de la Cancelación (Obligatorio)"
                value={motivoCancelacion}
                onChange={(e) => setMotivoCancelacion(e.target.value)}
                placeholder="Ej: Reprogramación por razones operativas, ausencia imprevista del médico, etc."
                required
              />
            </DialogContent>
            <DialogActions sx={{ p: 2 }}>
              <Button onClick={handleCloseCancelDialog} color="inherit" disabled={submittingCancel}>
                Volver
              </Button>
              <Button
                variant="contained"
                color="error"
                onClick={handleConfirmarCancelacion}
                disabled={submittingCancel || !motivoCancelacion.trim()}
              >
                {submittingCancel ? <CircularProgress size={20} color="inherit" /> : 'Confirmar y Enviar Email'}
              </Button>
            </DialogActions>
          </Dialog>
        </Box>
      )}
    </Box>
  );
};

export default SecretariaAgenda;
