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

const AdminNuevoDoctor = () => {
  const navigate = useNavigate();
  const [especialidades, setEspecialidades] = useState([]);

  // Form states
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [nombre, setNombre] = useState('');
  const [apellido, setApellido] = useState('');
  const [fotoUrl, setFotoUrl] = useState('');
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
      await authService.registrarDoctor({
        email,
        password,
        nombre,
        apellido,
        fotoUrl,
        especialidadIds: selectedEspecialidadIds,
      });

      setSuccess(`¡Doctor/a Dr. ${nombre} ${apellido} registrado con éxito!`);
      setTimeout(() => {
        navigate('/admin/doctores');
      }, 1500);
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
          Completá las credenciales, foto de perfil y especialidades para dar de alta un nuevo profesional.
        </Typography>

        {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
        {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

        <Box component="form" onSubmit={handleSubmit}>
          {/* Previsualización de Avatar */}
          <Box display="flex" alignItems="center" gap={2.5} mb={3} p={2} bgcolor="background.default" borderRadius={2}>
            <Avatar
              src={fotoUrl}
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
                Ingresá la URL pública de la imagen del profesional para que sea visible en el catálogo de pacientes.
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

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                type="email"
                label="Correo Electrónico"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                type="password"
                label="Contraseña"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                helperText="Mínimo 6 caracteres"
              />
            </Grid>

            <Grid item xs={12}>
              <Box display="flex" gap={1.5} alignItems="flex-start">
                <TextField
                  fullWidth
                  label="URL o Foto de Perfil (JPEG, PNG, WebP)"
                  placeholder="https://ejemplo.com/foto.jpg o subir archivo local"
                  value={fotoUrl}
                  onChange={(e) => setFotoUrl(e.target.value)}
                  helperText="Podés pegar un enlace directo a una imagen JPEG/PNG o subir una foto desde tu PC"
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
                      const reader = new FileReader();
                      reader.onload = (event) => {
                        const img = new Image();
                        img.onload = () => {
                          const canvas = document.createElement('canvas');
                          const maxDim = 800;
                          let width = img.width;
                          let height = img.height;
                          if (width > height) {
                            if (width > maxDim) {
                              height = Math.round((height * maxDim) / width);
                              width = maxDim;
                            }
                          } else {
                            if (height > maxDim) {
                              width = Math.round((width * maxDim) / height);
                              height = maxDim;
                            }
                          }
                          canvas.width = width;
                          canvas.height = height;
                          const ctx = canvas.getContext('2d');
                          ctx.imageSmoothingEnabled = true;
                          ctx.imageSmoothingQuality = 'high';
                          ctx.drawImage(img, 0, 0, width, height);
                          setFotoUrl(canvas.toDataURL('image/jpeg', 0.95));
                        };
                        img.src = event.target.result;
                      };
                      reader.readAsDataURL(file);
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
            disabled={loading || !email || !password || !nombre || !apellido}
            sx={{ mt: 4 }}
          >
            {loading ? <CircularProgress size={24} color="inherit" /> : 'Registrar Profesional'}
          </Button>
        </Box>
      </Paper>
    </Box>
  );
};

export default AdminNuevoDoctor;
