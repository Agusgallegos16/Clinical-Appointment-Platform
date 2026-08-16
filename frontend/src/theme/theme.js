import { createTheme } from '@mui/material/styles';

export const getCustomTheme = (mode) =>
  createTheme({
    palette: {
      mode,
      primary: {
        main: '#0284c7', // Azul Médico Claro Destacado
        light: '#e0f2fe',
        dark: '#0369a1',
        contrastText: '#ffffff',
      },
      secondary: {
        main: '#0d9488', // Teal Salud Complementario
        light: '#ccfbf1',
        dark: '#0f766e',
      },
      background: {
        default: mode === 'light' ? '#ffffff' : '#0f172a',
        paper: mode === 'light' ? '#ffffff' : '#1e293b',
      },
      text: {
        primary: mode === 'light' ? '#0f172a' : '#f8fafc',
        secondary: mode === 'light' ? '#475569' : '#94a3b8',
      },
    },
    typography: {
      fontFamily: '"Rubik", "Roboto", "Helvetica", "Arial", sans-serif',
      h4: {
        fontWeight: 700,
        letterSpacing: '-0.02em',
      },
      h5: {
        fontWeight: 600,
        letterSpacing: '-0.01em',
      },
      h6: {
        fontWeight: 600,
      },
      button: {
        textTransform: 'none',
        fontWeight: 600,
      },
    },
    shape: {
      borderRadius: 12,
    },
    components: {
      MuiButton: {
        styleOverrides: {
          root: {
            borderRadius: 10,
            boxShadow: 'none',
            paddingTop: 10,
            paddingBottom: 10,
            '&:hover': {
              boxShadow: '0 4px 14px rgba(2, 132, 199, 0.3)',
            },
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            backgroundColor: mode === 'light' ? '#ffffff' : '#1e293b',
            boxShadow: mode === 'light'
              ? '0 4px 12px rgba(2, 132, 199, 0.08)'
              : '0 4px 12px rgba(0, 0, 0, 0.4)',
            border: mode === 'light' ? '2px solid #0284c7' : '1px solid #334155',
            transition: 'all 0.2s ease-in-out',
          },
        },
      },
      MuiPaper: {
        styleOverrides: {
          root: {
            backgroundColor: mode === 'light' ? '#ffffff' : '#1e293b',
            border: mode === 'light' ? '1px solid #cbd5e1' : '1px solid #334155',
          },
        },
      },
    },
  });
