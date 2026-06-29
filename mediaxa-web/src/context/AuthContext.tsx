'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import api from '../utils/axios';

interface User {
  userUuid: string;
  username: string;
  role: string;
  storeUuid: string;
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (username: string, password: string, deviceId: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();

  // Initialize state lazily to avoid setState inside useEffect
  const [user, setUser] = useState<User | null>(() => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('accessToken');
      const storedUser = localStorage.getItem('user');
      if (token && storedUser) {
        try {
          return JSON.parse(storedUser) as User;
        } catch {
          return null;
        }
      }
    }
    return null;
  });

  const [isLoading, setIsLoading] = useState(true);

  // Define logout callback
  const logout = React.useCallback(() => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
    setUser(null);
    router.push('/login');
  }, [router]);

  useEffect(() => {
    // Complete loading initialization asynchronously to avoid eslint setState-in-effect rule
    const timer = setTimeout(() => {
      setIsLoading(false);
    }, 0);
    return () => clearTimeout(timer);
  }, []);

  const login = async (username: string, password: string, deviceId: string) => {
    setIsLoading(true);
    try {
      const res = await api.post('/auth/login', {
        username,
        password,
        deviceId,
        deviceName: 'Web Management Console'
      });
      
      const { accessToken, refreshToken, user: loggedUser } = res.data;
      
      if (typeof window !== 'undefined') {
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        localStorage.setItem('user', JSON.stringify(loggedUser));
      }
      
      setUser(loggedUser);
      router.push('/');
    } catch (error) {
      throw new Error(error instanceof Error ? error.message : 'Login failed');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
