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
  Avatar,
  IconButton,
  Alert,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  OutlinedInput,
  Chip,
  Grid,
} from '@mui/material';
import {
  PhotoCamera as PhotoCameraIcon,
  Edit as EditIcon,
} from '@mui/icons-material';
import { pacienteService } from '../api/pacienteService';
import { doctorService } from '../api/doctorService';
import { especialidadService } from '../api/especialidadService';
import { uploadDoctorAvatar } from '../api/storageService';
import { useAuth } from '../context/AuthContext';

const EditarPerfilModal = ({ open, onClose }) => {
  const { role, updateUser, user: authUser } = useAuth();

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [uploadingFoto, setUploadingFoto] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Form Fields pre-cargados
  const [nombre, setNombre] = useState('');
  const [apellido, setApellido] = useState('');
  const [email, setEmail] = useState('');
  const [telefono, setTelefono] = useState('');

  // Doctor Specific Fields
  const [doctorId, setDoctorId] = useState(null);
  const [fotoUrl, setFotoUrl] = useState('');
  const [especialidadesSeleccionadas, setEspecialidadesSeleccionadas] = useState([]);
  const [todasEspecialidades, setTodasEspecialidades] = useState([]);

  useEffect(() => {
    if (open) {
      cargarPerfil();
    } else {
      resetForm();
    }
  }, [open]);

  const resetForm = () => {
    setError('');
    setSuccessMsg('');
  };

  const cargarPerfil = async () => {
    setLoading(true);
    setError('');
    try {
      if (role === 'PACIENTE') {
        const data = await pacienteService.obtenerMiPerfil();
        setNombre(data.nombre || '');
        setApellido(data.apellido || '');
        setTelefono(data.telefono || '');
        setEmail(data.usuario?.email || authUser?.email || '');
      } else if (role === 'DOCTOR') {
        const [data, espList] = await Promise.all([
          doctorService.obtenerMiPerfil(),
          especialidadService.listarTodas(),
        ]);
        setDoctorId(data.id);
        setNombre(data.nombre || '');
        setApellido(data.apellido || '');
        setFotoUrl(data.fotoUrl || '');
        setEmail(data.usuario?.email || authUser?.email || '');
        setTodasEspecialidades(espList || []);

        const espIds = data.especialidades ? data.especialidades.map((e) => e.id) : [];
        setEspecialidadesSeleccionadas(espIds);
      }
    } catch (err) {
      console.error('Error al cargar datos del perfil:', err);
      setError(err.response?.data?.message || 'No se pudieron cargar los datos del perfil.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubirFoto = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setUploadingFoto(true);
    setError('');
    try {
      const publicUrl = await uploadDoctorAvatar(file, doctorId || 'profile-edit');
      setFotoUrl(publicUrl);
      setSuccessMsg('Foto de perfil subida correctamente.');
    } catch (err) {
      console.error('Error al subir foto:', err);
      setError('No se pudo subir la foto de perfil.');
    } finally {
      setUploadingFoto(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');

    setSaving(true);
    try {
      if (role === 'PACIENTE') {
        const payload = {
          nombre,
          apellido,
          telefono,
          email,
        };
        const updated = await pacienteService.actualizarMiPerfil(payload);
        setSuccessMsg('¡Perfil actualizado con éxito!');
        
        if (updated.usuario?.email) {
          updateUser({ email: updated.usuario.email });
        }
      } else if (role === 'DOCTOR') {
        const payload = {
          nombre,
          apellido,
          email,
          fotoUrl,
          especialidadIds: especialidadesSeleccionadas,
        };
        const updated = await doctorService.actualizarMiPerfil(payload);
        setSuccessMsg('¡Perfil médico actualizado con éxito!');

        if (updated.usuario?.email) {
          updateUser({ email: updated.usuario.email });
        }
      }

      setTimeout(() => {
        onClose();
      }, 1400);
    } catch (err) {
      console.error('Error al actualizar perfil:', err);
      setError(err.response?.data?.message || 'Error al guardar los cambios del perfil.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ fontWeight: 700, pb: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
        <EditIcon color="primary" /> Editar Mi Perfil
      </DialogTitle>

      <form onSubmit={handleSubmit}>
        <DialogContent dividers>
          {loading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : (
            <Box display="flex" flexDirection="column" gap={2.5}>
              {error && <Alert severity="error">{error}</Alert>}
              {successMsg && <Alert severity="success">{successMsg}</Alert>}

              {/* Subida de foto de perfil (Exclusivo para Doctores) */}
              {role === 'DOCTOR' && (
                <Box display="flex" flexDirection="column" alignItems="center" gap={1.5} my={1}>
                  <Box position="relative">
                    <Avatar
                      src={fotoUrl}
                      sx={{ width: 90, height: 90, border: '3px solid #0284c7', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
                    >
                      {nombre?.charAt(0).toUpperCase()}
                    </Avatar>
                    <IconButton
                      color="primary"
                      aria-label="upload picture"
                      component="label"
                      disabled={uploadingFoto}
                      sx={{
                        position: 'absolute',
                        bottom: -4,
                        right: -4,
                        bgcolor: '#ffffff',
                        boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
                        '&:hover': { bgcolor: '#f0f9ff' },
                      }}
                    >
                      <input hidden accept="image/*" type="file" onChange={handleSubirFoto} />
                      {uploadingFoto ? <CircularProgress size={20} /> : <PhotoCameraIcon fontSize="small" />}
                    </IconButton>
                  </Box>
                  <Typography variant="caption" color="text.secondary">
                    Presiona la cámara para cambiar tu foto de perfil
                  </Typography>
                </Box>
              )}

              <Grid container spacing={2}>
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
              </Grid>

              <TextField
                fullWidth
                label="Correo Electrónico"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                helperText="Si cambias tu correo, este será tu nuevo usuario de acceso"
              />

              {/* Teléfono (Solo para Pacientes) */}
              {role === 'PACIENTE' && (
                <TextField
                  fullWidth
                  label="Teléfono de Contacto"
                  value={telefono}
                  onChange={(e) => setTelefono(e.target.value)}
                  placeholder="Ej: +54 11 1234-5678"
                />
              )}

              {/* Especialidades médicas (Solo para Doctores) */}
              {role === 'DOCTOR' && (
                <FormControl fullWidth>
                  <InputLabel id="select-especialidades-label">Especialidades Médicas</InputLabel>
                  <Select
                    labelId="select-especialidades-label"
                    multiple
                    value={especialidadesSeleccionadas}
                    onChange={(e) => setEspecialidadesSeleccionadas(e.target.value)}
                    input={<OutlinedInput label="Especialidades Médicas" />}
                    renderValue={(selected) => (
                      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                        {selected.map((value) => {
                          const esp = todasEspecialidades.find((e) => e.id === value);
                          return <Chip key={value} label={esp ? esp.nombre : value} size="small" color="primary" />;
                        })}
                      </Box>
                    )}
                  >
                    {todasEspecialidades.map((esp) => (
                      <MenuItem key={esp.id} value={esp.id}>
                        {esp.nombre}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              )}
            </Box>
          )}
        </DialogContent>

        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={onClose} color="inherit" disabled={saving}>
            Cancelar
          </Button>
          <Button type="submit" variant="contained" color="primary" disabled={loading || saving || uploadingFoto}>
            {saving ? <CircularProgress size={24} color="inherit" /> : 'Guardar Cambios'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default EditarPerfilModal;
