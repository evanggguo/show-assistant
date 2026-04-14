'use client'

import { useState, useEffect } from 'react'
import { Save, Loader2 } from 'lucide-react'
import type { OwnerProfileData, SuggestionData } from '@/lib/admin-types'
import { fetchAdminProfile, updateAdminProfile, fetchSuggestions } from '@/lib/admin-api'
import SuggestionManager from '@/components/admin/SuggestionManager'

export default function ProfilePage() {
  const [profile, setProfile] = useState<OwnerProfileData | null>(null)
  const [suggestions, setSuggestions] = useState<SuggestionData[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadData = async () => {
    try {
      const [p, s] = await Promise.all([fetchAdminProfile(), fetchSuggestions()])
      setProfile(p)
      setSuggestions(s)
    } catch (e) {
      setError(e instanceof Error ? e.message : '加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [])

  const handleSave = async () => {
    if (!profile) return
    setSaving(true)
    setError(null)
    try {
      const updated = await updateAdminProfile({
        name: profile.name,
        tagline: profile.tagline,
        avatarUrl: profile.avatarUrl,
      })
      setProfile(updated)
      setSaved(true)
      setTimeout(() => setSaved(false), 2000)
    } catch (e) {
      setError(e instanceof Error ? e.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-48">
        <Loader2 className="w-6 h-6 animate-spin text-blue-500" />
      </div>
    )
  }

  return (
    <div className="max-w-2xl space-y-8">
      <div>
        <h1 className="text-lg font-semibold text-gray-800">Owner 信息</h1>
        <p className="text-sm text-gray-500 mt-1">展示给访客的个人信息</p>
      </div>

      {error && (
        <div className="bg-red-50 text-red-700 text-sm rounded-xl px-4 py-3">{error}</div>
      )}

      {/* 基本信息表单 */}
      <div className="bg-white rounded-2xl border border-gray-100 p-6 space-y-4">
        <h2 className="text-sm font-medium text-gray-700">基本信息</h2>

        <div>
          <label className="text-sm text-gray-600 mb-1 block">姓名 / 展示名</label>
          <input
            value={profile?.name || ''}
            onChange={(e) => setProfile(p => p ? { ...p, name: e.target.value } : p)}
            className="w-full border border-gray-300 rounded-xl px-3 py-2 text-sm
                       focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
          />
        </div>

        <div>
          <label className="text-sm text-gray-600 mb-1 block">简介标语</label>
          <input
            value={profile?.tagline || ''}
            onChange={(e) => setProfile(p => p ? { ...p, tagline: e.target.value } : p)}
            placeholder="如：全栈开发者 & 独立产品人"
            className="w-full border border-gray-300 rounded-xl px-3 py-2 text-sm
                       focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
          />
        </div>

        <div>
          <label className="text-sm text-gray-600 mb-1 block">头像 URL</label>
          <input
            value={profile?.avatarUrl || ''}
            onChange={(e) => setProfile(p => p ? { ...p, avatarUrl: e.target.value } : p)}
            placeholder="https://..."
            className="w-full border border-gray-300 rounded-xl px-3 py-2 text-sm
                       focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
          />
        </div>

        <div className="flex justify-end pt-2">
          <button
            onClick={handleSave}
            disabled={saving}
            className="flex items-center gap-2 px-4 py-2 bg-blue-500 hover:bg-blue-600
                       text-white text-sm rounded-xl transition-colors disabled:opacity-60"
          >
            {saving ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Save className="w-4 h-4" />
            )}
            {saved ? '已保存 ✓' : '保存'}
          </button>
        </div>
      </div>

      {/* 初始提示词管理 */}
      <div className="bg-white rounded-2xl border border-gray-100 p-6">
        <h2 className="text-sm font-medium text-gray-700 mb-4">初始提示词</h2>
        <p className="text-xs text-gray-400 mb-4">展示在聊天首屏的引导问题，供访客快速开始对话</p>
        <SuggestionManager
          suggestions={suggestions}
          onRefresh={async () => {
            const s = await fetchSuggestions()
            setSuggestions(s)
          }}
        />
      </div>
    </div>
  )
}
