import React, { useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  TextField,
  Button,
  CircularProgress,
  Alert,
  Paper,
} from '@mui/material';
import { Send as SendIcon, Assessment as ReportIcon } from '@mui/icons-material';
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
        Gestión de Reportes por Correo Electrónico
      </Typography>
      <Typography variant="body1" color="text.secondary" mb={3}>
        Ejecutá manualmente el envío de resúmenes diarios y semanales de agenda a los médicos.
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      <Grid container spacing={3}>
        {/* Reporte Diario */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="h6" fontWeight={600} mb={1}>
              📅 Resumen Diario por Email
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

        {/* Reporte Semanal */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="h6" fontWeight={600} mb={1}>
              📊 Reporte Semanal de Actividad
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
