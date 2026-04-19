'use client'

/**
 * MessageBubble — Single message bubble.
 * Corresponds to TDD 1.4 message UI design.
 *
 * - role=user: right-aligned, gray background
 * - role=assistant: left-aligned, white background, Markdown rendered
 * - Dynamic suggestion cards shown below assistant messages (when suggestions exist)
 */

import React from 'react'
import ReactMarkdown from 'react-markdown'
import type { Message } from '@/lib/types'
import SuggestionCards from './SuggestionCards'

interface MessageBubbleProps {
  message: Message
  /** Callback when a suggestion card is clicked */
  onSuggestionSelect?: (text: string) => void
  /** Whether suggestion cards are disabled (during streaming) */
  suggestionsDisabled?: boolean
}

export default function MessageBubble({
  message,
  onSuggestionSelect,
  suggestionsDisabled = false,
}: MessageBubbleProps) {
  const isUser = message.role === 'user'

  if (isUser) {
    return (
      <div className="flex justify-end mb-4">
        <div className="max-w-[85%] sm:max-w-[75%]">
          {/* User message: right-aligned, rounded gray bubble */}
          <div className="bg-gray-100 rounded-2xl rounded-tr-sm px-4 py-3">
            <p className="text-sm text-gray-800 whitespace-pre-wrap break-words">
              {message.content}
            </p>
          </div>
        </div>
      </div>
    )
  }

  // Assistant message
  return (
    <div className="flex flex-col mb-6">
      {/* AI response: full width, no bubble border, following mainstream AI product styles */}
      <div className="w-full">
        <div className="prose prose-sm max-w-none text-gray-800">
          <ReactMarkdown>{message.content}</ReactMarkdown>
        </div>
      </div>

      {/* Dynamic suggestion cards (shown after the done event) */}
      {message.suggestions && message.suggestions.length > 0 && onSuggestionSelect && (
        <div className="mt-3 w-full">
          <SuggestionCards
            suggestions={message.suggestions}
            onSelect={onSuggestionSelect}
            disabled={suggestionsDisabled}
          />
        </div>
      )}
    </div>
  )
}
