'use client'

import { useState } from 'react'
import { Plus, Trash2, Edit2, Check, X } from 'lucide-react'
import type { SuggestionData } from '@/lib/admin-types'
import {
  createSuggestion,
  updateSuggestion,
  deleteSuggestion,
} from '@/lib/admin-api'

interface Props {
  suggestions: SuggestionData[]
  onRefresh: () => void
}

export default function SuggestionManager({ suggestions, onRefresh }: Props) {
  const [newText, setNewText] = useState('')
  const [adding, setAdding] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editText, setEditText] = useState('')
  const [loading, setLoading] = useState(false)

  const handleAdd = async () => {
    if (!newText.trim()) return
    setLoading(true)
    try {
      await createSuggestion({
        text: newText.trim(),
        sortOrder: suggestions.length + 1,
        enabled: true,
      })
      setNewText('')
      setAdding(false)
      onRefresh()
    } catch (e) {
      alert(e instanceof Error ? e.message : '添加失败')
    } finally {
      setLoading(false)
    }
  }

  const handleUpdate = async (id: number) => {
    if (!editText.trim()) return
    setLoading(true)
    try {
      await updateSuggestion(id, { text: editText.trim() })
      setEditingId(null)
      onRefresh()
    } catch (e) {
      alert(e instanceof Error ? e.message : '更新失败')
    } finally {
      setLoading(false)
    }
  }

  const handleToggleEnabled = async (s: SuggestionData) => {
    try {
      await updateSuggestion(s.id, { enabled: !s.enabled })
      onRefresh()
    } catch (e) {
      alert(e instanceof Error ? e.message : '更新失败')
    }
  }

  const handleDelete = async (id: number) => {
    if (!confirm('确认删除此提示词？')) return
    try {
      await deleteSuggestion(id)
      onRefresh()
    } catch (e) {
      alert(e instanceof Error ? e.message : '删除失败')
    }
  }

  return (
    <div className="space-y-3">
      {suggestions.map((s) => (
        <div
          key={s.id}
          className="flex items-center gap-3 p-3 bg-white rounded-xl border border-gray-100"
        >
          {editingId === s.id ? (
            <>
              <input
                value={editText}
                onChange={(e) => setEditText(e.target.value)}
                className="flex-1 border border-gray-300 rounded-lg px-2 py-1 text-sm
                           focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
                onKeyDown={(e) => e.key === 'Enter' && handleUpdate(s.id)}
                autoFocus
              />
              <button
                onClick={() => handleUpdate(s.id)}
                disabled={loading}
                className="p-1.5 text-green-600 hover:bg-green-50 rounded-lg transition-colors"
              >
                <Check className="w-4 h-4" />
              </button>
              <button
                onClick={() => setEditingId(null)}
                className="p-1.5 text-gray-400 hover:bg-gray-50 rounded-lg transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </>
          ) : (
            <>
              <span
                className={[
                  'flex-1 text-sm',
                  s.enabled ? 'text-gray-700' : 'text-gray-400 line-through',
                ].join(' ')}
              >
                {s.text}
              </span>
              <button
                onClick={() => handleToggleEnabled(s)}
                className={[
                  'text-xs px-2 py-1 rounded-lg border transition-colors',
                  s.enabled
                    ? 'border-green-200 text-green-700 hover:bg-green-50'
                    : 'border-gray-200 text-gray-500 hover:bg-gray-50',
                ].join(' ')}
              >
                {s.enabled ? '启用' : '禁用'}
              </button>
              <button
                onClick={() => {
                  setEditingId(s.id)
                  setEditText(s.text)
                }}
                className="p-1.5 text-gray-400 hover:bg-gray-50 rounded-lg transition-colors"
              >
                <Edit2 className="w-4 h-4" />
              </button>
              <button
                onClick={() => handleDelete(s.id)}
                className="p-1.5 text-red-400 hover:bg-red-50 rounded-lg transition-colors"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </>
          )}
        </div>
      ))}

      {adding ? (
        <div className="flex items-center gap-2 p-3 bg-white rounded-xl border border-blue-200">
          <input
            value={newText}
            onChange={(e) => setNewText(e.target.value)}
            placeholder="输入新提示词..."
            className="flex-1 text-sm focus:outline-none"
            onKeyDown={(e) => e.key === 'Enter' && handleAdd()}
            autoFocus
          />
          <button
            onClick={handleAdd}
            disabled={loading || !newText.trim()}
            className="p-1.5 text-green-600 hover:bg-green-50 rounded-lg transition-colors disabled:opacity-40"
          >
            <Check className="w-4 h-4" />
          </button>
          <button
            onClick={() => { setAdding(false); setNewText('') }}
            className="p-1.5 text-gray-400 hover:bg-gray-50 rounded-lg transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      ) : (
        <button
          onClick={() => setAdding(true)}
          className="flex items-center gap-2 px-3 py-2 text-sm text-blue-600 hover:bg-blue-50
                     rounded-xl border border-dashed border-blue-200 w-full transition-colors"
        >
          <Plus className="w-4 h-4" />
          添加提示词
        </button>
      )}
    </div>
  )
}
