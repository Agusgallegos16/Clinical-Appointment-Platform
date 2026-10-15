import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Box,
  Typography,
  Alert,
  CircularProgress,
  InputAdornment,
} from '@mui/material';
import {
  Badge as DniIcon,
  Phone as PhoneIcon,
  Cake as BirthdayIcon,
  AssignmentInd as ProfileIcon,
  CheckCircleOutline as CheckIcon,
} from '@mui/icons-material';
import { pacienteService } from '../../api/pacienteService';
import dayjs from 'dayjs';

const CompletarPerfilModal = ({ open, onClose, onSuccess, initialData = {} }) => {
  const [dni, setDni] = useState('');
  const [telefono, setTelefono] = useState('');
  const [fechaNacimiento, setFechaNacimiento] = useState('');

  const [formErrors, setFormErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const todayStr = dayjs().format('YYYY-MM-DD');

  useEffect(() => {
    if (open) {
      setDni(initialData?.dni ? String(initialData.dni) : '');
      setTelefono(initialData?.telefono || '');
      setFechaNacimiento(initialData?.fechaNacimiento || '');
      setFormErrors({});
      setServerError('');
    }
  }, [open, initialData]);

  const validate = () => {
    const errors = {};

    // Validar DNI
    const dniNum = Number(dni.trim());
    if (!dni.trim()) {
      errors.dni = 'El número de DNI / Documento es obligatorio.';
    } else if (isNaN(dniNum) || dniNum < 1000000 || dniNum > 999999999) {
      errors.dni = 'Ingresá un número de DNI válido (mínimo 7 dígitos).';
    }

    // Validar Teléfono
    if (!telefono.trim()) {
      errors.telefono = 'El teléfono de contacto es obligatorio.';
    } else if (telefono.trim().length < 6) {
      errors.telefono = 'Ingresá un teléfono válido (al menos 6 caracteres).';
    }

    // Validar Fecha de Nacimiento
    if (!fechaNacimiento) {
      errors.fechaNacimiento = 'La fecha de nacimiento es obligatoria.';
    } else if (dayjs(fechaNacimiento).isAfter(dayjs())) {
      errors.fechaNacimiento = 'La fecha de nacimiento no puede ser posterior a hoy.';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError('');

    if (!validate()) {
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        dni: Number(dni.trim()),
        telefono: telefono.trim(),
        fechaNacimiento: fechaNacimiento,
      };

      const pacienteActualizado = await pacienteService.completarPerfil(payload);

      if (onSuccess) {
        onSuccess(pacienteActualizado);
      }
    } catch (err) {
      const mensaje =
        err.response?.data?.mensaje ||
        err.response?.data?.message ||
        'Error al completar los datos del perfil. Verifique la información ingresada.';
      setServerError(mensaje);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={(e, reason) => {
        // Modal bloqueante: no se cierra haciendo clic en el backdrop ni con tecla escape durante el flujo
        if (reason === 'backdropClick' || reason === 'escapeKeyDown') return;
        if (onClose) onClose();
      }}
      maxWidth="sm"
      fullWidth
      PaperProps={{
        sx: {
          borderRadius: 3,
          p: { xs: 1, sm: 2 },
          boxShadow: '0 12px 32px rgba(0,0,0,0.18)',
        },
      }}
    >
      <DialogTitle sx={{ pb: 1 }}>
        <Box display="flex" alignItems="center" gap={1.5}>
          <Box
            sx={{
              bgcolor: 'primary.light',
              color: 'primary.main',
              p: 1,
              borderRadius: 2,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <ProfileIcon fontSize="medium" />
          </Box>
          <Box>
            <Typography variant="h6" fontWeight={700}>
              Completá tus datos para reservar el turno
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Por única vez requerimos estos datos para completar tu perfil
            </Typography>
          </Box>
        </Box>
      </DialogTitle>

      <Box component="form" onSubmit={handleSubmit}>
        <DialogContent sx={{ pt: 1.5 }}>
          {serverError && (
            <Alert severity="error" sx={{ mb: 2.5 }}>
              {serverError}
            </Alert>
          )}

          <Alert severity="info" sx={{ mb: 3, bgcolor: '#f0f9ff', color: '#0369a1', border: '1px solid #bae6fd' }}>
            Al completar estos datos, tu turno se confirmará automáticamente.
          </Alert>

          {/* Campo DNI */}
          <TextField
            fullWidth
            label="DNI / Documento de Identidad"
            type="number"
            value={dni}
            onChange={(e) => {
              setDni(e.target.value);
              if (formErrors.dni) setFormErrors({ ...formErrors, dni: null });
            }}
            error={!!formErrors.dni}
            helperText={formErrors.dni || 'Sin puntos ni espacios'}
            required
            margin="normal"
            disabled={isSubmitting}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <DniIcon color="action" />
                </InputAdornment>
              ),
            }}
          />

          {/* Campo Teléfono */}
          <TextField
            fullWidth
            label="Teléfono de Contacto"
            type="tel"
            value={telefono}
            onChange={(e) => {
              setTelefono(e.target.value);
              if (formErrors.telefono) setFormErrors({ ...formErrors, telefono: null });
            }}
            error={!!formErrors.telefono}
            helperText={formErrors.telefono}
            required
            margin="normal"
            disabled={isSubmitting}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <PhoneIcon color="action" />
                </InputAdornment>
              ),
            }}
          />

          {/* Campo Fecha de Nacimiento */}
          <TextField
            fullWidth
            label="Fecha de Nacimiento"
            type="date"
            value={fechaNacimiento}
            onChange={(e) => {
              setFechaNacimiento(e.target.value);
              if (formErrors.fechaNacimiento) setFormErrors({ ...formErrors, fechaNacimiento: null });
            }}
            error={!!formErrors.fechaNacimiento}
            helperText={formErrors.fechaNacimiento}
            required
            margin="normal"
            disabled={isSubmitting}
            InputLabelProps={{ shrink: true }}
            inputProps={{ max: todayStr }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <BirthdayIcon color="action" />
                </InputAdornment>
              ),
            }}
          />
        </DialogContent>

        <DialogActions sx={{ px: 3, pb: 2, pt: 1, justifyContent: 'space-between' }}>
          <Button
            onClick={onClose}
            color="inherit"
            disabled={isSubmitting}
            sx={{ textTransform: 'none', fontWeight: 600 }}
          >
            Volver / Cancelar
          </Button>

          <Button
            type="submit"
            variant="contained"
            color="primary"
            disabled={isSubmitting}
            startIcon={isSubmitting ? <CircularProgress size={18} color="inherit" /> : <CheckIcon />}
            sx={{
              textTransform: 'none',
              fontWeight: 700,
              px: 3,
              py: 1,
              borderRadius: 2,
            }}
          >
            {isSubmitting ? 'Guardando...' : 'Guardar y Confirmar Turno'}
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
};

export default CompletarPerfilModal;
