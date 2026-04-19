'use client'

/**
 * MessageList — Scrollable message list area.
 * Corresponds to TDD 4.3 message display.
 *
 * Features:
 * - Auto-scrolls to the bottom after new messages
 * - Shows StreamingBubble during streaming output
 */

import React, { useEffect, useRef } from 'react'
import type { Message } from '@/lib/types'
import MessageBubble from './MessageBubble'
import StreamingBubble from './StreamingBubble'

interface MessageListProps {
  messages: Message[]
  /** Currently streaming text (empty string means not streaming) */
  streamingText: string
  isStreaming: boolean
  /** Callback when a suggestion card is clicked */
  onSuggestionSelect: (text: string) => void
}

export default function MessageList({
  messages,
  streamingText,
  isStreaming,
  onSuggestionSelect,
}: MessageListProps) {
  const bottomRef = useRef<HTMLDivElement>(null)

  // Auto-scroll to bottom when messages or streaming text changes
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

        {/* Streaming bubble: shown while output is in progress */}
        {isStreaming && streamingText && (
          <StreamingBubble text={streamingText} />
        )}

        {/* Scroll anchor */}
        <div ref={bottomRef} />
      </div>
    </div>
  )
}
