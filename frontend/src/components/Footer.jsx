import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Box, Container, Link, Typography, useTheme } from '@mui/material';

const Footer = () => {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  const linkStyle = {
    color: isDark ? '#cbd5e1' : '#475569',
    fontWeight: 600,
    fontSize: '0.92rem',
    textDecoration: 'none',
    transition: 'all 0.2s ease-in-out',
    '&:hover': {
      color: isDark ? '#38bdf8' : '#0284c7',
      textDecoration: 'underline',
      transform: 'translateY(-1px)',
    },
  };

  return (
    <Box
      component="footer"
      sx={{
        py: 2.5,
        px: 2,
        mt: 'auto',
        backgroundColor: isDark ? '#0b1329' : '#ffffff',
        borderTop: '1px solid',
        borderColor: isDark ? '#1e293b' : '#e2e8f0',
        boxShadow: isDark
          ? '0 -4px 20px rgba(0,0,0,0.25)'
          : '0 -2px 12px rgba(0,0,0,0.04)',
      }}
    >
      <Container maxWidth="lg">
        <Box
          display="flex"
          justifyContent="center"
          alignItems="center"
          flexWrap="wrap"
          gap={{ xs: 2.5, sm: 5 }}
        >
          <Link component={RouterLink} to="/terminos-y-condiciones" sx={linkStyle}>
            Términos y condiciones
          </Link>

          <Link
            href="https://forms.gle/BbLdctpipW2wPxvX7"
            target="_blank"
            rel="noopener noreferrer"
            sx={linkStyle}
          >
            Déjanos tu comentario
          </Link>

          <Link component={RouterLink} to="/contacto" sx={linkStyle}>
            Contacto
          </Link>
        </Box>

        {/* Leyenda discreta de verificación para indexación SEO y bots de búsqueda */}
        <Box textAlign="center" mt={1.5}>
          <Typography
            variant="caption"
            sx={{
              fontSize: '0.7rem',
              color: isDark ? 'rgba(203, 213, 225, 0.4)' : 'rgba(100, 116, 139, 0.5)',
              display: 'block',
              lineHeight: 1.3,
            }}
          >
            Sistema de gestión y reserva de turnos médicos online para pacientes y profesionales del Instituto Doctor Oscar Bustos.
          </Typography>
        </Box>
      </Container>
    </Box>
  );
};

export default Footer;
