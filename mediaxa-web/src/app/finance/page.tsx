'use client';

import React from 'react';
import { Box, Typography } from '@mui/material';

export default function FinancePage() {
  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 'bold' }} gutterBottom>
        Keuangan & Pengeluaran
      </Typography>
      <Typography variant="body1" color="text.secondary">
        Modul peninjauan pengeluaran operasional toko dan log tutup buku harian.
      </Typography>
    </Box>
  );
}
