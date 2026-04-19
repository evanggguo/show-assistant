'use client'

/**
 * useChatStream — Core SSE streaming chat hook.
 * Corresponds to TDD 4.3.3.
 *
 * Design notes:
 * 1. Uses fetch + ReadableStream to receive SSE instead of EventSource
 *    (EventSource only supports GET and cannot carry a JSON body)
 * 2. Three SSE event types: token / done / error
 * 3. Guest mode: message history is stored in localStorage keyed by ownerUsername for isolation
 * 4. After the done event, streamingText is committed as a formal Message entry
 */

import { useState, useCallback, useRef, useEffect } from 'react'
import type { Message, ChatRequest, SseTokenData, SseDoneData, SseErrorData } from '@/lib/types'
import { loadGuestMessages, saveGuestMessages } from '@/lib/storage'
import { toFriendlyMessage } from '@/lib/error-utils'

const API_BASE =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

// ─── SSE Parser ───────────────────────────────────────────────

interface SseEvent {
  event: string
  data: unknown
}

function parseSSEChunk(chunk: string): SseEvent[] {
  const events: SseEvent[] = []
  const blocks = chunk.split(/\n\n/)
  for (const block of blocks) {
    if (!block.trim()) continue
    let eventType = 'message'
    let dataStr = ''
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) {
        eventType = line.slice('event:'.length).trim()
      } else if (line.startsWith('data:')) {
        dataStr = line.slice('data:'.length).trim()
      }
    }
    if (!dataStr) continue
    try {
      events.push({ event: eventType, data: JSON.parse(dataStr) })
    } catch {
      // Skip events with unparseable JSON
    }
  }
  return events
}

// ─── Hook ────────────────────────────────────────────────────

export interface UseChatStreamReturn {
  messages: Message[]
  streamingText: string
  isStreaming: boolean
  currentSuggestions: string[]
  error: string | null
  lastFailedMessage: string | null
  sendMessage: (text: string) => void
  retryLastMessage: () => void
  clearError: () => void
}

/**
 * Core hook for managing streaming chat state.
 *
 * @param ownerUsername      Owner username from the URL path, used for routing and localStorage isolation
 * @param initialSuggestions Initial prompt suggestions displayed on the first screen
 */
export function useChatStream(
  ownerUsername: string,
  initialSuggestions: string[]
): UseChatStreamReturn {
  const [messages, setMessages] = useState<Message[]>([])

  useEffect(() => {
    const stored = loadGuestMessages(ownerUsername)
    if (stored.length > 0) {
      setMessages(stored)
    }
  // Reload only when ownerUsername changes, not on every messages update
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ownerUsername])

  const [streamingText, setStreamingText] = useState('')
  const [isStreaming, setIsStreaming] = useState(false)
  const [currentSuggestions, setCurrentSuggestions] = useState<string[]>(initialSuggestions)
  const [error, setError] = useState<string | null>(null)
  const [lastFailedMessage, setLastFailedMessage] = useState<string | null>(null)

  const streamingTextRef = useRef('')

  const clearError = useCallback(() => {
    setError(null)
    setLastFailedMessage(null)
  }, [])

  const sendMessage = useCallback(
    async (text: string) => {
      if (!text.trim() || isStreaming) return

      const userMessage: Message = { role: 'user', content: text.trim() }

      setMessages((prev) => {
        const next = [...prev, userMessage]
        saveGuestMessages(ownerUsername, next)
        return next
      })
      setStreamingText('')
      streamingTextRef.current = ''
      setIsStreaming(true)
      setError(null)

      const history = messages.map((m) => ({ role: m.role, content: m.content }))
      const requestBody: ChatRequest = {
        message: text.trim(),
        history: [...history, { role: 'user', content: text.trim() }],
      }

      let buffer = ''

      try {
        const res = await fetch(
          `${API_BASE}/api/owners/${ownerUsername}/chat/stream`,
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              Accept: 'text/event-stream',
            },
            body: JSON.stringify(requestBody),
          }
        )

        if (!res.ok) {
          throw new Error(`Request failed: ${res.status} ${res.statusText}`)
        }

        if (!res.body) {
          throw new Error('Response body is empty, SSE unavailable')
        }

        const reader = res.body.getReader()
        const decoder = new TextDecoder('utf-8')

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })

          const lastDoubleNewline = buffer.lastIndexOf('\n\n')
          if (lastDoubleNewline === -1) continue

          const toProcess = buffer.slice(0, lastDoubleNewline + 2)
          buffer = buffer.slice(lastDoubleNewline + 2)

          const sseEvents = parseSSEChunk(toProcess)

          for (const { event, data } of sseEvents) {
            if (event === 'token') {
              const tokenData = data as SseTokenData
              streamingTextRef.current += tokenData.text
              setStreamingText(streamingTextRef.current)
            } else if (event === 'done') {
              const doneData = data as SseDoneData
              const suggestions = doneData.suggestions ?? []
              const assistantMessage: Message = {
                id: doneData.messageId,
                role: 'assistant',
                content: streamingTextRef.current,
                suggestions,
              }
              setMessages((prev) => {
                const next = [...prev, assistantMessage]
                saveGuestMessages(ownerUsername, next)
                return next
              })
              setCurrentSuggestions(suggestions.length > 0 ? suggestions : initialSuggestions)
              setStreamingText('')
              streamingTextRef.current = ''
              setIsStreaming(false)
            } else if (event === 'error') {
              const errData = data as SseErrorData
              throw new Error(errData.message || 'Server returned an error')
            }
          }
        }
      } catch (err: unknown) {
        setError(toFriendlyMessage(err))
        setLastFailedMessage(text.trim())
        // Remove the optimistically added user message on failure
        setMessages((prev) => {
          const next = prev.slice(0, -1)
          saveGuestMessages(ownerUsername, next)
          return next
        })
        setStreamingText('')
        streamingTextRef.current = ''
        setIsStreaming(false)
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [isStreaming, messages, initialSuggestions, ownerUsername]
  )

  const retryLastMessage = useCallback(() => {
    if (lastFailedMessage) {
      setError(null)
      setLastFailedMessage(null)
      sendMessage(lastFailedMessage)
    }
  }, [lastFailedMessage, sendMessage])

  return {
    messages,
    streamingText,
    isStreaming,
    currentSuggestions,
    error,
    lastFailedMessage,
    sendMessage,
    retryLastMessage,
    clearError,
  }
}
