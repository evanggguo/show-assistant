/**
 * 根布局
 * 使用系统字体（避免 Google Fonts 网络请求），配置全局元数据
 */

import type { Metadata, Viewport } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'Show Assistant',
  description: '智能对话助手，了解 Owner 的项目和经历',
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
    <html lang="zh-CN" className="h-full antialiased">
      <body className="h-full">{children}</body>
    </html>
  )
}
