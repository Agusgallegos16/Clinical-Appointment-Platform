import React from 'react';
import { Container, Paper, Typography, Box, Divider, Button, Card, CardContent } from '@mui/material';
import { ArrowBack as ArrowBackIcon, Gavel as GavelIcon, Security as SecurityIcon, EventAvailable as EventIcon, ChildCare as ChildIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import Footer from '../../components/Footer';

const TerminosYCondiciones = () => {
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
          <Box display="flex" alignItems="center" gap={2} mb={2}>
            <GavelIcon color="primary" sx={{ fontSize: 40 }} />
            <Box>
              <Typography variant="h4" fontWeight={800} color="primary">
                Términos y Condiciones de Uso
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Última actualización: Agosto 2026 — Consultorio Médico
              </Typography>
            </Box>
          </Box>

          <Divider sx={{ my: 3 }} />

          <Typography variant="body1" paragraph paragraph color="text.primary" sx={{ lineHeight: 1.7 }}>
            Bienvenido al sistema de Gestión de Turnos del **Consultorio Médico**. Al acceder y utilizar nuestros servicios web, el usuario (paciente, profesional o administrador) acepta cumplir con los siguientes términos y condiciones de uso.
          </Typography>

          <Card variant="outlined" sx={{ my: 3, borderRadius: 3, bgcolor: 'background.paper' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1.5} mb={1}>
                <EventIcon color="primary" />
                <Typography variant="h6" fontWeight={700}>
                  1. Reserva y Cancelación de Turnos
                </Typography>
              </Box>
              <Typography variant="body2" color="text.secondary" paragraph>
                - Los turnos reservados a través de la plataforma son de carácter personal e intransferible.
              </Typography>
              <Typography variant="body2" color="text.secondary" paragraph>
                - En caso de no poder asistir, se solicita cancelar el turno con al menos **24 horas de anticipación** para permitir que otro paciente pueda ocupar el espacio.
              </Typography>
              <Typography variant="body2" color="text.secondary">
                - El sistema enviará recordatorios automáticos por correo electrónico previa a la consulta programada.
              </Typography>
            </CardContent>
          </Card>

          <Card variant="outlined" sx={{ my: 3, borderRadius: 3, bgcolor: 'background.paper' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1.5} mb={1}>
                <ChildIcon color="secondary" />
                <Typography variant="h6" fontWeight={700}>
                  2. Gestión de Menores de Edad a Cargo
                </Typography>
              </Box>
              <Typography variant="body2" color="text.secondary" paragraph>
                - Los tutores legales o adultos a cargo pueden vincular y solicitar turnos para menores de edad registrados en su perfil.
              </Typography>
              <Typography variant="body2" color="text.secondary">
                - La persona mayor de edad registrada asume la responsabilidad total de las reservas y notificaciones asociadas al menor.
              </Typography>
            </CardContent>
          </Card>

          <Card variant="outlined" sx={{ my: 3, borderRadius: 3, bgcolor: 'background.paper' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1.5} mb={1}>
                <SecurityIcon color="success" />
                <Typography variant="h6" fontWeight={700}>
                  3. Privacidad y Protección de Datos
                </Typography>
              </Box>
              <Typography variant="body2" color="text.secondary" paragraph>
                - Todos los datos personales y de contacto recolectados son tratados bajo estrictos estándares de confidencialidad médica.
              </Typography>
              <Typography variant="body2" color="text.secondary">
                - No compartimos ni comercializamos información médica o de identidad con terceros bajo ningún concepto.
              </Typography>
            </CardContent>
          </Card>

          <Typography variant="subtitle2" color="text.secondary" textAlign="center" sx={{ mt: 4 }}>
            Si tiene alguna duda sobre estos términos, puede contactarse con la administración a través de nuestra sección de Contacto.
          </Typography>
        </Paper>
      </Container>
      <Footer />
    </Box>
  );
};

export default TerminosYCondiciones;
