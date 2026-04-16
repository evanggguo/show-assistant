'use client'

import { useState, useEffect } from 'react'
import { Loader2 } from 'lucide-react'
import type { DocumentData } from '@/lib/admin-types'
import { fetchDocuments } from '@/lib/admin-api'
import { toFriendlyMessage } from '@/lib/error-utils'
import ErrorAlert from '@/components/ErrorAlert'
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
      setError(toFriendlyMessage(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [])

  return (
    <div className="max-w-3xl space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-gray-800">Documents</h1>
        <p className="text-sm text-gray-500 mt-1">
          Upload PDF / TXT / DOCX files and the system will automatically parse and add them to the knowledge base.
        </p>
      </div>

      {error && (
        <ErrorAlert message={error} onRetry={loadData} onDismiss={() => setError(null)} />
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
