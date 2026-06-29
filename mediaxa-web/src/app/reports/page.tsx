'use client';

import React from 'react';
import { Box, Typography } from '@mui/material';

export default function ReportsPage() {
  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 'bold' }} gutterBottom>
        Laporan Penjualan
      </Typography>
      <Typography variant="body1" color="text.secondary">
        Modul penayangan grafik laba-rugi, komparasi produk terlaris, dan ekspor data spreadsheet.
      </Typography>
    </Box>
  );
}
