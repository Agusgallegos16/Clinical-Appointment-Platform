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
  RadioGroup,
  Radio,
  FormControl,
} from '@mui/material';
import {
  PersonAdd as PersonAddIcon,
  ArrowBack as ArrowBackIcon,
  LocalHospital as HospitalIcon,
  Badge as BadgeIcon,
  Person as PersonIcon,
} from '@mui/icons-material';
import { adminService } from '../../api/adminService';
import { especialidadService } from '../../api/especialidadService';
import { uploadDoctorAvatar } from '../../api/storageService';

const AdminNuevoUsuario = () => {
  const navigate = useNavigate();

  // Rol seleccionado: 'DOCTOR', 'SECRETARIA', 'PACIENTE'
  const [rol, setRol] = useState('DOCTOR');

  // Campos comunes
  const [email, setEmail] = useState('');
  const [nombre, setNombre] = useState('');
  const [apellido, setApellido] = useState('');
  const [dni, setDni] = useState('');
  const [telefono, setTelefono] = useState('');

  // Campos específicos de Paciente
  const [fechaNacimiento, setFechaNacimiento] = useState('');

  // Campos específicos de Doctor
  const [especialidades, setEspecialidades] = useState([]);
  const [selectedEspecialidadIds, setSelectedEspecialidadIds] = useState([]);
  const [fotoUrl, setFotoUrl] = useState('');
  const [previewUrl, setPreviewUrl] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);

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
      console.error('Error al cargar especialidades:', err);
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
      let finalFotoUrl = fotoUrl;

      if (rol === 'DOCTOR' && selectedFile) {
        const uploadedUrl = await uploadDoctorAvatar(selectedFile);
        if (!uploadedUrl) {
          setError('Error al subir la imagen de perfil. Revisa la consola.');
          setLoading(false);
          return;
        }
        finalFotoUrl = uploadedUrl;
      }

      const payload = {
        rol,
        email: email.trim(),
        nombre: nombre.trim(),
        apellido: apellido.trim(),
        dni: dni ? Number(dni) : null,
        telefono: telefono.trim() || null,
        fechaNacimiento: rol === 'PACIENTE' && fechaNacimiento ? fechaNacimiento : null,
        especialidadIds: rol === 'DOCTOR' ? selectedEspecialidadIds : null,
        fotoUrl: rol === 'DOCTOR' ? finalFotoUrl : null,
      };

      await adminService.registrarUsuario(payload);

      const rolTexto = rol === 'DOCTOR' ? 'Doctor/a' : rol === 'SECRETARIA' ? 'Secretaria/o' : 'Paciente';
      setSuccess(`¡${rolTexto} ${nombre} ${apellido} registrado/a con éxito! Se envió una invitación por mail a ${email} para configurar la clave.`);

      setTimeout(() => {
        navigate(rol === 'DOCTOR' ? '/admin/doctores' : '/admin/usuarios');
      }, 2200);
    } catch (err) {
      let msg = 'Error al registrar al usuario en el sistema.';
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

  const getIconoRol = () => {
    switch (rol) {
      case 'DOCTOR': return <HospitalIcon sx={{ fontSize: 32 }} />;
      case 'SECRETARIA': return <BadgeIcon sx={{ fontSize: 32 }} />;
      default: return <PersonIcon sx={{ fontSize: 32 }} />;
    }
  };

  return (
    <Box maxWidth="750px" mx="auto">
      <Button
        startIcon={<ArrowBackIcon />}
        onClick={() => navigate('/admin')}
        sx={{ mb: 2, fontWeight: 600 }}
      >
        Volver al Panel Admin
      </Button>

      <Paper sx={{ p: 4, borderRadius: 3 }}>
        <Typography variant="h5" fontWeight={700} color="primary" mb={1}>
          Registrar Usuario en el Sistema 🛡️
        </Typography>
        <Typography variant="body2" color="text.secondary" mb={3}>
          Seleccioná el rol del usuario e ingresá sus datos personales. Se le enviará automáticamente un correo electrónico con un enlace seguro para que configure su contraseña de acceso.
        </Typography>

        {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
        {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

        <Box component="form" onSubmit={handleSubmit}>
          {/* Selección de Rol */}
          <Paper variant="outlined" sx={{ p: 2.5, mb: 3.5, borderRadius: 2, bgcolor: 'action.hover' }}>
            <FormLabel component="legend" sx={{ fontWeight: 700, color: 'text.primary', mb: 1 }}>
              Seleccionar Rol del Usuario a Registrar:
            </FormLabel>
            <FormControl component="fieldset">
              <RadioGroup
                row
                value={rol}
                onChange={(e) => setRol(e.target.value)}
              >
                <FormControlLabel
                  value="DOCTOR"
                  control={<Radio color="primary" />}
                  label={
                    <Box display="flex" alignItems="center" gap={0.5}>
                      <Typography fontWeight={700}>🩺 Doctor / Médico</Typography>
                    </Box>
                  }
                />
                <FormControlLabel
                  value="SECRETARIA"
                  control={<Radio color="primary" />}
                  label={
                    <Box display="flex" alignItems="center" gap={0.5}>
                      <Typography fontWeight={700}>👩‍💼 Secretaría</Typography>
                    </Box>
                  }
                />
                <FormControlLabel
                  value="PACIENTE"
                  control={<Radio color="primary" />}
                  label={
                    <Box display="flex" alignItems="center" gap={0.5}>
                      <Typography fontWeight={700}>👤 Paciente</Typography>
                    </Box>
                  }
                />
              </RadioGroup>
            </FormControl>
          </Paper>

          {/* Banner de Avatar (Si es DOCTOR) */}
          {rol === 'DOCTOR' && (
            <Box display="flex" alignItems="center" gap={2.5} mb={3} p={2} bgcolor="background.default" borderRadius={2} border="1px solid" borderColor="divider">
              <Avatar
                src={previewUrl || fotoUrl}
                alt={`${nombre} ${apellido}`}
                sx={{ width: 72, height: 72, bgcolor: 'primary.main', fontSize: '1.8rem', fontWeight: 700 }}
              >
                {nombre ? nombre.charAt(0).toUpperCase() : getIconoRol()}
              </Avatar>
              <Box flex={1}>
                <Typography variant="subtitle2" fontWeight={700} color="primary">
                  Foto de Perfil del Médico (Opcional)
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Podés subir un archivo de imagen o ingresar una URL pública.
                </Typography>
              </Box>
            </Box>
          )}

          {/* Formulario de Campos */}
          <Grid container spacing={2.5}>
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
                label="Teléfono de Contacto"
                value={telefono}
                onChange={(e) => setTelefono(e.target.value)}
                required
                placeholder="Ej: 11 2233-4455"
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                type="email"
                label="Correo Electrónico"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                helperText="Se enviará a esta casilla la invitación con el enlace seguro para configurar su contraseña de acceso."
              />
            </Grid>

            {/* Campo Específico: PACIENTE */}
            {rol === 'PACIENTE' && (
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
            )}

            {/* Campos Específicos: DOCTOR */}
            {rol === 'DOCTOR' && (
              <>
                <Grid item xs={12}>
                  <Box display="flex" gap={1.5} alignItems="flex-start">
                    <TextField
                      fullWidth
                      label="URL de Foto de Perfil (Opcional)"
                      placeholder="https://ejemplo.com/foto.jpg"
                      value={fotoUrl}
                      onChange={(e) => {
                        setFotoUrl(e.target.value);
                        if (!selectedFile) setPreviewUrl('');
                      }}
                    />
                    <Button
                      variant="outlined"
                      component="label"
                      sx={{ minWidth: 160, height: 56, whiteSpace: 'nowrap', fontWeight: 600 }}
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
                            setError('La imagen seleccionada supera los 8MB.');
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
              </>
            )}
          </Grid>

          <Button
            type="submit"
            variant="contained"
            size="large"
            fullWidth
            startIcon={loading ? <CircularProgress size={24} color="inherit" /> : <PersonAddIcon />}
            disabled={loading || !email || !nombre || !apellido || !dni}
            sx={{ mt: 4, py: 1.8, fontWeight: 700, borderRadius: 2 }}
          >
            Registrar Usuario y Enviar Invitación
          </Button>
        </Box>
      </Paper>
    </Box>
  );
};

export default AdminNuevoUsuario;
