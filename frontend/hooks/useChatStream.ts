'use client'

/**
 * useChatStream — 核心 SSE 流式对话 Hook
 * 对应 TDD 4.3.3
 *
 * 设计要点：
 * 1. 使用 fetch + ReadableStream 接收 SSE，而非 EventSource
 *    原因：EventSource 只支持 GET，无法携带 JSON body
 * 2. 三种 SSE 事件：token / done / error
 * 3. 游客模式：历史消息存 localStorage，每次请求携带 history
 * 4. done 事件后，将 streamingText 转为正式 Message 加入列表
 */

import { useState, useCallback, useRef } from 'react'
import type { Message, ChatRequest, SseTokenData, SseDoneData, SseErrorData } from '@/lib/types'
import { loadGuestMessages, saveGuestMessages } from '@/lib/storage'

const API_BASE =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

// ─── SSE 解析工具 ─────────────────────────────────────────────

interface SseEvent {
  event: string
  data: unknown
}

/**
 * 解析一段 SSE 文本块（可能包含多个事件）
 * SSE 格式：event 之间以空行分隔，每行格式为 "field: value"
 *
 * @param chunk 原始文本片段（可能不完整，由调用方维护 buffer）
 * @returns 解析出的完整事件列表
 */
function parseSSEChunk(chunk: string): SseEvent[] {
  const events: SseEvent[] = []
  // 按双换行分割独立事件块
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
      // JSON 解析失败时跳过该事件
    }
  }
  return events
}

// ─── Hook ─────────────────────────────────────────────────────

export interface UseChatStreamReturn {
  messages: Message[]
  streamingText: string
  isStreaming: boolean
  currentSuggestions: string[]
  error: string | null
  sendMessage: (text: string) => void
  clearError: () => void
}

/**
 * 管理流式对话状态的核心 Hook
 *
 * @param initialSuggestions 首屏展示的初始提示词（从后端获取或降级默认值）
 */
export function useChatStream(initialSuggestions: string[]): UseChatStreamReturn {
  // 从 localStorage 加载游客历史消息（SSR 安全：loadGuestMessages 内部判断 window）
  const [messages, setMessages] = useState<Message[]>(() => loadGuestMessages())
  const [streamingText, setStreamingText] = useState('')
  const [isStreaming, setIsStreaming] = useState(false)
  const [currentSuggestions, setCurrentSuggestions] = useState<string[]>(initialSuggestions)
  const [error, setError] = useState<string | null>(null)

  // 用 ref 追踪流式文本，避免闭包陈旧值问题
  const streamingTextRef = useRef('')

  const clearError = useCallback(() => setError(null), [])

  /**
   * 发送消息并开启 SSE 流
   * @param text 用户输入的文本
   */
  const sendMessage = useCallback(
    async (text: string) => {
      if (!text.trim() || isStreaming) return

      // 构造用户消息
      const userMessage: Message = { role: 'user', content: text.trim() }

      // 更新消息列表，加入用户消息
      setMessages((prev) => {
        const next = [...prev, userMessage]
        saveGuestMessages(next)
        return next
      })
      setStreamingText('')
      streamingTextRef.current = ''
      setIsStreaming(true)
      setError(null)

      // 构造请求体（游客 MVP：不传 conversationId，携带历史）
      const history = messages.map((m) => ({ role: m.role, content: m.content }))
      const requestBody: ChatRequest = {
        message: text.trim(),
        history: [...history, { role: 'user', content: text.trim() }],
      }

      // ── SSE fetch ────────────────────────────────────────────
      let buffer = '' // 未完整处理的文本缓冲

      try {
        const res = await fetch(`${API_BASE}/api/chat/stream`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'text/event-stream',
          },
          body: JSON.stringify(requestBody),
        })

        if (!res.ok) {
          throw new Error(`请求失败：${res.status} ${res.statusText}`)
        }

        if (!res.body) {
          throw new Error('响应体为空，SSE 不可用')
        }

        const reader = res.body.getReader()
        const decoder = new TextDecoder('utf-8')

        // 逐块读取流
        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })

          // 按双换行切分完整事件，保留未完整部分留待下次处理
          const lastDoubleNewline = buffer.lastIndexOf('\n\n')
          if (lastDoubleNewline === -1) continue

          const toProcess = buffer.slice(0, lastDoubleNewline + 2)
          buffer = buffer.slice(lastDoubleNewline + 2)

          const sseEvents = parseSSEChunk(toProcess)

          for (const { event, data } of sseEvents) {
            if (event === 'token') {
              // 追加流式文本
              const tokenData = data as SseTokenData
              streamingTextRef.current += tokenData.text
              setStreamingText(streamingTextRef.current)
            } else if (event === 'done') {
              // 流结束：将 streamingText 转为正式 assistant 消息
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
                saveGuestMessages(next)
                return next
              })
              setCurrentSuggestions(suggestions.length > 0 ? suggestions : initialSuggestions)
              setStreamingText('')
              streamingTextRef.current = ''
              setIsStreaming(false)
            } else if (event === 'error') {
              // SSE 错误事件
              const errData = data as SseErrorData
              throw new Error(errData.message || '服务器返回错误')
            }
          }
        }
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : '请求失败，请稍后重试'
        setError(message)
        setStreamingText('')
        streamingTextRef.current = ''
        setIsStreaming(false)
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [isStreaming, messages, initialSuggestions]
  )

  return {
    messages,
    streamingText,
    isStreaming,
    currentSuggestions,
    error,
    sendMessage,
    clearError,
  }
}
