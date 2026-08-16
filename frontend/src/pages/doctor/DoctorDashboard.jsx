import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  Stack,
  CircularProgress,
  Alert,
  Switch,
  Chip,
  Paper,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  TextField,
} from '@mui/material';
import {
  CalendarMonth as CalendarIcon,
  Schedule as ScheduleIcon,
  FolderCopy as TemplateIcon,
  ArrowForward as ArrowForwardIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
  Warning as WarningIcon,
  Info as InfoIcon,
  Edit as EditIcon,
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { doctorService } from '../../api/doctorService';
import dayjs from 'dayjs';

const DoctorDashboard = () => {
  const { user, entidadId } = useAuth();
  const [doctorInfo, setDoctorInfo] = useState(null);
  const [turnosHoy, setTurnosHoy] = useState([]);

  // Visibilidad General
  const [disponibleParaTurnos, setDisponibleParaTurnos] = useState(true);

  // Advertencia 1: Bloqueante (Impide Reserva Web)
  const [tieneBloqueante, setTieneBloqueante] = useState(false);
  const [mensajeBloqueante, setMensajeBloqueante] = useState('');

  // Advertencia 2: Informativa (Permite Continuar Reserva Web)
  const [tieneInformativa, setTieneInformativa] = useState(false);
  const [mensajeInformativo, setMensajeInformativo] = useState('');

  // Estados de Carga
  const [loading, setLoading] = useState(true);
  const [toggling, setToggling] = useState(false);
  const [savingBloqueante, setSavingBloqueante] = useState(false);
  const [savingInformativa, setSavingInformativa] = useState(false);

  // Estados de Modal de Edición de Mensajes
  const [openModalType, setOpenModalType] = useState(null); // 'BLOQUEANTE' | 'INFORMATIVA' | null
  const [tempMensaje, setTempMensaje] = useState('');

  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    if (entidadId) {
      cargarDatos();
    } else {
      setLoading(false);
    }
  }, [entidadId]);

  const cargarDatos = async () => {
    setLoading(true);
    try {
      const hoyStr = dayjs().format('YYYY-MM-DD');
      const [docData, turnosData] = await Promise.all([
        doctorService.obtenerPorId(entidadId).catch(() => null),
        doctorService.obtenerAgenda(entidadId, hoyStr).catch(() => []),
      ]);
      if (docData) {
        setDoctorInfo(docData);
        setDisponibleParaTurnos(docData.disponibleParaTurnos !== false);
        setTieneBloqueante(docData.tieneAdvertenciaBloqueante === true);
        setMensajeBloqueante(docData.mensajeAdvertenciaBloqueante || '');
        setTieneInformativa(docData.tieneAdvertenciaInformativa === true);
        setMensajeInformativo(docData.mensajeAdvertenciaInformativa || '');
      }
      if (turnosData) setTurnosHoy(turnosData);
    } catch (err) {
      setError('No se pudo cargar la agenda del día.');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleDisponibilidad = async (e) => {
    const nuevoEstado = e.target.checked;
    setToggling(true);
    setError('');
    setSuccess('');
    try {
      const docActualizado = await doctorService.cambiarDisponibilidadTurnos(entidadId, nuevoEstado);
      setDisponibleParaTurnos(docActualizado.disponibleParaTurnos !== false);
      setSuccess(
        nuevoEstado
          ? '¡Tu perfil ahora está VISIBLE públicamente para la reserva de pacientes!'
          : '¡Perfil OCULTO! Ningún paciente podrá ver tu perfil ni reservar turnos.'
      );
    } catch (err) {
      setError('No se pudo cambiar la visibilidad de turnos.');
    } finally {
      setToggling(false);
    }
  };

  // HANDLERS ADVERTENCIA 1: BLOQUEANTE
  const handleToggleBloqueante = async (e) => {
    const nuevoEstado = e.target.checked;
    if (nuevoEstado && !mensajeBloqueante.trim()) {
      setTempMensaje(mensajeBloqueante || '');
      setOpenModalType('BLOQUEANTE');
      return;
    }

    setSavingBloqueante(true);
    setError('');
    setSuccess('');
    try {
      const doc = await doctorService.configurarAdvertenciaBloqueante(entidadId, nuevoEstado, mensajeBloqueante);
      setTieneBloqueante(doc.tieneAdvertenciaBloqueante === true);
      setSuccess(
        nuevoEstado
          ? '¡Advertencia Bloqueante ACTIVADA! Se impedirá la reserva web desplegando tu mensaje.'
          : '¡Advertencia Bloqueante DESACTIVADA!'
      );
    } catch (err) {
      setError('Error al cambiar advertencia bloqueante.');
    } finally {
      setSavingBloqueante(false);
    }
  };

  // HANDLERS ADVERTENCIA 2: INFORMATIVA
  const handleToggleInformativa = async (e) => {
    const nuevoEstado = e.target.checked;
    if (nuevoEstado && !mensajeInformativo.trim()) {
      setTempMensaje(mensajeInformativo || '');
      setOpenModalType('INFORMATIVA');
      return;
    }

    setSavingInformativa(true);
    setError('');
    setSuccess('');
    try {
      const doc = await doctorService.configurarAdvertenciaInformativa(entidadId, nuevoEstado, mensajeInformativo);
      setTieneInformativa(doc.tieneAdvertenciaInformativa === true);
      setSuccess(
        nuevoEstado
          ? '¡Advertencia Informativa ACTIVADA! Se desplegará el mensaje informativo antes de avanzar.'
          : '¡Advertencia Informativa DESACTIVADA!'
      );
    } catch (err) {
      setError('Error al cambiar advertencia informativa.');
    } finally {
      setSavingInformativa(false);
    }
  };

  const handleOpenEditModal = (type) => {
    setOpenModalType(type);
    setTempMensaje(type === 'BLOQUEANTE' ? mensajeBloqueante : mensajeInformativo);
  };

  const handleGuardarMensajeModal = async () => {
    if (!tempMensaje.trim()) {
      setError('Por favor redactá el mensaje antes de guardar.');
      return;
    }

    setError('');
    setSuccess('');

    if (openModalType === 'BLOQUEANTE') {
      setSavingBloqueante(true);
      try {
        const doc = await doctorService.configurarAdvertenciaBloqueante(entidadId, true, tempMensaje.trim());
        setTieneBloqueante(true);
        setMensajeBloqueante(doc.mensajeAdvertenciaBloqueante || tempMensaje.trim());
        setOpenModalType(null);
        setSuccess('¡Advertencia Bloqueante guardada y ACTIVADA exitosamente!');
      } catch (err) {
        setError('Error al guardar mensaje bloqueante.');
      } finally {
        setSavingBloqueante(false);
      }
    } else if (openModalType === 'INFORMATIVA') {
      setSavingInformativa(true);
      try {
        const doc = await doctorService.configurarAdvertenciaInformativa(entidadId, true, tempMensaje.trim());
        setTieneInformativa(true);
        setMensajeInformativo(doc.mensajeAdvertenciaInformativa || tempMensaje.trim());
        setOpenModalType(null);
        setSuccess('¡Advertencia Informativa guardada y ACTIVADA exitosamente!');
      } catch (err) {
        setError('Error al guardar mensaje informativo.');
      } finally {
        setSavingInformativa(false);
      }
    }
  };

  const nombreDoctor = doctorInfo
    ? `${doctorInfo.nombre} ${doctorInfo.apellido}`
    : user?.email?.split('@')[0] || '';

  return (
    <Box
      sx={{
        minHeight: '75vh',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        width: '100%',
        py: 3,
      }}
    >
      <Box maxWidth="480px" width="100%" textAlign="center">
        <Box mb={3}>
          <Typography variant="h4" fontWeight={700} color="primary" gutterBottom>
            Portal Médico — Dr/a. {nombreDoctor}
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Seleccioná lo que querés hacer hoy:
          </Typography>
        </Box>

        {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
        {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

        {/* Resumen Diario */}
        <Card sx={{ mb: 3, bgcolor: 'primary.light', borderColor: 'primary.main', borderRadius: 3 }}>
          <CardContent sx={{ py: 2 }}>
            <Typography variant="subtitle2" color="primary.dark" fontWeight={700}>
              📊 FECHA: {dayjs().format('DD/MM/YYYY')}
            </Typography>
            <Typography variant="h6" fontWeight={700} color="primary.dark" mt={0.5}>
              {loading ? <CircularProgress size={20} /> : `${turnosHoy.length} Pacientes Agendados para Hoy`}
            </Typography>
          </CardContent>
        </Card>

        {/* 1. Botones Principales de Navegación */}
        <Stack spacing={2.5}>
          <Button
            variant="contained"
            color="primary"
            size="large"
            fullWidth
            onClick={() => navigate('/doctor/agenda')}
            startIcon={<CalendarIcon sx={{ fontSize: 36 }} />}
            endIcon={<ArrowForwardIcon />}
            sx={{
              py: 2.8,
              px: 3.5,
              justifyContent: 'space-between',
              fontSize: '1.15rem',
              fontWeight: 700,
              borderRadius: 3,
              boxShadow: '0 4px 14px rgba(2, 132, 199, 0.25)',
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Ver y Gestionar Mi Agenda del Día
              </Typography>
            </Box>
          </Button>

          <Button
            variant="contained"
            color="primary"
            size="large"
            fullWidth
            onClick={() => navigate('/doctor/horarios')}
            startIcon={<ScheduleIcon sx={{ fontSize: 36 }} />}
            endIcon={<ArrowForwardIcon />}
            sx={{
              py: 2.8,
              px: 3.5,
              justifyContent: 'space-between',
              fontSize: '1.15rem',
              fontWeight: 700,
              borderRadius: 3,
              boxShadow: '0 4px 14px rgba(2, 132, 199, 0.25)',
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Configurar Mis Horarios de Atención
              </Typography>
            </Box>
          </Button>

          <Button
            variant="contained"
            color="primary"
            size="large"
            fullWidth
            onClick={() => navigate('/doctor/plantillas')}
            startIcon={<TemplateIcon sx={{ fontSize: 36 }} />}
            endIcon={<ArrowForwardIcon />}
            sx={{
              py: 2.8,
              px: 3.5,
              justifyContent: 'space-between',
              fontSize: '1.15rem',
              fontWeight: 700,
              borderRadius: 3,
              boxShadow: '0 4px 14px rgba(2, 132, 199, 0.25)',
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Ver Mis Plantillas de Agenda
              </Typography>
            </Box>
          </Button>
        </Stack>

        {/* 2. Lista de Controles de Visibilidad y Advertencias (Ubicados un poco más abajo, con Borde Azul y Centro Claro) */}
        <Stack spacing={2} sx={{ mt: 3.5 }}>
          {/* OPCIÓN 1: Switch de Visibilidad Pública */}
          <Paper
            elevation={0}
            sx={{
              p: 2.5,
              px: 3,
              borderRadius: 3,
              border: '2px solid #0284c7',
              bgcolor: '#e0f2fe',
              boxShadow: '0 4px 12px rgba(2, 132, 199, 0.12)',
              transition: '0.2s',
            }}
          >
            <Box display="flex" justifyContent="space-between" alignItems="center" gap={2}>
              <Box textAlign="left">
                <Box display="flex" alignItems="center" gap={1} mb={0.5}>
                  {disponibleParaTurnos ? (
                    <VisibilityIcon sx={{ color: '#0284c7', fontSize: 24 }} />
                  ) : (
                    <VisibilityOffIcon sx={{ color: '#d97706', fontSize: 24 }} />
                  )}
                  <Typography variant="subtitle1" fontWeight={700} color="#0369a1">
                    Visibilidad para Pacientes
                  </Typography>
                </Box>
                <Chip
                  label={disponibleParaTurnos ? '🟢 PERFIL VISIBLE' : '🟡 PERFIL OCULTO'}
                  size="small"
                  sx={{
                    fontWeight: 700,
                    mb: 0.5,
                    fontSize: '0.72rem',
                    bgcolor: disponibleParaTurnos ? '#0284c7' : '#fef08a',
                    color: disponibleParaTurnos ? '#ffffff' : '#854d0e',
                  }}
                />
                <Typography variant="caption" sx={{ color: '#0369a1', opacity: 0.9, display: 'block' }}>
                  {disponibleParaTurnos
                    ? 'Tus turnos se muestran a los pacientes para reserva.'
                    : 'Ningún paciente podrá verte ni agendar.'}
                </Typography>
              </Box>

              <Box display="flex" alignItems="center">
                {toggling ? (
                  <CircularProgress size={24} color="primary" />
                ) : (
                  <Switch
                    checked={disponibleParaTurnos}
                    onChange={handleToggleDisponibilidad}
                    color="primary"
                  />
                )}
              </Box>
            </Box>
          </Paper>

          {/* OPCIÓN 2: Advertencia Bloqueante (No Permite Reserva Web) */}
          <Paper
            elevation={0}
            sx={{
              p: 2.5,
              px: 3,
              borderRadius: 3,
              border: '2px solid #0284c7',
              bgcolor: '#e0f2fe',
              boxShadow: '0 4px 12px rgba(2, 132, 199, 0.12)',
              transition: '0.2s',
            }}
          >
            <Box display="flex" justifyContent="space-between" alignItems="center" gap={2}>
              <Box textAlign="left">
                <Box display="flex" alignItems="center" gap={1} mb={0.5}>
                  <WarningIcon sx={{ color: tieneBloqueante ? '#d97706' : '#0284c7', fontSize: 24 }} />
                  <Typography variant="subtitle1" fontWeight={700} color="#0369a1">
                    Advertencia Bloqueante
                  </Typography>
                </Box>
                <Chip
                  label={tieneBloqueante ? '⚠️ BLOQUEO WEB ACTIVO' : '⚪ DESACTIVADA'}
                  size="small"
                  sx={{
                    fontWeight: 700,
                    mb: 0.5,
                    fontSize: '0.72rem',
                    bgcolor: tieneBloqueante ? '#fef08a' : 'rgba(2, 132, 199, 0.15)',
                    color: tieneBloqueante ? '#854d0e' : '#0369a1',
                  }}
                />
                <Typography variant="caption" sx={{ color: '#0369a1', opacity: 0.9, display: 'block' }}>
                  Muestra la advertencia y no permite avanzar al paciente con la reserva.
                </Typography>
                <Button
                  size="small"
                  onClick={() => handleOpenEditModal('BLOQUEANTE')}
                  startIcon={<EditIcon />}
                  sx={{
                    mt: 1,
                    fontWeight: 700,
                    textTransform: 'none',
                    borderColor: '#0284c7',
                    color: '#0284c7',
                    bgcolor: '#ffffff',
                    '&:hover': { bgcolor: '#f0f9ff' },
                  }}
                  variant="outlined"
                >
                  {mensajeBloqueante ? 'Editar Mensaje Bloqueante' : 'Redactar Mensaje Bloqueante'}
                </Button>
              </Box>

              <Box display="flex" alignItems="center">
                {savingBloqueante ? (
                  <CircularProgress size={24} color="primary" />
                ) : (
                  <Switch
                    checked={tieneBloqueante}
                    onChange={handleToggleBloqueante}
                    color="primary"
                  />
                )}
              </Box>
            </Box>
          </Paper>

          {/* OPCIÓN 3: Advertencia Informativa (Permite Continuar Reserva Web) */}
          <Paper
            elevation={0}
            sx={{
              p: 2.5,
              px: 3,
              borderRadius: 3,
              border: '2px solid #0284c7',
              bgcolor: '#e0f2fe',
              boxShadow: '0 4px 12px rgba(2, 132, 199, 0.12)',
              transition: '0.2s',
            }}
          >
            <Box display="flex" justifyContent="space-between" alignItems="center" gap={2}>
              <Box textAlign="left">
                <Box display="flex" alignItems="center" gap={1} mb={0.5}>
                  <InfoIcon sx={{ color: '#0284c7', fontSize: 24 }} />
                  <Typography variant="subtitle1" fontWeight={700} color="#0369a1">
                    Advertencia Informativa
                  </Typography>
                </Box>
                <Chip
                  label={tieneInformativa ? 'ℹ️ AVISO INFORMATIVO ACTIVO' : '⚪ SIN AVISO'}
                  size="small"
                  sx={{
                    fontWeight: 700,
                    mb: 0.5,
                    fontSize: '0.72rem',
                    bgcolor: tieneInformativa ? '#0284c7' : 'rgba(2, 132, 199, 0.15)',
                    color: tieneInformativa ? '#ffffff' : '#0369a1',
                  }}
                />
                <Typography variant="caption" sx={{ color: '#0369a1', opacity: 0.9, display: 'block' }}>
                  Muestra el cartel informativo y permite continuar con la reserva.
                </Typography>
                <Button
                  size="small"
                  onClick={() => handleOpenEditModal('INFORMATIVA')}
                  startIcon={<EditIcon />}
                  sx={{
                    mt: 1,
                    fontWeight: 700,
                    textTransform: 'none',
                    borderColor: '#0284c7',
                    color: '#0284c7',
                    bgcolor: '#ffffff',
                    '&:hover': { bgcolor: '#f0f9ff' },
                  }}
                  variant="outlined"
                >
                  {mensajeInformativo ? 'Editar Mensaje Informativo' : 'Redactar Mensaje Informativo'}
                </Button>
              </Box>

              <Box display="flex" alignItems="center">
                {savingInformativa ? (
                  <CircularProgress size={24} color="primary" />
                ) : (
                  <Switch
                    checked={tieneInformativa}
                    onChange={handleToggleInformativa}
                    color="primary"
                  />
                )}
              </Box>
            </Box>
          </Paper>
        </Stack>
      </Box>

      {/* Modal Redactar / Editar Mensaje (Bloqueante o Informativo) */}
      <Dialog open={!!openModalType} onClose={() => setOpenModalType(null)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: 1 }}>
          {openModalType === 'BLOQUEANTE' ? <WarningIcon color="warning" /> : <InfoIcon color="info" />}
          {openModalType === 'BLOQUEANTE'
            ? 'Configurar Advertencia Bloqueante'
            : 'Configurar Advertencia Informativa'}
        </DialogTitle>
        <DialogContent dividers>
          <DialogContentText sx={{ mb: 2 }}>
            {openModalType === 'BLOQUEANTE' ? (
              <>
                Redactá las instrucciones para los pacientes. Al seleccionarte en la web, <strong>se desplegará este cartel y se les impedirá avanzar</strong> a la selección de fechas/horarios.
              </>
            ) : (
              <>
                Redactá las indicaciones para los pacientes. Al seleccionarte, <strong>se desplegará este aviso</strong>.
              </>
            )}
          </DialogContentText>

          <TextField
            fullWidth
            multiline
            rows={4}
            label={openModalType === 'BLOQUEANTE' ? 'Mensaje Bloqueante' : 'Mensaje Informativo'}
            value={tempMensaje}
            onChange={(e) => setTempMensaje(e.target.value)}
            placeholder={
              openModalType === 'BLOQUEANTE'
                ? 'Ej: Estimado paciente, los turnos con el Dr. Pérez se solicitan únicamente por WhatsApp al 11-XXXX-XXXX. No se reservan por este sitio.'
                : 'Ej: Estimado paciente, los turnos relacionados a prácticas solo se reservarán presencial.'
            }
            required
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setOpenModalType(null)}>Cancelar</Button>
          <Button
            variant="contained"
            color="primary"
            onClick={handleGuardarMensajeModal}
            disabled={savingBloqueante || savingInformativa || !tempMensaje.trim()}
          >
            {(savingBloqueante || savingInformativa) ? <CircularProgress size={20} color="inherit" /> : 'Guardar y Activar'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DoctorDashboard;
