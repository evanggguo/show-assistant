'use client'

import { useState, useEffect } from 'react'
import { Loader2 } from 'lucide-react'
import type { DocumentData } from '@/lib/admin-types'
import { fetchDocuments } from '@/lib/admin-api'
import DocumentUploader from '@/components/admin/DocumentUploader'

export default function DocumentsPage() {
  const [documents, setDocuments] = useState<DocumentData[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadData = async () => {
    try {
      const data = await fetchDocuments()
      setDocuments(data)
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
        <h1 className="text-lg font-semibold text-gray-800">文档管理</h1>
        <p className="text-sm text-gray-500 mt-1">
          上传 PDF / TXT / DOCX 文件，系统自动解析并添加到知识库
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
        <DocumentUploader documents={documents} onRefresh={loadData} />
      )}
    </div>
  )
}
