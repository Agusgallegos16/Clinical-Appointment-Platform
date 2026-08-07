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
} from '@mui/material';
import { PersonAdd as PersonAddIcon, ArrowBack as ArrowBackIcon } from '@mui/icons-material';
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
        especialidadIds: selectedEspecialidadIds,
      });

      setSuccess(`¡Doctor/a Dr. ${nombre} ${apellido} registrado con éxito!`);
      setTimeout(() => {
        navigate('/admin/doctores');
      }, 1500);
    } catch (err) {
      setError(err.response?.data?.mensaje || err.response?.data?.message || 'Error al registrar al profesional.');
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
          Completá las credenciales y especialidades para dar de alta un nuevo profesional.
        </Typography>

        {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
        {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

        <Box component="form" onSubmit={handleSubmit}>
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
                label="Correo Electrónico (Usuario)"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                type="password"
                label="Contraseña Inicial"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                helperText="Mínimo 6 caracteres"
              />
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
