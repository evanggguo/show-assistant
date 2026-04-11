'use client'

/**
 * MessageList — 消息列表滚动区域
 * 对应 TDD 4.3 消息展示
 *
 * 特性：
 * - 新消息后自动滚动到底部
 * - 流式输出中展示 StreamingBubble
 */

import React, { useEffect, useRef } from 'react'
import type { Message } from '@/lib/types'
import MessageBubble from './MessageBubble'
import StreamingBubble from './StreamingBubble'

interface MessageListProps {
  messages: Message[]
  /** 当前流式输出文本（空字符串表示未在流式中） */
  streamingText: string
  isStreaming: boolean
  /** 点击提示词卡片的回调 */
  onSuggestionSelect: (text: string) => void
}

export default function MessageList({
  messages,
  streamingText,
  isStreaming,
  onSuggestionSelect,
}: MessageListProps) {
  const bottomRef = useRef<HTMLDivElement>(null)

  // 新消息或流式文本变化时自动滚动到底部
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, streamingText])

  return (
    <div className="flex-1 overflow-y-auto px-4 py-6">
      <div className="max-w-[800px] mx-auto">
        {messages.map((msg, idx) => (
          <MessageBubble
            key={msg.id ?? idx}
            message={msg}
            onSuggestionSelect={onSuggestionSelect}
            suggestionsDisabled={isStreaming}
          />
        ))}

        {/* 流式气泡：正在输出时展示 */}
        {isStreaming && streamingText && (
          <StreamingBubble text={streamingText} />
        )}

        {/* 滚动锚点 */}
        <div ref={bottomRef} />
      </div>
    </div>
  )
}
