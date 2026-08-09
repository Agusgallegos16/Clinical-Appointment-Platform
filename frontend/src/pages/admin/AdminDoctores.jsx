import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Button,
  Chip,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  TextField,
  FormGroup,
  FormControlLabel,
  Checkbox,
  FormLabel,
  IconButton,
} from '@mui/material';
import {
  PersonAdd as PersonAddIcon,
  LocalHospital as HospitalIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material';
import { doctorService } from '../../api/doctorService';
import { especialidadService } from '../../api/especialidadService';

const AdminDoctores = () => {
  const [doctores, setDoctores] = useState([]);
  const [especialidades, setEspecialidades] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const navigate = useNavigate();

  // Modal Editar Doctor
  const [selectedDoctorEdit, setSelectedDoctorEdit] = useState(null);
  const [editFormData, setEditFormData] = useState({ nombre: '', apellido: '', email: '' });
  const [editEspecialidadIds, setEditEspecialidadIds] = useState([]);
  const [updating, setUpdating] = useState(false);

  // Modal Eliminar Doctor
  const [selectedDoctorDelete, setSelectedDoctorDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    cargarDoctores();
    cargarEspecialidades();
  }, []);

  const cargarDoctores = async () => {
    try {
      const data = await doctorService.listarDoctores();
      setDoctores(data);
    } catch (err) {
      setError('Error al obtener la lista de profesionales.');
    } finally {
      setLoading(false);
    }
  };

  const cargarEspecialidades = async () => {
    try {
      const data = await especialidadService.listarTodas();
      setEspecialidades(data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleOpenEditModal = (doc) => {
    setSelectedDoctorEdit(doc);
    setEditFormData({
      nombre: doc.nombre,
      apellido: doc.apellido,
      email: doc.usuario?.email || '',
    });
    setEditEspecialidadIds(doc.especialidades?.map((e) => e.id) || []);
  };

  const handleCheckboxChange = (espId) => {
    if (editEspecialidadIds.includes(espId)) {
      setEditEspecialidadIds(editEspecialidadIds.filter((id) => id !== espId));
    } else {
      setEditEspecialidadIds([...editEspecialidadIds, espId]);
    }
  };

  const handleSaveEditDoctor = async () => {
    if (!selectedDoctorEdit) return;
    setUpdating(true);
    setError('');
    setSuccess('');

    try {
      await doctorService.actualizarDoctor(selectedDoctorEdit.id, {
        nombre: editFormData.nombre,
        apellido: editFormData.apellido,
        email: editFormData.email,
        especialidadIds: editEspecialidadIds,
      });

      setSuccess(`¡Datos del Dr/a. ${editFormData.apellido} actualizados correctamente!`);
      setSelectedDoctorEdit(null);
      cargarDoctores();
    } catch (err) {
      let msg = 'Error al actualizar el profesional.';
      if (err.response?.data?.mensaje) msg = err.response.data.mensaje;
      else if (err.response?.data?.message) msg = err.response.data.message;
      else if (err.response?.data?.detalles) {
        const d = err.response.data.detalles;
        msg = typeof d === 'object' ? Object.values(d).join(', ') : String(d);
      }
      setError(msg);
    } finally {
      setUpdating(false);
    }
  };

  const handleConfirmEliminarDoctor = async () => {
    if (!selectedDoctorDelete) return;
    setDeleting(true);
    setError('');
    setSuccess('');

    try {
      await doctorService.eliminarDoctor(selectedDoctorDelete.id);
      setSuccess(`Doctor/a Dr. ${selectedDoctorDelete.nombre} ${selectedDoctorDelete.apellido} eliminado con éxito.`);
      setSelectedDoctorDelete(null);
      cargarDoctores();
    } catch (err) {
      setError(err.response?.data?.mensaje || err.response?.data?.message || 'No se pudo eliminar el doctor.');
      setSelectedDoctorDelete(null);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box>
          <Typography variant="h4" fontWeight={700} color="primary" gutterBottom>
            Nómina de Profesionales Médicos
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Lista completa de doctores activos en el consultorio.
          </Typography>
        </Box>

        <Button
          variant="contained"
          startIcon={<PersonAddIcon />}
          onClick={() => navigate('/admin/doctores/nuevo')}
        >
          Registrar Nuevo Doctor
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

      {loading ? (
        <CircularProgress />
      ) : (
        <Grid container spacing={2.5}>
          {doctores.map((doc) => (
            <Grid item xs={12} sm={6} md={4} key={doc.id}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                <CardContent>
                  <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={1}>
                    <Box display="flex" alignItems="center" gap={1.5}>
                      <HospitalIcon color="primary" sx={{ fontSize: 32 }} />
                      <Box>
                        <Typography variant="subtitle1" fontWeight={700}>
                          Dr/a. {doc.nombre} {doc.apellido}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          📧 {doc.usuario?.email}
                        </Typography>
                      </Box>
                    </Box>
                    <IconButton
                      color="error"
                      size="small"
                      onClick={() => setSelectedDoctorDelete(doc)}
                      title="Eliminar médico"
                    >
                      <DeleteIcon />
                    </IconButton>
                  </Box>

                  <Typography variant="caption" fontWeight={600} display="block" mt={1.5} mb={0.5}>
                    Especialidades:
                  </Typography>
                  <Box display="flex" flexWrap="wrap" gap={0.5} mb={2}>
                    {doc.especialidades?.map((esp) => (
                      <Chip key={esp.id} label={esp.nombre} size="small" color="primary" variant="outlined" />
                    ))}
                  </Box>
                </CardContent>

                <Box p={2} pt={0}>
                  <Button
                    variant="outlined"
                    size="small"
                    fullWidth
                    startIcon={<EditIcon />}
                    onClick={() => handleOpenEditModal(doc)}
                  >
                    Editar Doctor
                  </Button>
                </Box>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Modal Editar Doctor */}
      <Dialog open={!!selectedDoctorEdit} onClose={() => setSelectedDoctorEdit(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Editar Datos del Profesional</DialogTitle>
        <DialogContent dividers>
          <TextField
            fullWidth
            margin="normal"
            label="Nombre"
            value={editFormData.nombre}
            onChange={(e) => setEditFormData({ ...editFormData, nombre: e.target.value })}
          />
          <TextField
            fullWidth
            margin="normal"
            label="Apellido"
            value={editFormData.apellido}
            onChange={(e) => setEditFormData({ ...editFormData, apellido: e.target.value })}
          />
          <TextField
            fullWidth
            margin="normal"
            label="Correo Electrónico (Email)"
            type="email"
            value={editFormData.email}
            onChange={(e) => setEditFormData({ ...editFormData, email: e.target.value })}
          />

          <Box mt={2}>
            <FormLabel component="legend" sx={{ fontWeight: 600, color: 'text.primary', mb: 1 }}>
              Especialidades Asignadas:
            </FormLabel>
            <FormGroup row>
              {especialidades.map((esp) => (
                <FormControlLabel
                  key={esp.id}
                  control={
                    <Checkbox
                      checked={editEspecialidadIds.includes(esp.id)}
                      onChange={() => handleCheckboxChange(esp.id)}
                    />
                  }
                  label={esp.nombre}
                />
              ))}
            </FormGroup>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedDoctorEdit(null)}>Cancelar</Button>
          <Button onClick={handleSaveEditDoctor} variant="contained" disabled={updating}>
            {updating ? <CircularProgress size={20} color="inherit" /> : 'Guardar Cambios'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Modal Confirmar Eliminación de Doctor */}
      <Dialog open={!!selectedDoctorDelete} onClose={() => setSelectedDoctorDelete(null)}>
        <DialogTitle>¿Eliminar al Dr/a. {selectedDoctorDelete?.nombre} {selectedDoctorDelete?.apellido}?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Esta acción eliminará la cuenta del médico, sus horarios configurados y credenciales de acceso.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedDoctorDelete(null)}>Cancelar</Button>
          <Button onClick={handleConfirmEliminarDoctor} color="error" variant="contained" disabled={deleting}>
            {deleting ? <CircularProgress size={20} color="inherit" /> : 'Sí, Eliminar Doctor'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default AdminDoctores;
