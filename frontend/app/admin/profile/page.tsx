'use client'

import { useState, useEffect } from 'react'
import { Save, Loader2, Eye, EyeOff } from 'lucide-react'
import type { OwnerProfileData, SuggestionData } from '@/lib/admin-types'
import {
  fetchAdminProfile, updateAdminProfile, fetchSuggestions,
  changeUsername, changePassword,
} from '@/lib/admin-api'
import { toFriendlyMessage } from '@/lib/error-utils'
import ErrorAlert from '@/components/ErrorAlert'
import SuggestionManager from '@/components/admin/SuggestionManager'

export default function ProfilePage() {
  const [profile, setProfile] = useState<OwnerProfileData | null>(null)
  const [suggestions, setSuggestions] = useState<SuggestionData[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 修改用户名
  const [newUsername, setNewUsername] = useState('')
  const [usernameError, setUsernameError] = useState('')
  const [usernameSaving, setUsernameSaving] = useState(false)
  const [usernameSaved, setUsernameSaved] = useState(false)

  // 修改密码
  const [oldPw, setOldPw] = useState('')
  const [newPw, setNewPw] = useState('')
  const [confirmPw, setConfirmPw] = useState('')
  const [showOldPw, setShowOldPw] = useState(false)
  const [showNewPw, setShowNewPw] = useState(false)
  const [pwError, setPwError] = useState('')
  const [pwSaving, setPwSaving] = useState(false)
  const [pwSaved, setPwSaved] = useState(false)

  const loadData = async () => {
    try {
      const [p, s] = await Promise.all([fetchAdminProfile(), fetchSuggestions()])
      setProfile(p)
      setSuggestions(s)
    } catch (e) {
      setError(toFriendlyMessage(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [])

  // ── 保存 Owner 信息 ───────────────────────────────────────────────
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
      setError(toFriendlyMessage(e))
    } finally {
      setSaving(false)
    }
  }

  // ── 修改用户名 ────────────────────────────────────────────────────
  const handleChangeUsername = async () => {
    if (!newUsername.trim()) { setUsernameError('Username is required'); return }
    if (!/^[a-zA-Z0-9]+$/.test(newUsername)) { setUsernameError('Only letters and numbers are allowed'); return }
    setUsernameSaving(true)
    setUsernameError('')
    try {
      await changeUsername(newUsername.trim())
      setUsernameSaved(true)
      setNewUsername('')
      setTimeout(() => setUsernameSaved(false), 2000)
    } catch (e) {
      setUsernameError(toFriendlyMessage(e))
    } finally {
      setUsernameSaving(false)
    }
  }

  // ── 修改密码 ──────────────────────────────────────────────────────
  const handleChangePassword = async () => {
    if (!oldPw) { setPwError('Current password is required'); return }
    if (!newPw || newPw.length < 6) { setPwError('New password must be at least 6 characters'); return }
    if (newPw !== confirmPw) { setPwError('Passwords do not match'); return }
    setPwSaving(true)
    setPwError('')
    try {
      await changePassword(oldPw, newPw)
      setOldPw(''); setNewPw(''); setConfirmPw('')
      setPwSaved(true)
      setTimeout(() => setPwSaved(false), 2000)
    } catch (e) {
      setPwError(toFriendlyMessage(e))
    } finally {
      setPwSaving(false)
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
        <h1 className="text-lg font-semibold text-gray-800">Owner Profile</h1>
        <p className="text-sm text-gray-500 mt-1">Personal information displayed to visitors</p>
      </div>

      {error && (
        <ErrorAlert message={error} onDismiss={() => setError(null)} />
      )}

      {/* 基本信息表单 */}
      <div className="bg-white rounded-2xl border border-gray-100 p-6 space-y-4">
        <h2 className="text-sm font-medium text-gray-700">Basic Info</h2>

        <div>
          <label className="text-sm text-gray-600 mb-1 block">Name / Display Name</label>
          <input
            value={profile?.name || ''}
            onChange={(e) => setProfile(p => p ? { ...p, name: e.target.value } : p)}
            className="w-full border border-gray-300 rounded-xl px-3 py-2 text-sm
                       focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
          />
        </div>

        <div>
          <label className="text-sm text-gray-600 mb-1 block">Tagline</label>
          <input
            value={profile?.tagline || ''}
            onChange={(e) => setProfile(p => p ? { ...p, tagline: e.target.value } : p)}
            placeholder="e.g. Full-stack Developer & Indie Maker"
            className="w-full border border-gray-300 rounded-xl px-3 py-2 text-sm
                       focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
          />
        </div>

        <div>
          <label className="text-sm text-gray-600 mb-1 block">Avatar URL</label>
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
            {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
            {saved ? 'Saved ✓' : 'Save'}
          </button>
        </div>
      </div>

      {/* 修改用户名 */}
      <div className="bg-white rounded-2xl border border-gray-100 p-6 space-y-4">
        <h2 className="text-sm font-medium text-gray-700">Change Username</h2>
        <p className="text-xs text-gray-400">You will need to log in again with the new username after changing it.</p>

        <div>
          <input
            value={newUsername}
            onChange={e => { setNewUsername(e.target.value); setUsernameError('') }}
            placeholder="New username (letters and numbers only)"
            className={[
              'w-full border rounded-xl px-3 py-2 text-sm',
              'focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400',
              usernameError ? 'border-red-400' : 'border-gray-300',
            ].join(' ')}
          />
          {usernameError && <p className="text-xs text-red-500 mt-1">{usernameError}</p>}
        </div>

        <div className="flex justify-end">
          <button
            onClick={handleChangeUsername}
            disabled={usernameSaving || !newUsername}
            className="flex items-center gap-2 px-4 py-2 bg-blue-500 hover:bg-blue-600
                       text-white text-sm rounded-xl transition-colors disabled:opacity-60"
          >
            {usernameSaving && <Loader2 className="w-4 h-4 animate-spin" />}
            {usernameSaved ? 'Updated ✓' : 'Confirm'}
          </button>
        </div>
      </div>

      {/* 修改密码 */}
      <div className="bg-white rounded-2xl border border-gray-100 p-6 space-y-4">
        <h2 className="text-sm font-medium text-gray-700">Change Password</h2>

        <div className="relative">
          <input
            type={showOldPw ? 'text' : 'password'}
            value={oldPw}
            onChange={e => { setOldPw(e.target.value); setPwError('') }}
            placeholder="Current password"
            className="w-full border border-gray-300 rounded-xl px-3 py-2 text-sm pr-10
                       focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
          />
          <button
            type="button"
            onClick={() => setShowOldPw(v => !v)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
          >
            {showOldPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
          </button>
        </div>

        <div className="relative">
          <input
            type={showNewPw ? 'text' : 'password'}
            value={newPw}
            onChange={e => { setNewPw(e.target.value); setPwError('') }}
            placeholder="New password (at least 6 characters)"
            className="w-full border border-gray-300 rounded-xl px-3 py-2 text-sm pr-10
                       focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
          />
          <button
            type="button"
            onClick={() => setShowNewPw(v => !v)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
          >
            {showNewPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
          </button>
        </div>

        <input
          type="password"
          value={confirmPw}
          onChange={e => { setConfirmPw(e.target.value); setPwError('') }}
          placeholder="Confirm new password"
          className="w-full border border-gray-300 rounded-xl px-3 py-2 text-sm
                     focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
        />

        {pwError && <p className="text-xs text-red-500">{pwError}</p>}

        <div className="flex justify-end">
          <button
            onClick={handleChangePassword}
            disabled={pwSaving || !oldPw || !newPw || !confirmPw}
            className="flex items-center gap-2 px-4 py-2 bg-blue-500 hover:bg-blue-600
                       text-white text-sm rounded-xl transition-colors disabled:opacity-60"
          >
            {pwSaving && <Loader2 className="w-4 h-4 animate-spin" />}
            {pwSaved ? 'Updated ✓' : 'Confirm'}
          </button>
        </div>
      </div>

      {/* 初始提示词管理 */}
      <div className="bg-white rounded-2xl border border-gray-100 p-6">
        <h2 className="text-sm font-medium text-gray-700 mb-4">Initial Suggestions</h2>
        <p className="text-xs text-gray-400 mb-4">Prompt cards shown on the chat home screen to help visitors start a conversation</p>
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
