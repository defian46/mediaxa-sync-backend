'use client';

import React from 'react';
import { useAuth } from '../context/AuthContext';
import {
  Grid,
  Card,
  CardContent,
  Typography,
  Box
} from '@mui/material';
import {
  TrendingUp as RevenueIcon,
  Receipt as TxIcon,
  Warning as WarningIcon,
  AccountBalanceWallet as FinanceIcon
} from '@mui/icons-material';

export default function Home() {
  const { user } = useAuth();

  const stats = [
    {
      title: 'Omzet Hari Ini',
      value: 'Rp 2.450.000',
      change: '+12% dari kemarin',
      icon: <RevenueIcon fontSize="large" color="primary" />
    },
    {
      title: 'Total Transaksi',
      value: '142 Pesanan',
      change: '+5.3% dari kemarin',
      icon: <TxIcon fontSize="large" color="secondary" />
    },
    {
      title: 'Stok Kritis',
      value: '3 Item',
      change: 'Perlu restok segera',
      icon: <WarningIcon fontSize="large" color="error" />
    },
    {
      title: 'Pengeluaran Bulan Ini',
      value: 'Rp 5.200.000',
      change: 'Sesuai dengan anggaran',
      icon: <FinanceIcon fontSize="large" color="warning" />
    }
  ];

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 'bold' }} gutterBottom>
          Selamat Datang, {user?.username}!
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Berikut adalah ringkasan performa bisnis toko Anda hari ini.
        </Typography>
      </Box>

      <Grid container spacing={3}>
        {stats.map((stat) => (
          <Grid size={{ xs: 12, sm: 6, md: 3 }} key={stat.title}>
            <Card sx={{ backgroundColor: '#111b36', border: '1px solid #1e293b' }}>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ fontWeight: 'bold' }}>
                    {stat.title}
                  </Typography>
                  {stat.icon}
                </Box>
                <Typography variant="h5" sx={{ fontWeight: 'bold', mb: 1 }}>
                  {stat.value}
                </Typography>
                <Typography variant="caption" color={stat.title.includes('Kritis') ? 'error.main' : 'primary.main'}>
                  {stat.change}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}
