'use client'

import { useState } from 'react'
import { Plus, Trash2, ChevronDown, ChevronUp } from 'lucide-react'
import type { KnowledgeData, CreateKnowledgeData } from '@/lib/admin-types'
import { createKnowledge, deleteKnowledge } from '@/lib/admin-api'

interface Props {
  entries: KnowledgeData[]
  onRefresh: () => void
}

const KNOWLEDGE_TYPES = ['TEXT', 'FAQ', 'STRUCTURED']

export default function KnowledgeTable({ entries, onRefresh }: Props) {
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState<CreateKnowledgeData>({ type: 'TEXT', title: '', content: '' })
  const [loading, setLoading] = useState(false)
  const [expandedId, setExpandedId] = useState<number | null>(null)

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
      onRefresh()
    } catch (e) {
      alert(e instanceof Error ? e.message : '创建失败')
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (id: number) => {
    if (!confirm('确认删除此知识条目？')) return
    try {
      await deleteKnowledge(id)
      onRefresh()
    } catch (e) {
      alert(e instanceof Error ? e.message : '删除失败')
    }
  }

  return (
    <div className="space-y-3">
      {/* 知识条目列表 */}
      {entries.length === 0 ? (
        <p className="text-sm text-gray-400 text-center py-8">暂无知识条目</p>
      ) : (
        entries.map((entry) => (
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
                {new Date(entry.createdAt).toLocaleDateString('zh-CN')}
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
        ))
      )}

      {/* 新增表单 */}
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
              placeholder="标题（可选）"
              className="flex-1 border border-gray-300 rounded-xl px-3 py-2 text-sm
                         focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
            />
          </div>
          <textarea
            value={form.content}
            onChange={(e) => setForm({ ...form, content: e.target.value })}
            placeholder="知识内容..."
            rows={4}
            className="w-full border border-gray-300 rounded-xl px-3 py-2 text-sm resize-none
                       focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
          />
          <div className="flex gap-2 justify-end">
            <button
              onClick={() => setShowForm(false)}
              className="px-4 py-2 text-sm text-gray-600 hover:bg-gray-50 rounded-xl transition-colors"
            >
              取消
            </button>
            <button
              onClick={handleCreate}
              disabled={loading || !form.content.trim()}
              className="px-4 py-2 text-sm bg-blue-500 hover:bg-blue-600 text-white
                         rounded-xl transition-colors disabled:opacity-60"
            >
              {loading ? '保存中...' : '保存'}
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
          添加知识条目
        </button>
      )}
    </div>
  )
}
