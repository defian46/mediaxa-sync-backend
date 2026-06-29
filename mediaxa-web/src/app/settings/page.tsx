'use client';

import React from 'react';
import { Box, Typography } from '@mui/material';

export default function SettingsPage() {
  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 'bold' }} gutterBottom>
        Pengaturan Toko
      </Typography>
      <Typography variant="body1" color="text.secondary">
        Modul penyesuaian profil outlet, konfigurasi pajak, cetak struk Bluetooth, dan integrasi cloud.
      </Typography>
    </Box>
  );
}
