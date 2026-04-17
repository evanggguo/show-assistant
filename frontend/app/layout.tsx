/**
 * 根布局
 * 使用系统字体（避免 Google Fonts 网络请求），配置全局元数据
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
