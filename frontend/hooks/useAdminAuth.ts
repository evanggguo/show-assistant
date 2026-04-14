'use client'

import { useState, useEffect } from 'react'

/**
 * 管理端认证状态 Hook
 * 从 localStorage 读取 JWT token，提供 logout 方法
 */
export function useAdminAuth() {
  const [token, setToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const stored = localStorage.getItem('admin_token')
    setToken(stored)
    setIsLoading(false)
  }, [])

  const logout = () => {
    localStorage.removeItem('admin_token')
    setToken(null)
  }

  return { token, isLoading, logout }
}
