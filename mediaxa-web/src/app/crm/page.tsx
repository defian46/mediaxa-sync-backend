'use client';

import React from 'react';
import { Box, Typography } from '@mui/material';

export default function CrmPage() {
  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 'bold' }} gutterBottom>
        CRM & Loyalty Pelanggan
      </Typography>
      <Typography variant="body1" color="text.secondary">
        Modul pengelolaan data member, pengisian saldo poin loyalitas, dan penawaran promosi personal.
      </Typography>
    </Box>
  );
}
