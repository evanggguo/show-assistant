'use client'

import { useState, useEffect } from 'react'

/**
 * Admin console authentication state hook.
 * Reads the JWT token from localStorage and provides a logout method.
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
