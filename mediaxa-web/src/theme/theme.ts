'use client';

import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: '#2563eb', // Premium Royal Blue
      contrastText: '#ffffff',
    },
    secondary: {
      main: '#7c3aed', // Purple Accent
    },
    background: {
      default: '#0b1329', // Premium Dark Blue Slate
      paper: '#111b36',   // Cards background
    },
    text: {
      primary: '#f8fafc',
      secondary: '#94a3b8',
    },
  },
  shape: {
    borderRadius: 16, // Consistent rounded corners matching Design System
  },
  typography: {
    fontFamily: 'Inter, system-ui, sans-serif',
    button: {
      textTransform: 'none', // Premium clean lowercase buttons
      fontWeight: 600,
    },
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          border: '1px solid #1e293b',
          boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
        },
      },
    },
  },
});

export default theme;
