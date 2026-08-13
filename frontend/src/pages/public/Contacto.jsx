import React from 'react';
import { Container, Paper, Typography, Box, Divider, Button, Grid, Card, CardContent } from '@mui/material';
import { ArrowBack as ArrowBackIcon, Email as EmailIcon, Phone as PhoneIcon, LocationOn as LocationIcon, AccessTime as TimeIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import Footer from '../../components/Footer';

const Contacto = () => {
  const navigate = useNavigate();

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', bgcolor: 'background.default' }}>
      <Container maxWidth="md" sx={{ py: 5, flex: 1 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate(-1)}
          sx={{ mb: 3, fontWeight: 700 }}
          variant="outlined"
        >
          Volver
        </Button>

        <Paper elevation={0} sx={{ p: { xs: 3, md: 5 }, borderRadius: 4, border: '1px solid', borderColor: 'divider', boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}>
          <Typography variant="h4" fontWeight={800} color="primary" mb={1}>
            Medios de Contacto y Atención
          </Typography>
          <Typography variant="body1" color="text.secondary" mb={3}>
            Canales oficiales de información y atención de nuestro Consultorio Médico.
          </Typography>

          <Divider sx={{ mb: 4 }} />

          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <Card variant="outlined" sx={{ borderRadius: 3, height: '100%' }}>
                <CardContent>
                  <Box display="flex" alignItems="center" gap={1.5} mb={1.5}>
                    <PhoneIcon color="primary" />
                    <Typography variant="h6" fontWeight={700}>
                      Número de Teléfono
                    </Typography>
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    +54 11 4433-2211
                  </Typography>
                  <Typography variant="caption" color="text.disabled" display="block">
                    Atención directa en recepción
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} sm={6}>
              <Card variant="outlined" sx={{ borderRadius: 3, height: '100%' }}>
                <CardContent>
                  <Box display="flex" alignItems="center" gap={1.5} mb={1.5}>
                    <EmailIcon color="secondary" />
                    <Typography variant="h6" fontWeight={700}>
                      Correo Electrónico
                    </Typography>
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    contacto@consultoriomedico.com
                  </Typography>
                  <Typography variant="caption" color="text.disabled" display="block">
                    Consultas administrativas e informes
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} sm={6}>
              <Card variant="outlined" sx={{ borderRadius: 3, height: '100%' }}>
                <CardContent>
                  <Box display="flex" alignItems="center" gap={1.5} mb={1.5}>
                    <LocationIcon color="error" />
                    <Typography variant="h6" fontWeight={700}>
                      Ubicación
                    </Typography>
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    Av. Corrientes 1234, Piso 4 - CABA, Argentina
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} sm={6}>
              <Card variant="outlined" sx={{ borderRadius: 3, height: '100%' }}>
                <CardContent>
                  <Box display="flex" alignItems="center" gap={1.5} mb={1.5}>
                    <TimeIcon color="success" />
                    <Typography variant="h6" fontWeight={700}>
                      Horarios de Atención
                    </Typography>
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    Lunes a Viernes: 08:00 a 20:00 hs
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Sábados: 09:00 a 13:00 hs
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </Paper>
      </Container>
      <Footer />
    </Box>
  );
};

export default Contacto;
