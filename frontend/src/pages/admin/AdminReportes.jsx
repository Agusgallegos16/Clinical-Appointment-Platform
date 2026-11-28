import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Grid,
  TextField,
  Button,
  CircularProgress,
  Alert,
  Paper,
  Switch,
  FormControlLabel,
  Divider,
  Chip,
} from '@mui/material';
import {
  Send as SendIcon,
  Assessment as ReportIcon,
  Settings as SettingsIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import dayjs from 'dayjs';
import { adminService } from '../../api/adminService';

const AdminReportes = () => {
  const [fechaDiario, setFechaDiario] = useState('');
  const [desdeSemanal, setDesdeSemanal] = useState('');
  const [hastaSemanal, setHastaSemanal] = useState('');

  const [loadingDiario, setLoadingDiario] = useState(false);
  const [loadingSemanal, setLoadingSemanal] = useState(false);
  const [msgDiario, setMsgDiario] = useState('');
  const [msgSemanal, setMsgSemanal] = useState('');
  const [error, setError] = useState('');

  // Estados de configuración global de resúmenes automáticos
  const [config, setConfig] = useState({
    resumenDiarioMedicosHabilitado: true,
    resumenSemanalMedicosHabilitado: true,
    fechaModificacion: null,
    modificadoPor: null,
  });
  const [loadingConfig, setLoadingConfig] = useState(true);
  const [updatingConfig, setUpdatingConfig] = useState(false);
  const [configSuccessMsg, setConfigSuccessMsg] = useState('');
  const [configErrorMsg, setConfigErrorMsg] = useState('');

  useEffect(() => {
    cargarConfiguracion();
  }, []);

  const cargarConfiguracion = async () => {
    setLoadingConfig(true);
    setConfigErrorMsg('');
    try {
      const data = await adminService.obtenerConfiguracionNotificaciones();
      setConfig(data);
    } catch (err) {
      console.error('Error al cargar la configuración de notificaciones:', err);
      setConfigErrorMsg('No se pudo cargar la configuración de resúmenes automáticos.');
    } finally {
      setLoadingConfig(false);
    }
  };

  const handleToggleDiario = async (e) => {
    const nuevoValor = e.target.checked;
    setUpdatingConfig(true);
    setConfigSuccessMsg('');
    setConfigErrorMsg('');
    try {
      const data = await adminService.actualizarConfiguracionNotificaciones({
        resumenDiarioMedicosHabilitado: nuevoValor,
      });
      setConfig(data);
      setConfigSuccessMsg(`Resumen diario automático ${nuevoValor ? 'habilitado' : 'deshabilitado'} exitosamente.`);
    } catch (err) {
      console.error('Error al actualizar resumen diario:', err);
      setConfigErrorMsg('Error al guardar el estado del resumen diario.');
    } finally {
      setUpdatingConfig(false);
    }
  };

  const handleToggleSemanal = async (e) => {
    const nuevoValor = e.target.checked;
    setUpdatingConfig(true);
    setConfigSuccessMsg('');
    setConfigErrorMsg('');
    try {
      const data = await adminService.actualizarConfiguracionNotificaciones({
        resumenSemanalMedicosHabilitado: nuevoValor,
      });
      setConfig(data);
      setConfigSuccessMsg(`Reporte semanal automático ${nuevoValor ? 'habilitado' : 'deshabilitado'} exitosamente.`);
    } catch (err) {
      console.error('Error al actualizar reporte semanal:', err);
      setConfigErrorMsg('Error al guardar el estado del reporte semanal.');
    } finally {
      setUpdatingConfig(false);
    }
  };

  const handleEjecutarDiario = async () => {
    setLoadingDiario(true);
    setError('');
    setMsgDiario('');
    try {
      const res = await adminService.ejecutarResumenDiario(fechaDiario);
      setMsgDiario(res);
    } catch (err) {
      setError('Error al ejecutar el resumen diario.');
    } finally {
      setLoadingDiario(false);
    }
  };

  const handleEjecutarSemanal = async () => {
    setLoadingSemanal(true);
    setError('');
    setMsgSemanal('');
    try {
      const res = await adminService.ejecutarResumenSemanal(desdeSemanal, hastaSemanal);
      setMsgSemanal(res);
    } catch (err) {
      setError('Error al ejecutar el reporte semanal.');
    } finally {
      setLoadingSemanal(false);
    }
  };

  return (
    <Box maxWidth="900px" mx="auto">
      <Typography variant="h4" fontWeight={700} color="primary" mb={1}>
        Gestión de Reportes y Notificaciones a Profesionales
      </Typography>
      <Typography variant="body1" color="text.secondary" mb={3}>
        Administrá los envíos automáticos periódicos y ejecutá manualmente resúmenes de agenda para el cuerpo médico.
      </Typography>

      {/* Panel de Configuración Global de Resúmenes Automáticos */}
      <Paper sx={{ p: 3, mb: 4, borderRadius: 2, border: '1px solid', borderColor: 'divider' }}>
        <Box display="flex" alignItems="center" justifyContent="space-between" mb={1}>
          <Box display="flex" alignItems="center" gap={1}>
            <SettingsIcon color="primary" />
            <Typography variant="h6" fontWeight={600}>
              Control de Resúmenes Automáticos
            </Typography>
          </Box>
          {loadingConfig && <CircularProgress size={20} />}
        </Box>
        <Typography variant="body2" color="text.secondary" mb={2}>
          Active o desactive los envíos automáticos de resúmenes por correo a los médicos. Desactivarlos no impide ejecutar envíos manuales.
        </Typography>

        {configSuccessMsg && (
          <Alert severity="success" icon={<CheckCircleIcon />} sx={{ mb: 2 }}>
            {configSuccessMsg}
          </Alert>
        )}
        {configErrorMsg && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {configErrorMsg}
          </Alert>
        )}

        <Grid container spacing={3} alignItems="center">
          <Grid item xs={12} sm={6}>
            <Box sx={{ p: 2, bgcolor: 'background.default', borderRadius: 2, border: '1px solid', borderColor: 'divider' }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={Boolean(config.resumenDiarioMedicosHabilitado)}
                    onChange={handleToggleDiario}
                    disabled={loadingConfig || updatingConfig}
                    color="primary"
                  />
                }
                label={
                  <Box>
                    <Typography variant="subtitle2" fontWeight={600}>
                      Resumen Diario Automático
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Todos los días a las 20:00 hs (agenda del día siguiente).
                    </Typography>
                  </Box>
                }
              />
              <Box mt={1}>
                <Chip
                  size="small"
                  label={config.resumenDiarioMedicosHabilitado ? 'Habilitado' : 'Deshabilitado'}
                  color={config.resumenDiarioMedicosHabilitado ? 'success' : 'default'}
                />
              </Box>
            </Box>
          </Grid>

          <Grid item xs={12} sm={6}>
            <Box sx={{ p: 2, bgcolor: 'background.default', borderRadius: 2, border: '1px solid', borderColor: 'divider' }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={Boolean(config.resumenSemanalMedicosHabilitado)}
                    onChange={handleToggleSemanal}
                    disabled={loadingConfig || updatingConfig}
                    color="secondary"
                  />
                }
                label={
                  <Box>
                    <Typography variant="subtitle2" fontWeight={600}>
                      Reporte Semanal Automático
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Todos los domingos a las 20:00 hs (balance semanal).
                    </Typography>
                  </Box>
                }
              />
              <Box mt={1}>
                <Chip
                  size="small"
                  label={config.resumenSemanalMedicosHabilitado ? 'Habilitado' : 'Deshabilitado'}
                  color={config.resumenSemanalMedicosHabilitado ? 'secondary' : 'default'}
                />
              </Box>
            </Box>
          </Grid>
        </Grid>

        {config.fechaModificacion && (
          <Typography variant="caption" color="text.secondary" display="block" mt={2}>
            Última modificación: {dayjs(config.fechaModificacion).format('DD/MM/YYYY HH:mm')} por {config.modificadoPor || 'ADMIN'}
          </Typography>
        )}
      </Paper>

      <Divider sx={{ mb: 4 }} />

      <Typography variant="h5" fontWeight={600} color="text.primary" mb={2}>
        Ejecución Manual de Envíos
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      <Grid container spacing={3}>
        {/* Reporte Diario Manual */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3, height: '100%', borderRadius: 2 }}>
            <Typography variant="h6" fontWeight={600} mb={1}>
              Resumen Diario por Email
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              Envía la nómina de turnos confirmados a cada médico para el día especificado (por defecto, el día de mañana).
            </Typography>

            <TextField
              fullWidth
              margin="normal"
              label="Fecha Objetivo (Opcional)"
              type="date"
              value={fechaDiario}
              onChange={(e) => setFechaDiario(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />

            {msgDiario && <Alert severity="success" sx={{ my: 2 }}>{msgDiario}</Alert>}

            <Button
              variant="contained"
              fullWidth
              startIcon={<SendIcon />}
              disabled={loadingDiario}
              onClick={handleEjecutarDiario}
              sx={{ mt: 2 }}
            >
              {loadingDiario ? <CircularProgress size={24} color="inherit" /> : 'Ejecutar Resumen Diario'}
            </Button>
          </Paper>
        </Grid>

        {/* Reporte Semanal Manual */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3, height: '100%', borderRadius: 2 }}>
            <Typography variant="h6" fontWeight={600} mb={1}>
              Reporte Semanal de Actividad
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              Envía el informe semanal detallado (turnos completados, confirmados y cancelados) a cada médico para el rango indicado.
            </Typography>

            <Grid container spacing={1}>
              <Grid item xs={6}>
                <TextField
                  fullWidth
                  margin="normal"
                  label="Desde"
                  type="date"
                  value={desdeSemanal}
                  onChange={(e) => setDesdeSemanal(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={6}>
                <TextField
                  fullWidth
                  margin="normal"
                  label="Hasta"
                  type="date"
                  value={hastaSemanal}
                  onChange={(e) => setHastaSemanal(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
            </Grid>

            {msgSemanal && <Alert severity="success" sx={{ my: 2 }}>{msgSemanal}</Alert>}

            <Button
              variant="contained"
              color="secondary"
              fullWidth
              startIcon={<ReportIcon />}
              disabled={loadingSemanal}
              onClick={handleEjecutarSemanal}
              sx={{ mt: 2 }}
            >
              {loadingSemanal ? <CircularProgress size={24} color="inherit" /> : 'Ejecutar Reporte Semanal'}
            </Button>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default AdminReportes;
