import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Grid,
  CircularProgress,
  Alert,
  FormGroup,
  FormControlLabel,
  Checkbox,
  FormLabel,
  Avatar,
} from '@mui/material';
import { PersonAdd as PersonAddIcon, ArrowBack as ArrowBackIcon, LocalHospital as HospitalIcon } from '@mui/icons-material';
import { authService } from '../../api/authService';
import { especialidadService } from '../../api/especialidadService';
import { uploadDoctorAvatar } from '../../api/storageService';

const AdminNuevoDoctor = () => {
  const navigate = useNavigate();
  const [especialidades, setEspecialidades] = useState([]);

  // Form states
  const [email, setEmail] = useState('');
  const [nombre, setNombre] = useState('');
  const [apellido, setApellido] = useState('');
  const [fotoUrl, setFotoUrl] = useState('');
  const [previewUrl, setPreviewUrl] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [selectedEspecialidadIds, setSelectedEspecialidadIds] = useState([]);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    cargarEspecialidades();
  }, []);

  const cargarEspecialidades = async () => {
    try {
      const data = await especialidadService.listarTodas();
      setEspecialidades(data);
    } catch (err) {
      setError('Error al cargar la lista de especialidades.');
    }
  };

  const handleCheckboxChange = (id) => {
    if (selectedEspecialidadIds.includes(id)) {
      setSelectedEspecialidadIds(selectedEspecialidadIds.filter((espId) => espId !== id));
    } else {
      setSelectedEspecialidadIds([...selectedEspecialidadIds, id]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      let finalFotoUrl = fotoUrl; // Mantiene la URL ingresada manualmente si existe

      if (selectedFile) {
        console.log('Iniciando subida a Supabase Storage...');
        const uploadedUrl = await uploadDoctorAvatar(selectedFile);

        if (!uploadedUrl) {
          setError('Error al subir la imagen a Supabase Storage. Revisa la consola (F12).');
          setLoading(false);
          return; // Detener el guardado si falló la subida
        }

        finalFotoUrl = uploadedUrl;
        console.log('URL Pública obtenida de Supabase:', finalFotoUrl);
      }

      await authService.registrarDoctor({
        email,
        nombre,
        apellido,
        fotoUrl: finalFotoUrl,
        especialidadIds: selectedEspecialidadIds,
      });

      setSuccess(`¡Doctor/a Dr. ${nombre} ${apellido} registrado con éxito! Se envió un mail de activación a ${email}.`);
      setTimeout(() => {
        navigate('/admin/doctores');
      }, 2000);
    } catch (err) {
      let msg = 'Error al registrar al profesional.';
      if (err.response?.data?.mensaje) {
        msg = err.response.data.mensaje;
      } else if (err.response?.data?.message) {
        msg = err.response.data.message;
      } else if (err.response?.data?.detalles) {
        const d = err.response.data.detalles;
        msg = typeof d === 'object' ? Object.values(d).join(', ') : String(d);
      }
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box maxWidth="700px" mx="auto">
      <Button
        startIcon={<ArrowBackIcon />}
        onClick={() => navigate('/admin/doctores')}
        sx={{ mb: 2 }}
      >
        Volver a la Nómina
      </Button>

      <Paper sx={{ p: 4 }}>
        <Typography variant="h5" fontWeight={700} color="primary" mb={1}>
          Alta de Nuevo Médico
        </Typography>
        <Typography variant="body2" color="text.secondary" mb={3}>
          Completá los datos personales, foto de perfil y especialidades para dar de alta a un profesional. Se le enviará un correo electrónico de activación para que configure su contraseña de acceso.
        </Typography>

        {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
        {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

        <Box component="form" onSubmit={handleSubmit}>
          {/* Previsualización de Avatar */}
          <Box display="flex" alignItems="center" gap={2.5} mb={3} p={2} bgcolor="background.default" borderRadius={2}>
            <Avatar
              src={previewUrl || fotoUrl}
              alt={`${nombre} ${apellido}`}
              sx={{ width: 72, height: 72, bgcolor: 'primary.main', fontSize: '1.8rem', fontWeight: 700 }}
            >
              {nombre ? nombre.charAt(0).toUpperCase() : <HospitalIcon sx={{ fontSize: 36 }} />}
            </Avatar>
            <Box flex={1}>
              <Typography variant="subtitle2" fontWeight={700} color="primary">
                Vista Previa de Foto de Perfil
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Subí una foto desde tu PC o ingresá una URL pública directa.
              </Typography>
            </Box>
          </Box>

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

            <Grid item xs={12}>
              <TextField
                fullWidth
                type="email"
                label="Correo Electrónico del Médico"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                helperText="Se enviará a esta casilla una invitación segura con un enlace para que configure su clave de acceso (expira en 24hs)."
              />
            </Grid>

            <Grid item xs={12}>
              <Box display="flex" gap={1.5} alignItems="flex-start">
                <TextField
                  fullWidth
                  label="URL manual de la imagen (Opcional)"
                  placeholder="https://ejemplo.com/foto.jpg"
                  value={fotoUrl}
                  onChange={(e) => {
                    setFotoUrl(e.target.value);
                    if (!selectedFile) setPreviewUrl('');
                  }}
                  helperText="Podés pegar un enlace directo o seleccionar un archivo a continuación"
                />
                <Button
                  variant="outlined"
                  component="label"
                  sx={{ minWidth: 170, height: 56, whiteSpace: 'nowrap', fontWeight: 600 }}
                >
                  Subir Foto
                  <input
                    type="file"
                    accept="image/*"
                    hidden
                    onChange={(e) => {
                      const file = e.target.files[0];
                      if (!file) return;
                      if (file.size > 8 * 1024 * 1024) {
                        setError('La imagen seleccionada supera el tamaño máximo permitido de 8MB.');
                        return;
                      }
                      setSelectedFile(file);
                      setPreviewUrl(URL.createObjectURL(file));
                    }}
                  />
                </Button>
              </Box>
            </Grid>

            <Grid item xs={12}>
              <FormLabel component="legend" sx={{ fontWeight: 600, color: 'text.primary', mt: 1, mb: 1 }}>
                Seleccionar Especialidades Asignadas:
              </FormLabel>
              <FormGroup row>
                {especialidades.map((esp) => (
                  <FormControlLabel
                    key={esp.id}
                    control={
                      <Checkbox
                        checked={selectedEspecialidadIds.includes(esp.id)}
                        onChange={() => handleCheckboxChange(esp.id)}
                      />
                    }
                    label={esp.nombre}
                  />
                ))}
              </FormGroup>
            </Grid>
          </Grid>

          <Button
            type="submit"
            variant="contained"
            size="large"
            fullWidth
            startIcon={<PersonAddIcon />}
            disabled={loading || !email || !nombre || !apellido}
            sx={{ mt: 4 }}
          >
            {loading ? <CircularProgress size={24} color="inherit" /> : 'Registrar Profesional y Enviar Invitación'}
          </Button>
        </Box>
      </Paper>
    </Box>
  );
};

export default AdminNuevoDoctor;
