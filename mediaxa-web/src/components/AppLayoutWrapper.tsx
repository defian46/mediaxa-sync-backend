'use client';

import React from 'react';
import { usePathname } from 'next/navigation';
import LayoutShell from './LayoutShell';

export default function AppLayoutWrapper({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  if (pathname === '/login') {
    return <>{children}</>;
  }

  return <LayoutShell>{children}</LayoutShell>;
}
