'use client'

import { useState, useRef } from 'react'
import { Upload, Trash2, Play, RefreshCw } from 'lucide-react'
import type { DocumentData } from '@/lib/admin-types'
import { uploadDocument, processDocument, deleteDocument } from '@/lib/admin-api'

interface Props {
  documents: DocumentData[]
  onRefresh: () => void
}

const STATUS_LABELS: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待处理', color: 'text-yellow-600 bg-yellow-50' },
  PROCESSING: { label: '处理中', color: 'text-blue-600 bg-blue-50' },
  COMPLETED: { label: '已完成', color: 'text-green-600 bg-green-50' },
  FAILED: { label: '失败', color: 'text-red-600 bg-red-50' },
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

export default function DocumentUploader({ documents, onRefresh }: Props) {
  const [uploading, setUploading] = useState(false)
  const [dragOver, setDragOver] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleUpload = async (files: FileList | null) => {
    if (!files || files.length === 0) return
    setUploading(true)
    try {
      for (const file of Array.from(files)) {
        await uploadDocument(file)
      }
      onRefresh()
    } catch (e) {
      alert(e instanceof Error ? e.message : '上传失败')
    } finally {
      setUploading(false)
    }
  }

  const handleProcess = async (id: number) => {
    try {
      await processDocument(id)
      onRefresh()
    } catch (e) {
      alert(e instanceof Error ? e.message : '触发解析失败')
    }
  }

  const handleDelete = async (id: number) => {
    if (!confirm('确认删除此文档？相关知识条目不会被自动删除。')) return
    try {
      await deleteDocument(id)
      onRefresh()
    } catch (e) {
      alert(e instanceof Error ? e.message : '删除失败')
    }
  }

  return (
    <div className="space-y-4">
      {/* 拖拽上传区域 */}
      <div
        onClick={() => fileInputRef.current?.click()}
        onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(e) => {
          e.preventDefault()
          setDragOver(false)
          handleUpload(e.dataTransfer.files)
        }}
        className={[
          'border-2 border-dashed rounded-2xl p-8 text-center cursor-pointer transition-colors',
          dragOver
            ? 'border-blue-400 bg-blue-50'
            : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50',
        ].join(' ')}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,.txt,.docx"
          multiple
          className="hidden"
          onChange={(e) => handleUpload(e.target.files)}
        />
        <Upload className="w-8 h-8 text-gray-400 mx-auto mb-3" />
        {uploading ? (
          <p className="text-sm text-blue-600">上传中...</p>
        ) : (
          <>
            <p className="text-sm text-gray-600 font-medium">点击或拖拽文件到此处上传</p>
            <p className="text-xs text-gray-400 mt-1">支持 PDF、TXT、DOCX，多文件同时上传</p>
          </>
        )}
      </div>

      {/* 文档列表 */}
      {documents.length === 0 ? (
        <p className="text-sm text-gray-400 text-center py-4">暂无文档</p>
      ) : (
        <div className="space-y-2">
          {documents.map((doc) => {
            const status = STATUS_LABELS[doc.status] ?? { label: doc.status, color: 'text-gray-600 bg-gray-50' }
            return (
              <div
                key={doc.id}
                className="flex items-center gap-3 p-3 bg-white rounded-xl border border-gray-100"
              >
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-gray-700 font-medium truncate">{doc.filename}</p>
                  <p className="text-xs text-gray-400 mt-0.5">
                    {doc.fileType.toUpperCase()} · {formatFileSize(doc.fileSize)} ·{' '}
                    {new Date(doc.createdAt).toLocaleDateString('zh-CN')}
                  </p>
                </div>
                <span className={`text-xs px-2 py-0.5 rounded-lg font-medium ${status.color}`}>
                  {status.label}
                </span>
                {(doc.status === 'PENDING' || doc.status === 'FAILED') && (
                  <button
                    onClick={() => handleProcess(doc.id)}
                    title="触发解析"
                    className="p-1.5 text-blue-500 hover:bg-blue-50 rounded-lg transition-colors"
                  >
                    {doc.status === 'FAILED' ? (
                      <RefreshCw className="w-4 h-4" />
                    ) : (
                      <Play className="w-4 h-4" />
                    )}
                  </button>
                )}
                <button
                  onClick={() => handleDelete(doc.id)}
                  className="p-1.5 text-red-400 hover:bg-red-50 rounded-lg transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
