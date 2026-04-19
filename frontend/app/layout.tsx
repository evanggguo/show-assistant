/**
 * Root layout
 * Uses system fonts (avoids Google Fonts network requests) and configures global metadata
 */

import type { Metadata, Viewport } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'Dossier',
  description: 'AI-powered personal assistant — learn about the owner\'s projects and experience.',
}

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="h-full">{children}</body>
    </html>
  )
}
