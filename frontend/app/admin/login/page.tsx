'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { adminLogin } from '@/lib/admin-api'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const router = useRouter()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const { token } = await adminLogin(username, password)
      localStorage.setItem('admin_token', token)
      router.replace('/admin/profile')
    } catch (err) {
      setError(err instanceof Error ? err.message : '登录失败，请检查用户名和密码')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="w-full max-w-sm bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
        <h1 className="text-xl font-semibold text-gray-800 mb-2 text-center">管理后台</h1>
        <p className="text-sm text-gray-500 text-center mb-6">Show Assistant</p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-sm text-gray-600 mb-1 block">用户名</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full border border-gray-300 rounded-xl px-3 py-2 text-sm
                         focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400
                         transition-colors"
              required
              autoComplete="username"
            />
          </div>
          <div>
            <label className="text-sm text-gray-600 mb-1 block">密码</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full border border-gray-300 rounded-xl px-3 py-2 text-sm
                         focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400
                         transition-colors"
              required
              autoComplete="current-password"
            />
          </div>

          {error && (
            <p className="text-sm text-red-600 bg-red-50 rounded-xl px-3 py-2">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-500 hover:bg-blue-600 text-white rounded-xl py-2.5
                       text-sm font-medium transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {loading ? '登录中...' : '登录'}
          </button>
        </form>
      </div>
    </div>
  )
}
