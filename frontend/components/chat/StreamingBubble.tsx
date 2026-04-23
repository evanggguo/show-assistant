'use client'

/**
 * StreamingBubble — AI bubble shown during streaming output.
 * Corresponds to TDD 4.3 streaming rendering.
 *
 * Features:
 * - Blinking cursor ▍ at the end (CSS animate-pulse)
 * - Renders Markdown incrementally using react-markdown
 */

import React from 'react'
import ReactMarkdown from 'react-markdown'
import TypingIndicator from './TypingIndicator'

interface StreamingBubbleProps {
  text: string
}

export default function StreamingBubble({ text }: StreamingBubbleProps) {
  return (
    <div className="mb-6">
      {/* AI response text — absent while waiting for the first token */}
      {text && (
        <div className="prose prose-sm max-w-none text-gray-800">
          <ReactMarkdown>{text}</ReactMarkdown>
        </div>
      )}
      {/* Three dots always visible until streaming completes */}
      <TypingIndicator />
    </div>
  )
}
