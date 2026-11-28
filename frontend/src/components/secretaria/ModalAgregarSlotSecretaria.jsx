import React, { useEffect, useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Typography,
  Alert,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import {
  AddCircleOutline as AddCircleOutlineIcon,
  EventAvailable as EventAvailableIcon,
} from '@mui/icons-material';
import dayjs from 'dayjs';
import { doctorService } from '../../api/doctorService';

const DURACIONES_DISPONIBLES = [15, 20, 30, 45, 60];

const ModalAgregarSlotSecretaria = ({
  open,
  onClose,
  doctor,
  fecha,
  defaultEspecialidadId = '',
  onSlotCreado,
}) => {
  const [horaInicio, setHoraInicio] = useState('');
  const [duracionMinutos, setDuracionMinutos] = useState(30);
  const [especialidadId, setEspecialidadId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (open) {
      setHoraInicio('');
      setDuracionMinutos(30);
      setEspecialidadId(defaultEspecialidadId || '');
      setError('');
    }
  }, [open, defaultEspecialidadId]);

  const crearSlot = async (agendarInmediatamente) => {
    setError('');
    if (!horaInicio) {
      setError('Por favor seleccione una hora de inicio.');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        fecha,
        horaInicio,
        duracionMinutos: Number(duracionMinutos),
        especialidadId: especialidadId ? Number(especialidadId) : null,
      };

      const slotCreado = await doctorService.crearSlotIndividual(doctor.id, payload);
      onClose();
      if (onSlotCreado) {
        await onSlotCreado(slotCreado, agendarInmediatamente);
      }
    } catch (err) {
      console.error('Error al crear horario puntual:', err);
      const detalles = err.response?.data?.detalles;
      const detalleMsg = detalles && typeof detalles === 'object' ? Object.values(detalles)[0] : null;
      setError(
        detalleMsg ||
        err.response?.data?.mensaje ||
        err.response?.data?.message ||
        'No se pudo agregar el horario. Verifique los datos ingresados.'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    crearSlot(false);
  };

  return (
    <Dialog open={open} onClose={loading ? undefined : onClose} maxWidth="xs" fullWidth aria-labelledby="agregar-slot-secretaria-title">
      <DialogTitle
        id="agregar-slot-secretaria-title"
        sx={{ fontWeight: 700, pb: 1, display: 'flex', alignItems: 'center', gap: 1 }}
      >
        <AddCircleOutlineIcon color="primary" /> Agregar Horario Puntual
      </DialogTitle>

      <form onSubmit={handleSubmit}>
        <DialogContent dividers>
          <Box display="flex" flexDirection="column" gap={2}>
            <Box sx={{ p: 1.5, bgcolor: 'action.hover', borderRadius: 2, border: '1px solid', borderColor: 'divider' }}>
              <Typography variant="subtitle2" color="primary" fontWeight={700}>
                Dr. {doctor?.nombre} {doctor?.apellido}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Fecha: <strong>{fecha ? dayjs(fecha).format('DD/MM/YYYY') : ''}</strong>
              </Typography>
            </Box>

            {error && <Alert severity="error">{error}</Alert>}

            <TextField
              fullWidth
              label="Hora de inicio"
              type="time"
              value={horaInicio}
              onChange={(e) => setHoraInicio(e.target.value)}
              required
              InputLabelProps={{ shrink: true }}
              inputProps={{ step: 300 }}
            />

            <FormControl fullWidth>
              <InputLabel id="duracion-slot-secretaria-label">Duración del turno</InputLabel>
              <Select
                labelId="duracion-slot-secretaria-label"
                value={duracionMinutos}
                label="Duración del turno"
                onChange={(e) => setDuracionMinutos(e.target.value)}
              >
                {DURACIONES_DISPONIBLES.map((min) => (
                  <MenuItem key={min} value={min}>
                    {min} minutos
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            {doctor?.especialidades && doctor.especialidades.length > 1 && (
              <FormControl fullWidth>
                <InputLabel id="especialidad-slot-secretaria-label">Especialidad</InputLabel>
                <Select
                  labelId="especialidad-slot-secretaria-label"
                  value={especialidadId}
                  label="Especialidad"
                  onChange={(e) => setEspecialidadId(e.target.value)}
                >
                  <MenuItem value="">Todas las especialidades del profesional</MenuItem>
                  {doctor.especialidades.map((esp) => (
                    <MenuItem key={esp.id} value={esp.id}>
                      {esp.nombre}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}
          </Box>
        </DialogContent>

        <DialogActions sx={{ px: 2.5, py: 2, flexWrap: 'wrap', gap: 1 }}>
          <Button onClick={onClose} color="inherit" disabled={loading}>
            Cancelar
          </Button>
          <Button
            type="submit"
            variant="outlined"
            color="primary"
            disabled={loading || !horaInicio}
          >
            {loading ? <CircularProgress size={18} color="inherit" /> : 'Crear horario'}
          </Button>
          <Button
            type="button"
            variant="contained"
            color="primary"
            disabled={loading || !horaInicio}
            startIcon={loading ? <CircularProgress size={18} color="inherit" /> : <EventAvailableIcon />}
            onClick={() => crearSlot(true)}
          >
            Crear y agendar
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default ModalAgregarSlotSecretaria;
