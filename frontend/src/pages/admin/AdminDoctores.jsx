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
  Avatar,
} from '@mui/material';
import {
  PersonAdd as PersonAddIcon,
  LocalHospital as HospitalIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material';
import { doctorService } from '../../api/doctorService';
import { especialidadService } from '../../api/especialidadService';
import { uploadDoctorAvatar } from '../../api/storageService';

const AdminDoctores = () => {
  const [doctores, setDoctores] = useState([]);
  const [especialidades, setEspecialidades] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const navigate = useNavigate();

  // Modal Editar Doctor
  const [selectedDoctorEdit, setSelectedDoctorEdit] = useState(null);
  const [editFormData, setEditFormData] = useState({ nombre: '', apellido: '', email: '', fotoUrl: '' });
  const [editPreviewUrl, setEditPreviewUrl] = useState('');
  const [editSelectedFile, setEditSelectedFile] = useState(null);
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
      fotoUrl: doc.fotoUrl || '',
    });
    setEditPreviewUrl(doc.fotoUrl || '');
    setEditSelectedFile(null);
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
      let finalFotoUrl = editFormData.fotoUrl;

      if (editSelectedFile) {
        console.log('Iniciando subida a Supabase Storage...');
        const uploadedUrl = await uploadDoctorAvatar(editSelectedFile, selectedDoctorEdit.id);

        if (!uploadedUrl) {
          setError('Error al subir la imagen a Supabase Storage. Revisa la consola (F12).');
          setUpdating(false);
          return; // Detener el guardado si falló la subida
        }

        finalFotoUrl = uploadedUrl;
        console.log('URL Pública obtenida de Supabase:', finalFotoUrl);
      }

      await doctorService.actualizarDoctor(selectedDoctorEdit.id, {
        nombre: editFormData.nombre,
        apellido: editFormData.apellido,
        email: editFormData.email,
        fotoUrl: finalFotoUrl,
        especialidadIds: editEspecialidadIds,
      });

      setSuccess(`¡Datos del Dr/a. ${editFormData.apellido} actualizados correctamente!`);
      setSelectedDoctorEdit(null);
      setEditSelectedFile(null);
      setEditPreviewUrl('');
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
                      <Avatar
                        src={doc.fotoUrl}
                        alt={`${doc.nombre} ${doc.apellido}`}
                        sx={{ width: 50, height: 50, bgcolor: 'primary.main', fontWeight: 700 }}
                      >
                        {doc.nombre ? doc.nombre.charAt(0).toUpperCase() : <HospitalIcon />}
                      </Avatar>
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
          <Box display="flex" alignItems="center" gap={2} mb={2}>
            <Avatar
              src={editPreviewUrl || editFormData.fotoUrl}
              alt={`${editFormData.nombre} ${editFormData.apellido}`}
              sx={{ width: 64, height: 64, bgcolor: 'primary.main', fontSize: '1.5rem', fontWeight: 700 }}
            >
              {editFormData.nombre ? editFormData.nombre.charAt(0).toUpperCase() : <HospitalIcon />}
            </Avatar>
            <Typography variant="body2" color="text.secondary">
              Previsualización de la foto de perfil del profesional.
            </Typography>
          </Box>

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
          <Box display="flex" gap={1.5} alignItems="flex-start" mt={2}>
            <TextField
              fullWidth
              label="URL manual de la imagen (Opcional)"
              placeholder="https://ejemplo.com/foto.jpg"
              value={editFormData.fotoUrl}
              onChange={(e) => {
                setEditFormData({ ...editFormData, fotoUrl: e.target.value });
                if (!editSelectedFile) setEditPreviewUrl('');
              }}
              helperText="Podés pegar una URL directa o seleccionar una foto de tu equipo"
            />
            <Button
              variant="outlined"
              component="label"
              sx={{ minWidth: 150, height: 56, whiteSpace: 'nowrap', fontWeight: 600 }}
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
                  setEditSelectedFile(file);
                  setEditPreviewUrl(URL.createObjectURL(file));
                }}
              />
            </Button>
          </Box>

          <FormLabel component="legend" sx={{ fontWeight: 600, mt: 2, mb: 1 }}>
            Especialidades Médicas:
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
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedDoctorEdit(null)}>Cancelar</Button>
          <Button onClick={handleSaveEditDoctor} variant="contained" disabled={updating}>
            {updating ? <CircularProgress size={20} color="inherit" /> : 'Guardar Cambios'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Modal Confirmar Eliminación */}
      <Dialog open={!!selectedDoctorDelete} onClose={() => setSelectedDoctorDelete(null)}>
        <DialogTitle>¿Eliminar a este profesional?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Vas a dar de baja al Dr/a. <strong>{selectedDoctorDelete?.nombre} {selectedDoctorDelete?.apellido}</strong>. Esta acción eliminará sus horarios y turnos asociados.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedDoctorDelete(null)}>Cancelar</Button>
          <Button onClick={handleConfirmEliminarDoctor} color="error" variant="contained" disabled={deleting}>
            {deleting ? <CircularProgress size={20} color="inherit" /> : 'Sí, Eliminar'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default AdminDoctores;
