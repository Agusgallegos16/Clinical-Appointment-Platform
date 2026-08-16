import React, { useEffect, useState } from 'react';
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
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
} from '@mui/material';
import {
  Add as AddIcon,
  MedicalServices as MedicalIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
} from '@mui/icons-material';
import { especialidadService } from '../../api/especialidadService';

const AdminEspecialidades = () => {
  const [especialidades, setEspecialidades] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');

  // Modal Editar
  const [selectedEdit, setSelectedEdit] = useState(null);
  const [editNombre, setEditNombre] = useState('');
  const [editDescripcion, setEditDescripcion] = useState('');
  const [updating, setUpdating] = useState(false);

  // Modal Eliminar
  const [selectedDelete, setSelectedDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    cargarEspecialidades();
  }, []);

  const cargarEspecialidades = async () => {
    try {
      const data = await especialidadService.listarTodas();
      setEspecialidades(data);
    } catch (err) {
      setError('Error al obtener la lista de especialidades.');
    } finally {
      setLoading(false);
    }
  };

  const handleCrearEspecialidad = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setSuccess('');

    try {
      await especialidadService.crear({ nombre, descripcion });
      setSuccess(`¡Especialidad "${nombre}" creada exitosamente!`);
      setNombre('');
      setDescripcion('');
      cargarEspecialidades();
    } catch (err) {
      setError(err.response?.data?.mensaje || err.response?.data?.message || 'Error al crear la especialidad.');
    } finally {
      setSaving(false);
    }
  };

  const handleOpenEditModal = (esp) => {
    setSelectedEdit(esp);
    setEditNombre(esp.nombre || '');
    setEditDescripcion(esp.descripcion || '');
  };

  const handleGuardarEdicion = async (e) => {
    e.preventDefault();
    if (!selectedEdit) return;
    setUpdating(true);
    setError('');
    setSuccess('');

    try {
      await especialidadService.actualizar(selectedEdit.id, {
        nombre: editNombre,
        descripcion: editDescripcion,
      });
      setSuccess(`¡Especialidad "${editNombre}" actualizada exitosamente!`);
      setSelectedEdit(null);
      cargarEspecialidades();
    } catch (err) {
      setError(err.response?.data?.mensaje || err.response?.data?.message || 'Error al actualizar la especialidad.');
    } finally {
      setUpdating(false);
    }
  };

  const handleConfirmEliminar = async () => {
    if (!selectedDelete) return;
    setDeleting(true);
    setError('');
    setSuccess('');
    try {
      await especialidadService.eliminar(selectedDelete.id);
      setSuccess(`Especialidad "${selectedDelete.nombre}" eliminada con éxito.`);
      setSelectedDelete(null);
      cargarEspecialidades();
    } catch (err) {
      const msg = err.response?.data?.mensaje || err.response?.data?.message || 'No se pudo eliminar la especialidad.';
      setError(msg);
      setSelectedDelete(null);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} color="primary" mb={1}>
        Catálogo de Especialidades Médicas
      </Typography>
      <Typography variant="body1" color="text.secondary" mb={3}>
        Administrá las disciplinas médicas ofrecidas en el centro de salud.
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

      <Grid container spacing={3}>
        {/* Formulario Nueva Especialidad */}
        <Grid item xs={12} md={5}>
          <Paper sx={{ p: 3, borderRadius: 3, boxShadow: '0 4px 20px rgba(0,0,0,0.06)' }}>
            <Typography variant="h6" fontWeight={600} mb={2}>
              Dar de Alta Especialidad
            </Typography>

            <Box component="form" onSubmit={handleCrearEspecialidad}>
              <TextField
                fullWidth
                margin="normal"
                label="Nombre de la Especialidad"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                placeholder="Ej: Traumatología"
                required
              />

              <TextField
                fullWidth
                margin="normal"
                multiline
                rows={3}
                label="Descripción"
                value={descripcion}
                onChange={(e) => setDescripcion(e.target.value)}
                placeholder="Descripción del campo de atención..."
                required
              />

              <Button
                type="submit"
                variant="contained"
                fullWidth
                size="large"
                startIcon={<AddIcon />}
                disabled={saving || !nombre}
                sx={{ mt: 2 }}
              >
                {saving ? <CircularProgress size={24} color="inherit" /> : 'Crear Especialidad'}
              </Button>
            </Box>
          </Paper>
        </Grid>

        {/* Lista de Especialidades */}
        <Grid item xs={12} md={7}>
          <Paper sx={{ p: 3, borderRadius: 3, boxShadow: '0 4px 20px rgba(0,0,0,0.06)' }}>
            <Typography variant="h6" fontWeight={600} mb={2}>
              Especialidades Existentes ({especialidades.length})
            </Typography>

            {loading ? (
              <CircularProgress />
            ) : (
              <Grid container spacing={2}>
                {especialidades.map((esp) => (
                  <Grid item xs={12} key={esp.id}>
                    <Card variant="outlined" sx={{ borderRadius: 2.5, transition: '0.2s', '&:hover': { borderColor: 'primary.main' } }}>
                      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                        <Box display="flex" justifyContent="space-between" alignItems="center">
                          <Box display="flex" alignItems="center" gap={1.5}>
                            <MedicalIcon color="primary" />
                            <Box>
                              <Typography variant="subtitle1" fontWeight={700}>
                                {esp.nombre}
                              </Typography>
                              <Typography variant="body2" color="text.secondary">
                                {esp.descripcion}
                              </Typography>
                            </Box>
                          </Box>
                          <Box display="flex" gap={0.5}>
                            <IconButton
                              color="primary"
                              onClick={() => handleOpenEditModal(esp)}
                              title="Editar especialidad"
                            >
                              <EditIcon />
                            </IconButton>
                            <IconButton
                              color="error"
                              onClick={() => setSelectedDelete(esp)}
                              title="Eliminar especialidad"
                            >
                              <DeleteIcon />
                            </IconButton>
                          </Box>
                        </Box>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            )}
          </Paper>
        </Grid>
      </Grid>

      {/* Modal Editar Especialidad */}
      <Dialog open={!!selectedEdit} onClose={() => setSelectedEdit(null)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 700 }}>
          Editar Especialidad Médica ✏️
        </DialogTitle>
        <Box component="form" onSubmit={handleGuardarEdicion}>
          <DialogContent dividers>
            <TextField
              fullWidth
              margin="normal"
              label="Nombre de la Especialidad"
              value={editNombre}
              onChange={(e) => setEditNombre(e.target.value)}
              required
            />
            <TextField
              fullWidth
              margin="normal"
              multiline
              rows={3}
              label="Descripción"
              value={editDescripcion}
              onChange={(e) => setEditDescripcion(e.target.value)}
              required
            />
          </DialogContent>
          <DialogActions sx={{ p: 2 }}>
            <Button onClick={() => setSelectedEdit(null)}>Cancelar</Button>
            <Button
              type="submit"
              variant="contained"
              color="primary"
              disabled={updating || !editNombre}
            >
              {updating ? <CircularProgress size={20} color="inherit" /> : 'Guardar Cambios'}
            </Button>
          </DialogActions>
        </Box>
      </Dialog>

      {/* Modal Confirmación Eliminación */}
      <Dialog open={!!selectedDelete} onClose={() => setSelectedDelete(null)}>
        <DialogTitle>¿Eliminar la especialidad "{selectedDelete?.nombre}"?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Esta acción eliminará la especialidad médica del catálogo.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedDelete(null)}>Cancelar</Button>
          <Button onClick={handleConfirmEliminar} color="error" variant="contained" disabled={deleting}>
            {deleting ? <CircularProgress size={20} color="inherit" /> : 'Sí, Eliminar'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default AdminEspecialidades;
