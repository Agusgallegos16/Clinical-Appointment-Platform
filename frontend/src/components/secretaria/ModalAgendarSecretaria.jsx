import React, { useState, useEffect } from 'react';
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
  FormControlLabel,
  Switch,
  Grid,
} from '@mui/material';
import {
  EventNote as EventNoteIcon,
  PersonAdd as PersonAddIcon,
  Search as SearchIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import axiosClient from '../../api/axiosClient';
import { pacienteService } from '../../api/pacienteService';

const ModalAgendarSecretaria = ({ open, onClose, onSuccess, doctor, especialidadId, especialidadNombre, fechaHora }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const [dni, setDni] = useState('');
  const [nombre, setNombre] = useState('');
  const [apellido, setApellido] = useState('');
  const [email, setEmail] = useState('');
  const [telefono, setTelefono] = useState('');
  const [fechaNacimiento, setFechaNacimiento] = useState('');
  const [tieneObraSocial, setTieneObraSocial] = useState(false);
  const [obraSocial, setObraSocial] = useState('');
  const [motivoConsulta, setMotivoConsulta] = useState('');

  const [buscandoDni, setBuscandoDni] = useState(false);
  const [pacienteAutocompletado, setPacienteAutocompletado] = useState(false);
  const [mensajeAutocompletado, setMensajeAutocompletado] = useState('');

  useEffect(() => {
    if (open) {
      setError('');
      setSuccessMsg('');
    } else {
      resetForm();
    }
  }, [open]);

  // Autocompletado automático al escribir un DNI registrado
  useEffect(() => {
    const cleanDni = dni ? String(dni).trim() : '';
    if (cleanDni.length >= 7 && cleanDni.length <= 9) {
      const timer = setTimeout(async () => {
        setBuscandoDni(true);
        try {
          const pacienteEncontrado = await pacienteService.buscarPorDni(cleanDni);
          if (pacienteEncontrado) {
            setNombre(pacienteEncontrado.nombre || '');
            setApellido(pacienteEncontrado.apellido || '');
            setEmail(pacienteEncontrado.email || pacienteEncontrado.usuario?.email || '');

            if (pacienteEncontrado.fechaNacimiento) {
              const fnStr = typeof pacienteEncontrado.fechaNacimiento === 'string'
                ? pacienteEncontrado.fechaNacimiento.split('T')[0]
                : '';
              setFechaNacimiento(fnStr);
            }

            setPacienteAutocompletado(true);
            setMensajeAutocompletado(`¡Paciente registrado encontrado! Se autocompletaron los datos de ${pacienteEncontrado.nombre} ${pacienteEncontrado.apellido}.`);
          } else {
            setPacienteAutocompletado(false);
            setMensajeAutocompletado('');
          }
        } catch (err) {
          console.error('Error al buscar paciente por DNI:', err);
        } finally {
          setBuscandoDni(false);
        }
      }, 400);

      return () => clearTimeout(timer);
    } else {
      setPacienteAutocompletado(false);
      setMensajeAutocompletado('');
    }
  }, [dni]);

  const resetForm = () => {
    setDni('');
    setNombre('');
    setApellido('');
    setEmail('');
    setTelefono('');
    setFechaNacimiento('');
    setTieneObraSocial(false);
    setObraSocial('');
    setMotivoConsulta('');
    setError('');
    setSuccessMsg('');
    setBuscandoDni(false);
    setPacienteAutocompletado(false);
    setMensajeAutocompletado('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');

    if (!dni || !nombre || !apellido) {
      setError('Por favor complete los campos obligatorios (DNI, Nombre y Apellido).');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        doctorId: doctor?.id,
        especialidadId: especialidadId,
        fechaHora: fechaHora,
        dni: Number(dni),
        nombre: nombre.trim(),
        apellido: apellido.trim(),
        email: email.trim() || null,
        telefono: null,
        fechaNacimiento: fechaNacimiento || null,
        tieneObraSocial,
        obraSocial: tieneObraSocial ? obraSocial.trim() : null,
        motivoConsulta: motivoConsulta.trim() || null,
      };

      await axiosClient.post('/turnos/secretaria', payload);
      setSuccessMsg('¡Turno agendado con éxito!');

      setTimeout(() => {
        onSuccess();
        onClose();
      }, 1200);
    } catch (err) {
      console.error('Error al agendar turno por secretaría:', err);
      setError(err.response?.data?.message || 'Error al agendar el turno. Por favor verifique los datos.');
    } finally {
      setLoading(false);
    }
  };

  const formatearFechaHora = (fh) => {
    if (!fh) return '';
    const date = new Date(fh);
    return date.toLocaleString('es-AR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ fontWeight: 700, pb: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
        <EventNoteIcon color="primary" /> Agendar Turno para Paciente
      </DialogTitle>

      <form onSubmit={handleSubmit}>
        <DialogContent dividers>
          <Box display="flex" flexDirection="column" gap={2}>
            {/* Resumen del turno */}
            <Box sx={{ p: 2, bgcolor: 'action.hover', borderRadius: 2, border: '1px solid', borderColor: 'divider' }}>
              <Typography variant="subtitle2" color="primary" fontWeight={700}>
                Médico: Dr. {doctor?.nombre} {doctor?.apellido}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Especialidad: {especialidadNombre || 'Consulta General'}
              </Typography>
              <Typography variant="body2" color="text.secondary" fontWeight={600} sx={{ mt: 0.5 }}>
                Fecha y Hora: {formatearFechaHora(fechaHora)}
              </Typography>
            </Box>

            {error && <Alert severity="error">{error}</Alert>}
            {successMsg && <Alert severity="success">{successMsg}</Alert>}

            {buscandoDni && (
              <Alert severity="info" icon={<CircularProgress size={18} color="inherit" />}>
                Buscando paciente registrado por DNI...
              </Alert>
            )}

            {pacienteAutocompletado && (
              <Alert severity="success" icon={<CheckCircleIcon />} sx={{ borderRadius: 2 }}>
                {mensajeAutocompletado}
              </Alert>
            )}

            <Typography variant="subtitle2" fontWeight={700} sx={{ mt: 1 }}>
              Datos del Paciente:
            </Typography>

            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="DNI"
                  type="number"
                  value={dni}
                  onChange={(e) => setDni(e.target.value)}
                  required
                  placeholder="Ej: 35123456"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Nombre"
                  value={nombre}
                  onChange={(e) => setNombre(e.target.value)}
                  required
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Apellido"
                  value={apellido}
                  onChange={(e) => setApellido(e.target.value)}
                  required
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Correo Electrónico"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  helperText="Se enviará la confirmación a esta casilla"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Fecha de Nacimiento"
                  type="date"
                  value={fechaNacimiento}
                  onChange={(e) => setFechaNacimiento(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
            </Grid>

            <FormControlLabel
              control={
                <Switch
                  checked={tieneObraSocial}
                  onChange={(e) => setTieneObraSocial(e.target.checked)}
                  color="primary"
                />
              }
              label="¿Cuenta con Cobertura Médica / Obra Social?"
            />

            {tieneObraSocial && (
              <TextField
                fullWidth
                label="Nombre de la Obra Social / Prepaga"
                value={obraSocial}
                onChange={(e) => setObraSocial(e.target.value)}
                placeholder="Ej: OSDE, Swiss Medical, IOMA"
              />
            )}

            <TextField
              fullWidth
              multiline
              rows={2}
              label="Motivo de la Consulta (Opcional)"
              value={motivoConsulta}
              onChange={(e) => setMotivoConsulta(e.target.value)}
            />
          </Box>
        </DialogContent>

        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={onClose} color="inherit" disabled={loading}>
            Cancelar
          </Button>
          <Button
            type="submit"
            variant="contained"
            color="primary"
            disabled={loading}
            startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <PersonAddIcon />}
          >
            Confirmar Turno
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default ModalAgendarSecretaria;
