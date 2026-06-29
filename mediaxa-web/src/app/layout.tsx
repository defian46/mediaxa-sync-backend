import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import Providers from "./providers";
import RouteGuard from "../components/RouteGuard";
import AppLayoutWrapper from "../components/AppLayoutWrapper";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "Mediaxa Business Suite",
  description: "Premium SaaS Management Console for UMKM",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="id">
      <body className={inter.className}>
        <Providers>
          <RouteGuard>
            <AppLayoutWrapper>
              {children}
            </AppLayoutWrapper>
          </RouteGuard>
        </Providers>
      </body>
    </html>
  );
}
