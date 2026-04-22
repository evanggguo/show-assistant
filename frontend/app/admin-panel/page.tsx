'use client'

/**
 * Super Admin page /admin-panel.
 * Login validates the entered token against the backend /capabilities endpoint.
 * The backend determines whether the token grants delete access; the frontend
 * renders the delete button only when canDelete is true.
 */

import { useState, useEffect, useCallback } from 'react'
import { Loader2, Plus, Trash2, ShieldAlert, Eye, EyeOff } from 'lucide-react'
import type { OwnerSummaryData } from '@/lib/admin-types'
import { fetchCapabilities, fetchOwners, createOwner, deleteOwner } from '@/lib/admin-api'

export default function AdminPanelPage() {
  const [authed, setAuthed] = useState(false)
  const [token, setToken] = useState('')
  const [canDelete, setCanDelete] = useState(false)
  const [pwInput, setPwInput] = useState('')
  const [showPw, setShowPw] = useState(false)
  const [pwError, setPwError] = useState(false)
  const [loginLoading, setLoginLoading] = useState(false)

  const [owners, setOwners] = useState<OwnerSummaryData[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Add form
  const [showForm, setShowForm] = useState(false)
  const [newUsername, setNewUsername] = useState('')
  const [creating, setCreating] = useState(false)
  const [usernameError, setUsernameError] = useState('')

  const loadOwners = useCallback(async (activeToken: string) => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchOwners(activeToken)
      setOwners(data)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (authed && token) loadOwners(token)
  }, [authed, token, loadOwners])

  const handleLogin = async () => {
    setLoginLoading(true)
    setPwError(false)
    try {
      const caps = await fetchCapabilities(pwInput)
      setToken(pwInput)
      setCanDelete(caps.canDelete)
      setAuthed(true)
    } catch {
      setPwError(true)
    } finally {
      setLoginLoading(false)
    }
  }

  const validateUsername = (v: string) => {
    if (!v) return 'Username is required'
    if (!/^[a-zA-Z0-9]+$/.test(v)) return 'Only letters and numbers are allowed'
    if (owners.some(o => o.username === v)) return 'Username already exists'
    return ''
  }

  const handleCreate = async () => {
    const err = validateUsername(newUsername)
    if (err) { setUsernameError(err); return }

    setCreating(true)
    try {
      await createOwner(newUsername, token)
      setNewUsername('')
      setShowForm(false)
      setUsernameError('')
      await loadOwners(token)
    } catch (e) {
      setUsernameError(e instanceof Error ? e.message : 'Failed to create')
    } finally {
      setCreating(false)
    }
  }

  const handleDelete = async (id: number, username: string) => {
    if (!confirm(`Are you sure you want to delete owner "${username}"? This action cannot be undone.`)) return
    try {
      await deleteOwner(id, token)
      await loadOwners(token)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to delete')
    }
  }

  // ── Password verification page ────────────────────────────────────
  if (!authed) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
        <div className="w-full max-w-sm bg-white rounded-2xl border border-gray-100 shadow-sm p-8 space-y-6">
          <div className="flex flex-col items-center gap-2">
            <div className="w-12 h-12 bg-red-50 rounded-2xl flex items-center justify-center">
              <ShieldAlert className="w-6 h-6 text-red-500" />
            </div>
            <h1 className="text-lg font-semibold text-gray-800">Super Admin</h1>
            <p className="text-xs text-gray-400 text-center">This page is restricted to system administrators</p>
          </div>

          <div className="space-y-3">
            <div className="relative">
              <input
                type={showPw ? 'text' : 'password'}
                value={pwInput}
                onChange={e => { setPwInput(e.target.value); setPwError(false) }}
                onKeyDown={e => e.key === 'Enter' && handleLogin()}
                placeholder="Enter admin password"
                className={[
                  'w-full border rounded-xl px-3 py-2.5 text-sm pr-10',
                  'focus:outline-none focus:ring-2 focus:ring-red-100 focus:border-red-400',
                  pwError ? 'border-red-400' : 'border-gray-300',
                ].join(' ')}
              />
              <button
                type="button"
                onClick={() => setShowPw(v => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
              >
                {showPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
            {pwError && <p className="text-xs text-red-500">Incorrect password</p>}
            <button
              onClick={handleLogin}
              disabled={loginLoading}
              className="w-full py-2.5 bg-red-500 hover:bg-red-600 text-white text-sm
                         rounded-xl transition-colors font-medium flex items-center justify-center gap-2
                         disabled:opacity-60"
            >
              {loginLoading && <Loader2 className="w-4 h-4 animate-spin" />}
              Enter Admin
            </button>
          </div>
        </div>
      </div>
    )
  }

  // ── Management interface ───────────────────────────────────────────
  return (
    <div className="min-h-screen bg-gray-50 px-4 py-10">
      <div className="max-w-2xl mx-auto space-y-6">
        {/* Title */}
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 bg-red-50 rounded-xl flex items-center justify-center">
            <ShieldAlert className="w-5 h-5 text-red-500" />
          </div>
          <div>
            <h1 className="text-lg font-semibold text-gray-800">Super Admin · Owner Accounts</h1>
            <p className="text-xs text-gray-400">Owner accounts can log in to /admin console. Default password: 888888</p>
          </div>
        </div>

        {error && (
          <div className="bg-red-50 text-red-700 text-sm rounded-xl px-4 py-3">{error}</div>
        )}

        {/* Add form */}
        {showForm ? (
          <div className="bg-white rounded-xl border border-blue-200 p-4 space-y-3">
            <p className="text-sm font-medium text-gray-700">Add Owner Account</p>
            <div>
              <input
                value={newUsername}
                onChange={e => { setNewUsername(e.target.value); setUsernameError('') }}
                onKeyDown={e => e.key === 'Enter' && handleCreate()}
                placeholder="Username (letters and numbers only)"
                className={[
                  'w-full border rounded-xl px-3 py-2 text-sm',
                  'focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400',
                  usernameError ? 'border-red-400' : 'border-gray-300',
                ].join(' ')}
              />
              {usernameError && <p className="text-xs text-red-500 mt-1">{usernameError}</p>}
              <p className="text-xs text-gray-400 mt-1">Default password: 888888. Can be changed in the admin console after login.</p>
            </div>
            <div className="flex gap-2 justify-end">
              <button
                onClick={() => { setShowForm(false); setNewUsername(''); setUsernameError('') }}
                className="px-4 py-2 text-sm text-gray-600 hover:bg-gray-50 rounded-xl transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleCreate}
                disabled={creating}
                className="flex items-center gap-1.5 px-4 py-2 text-sm bg-blue-500 hover:bg-blue-600
                           text-white rounded-xl transition-colors disabled:opacity-60"
              >
                {creating && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                Create
              </button>
            </div>
          </div>
        ) : (
          <button
            onClick={() => setShowForm(true)}
            className="flex items-center gap-2 px-3 py-2 text-sm text-blue-600 hover:bg-blue-50
                       rounded-xl border border-dashed border-blue-200 w-full transition-colors"
          >
            <Plus className="w-4 h-4" />
            Add Owner Account
          </button>
        )}

        {/* Owner list */}
        {loading ? (
          <div className="flex items-center justify-center h-32">
            <Loader2 className="w-6 h-6 animate-spin text-gray-400" />
          </div>
        ) : owners.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-10">No owner accounts yet</p>
        ) : (
          <div className="space-y-2">
            {owners.map(owner => (
              <div
                key={owner.id}
                className="bg-white rounded-xl border border-gray-100 flex items-center gap-3 px-4 py-3"
              >
                <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center
                                text-sm font-medium text-blue-600 flex-shrink-0">
                  {(owner.username || owner.name || '?')[0].toUpperCase()}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-800">{owner.username}</p>
                  {owner.name && owner.name !== owner.username && (
                    <p className="text-xs text-gray-400 truncate">{owner.name}</p>
                  )}
                </div>
                <span className="text-xs text-gray-400 flex-shrink-0">
                  {new Date(owner.createdAt).toLocaleDateString('en-US')}
                </span>
                {canDelete && (
                  <button
                    onClick={() => handleDelete(owner.id, owner.username)}
                    className="p-1.5 text-red-400 hover:bg-red-50 rounded-lg transition-colors flex-shrink-0"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
