'use client';

import React from 'react';
import { Box, Typography } from '@mui/material';

export default function InventoryPage() {
  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 'bold' }} gutterBottom>
        Belanja Stok & Bahan
      </Typography>
      <Typography variant="body1" color="text.secondary">
        Modul manajemen persediaan, resep makanan, dan pencatatan riwayat restok bahan baku.
      </Typography>
    </Box>
  );
}
