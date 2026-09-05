import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  Card,
  CardMedia,
  CardActions,
  Grid,
  CircularProgress,
  Alert,
  AlertTitle,
  IconButton,
  Container,
  Paper,
  Stack,
  Collapse,
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  CloudUpload as UploadIcon,
  Delete as DeleteIcon,
  Collections as GalleryIcon,
  Close as CloseIcon,
  CloudOff as CloudOffIcon,
} from '@mui/icons-material';

import {
  getInstitucionGallery,
  uploadInstitucionImage,
  deleteInstitucionImage,
} from '../../api/storageService';

const AdminGaleria = () => {
  const navigate = useNavigate();
  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState({ type: '', title: '', text: '' });

  const loadGallery = async () => {
    setLoading(true);
    try {
      const galleryData = await getInstitucionGallery();
      setImages(galleryData);
    } catch (err) {
      console.error('Error al cargar galería:', err);
      setMessage({
        type: 'error',
        title: 'Error de conexión',
        text: 'No se pudo conectar con el servicio de almacenamiento de Supabase Storage.',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadGallery();
  }, []);

  const handleFileUpload = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Validar tipo de archivo
    if (!file.type.startsWith('image/')) {
      setMessage({
        type: 'warning',
        title: 'Formato no soportado',
        text: 'Por favor seleccioná un archivo de imagen válido (JPG, PNG, WEBP, SVG).',
      });
      event.target.value = '';
      return;
    }

    // Validar tamaño máximo (5 MB)
    const MAX_SIZE_MB = 5;
    if (file.size > MAX_SIZE_MB * 1024 * 1024) {
      setMessage({
        type: 'warning',
        title: 'Archivo demasiado pesado',
        text: `La imagen elegida pesa ${(file.size / (1024 * 1024)).toFixed(1)} MB. El tamaño máximo permitido es de ${MAX_SIZE_MB} MB.`,
      });
      event.target.value = '';
      return;
    }

    setUploading(true);
    setMessage({ type: '', title: '', text: '' });

    try {
      const result = await uploadInstitucionImage(file);
      
      // Soporta tanto objeto devuelto { success, url, errorMsg } como URL string directa
      if (result && (result.success || typeof result === 'string')) {
        setMessage({
          type: 'success',
          title: '¡Imagen agregada con éxito!',
          text: 'La fotografía ya está disponible en la galería de la institución.',
        });
        await loadGallery();
      } else {
        const errorDetail = result?.errorMsg || 'No se pudo subir la foto a Supabase Storage.';
        setMessage({
          type: 'error',
          title: 'Error en la subida de la imagen',
          text: errorDetail,
        });
      }
    } catch (err) {
      console.error('Error al subir imagen:', err);
      setMessage({
        type: 'error',
        title: 'Fallo inesperado',
        text: 'Ocurrió un inconveniente durante el envío de la foto. Intentalo nuevamente.',
      });
    } finally {
      setUploading(false);
      event.target.value = '';
    }
  };

  const handleDelete = async (fileName) => {
    if (!window.confirm('¿Estás seguro de eliminar esta foto de la galería?')) return;

    setLoading(true);
    try {
      const success = await deleteInstitucionImage(fileName);
      if (success) {
        setMessage({
          type: 'success',
          title: 'Imagen eliminada',
          text: 'La foto fue removida correctamente del almacenamiento.',
        });
        await loadGallery();
      } else {
        setMessage({
          type: 'error',
          title: 'No se pudo eliminar',
          text: 'Ocurrió un problema al intentar borrar el archivo en Supabase Storage.',
        });
      }
    } catch (err) {
      console.error('Error al eliminar foto:', err);
      setMessage({
        type: 'error',
        title: 'Error de servidor',
        text: 'Ocurrió un error al procesar la solicitud de eliminación.',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Button
        startIcon={<ArrowBackIcon />}
        onClick={() => navigate('/admin')}
        sx={{ mb: 3, fontWeight: 600 }}
      >
        Volver al Panel Principal
      </Button>

      <Paper elevation={2} sx={{ p: { xs: 2.5, sm: 4 }, borderRadius: 3, mb: 4 }}>
        <Box display="flex" alignItems="center" justifyContent="space-between" flexWrap="wrap" gap={2} mb={3}>
          <Box display="flex" alignItems="center" gap={2}>
            <GalleryIcon color="primary" sx={{ fontSize: 40 }} />
            <Box>
              <Typography variant="h5" fontWeight={700} color="primary">
                Galería Institucional
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Administrá las fotos que se muestran en la presentación y carrusel de la Landing Page.
              </Typography>
            </Box>
          </Box>

          <Button
            variant="contained"
            component="label"
            startIcon={uploading ? <CircularProgress size={20} color="inherit" /> : <UploadIcon />}
            disabled={uploading}
            sx={{
              py: 1.2,
              px: 3,
              borderRadius: 2,
              fontWeight: 700,
              boxShadow: '0 4px 12px rgba(2, 132, 199, 0.25)',
            }}
          >
            {uploading ? 'Subiendo imagen...' : 'Agregar Nueva Foto'}
            <input
              type="file"
              hidden
              accept="image/*"
              onChange={handleFileUpload}
            />
          </Button>
        </Box>

        {/* Notificaciones y Alertas con diseño limpio de Material UI */}
        <Collapse in={Boolean(message.text)}>
          <Box mb={3}>
            <Alert
              severity={message.type || 'info'}
              action={
                <IconButton
                  aria-label="close"
                  color="inherit"
                  size="small"
                  onClick={() => setMessage({ type: '', title: '', text: '' })}
                >
                  <CloseIcon fontSize="inherit" />
                </IconButton>
              }
              sx={{
                borderRadius: 2.5,
                boxShadow: '0 4px 14px rgba(0,0,0,0.06)',
                border: '1px solid',
                borderColor: message.type === 'error' ? '#fecaca' : message.type === 'success' ? '#bbf7d0' : '#fed7aa',
              }}
            >
              {message.title && <AlertTitle sx={{ fontWeight: 700 }}>{message.title}</AlertTitle>}
              <Typography variant="body2" sx={{ lineHeight: 1.5 }}>
                {message.text}
              </Typography>
            </Alert>
          </Box>
        </Collapse>

        <Typography variant="subtitle1" fontWeight={600} mb={2}>
          Fotos subidas ({images.length})
        </Typography>

        {loading ? (
          <Box display="flex" justifyContent="center" py={6}>
            <CircularProgress />
          </Box>
        ) : images.length === 0 ? (
          <Paper
            variant="outlined"
            sx={{
              textAlign: 'center',
              py: 6,
              px: 3,
              borderRadius: 3,
              borderStyle: 'dashed',
              borderColor: 'divider',
              bgcolor: 'background.default',
            }}
          >
            <Stack alignItems="center" spacing={1.5}>
              <CloudOffIcon sx={{ fontSize: 48, color: 'text.disabled' }} />
              <Typography variant="subtitle1" fontWeight={600} color="text.secondary">
                No hay fotos guardadas en la galería institucional
              </Typography>
              <Typography variant="caption" color="text.disabled" maxWidth={450}>
                Presioná el botón "Agregar Nueva Foto" para subir imágenes de las instalaciones del instituto.
              </Typography>
            </Stack>
          </Paper>
        ) : (
          <Grid container spacing={3}>
            {images.map((img) => (
              <Grid item xs={12} sm={6} md={4} key={img.name}>
                <Card
                  sx={{
                    borderRadius: 2.5,
                    boxShadow: '0 4px 14px rgba(0,0,0,0.08)',
                    overflow: 'hidden',
                    transition: 'transform 0.2s ease, box-shadow 0.2s ease',
                    '&:hover': {
                      transform: 'translateY(-3px)',
                      boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
                    },
                  }}
                >
                  <CardMedia
                    component="img"
                    height="200"
                    image={img.url}
                    alt={img.name}
                    sx={{ objectFit: 'cover' }}
                  />
                  <CardActions sx={{ justifyContent: 'space-between', px: 2, py: 1.2, bgcolor: 'background.paper' }}>
                    <Typography variant="caption" color="text.secondary" noWrap sx={{ maxWidth: '75%', fontWeight: 500 }}>
                      {img.name}
                    </Typography>
                    <IconButton
                      color="error"
                      size="small"
                      onClick={() => handleDelete(img.name)}
                      title="Eliminar foto"
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </CardActions>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}
      </Paper>
    </Container>
  );
};

export default AdminGaleria;
