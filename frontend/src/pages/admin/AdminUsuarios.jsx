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
  TablePagination,
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
  const [searchQuery, setSearchQuery] = useState('');
  const [usuarios, setUsuarios] = useState([]);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  // Estados de paginación
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(15);
  const [totalElements, setTotalElements] = useState(0);

  const [alertInfo, setAlertInfo] = useState({ open: false, type: 'success', message: '' });

  // Estado para modal de confirmación de eliminación
  const [selectedUser, setSelectedUser] = useState(null);
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);

  const fetchUsuarios = async (queryParam = '', targetPage = page, targetSize = rowsPerPage) => {
    setLoading(true);
    try {
      const data = await adminService.buscarUsuarios(queryParam, targetPage, targetSize);
      if (data && data.content) {
        setUsuarios(data.content);
        setTotalElements(data.totalElements || 0);
      } else if (Array.isArray(data)) {
        setUsuarios(data);
        setTotalElements(data.length);
      } else {
        setUsuarios([]);
        setTotalElements(0);
      }
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
    fetchUsuarios('', 0, rowsPerPage);
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    fetchUsuarios(searchQuery, 0, rowsPerPage);
  };

  const handleClearSearch = () => {
    setSearchQuery('');
    setPage(0);
    fetchUsuarios('', 0, rowsPerPage);
  };

  const handleChangePage = (event, newPage) => {
    setPage(newPage);
    fetchUsuarios(searchQuery, newPage, rowsPerPage);
  };

  const handleChangeRowsPerPage = (event) => {
    const newSize = parseInt(event.target.value, 10);
    setRowsPerPage(newSize);
    setPage(0);
    fetchUsuarios(searchQuery, 0, newSize);
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
      fetchUsuarios(searchQuery, page, rowsPerPage);
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

      // Si se eliminó el único elemento de una página superior a la primera, retroceder una página
      const targetPage = usuarios.length === 1 && page > 0 ? page - 1 : page;
      if (targetPage !== page) {
        setPage(targetPage);
      }
      fetchUsuarios(searchQuery, targetPage, rowsPerPage);
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
      {/* Botones Superiores */}
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate('/admin')}
          sx={{ fontWeight: 600 }}
        >
          Volver al Panel de Inicio
        </Button>
      </Stack>

      <Typography variant="h4" fontWeight={700} color="primary" gutterBottom>
        Gestión de Usuarios 🛡️
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        Busca a cualquier usuario por nombre, apellido o correo electrónico.
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

      {/* Barra de Búsqueda por Nombre, Apellido o Email */}
      <Paper elevation={0} variant="outlined" sx={{ p: 2.5, mb: 4, borderRadius: 3 }}>
        <form onSubmit={handleSearch}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
            <TextField
              fullWidth
              variant="outlined"
              placeholder="Buscar por nombre, apellido o correo electrónico"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <PersonSearchIcon color="action" />
                  </InputAdornment>
                ),
                endAdornment: searchQuery && (
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
          <>
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
            <TablePagination
              rowsPerPageOptions={[15, 30, 50]}
              component="div"
              count={totalElements}
              rowsPerPage={rowsPerPage}
              page={page}
              onPageChange={handleChangePage}
              onRowsPerPageChange={handleChangeRowsPerPage}
              labelRowsPerPage="Usuarios por página:"
              labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count !== -1 ? count : `más de ${to}`}`}
              sx={{ borderTop: 1, borderColor: 'divider' }}
            />
          </>
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
