'use client'

import { useState } from 'react'
import { Plus, Trash2, ChevronDown, ChevronUp, ChevronLeft, ChevronRight } from 'lucide-react'
import type { KnowledgeData, CreateKnowledgeData } from '@/lib/admin-types'
import { createKnowledge, deleteKnowledge } from '@/lib/admin-api'

interface Props {
  entries: KnowledgeData[]
  onRefresh: () => void
}

const KNOWLEDGE_TYPES = ['TEXT', 'FAQ', 'STRUCTURED']
const PAGE_SIZE = 10

export default function KnowledgeTable({ entries, onRefresh }: Props) {
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState<CreateKnowledgeData>({ type: 'TEXT', title: '', content: '' })
  const [loading, setLoading] = useState(false)
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [page, setPage] = useState(1)

  const totalPages = Math.max(1, Math.ceil(entries.length / PAGE_SIZE))
  const pageEntries = entries.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)

  const handleCreate = async () => {
    if (!form.content.trim()) return
    setLoading(true)
    try {
      await createKnowledge({
        type: form.type,
        title: form.title || undefined,
        content: form.content,
      })
      setForm({ type: 'TEXT', title: '', content: '' })
      setShowForm(false)
      setPage(1)
      onRefresh()
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Failed to create')
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this knowledge entry?')) return
    try {
      await deleteKnowledge(id)
      // If the current page is now empty, go back one page
      if (pageEntries.length === 1 && page > 1) setPage(page - 1)
      onRefresh()
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Failed to delete')
    }
  }

  return (
    <div className="space-y-3">
      {/* Add knowledge entry (placed above the list) */}
      {showForm ? (
        <div className="bg-white rounded-xl border border-blue-200 p-4 space-y-3">
          <div className="flex gap-3">
            <select
              value={form.type}
              onChange={(e) => setForm({ ...form, type: e.target.value })}
              className="border border-gray-300 rounded-xl px-3 py-2 text-sm
                         focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
            >
              {KNOWLEDGE_TYPES.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
            <input
              value={form.title || ''}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              placeholder="Title (optional)"
              className="flex-1 border border-gray-300 rounded-xl px-3 py-2 text-sm
                         focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
            />
          </div>
          <textarea
            value={form.content}
            onChange={(e) => setForm({ ...form, content: e.target.value })}
            placeholder="Knowledge content..."
            rows={4}
            className="w-full border border-gray-300 rounded-xl px-3 py-2 text-sm resize-none
                       focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
          />
          <div className="flex gap-2 justify-end">
            <button
              onClick={() => setShowForm(false)}
              className="px-4 py-2 text-sm text-gray-600 hover:bg-gray-50 rounded-xl transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handleCreate}
              disabled={loading || !form.content.trim()}
              className="px-4 py-2 text-sm bg-blue-500 hover:bg-blue-600 text-white
                         rounded-xl transition-colors disabled:opacity-60"
            >
              {loading ? 'Saving...' : 'Save'}
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
          Add Knowledge Entry
        </button>
      )}

      {/* Knowledge entry list */}
      {entries.length === 0 ? (
        <p className="text-sm text-gray-400 text-center py-8">No knowledge entries yet</p>
      ) : (
        <>
          {pageEntries.map((entry) => (
            <div
              key={entry.id}
              className="bg-white rounded-xl border border-gray-100 overflow-hidden"
            >
              <div
                className="flex items-center gap-3 p-3 cursor-pointer hover:bg-gray-50 transition-colors"
                onClick={() => setExpandedId(expandedId === entry.id ? null : entry.id)}
              >
                <span className="text-xs px-2 py-0.5 bg-blue-50 text-blue-600 rounded-lg font-medium">
                  {entry.type}
                </span>
                <span className="flex-1 text-sm text-gray-700 font-medium truncate">
                  {entry.title || entry.content.slice(0, 50) + '...'}
                </span>
                <span className="text-xs text-gray-400">
                  {new Date(entry.createdAt).toLocaleDateString('en-US')}
                </span>
                {expandedId === entry.id ? (
                  <ChevronUp className="w-4 h-4 text-gray-400" />
                ) : (
                  <ChevronDown className="w-4 h-4 text-gray-400" />
                )}
                <button
                  onClick={(e) => { e.stopPropagation(); handleDelete(entry.id) }}
                  className="p-1.5 text-red-400 hover:bg-red-50 rounded-lg transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
              {expandedId === entry.id && (
                <div className="px-4 pb-4 border-t border-gray-50">
                  <p className="text-sm text-gray-600 whitespace-pre-wrap mt-3">{entry.content}</p>
                </div>
              )}
            </div>
          ))}

          {/* Pagination controls */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between pt-2">
              <span className="text-xs text-gray-400">
                Page {page} / {totalPages} · {entries.length} entries
              </span>
              <div className="flex items-center gap-1">
                <button
                  onClick={() => setPage(page - 1)}
                  disabled={page === 1}
                  className="p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 disabled:opacity-30
                             disabled:cursor-not-allowed transition-colors"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
                  <button
                    key={p}
                    onClick={() => setPage(p)}
                    className={[
                      'w-7 h-7 rounded-lg text-xs transition-colors',
                      p === page
                        ? 'bg-blue-500 text-white'
                        : 'text-gray-600 hover:bg-gray-100',
                    ].join(' ')}
                  >
                    {p}
                  </button>
                ))}
                <button
                  onClick={() => setPage(page + 1)}
                  disabled={page === totalPages}
                  className="p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 disabled:opacity-30
                             disabled:cursor-not-allowed transition-colors"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
