import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Button,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Alert,
  CircularProgress,
  Paper,
  Avatar,
  Divider,
  Chip,
} from '@mui/material';
import {
  Add as AddIcon,
  ChildCare as ChildIcon,
  CalendarMonth as CalendarIcon,
  EventNote as TurnosIcon,
  LinkOff as UnlinkIcon,
  ArrowBack as ArrowBackIcon,
} from '@mui/icons-material';
import { pacienteMenorService } from '../../api/pacienteMenorService';
import dayjs from 'dayjs';

const GestionMenores = () => {
  const navigate = useNavigate();
  const [menores, setMenores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Modal para agregar menor
  const [openModal, setOpenModal] = useState(false);
  const [formData, setFormData] = useState({
    nombre: '',
    apellido: '',
    dni: '',
    fechaNacimiento: '',
  });
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Modal para desvincular menor
  const [openUnlinkDialog, setOpenUnlinkDialog] = useState(false);
  const [menorADesvincular, setMenorADesvincular] = useState(null);
  const [unlinking, setUnlinking] = useState(false);

  useEffect(() => {
    cargarMenores();
  }, []);

  const cargarMenores = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await pacienteMenorService.listarMenores();
      setMenores(data);
    } catch (err) {
      setError('Error al consultar la lista de menores a cargo.');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = () => {
    setFormData({ nombre: '', apellido: '', dni: '', fechaNacimiento: '' });
    setFormError('');
    setOpenModal(true);
  };

  const handleCloseModal = () => {
    if (submitting) return;
    setOpenModal(false);
  };

  const handleFormChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmitNuevoMenor = async (e) => {
    e.preventDefault();
    setFormError('');

    if (!formData.nombre.trim() || !formData.apellido.trim() || !formData.dni || !formData.fechaNacimiento) {
      setFormError('Por favor completá todos los campos obligatorios.');
      return;
    }

    const edadCalculada = dayjs().diff(dayjs(formData.fechaNacimiento), 'year');
    if (edadCalculada >= 18) {
      setFormError('No es posible vincular a una persona mayor o igual a 18 años como menor a cargo.');
      return;
    }

    setSubmitting(true);
    try {
      await pacienteMenorService.registrarMenor({
        nombre: formData.nombre.trim(),
        apellido: formData.apellido.trim(),
        dni: Number(formData.dni),
        fechaNacimiento: formData.fechaNacimiento,
      });

      setSuccess('Menor registrado e incorporado exitosamente a tu cuenta.');
      setOpenModal(false);
      cargarMenores();
      setTimeout(() => setSuccess(''), 4000);
    } catch (err) {
      setFormError(err.response?.data?.mensaje || 'Error al registrar el menor a cargo.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleOpenUnlink = (menor) => {
    setMenorADesvincular(menor);
    setOpenUnlinkDialog(true);
  };

  const handleConfirmUnlink = async () => {
    if (!menorADesvincular) return;
    setUnlinking(true);
    try {
      await pacienteMenorService.desvincularMenor(menorADesvincular.id);
      setSuccess(`Se desvinculó a ${menorADesvincular.nombre} ${menorADesvincular.apellido} de tu tutela.`);
      setOpenUnlinkDialog(false);
      setMenorADesvincular(null);
      cargarMenores();
      setTimeout(() => setSuccess(''), 4000);
    } catch (err) {
      setError('Error al desvincular el menor seleccionado.');
    } finally {
      setUnlinking(false);
    }
  };

  return (
    <Box maxWidth="1000px" mx="auto">
      <Box display="flex" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={2} mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Button
            variant="outlined"
            size="small"
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate('/paciente')}
            sx={{ borderRadius: 2 }}
          >
            Volver al Inicio
          </Button>
          <Box>
            <Typography variant="h4" fontWeight={700} color="primary">
              Gestionar Menores a Cargo 🧒
            </Typography>
            <Typography variant="body1" color="text.secondary">
              Vinculá a tus hijos o menores bajo tu tutela para agendar y gestionar sus citas médicas.
            </Typography>
          </Box>
        </Box>

        <Button
          variant="contained"
          size="large"
          startIcon={<AddIcon />}
          onClick={handleOpenModal}
          sx={{ borderRadius: 3, fontWeight: 700, px: 3 }}
        >
          Agregar Menor a Cargo
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

      {loading ? (
        <Box display="flex" justifyContent="center" py={6}><CircularProgress size={44} /></Box>
      ) : menores.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center', borderRadius: 4, bgcolor: 'background.default' }}>
          <ChildIcon sx={{ fontSize: 60, color: 'text.secondary', mb: 1 }} />
          <Typography variant="h6" fontWeight={700} gutterBottom>
            No poseés menores a cargo vinculados a tu cuenta
          </Typography>
          <Typography variant="body2" color="text.secondary" mb={3}>
            Hacé clic en el botón superior para dar de alta a un menor e iniciar la reserva de sus turnos.
          </Typography>
        </Paper>
      ) : (
        <Grid container spacing={3}>
          {menores.map((menor) => (
            <Grid item xs={12} sm={6} key={menor.id}>
              <Card
                sx={{
                  borderRadius: 4,
                  boxShadow: '0 4px 16px rgba(0,0,0,0.06)',
                  borderLeft: '6px solid #0284c7',
                  transition: '0.2s',
                  '&:hover': { transform: 'translateY(-2px)' },
                }}
              >
                <CardContent>
                  <Box display="flex" alignItems="center" gap={2} mb={2}>
                    <Avatar sx={{ width: 56, height: 56, bgcolor: 'primary.main', fontWeight: 700, fontSize: '1.4rem' }}>
                      {menor.nombre ? menor.nombre.charAt(0).toUpperCase() : 'M'}
                    </Avatar>
                    <Box>
                      <Typography variant="h6" fontWeight={700}>
                        {menor.nombre} {menor.apellido}
                      </Typography>
                      <Chip
                        label={`${menor.edad !== null ? menor.edad : '-'} años`}
                        size="small"
                        color="primary"
                        variant="outlined"
                        sx={{ fontWeight: 600, mt: 0.5 }}
                      />
                    </Box>
                  </Box>

                  <Divider sx={{ my: 1.5 }} />

                  <Grid container spacing={1} mb={2.5}>
                    <Grid item xs={6}>
                      <Typography variant="caption" color="text.secondary" display="block">DNI</Typography>
                      <Typography variant="body2" fontWeight={700}>{menor.dni}</Typography>
                    </Grid>
                    <Grid item xs={6}>
                      <Typography variant="caption" color="text.secondary" display="block">Nacimiento</Typography>
                      <Typography variant="body2" fontWeight={700}>
                        {menor.fechaNacimiento ? dayjs(menor.fechaNacimiento).format('DD/MM/YYYY') : '-'}
                      </Typography>
                    </Grid>
                    <Grid item xs={12} sx={{ mt: 1 }}>
                      <Typography variant="caption" color="text.secondary" display="block">Teléfono de Contacto (Tutor)</Typography>
                      <Typography variant="body2" fontWeight={700}>{menor.telefono || '-'}</Typography>
                    </Grid>
                  </Grid>

                  <Box display="flex" gap= {1.5} flexWrap="wrap">
                    <Button
                      variant="contained"
                      color="info"
                      size="small"
                      startIcon={<TurnosIcon />}
                      onClick={() => navigate(`/paciente/turnos?pacienteId=${menor.id}&nombre=${encodeURIComponent(menor.nombre + ' ' + menor.apellido)}`)}
                      sx={{ flex: 1, borderRadius: 2, fontWeight: 600 }}
                    >
                      Ver Turnos
                    </Button>
                    <Button
                      variant="outlined"
                      color="error"
                      size="small"
                      startIcon={<UnlinkIcon />}
                      onClick={() => handleOpenUnlink(menor)}
                      sx={{ flex: 1, borderRadius: 2 }}
                      title="Desvincular menor de tu cuenta"
                    >
                      Desvincular
                    </Button>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Modal para Agregar Menor */}
      <Dialog open={openModal} onClose={handleCloseModal} maxWidth="xs" fullWidth>
        <form onSubmit={handleSubmitNuevoMenor}>
          <DialogTitle fontWeight={700} color="primary">
            Agregar Menor a Cargo
          </DialogTitle>
          <DialogContent>
            <Alert severity="info" sx={{ mb: 2 }}>
              Para los menores a cargo se registrará tu correo electrónico para la recepción de recordatorios.
            </Alert>

            {formError && <Alert severity="error" sx={{ mb: 2 }}>{formError}</Alert>}

            <TextField
              fullWidth
              margin="normal"
              label="Nombre del Menor *"
              name="nombre"
              value={formData.nombre}
              onChange={handleFormChange}
              required
            />
            <TextField
              fullWidth
              margin="normal"
              label="Apellido del Menor *"
              name="apellido"
              value={formData.apellido}
              onChange={handleFormChange}
              required
            />
            <TextField
              fullWidth
              margin="normal"
              label="DNI del Menor *"
              name="dni"
              type="number"
              value={formData.dni}
              onChange={handleFormChange}
              required
            />
            <TextField
              fullWidth
              margin="normal"
              label="Fecha de Nacimiento *"
              name="fechaNacimiento"
              type="date"
              value={formData.fechaNacimiento}
              onChange={handleFormChange}
              InputLabelProps={{ shrink: true }}
              inputProps={{ max: dayjs().format('YYYY-MM-DD') }}
              helperText="El menor debe tener menos de 18 años de edad."
              required
            />
          </DialogContent>
          <DialogActions sx={{ p: 2.5 }}>
            <Button onClick={handleCloseModal} color="inherit" disabled={submitting}>
              Cancelar
            </Button>
            <Button variant="contained" type="submit" disabled={submitting}>
              {submitting ? <CircularProgress size={24} color="inherit" /> : 'Registrar Menor'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Dialog para Desvincular Menor */}
      <Dialog open={openUnlinkDialog} onClose={() => setOpenUnlinkDialog(false)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700} color="error.main">
          Desvincular Menor
        </DialogTitle>
        <DialogContent>
          <Typography variant="body1">
            ¿Estás seguro de que querés desvincular a <strong>{menorADesvincular?.nombre} {menorADesvincular?.apellido}</strong> de tu tutela?
          </Typography>
          <Typography variant="body2" color="text.secondary" mt={1}>
            Ya no podrás agendar turnos a su nombre desde tu cuenta.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 2.5 }}>
          <Button onClick={() => setOpenUnlinkDialog(false)} color="inherit" disabled={unlinking}>
            Cancelar
          </Button>
          <Button variant="contained" color="error" onClick={handleConfirmUnlink} disabled={unlinking}>
            {unlinking ? <CircularProgress size={24} color="inherit" /> : 'Confirmar Desvinculación'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default GestionMenores;
