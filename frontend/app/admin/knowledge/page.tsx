'use client'

import { useState, useEffect } from 'react'
import { Loader2 } from 'lucide-react'
import type { KnowledgeData } from '@/lib/admin-types'
import { fetchKnowledge } from '@/lib/admin-api'
import KnowledgeTable from '@/components/admin/KnowledgeTable'

export default function KnowledgePage() {
  const [entries, setEntries] = useState<KnowledgeData[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadData = async () => {
    try {
      const data = await fetchKnowledge()
      setEntries(data)
    } catch (e) {
      setError(e instanceof Error ? e.message : '加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [])

  return (
    <div className="max-w-3xl space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-gray-800">知识库</h1>
        <p className="text-sm text-gray-500 mt-1">
          手动添加文本知识，AI 对话时会检索最相关的内容作为上下文
        </p>
      </div>

      {error && (
        <div className="bg-red-50 text-red-700 text-sm rounded-xl px-4 py-3">{error}</div>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-48">
          <Loader2 className="w-6 h-6 animate-spin text-blue-500" />
        </div>
      ) : (
        <>
          <div className="text-xs text-gray-400">共 {entries.length} 条</div>
          <KnowledgeTable entries={entries} onRefresh={loadData} />
        </>
      )}
    </div>
  )
}
