'use client';

import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useRouter, usePathname } from 'next/navigation';
import {
  Box,
  Drawer,
  AppBar,
  Toolbar,
  List,
  Typography,
  Divider,
  IconButton,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Avatar,
  Menu as MuiMenu,
  MenuItem,
  useMediaQuery,
  useTheme
} from '@mui/material';
import {
  Menu as MenuIcon,
  Dashboard as DashboardIcon,
  Inventory as InventoryIcon,
  AccountBalanceWallet as FinanceIcon,
  Group as CustomerIcon,
  Assessment as ReportIcon,
  Settings as SettingsIcon,
  ExitToApp as LogoutIcon
} from '@mui/icons-material';

const DRAWER_WIDTH = 240;

interface MenuItemProps {
  label: string;
  path: string;
  icon: React.ReactNode;
}

export default function LayoutShell({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const menuItems: MenuItemProps[] = [
    { label: 'Analitik', path: '/', icon: <DashboardIcon /> },
    { label: 'Belanja Stok', path: '/inventory', icon: <InventoryIcon /> },
    { label: 'Keuangan', path: '/finance', icon: <FinanceIcon /> },
    { label: 'CRM Pelanggan', path: '/crm', icon: <CustomerIcon /> },
    { label: 'Laporan', path: '/reports', icon: <ReportIcon /> },
    { label: 'Pengaturan', path: '/settings', icon: <SettingsIcon /> },
  ];

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleNavigation = (path: string) => {
    router.push(path);
    if (isMobile) setMobileOpen(false);
  };

  const drawerContent = (
    <Box sx={{ backgroundColor: '#111b36', height: '100%', color: '#f8fafc' }}>
      <Toolbar sx={{ justifyContent: 'center', py: 2 }}>
        <Typography variant="h6" sx={{ fontWeight: 'bold' }} color="primary.main">
          Mediaxa Suite
        </Typography>
      </Toolbar>
      <Divider sx={{ borderColor: '#1e293b' }} />
      <List>
        {menuItems.map((item) => {
          const active = pathname === item.path;
          return (
            <ListItem key={item.label} disablePadding>
              <ListItemButton
                onClick={() => handleNavigation(item.path)}
                sx={{
                  mx: 1,
                  my: 0.5,
                  borderRadius: 2,
                  backgroundColor: active ? 'rgba(37, 99, 235, 0.15)' : 'transparent',
                  color: active ? 'primary.main' : 'text.secondary',
                  '&:hover': {
                    backgroundColor: 'rgba(255, 255, 255, 0.05)',
                  },
                }}
              >
                <ListItemIcon sx={{ color: active ? 'primary.main' : 'text.secondary' }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText>
                  <Typography sx={{ fontWeight: active ? 600 : 500 }} variant="body2">
                    {item.label}
                  </Typography>
                </ListItemText>
              </ListItemButton>
            </ListItem>
          );
        })}
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', backgroundColor: '#0b1329' }}>
      {/* AppBar / Topbar */}
      <AppBar
        position="fixed"
        sx={{
          width: isMobile ? '100%' : `calc(100% - ${DRAWER_WIDTH}px)`,
          ml: isMobile ? 0 : `${DRAWER_WIDTH}px`,
          backgroundColor: '#111b36',
          borderBottom: '1px solid #1e293b',
          boxShadow: 'none',
        }}
      >
        <Toolbar sx={{ justifyContent: 'space-between' }}>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            {isMobile && (
              <IconButton
                color="inherit"
                aria-label="open drawer"
                edge="start"
                onClick={handleDrawerToggle}
                sx={{ mr: 2 }}
              >
                <MenuIcon />
              </IconButton>
            )}
            <Typography variant="h6" noWrap component="div" sx={{ fontWeight: 'bold' }}>
              Manajemen Bisnis
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Box sx={{ textAlign: 'right', display: { xs: 'none', sm: 'block' } }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>
                {user?.username}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {user?.role}
              </Typography>
            </Box>
            <IconButton onClick={handleMenuOpen} sx={{ p: 0.5 }}>
              <Avatar sx={{ bgcolor: 'primary.main', width: 36, height: 36 }}>
                {user?.username.charAt(0).toUpperCase()}
              </Avatar>
            </IconButton>
            <MuiMenu
              anchorEl={anchorEl}
              open={Boolean(anchorEl)}
              onClose={handleMenuClose}
              transformOrigin={{ horizontal: 'right', vertical: 'top' }}
              anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
              slotProps={{
                paper: {
                  sx: {
                    backgroundColor: '#111b36',
                    border: '1px solid #1e293b',
                    mt: 1.5,
                  },
                },
              }}
            >
              <MenuItem disabled sx={{ color: 'text.secondary', fontSize: '12px' }}>
                Store ID: {user?.storeUuid.substr(0, 8)}...
              </MenuItem>
              <Divider sx={{ borderColor: '#1e293b' }} />
              <MenuItem onClick={logout} sx={{ color: 'error.main', gap: 1 }}>
                <LogoutIcon fontSize="small" />
                Keluar
              </MenuItem>
            </MuiMenu>
          </Box>
        </Toolbar>
      </AppBar>

      {/* Navigation Drawers */}
      <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
        {isMobile ? (
          <Drawer
            variant="temporary"
            open={mobileOpen}
            onClose={handleDrawerToggle}
            ModalProps={{ keepMounted: true }}
            sx={{
              '& .MuiDrawer-paper': { boxSizing: 'border-box', width: DRAWER_WIDTH, border: 'none' },
            }}
          >
            {drawerContent}
          </Drawer>
        ) : (
          <Drawer
            variant="permanent"
            open
            sx={{
              '& .MuiDrawer-paper': { boxSizing: 'border-box', width: DRAWER_WIDTH, borderRight: '1px solid #1e293b' },
            }}
          >
            {drawerContent}
          </Drawer>
        )}
      </Box>

      {/* Main Content Layout Wrapper */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: isMobile ? '100%' : `calc(100% - ${DRAWER_WIDTH}px)`,
          mt: '64px',
        }}
      >
        {children}
      </Box>
    </Box>
  );
}
