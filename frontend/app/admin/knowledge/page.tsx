'use client'

import { useState, useEffect } from 'react'
import { Loader2 } from 'lucide-react'
import type { KnowledgeData } from '@/lib/admin-types'
import { fetchKnowledge } from '@/lib/admin-api'
import { toFriendlyMessage } from '@/lib/error-utils'
import ErrorAlert from '@/components/ErrorAlert'
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
      setError(toFriendlyMessage(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [])

  return (
    <div className="max-w-3xl space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-gray-800">Knowledge Base</h1>
        <p className="text-sm text-gray-500 mt-1">
          Manually add text entries. The most relevant content will be retrieved as context during AI conversations.
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
        <KnowledgeTable entries={entries} onRefresh={loadData} />
      )}
    </div>
  )
}
