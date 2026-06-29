'use client';

import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  CircularProgress
} from '@mui/material';

export default function LoginPage() {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [deviceId, setDeviceId] = useState('WEB-CONSOLE-DEFAULT-ID');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setLoading(true);

    try {
      await login(username, password, deviceId);
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'Invalid credentials');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        display: 'flex',
        minHeight: '100vh',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#0b1329',
        p: 2
      }}
    >
      <Card sx={{ width: '100%', maxWidth: 400, backgroundColor: '#111b36', border: '1px solid #1e293b' }}>
        <CardContent sx={{ p: 4 }}>
          <Box sx={{ textAlign: 'center', mb: 3 }}>
            <Typography variant="h5" sx={{ fontWeight: 'bold' }} color="primary.main">
              Mediaxa Suite
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Masuk ke Dashboard Manajemen
            </Typography>
          </Box>

          {errorMsg && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {errorMsg}
            </Alert>
          )}

          <form onSubmit={handleSubmit}>
            <TextField
              fullWidth
              label="Username"
              variant="outlined"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              sx={{ mb: 2 }}
              disabled={loading}
              required
            />
            <TextField
              fullWidth
              label="Password"
              variant="outlined"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              sx={{ mb: 2 }}
              disabled={loading}
              required
            />
            <TextField
              fullWidth
              label="Device ID"
              variant="outlined"
              value={deviceId}
              onChange={(e) => setDeviceId(e.target.value)}
              sx={{ mb: 3 }}
              disabled={loading}
              required
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              disabled={loading}
              sx={{ py: 1.5 }}
            >
              {loading ? <CircularProgress size={24} color="inherit" /> : 'Masuk'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </Box>
  );
}
