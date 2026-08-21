import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Chip,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Alert,
  CircularProgress,
  Stack,
  InputAdornment,
} from '@mui/material';
import {
  Search as SearchIcon,
  Block as BlockIcon,
  LockOpen as LockOpenIcon,
  DeleteForever as DeleteIcon,
  ArrowBack as ArrowBackIcon,
  Clear as ClearIcon,
  PersonSearch as PersonSearchIcon,
} from '@mui/icons-material';
import { adminService } from '../../api/adminService';

const AdminUsuarios = () => {
  const navigate = useNavigate();
  const [queryEmail, setQueryEmail] = useState('');
  const [usuarios, setUsuarios] = useState([]);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  const [alertInfo, setAlertInfo] = useState({ open: false, type: 'success', message: '' });

  // Estado para modal de confirmación de eliminación
  const [selectedUser, setSelectedUser] = useState(null);
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);

  const fetchUsuarios = async (emailParam = '') => {
    setLoading(true);
    try {
      const data = await adminService.buscarUsuarios(emailParam);
      setUsuarios(data);
    } catch (err) {
      setAlertInfo({
        open: true,
        type: 'error',
        message: err.response?.data?.message || 'Error al buscar los usuarios.',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsuarios('');
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    fetchUsuarios(queryEmail);
  };

  const handleClearSearch = () => {
    setQueryEmail('');
    fetchUsuarios('');
  };

  const handleToggleBloqueo = async (user) => {
    const nuevoEstado = !user.bloqueado;
    setActionLoading(true);
    try {
      await adminService.cambiarEstadoBloqueo(user.id, nuevoEstado);
      setAlertInfo({
        open: true,
        type: 'success',
        message: `El usuario ${user.email} fue ${nuevoEstado ? 'bloqueado' : 'desbloqueado'} con éxito.`,
      });
      fetchUsuarios(queryEmail);
    } catch (err) {
      setAlertInfo({
        open: true,
        type: 'error',
        message: err.response?.data?.message || 'No se pudo cambiar el estado del usuario.',
      });
    } finally {
      setActionLoading(false);
    }
  };

  const handleOpenDeleteModal = (user) => {
    setSelectedUser(user);
    setConfirmDeleteOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!selectedUser) return;
    setActionLoading(true);
    try {
      await adminService.eliminarUsuario(selectedUser.id);
      setAlertInfo({
        open: true,
        type: 'success',
        message: `El usuario ${selectedUser.email} ha sido eliminado definitivamente de la base de datos.`,
      });
      setConfirmDeleteOpen(false);
      setSelectedUser(null);
      fetchUsuarios(queryEmail);
    } catch (err) {
      setAlertInfo({
        open: true,
        type: 'error',
        message: err.response?.data?.message || 'No se pudo eliminar al usuario.',
      });
    } finally {
      setActionLoading(false);
    }
  };

  const getRolChipColor = (rol) => {
    switch (rol) {
      case 'ADMIN':
        return 'secondary';
      case 'DOCTOR':
        return 'primary';
      case 'SECRETARIA':
        return 'warning';
      case 'PACIENTE':
        return 'info';
      default:
        return 'default';
    }
  };

  return (
    <Box sx={{ maxWidth: 1000, mx: 'auto', py: 3, px: 2 }}>
      {/* Botón Volver al Dashboard Admin */}
      <Button
        startIcon={<ArrowBackIcon />}
        onClick={() => navigate('/admin')}
        sx={{ mb: 2, fontWeight: 600 }}
      >
        Volver al Panel de Inicio
      </Button>

      <Typography variant="h4" fontWeight={700} color="primary" gutterBottom>
        Gestión de Usuarios 🛡️
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        Busca a cualquier usuario por correo electrónico para bloquear su acceso a la plataforma o eliminarlo de la base de datos.
      </Typography>

      {alertInfo.open && (
        <Alert
          severity={alertInfo.type}
          onClose={() => setAlertInfo({ ...alertInfo, open: false })}
          sx={{ mb: 3 }}
        >
          {alertInfo.message}
        </Alert>
      )}

      {/* Bar de Búsqueda por Email */}
      <Paper elevation={0} variant="outlined" sx={{ p: 2.5, mb: 4, borderRadius: 3 }}>
        <form onSubmit={handleSearch}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
            <TextField
              fullWidth
              variant="outlined"
              placeholder="Ingresá el correo electrónico (ej: usuario@mail.com)..."
              value={queryEmail}
              onChange={(e) => setQueryEmail(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <PersonSearchIcon color="action" />
                  </InputAdornment>
                ),
                endAdornment: queryEmail && (
                  <InputAdornment position="end">
                    <IconButton size="small" onClick={handleClearSearch}>
                      <ClearIcon fontSize="small" />
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
            <Button
              type="submit"
              variant="contained"
              color="primary"
              size="large"
              startIcon={<SearchIcon />}
              sx={{ px: 3.5, py: 1.8, minWidth: 140, fontWeight: 700, borderRadius: 2 }}
            >
              Buscar
            </Button>
          </Stack>
        </form>
      </Paper>

      {/* Tabla de Resultados de Usuarios */}
      <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 3 }}>
        {loading ? (
          <Box display="flex" justifyContent="center" alignItems="center" py={6}>
            <CircularProgress />
          </Box>
        ) : usuarios.length === 0 ? (
          <Box textAlign="center" py={6}>
            <Typography variant="h6" color="text.secondary">
              No se encontraron usuarios que coincidan con la búsqueda.
            </Typography>
          </Box>
        ) : (
          <Table>
            <TableHead sx={{ backgroundColor: 'action.hover' }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 700 }}>ID</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Email</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Nombre y Apellido</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Rol</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Estado</TableCell>
                <TableCell align="center" sx={{ fontWeight: 700 }}>Acciones</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {usuarios.map((u) => (
                <TableRow key={u.id} hover>
                  <TableCell>{u.id}</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>{u.email}</TableCell>
                  <TableCell>
                    {u.nombre && u.apellido ? `${u.nombre} ${u.apellido}` : '—'}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={u.rol}
                      color={getRolChipColor(u.rol)}
                      size="small"
                      sx={{ fontWeight: 700 }}
                    />
                  </TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={1}>
                      {u.bloqueado ? (
                        <Chip label="Bloqueado" color="error" size="small" sx={{ fontWeight: 700 }} />
                      ) : (
                        <Chip label="Activo" color="success" size="small" sx={{ fontWeight: 700 }} />
                      )}
                      {!u.emailVerificado && (
                        <Chip label="Sin Verificar" color="warning" size="small" variant="outlined" />
                      )}
                    </Stack>
                  </TableCell>
                  <TableCell align="center">
                    <Stack direction="row" spacing={1} justifyContent="center">
                      {/* Botón Bloquear / Desbloquear */}
                      <Tooltip title={u.bloqueado ? 'Desbloquear usuario' : 'Bloquear usuario'}>
                        <IconButton
                          color={u.bloqueado ? 'success' : 'warning'}
                          onClick={() => handleToggleBloqueo(u)}
                          disabled={actionLoading}
                        >
                          {u.bloqueado ? <LockOpenIcon /> : <BlockIcon />}
                        </IconButton>
                      </Tooltip>

                      {/* Botón Eliminar */}
                      <Tooltip title="Eliminar usuario definitivamente">
                        <IconButton
                          color="error"
                          onClick={() => handleOpenDeleteModal(u)}
                          disabled={actionLoading}
                        >
                          <DeleteIcon />
                        </IconButton>
                      </Tooltip>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </TableContainer>

      {/* Modal Confirmar Eliminación */}
      <Dialog
        open={confirmDeleteOpen}
        onClose={() => setConfirmDeleteOpen(false)}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle sx={{ fontWeight: 700, color: 'error.main' }}>
          ⚠️ Confirmar Eliminación de Usuario
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            ¿Estás seguro de que deseas eliminar permanentemente a <strong>{selectedUser?.email}</strong> de la base de datos?
            <br /><br />
            Esta acción es irreversible y eliminará su cuenta y todos sus registros asociados.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setConfirmDeleteOpen(false)} disabled={actionLoading}>
            Cancelar
          </Button>
          <Button
            onClick={handleConfirmDelete}
            color="error"
            variant="contained"
            disabled={actionLoading}
            startIcon={actionLoading ? <CircularProgress size={20} /> : <DeleteIcon />}
            sx={{ fontWeight: 700 }}
          >
            Eliminar Definitivamente
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default AdminUsuarios;
