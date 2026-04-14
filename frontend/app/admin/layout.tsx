'use client'

import { useEffect } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { useAdminAuth } from '@/hooks/useAdminAuth'
import Sidebar from '@/components/admin/Sidebar'

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { token, isLoading } = useAdminAuth()
  const router = useRouter()
  const pathname = usePathname()

  const isLoginPage = pathname === '/admin/login'

  useEffect(() => {
    if (!isLoading && !token && !isLoginPage) {
      router.replace('/admin/login')
    }
  }, [token, isLoading, isLoginPage, router])

  // 登录页不需要侧边栏和认证守卫
  if (isLoginPage) {
    return <>{children}</>
  }

  if (isLoading) {
    return (
      <div className="h-screen flex items-center justify-center bg-gray-50">
        <div className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  if (!token) {
    return null
  }

  return (
    <div className="flex h-screen bg-gray-50 overflow-hidden">
      <Sidebar />
      <main className="flex-1 overflow-auto p-6">{children}</main>
    </div>
  )
}
