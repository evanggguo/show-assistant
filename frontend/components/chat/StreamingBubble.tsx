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

interface StreamingBubbleProps {
  text: string
}

export default function StreamingBubble({ text }: StreamingBubbleProps) {
  return (
    <div className="mb-6">
      {/* AI response: full width, no bubble border */}
      <div className="prose prose-sm max-w-none text-gray-800">
        <ReactMarkdown>{text || ''}</ReactMarkdown>
      </div>
      {/* Blinking cursor animation */}
      <span
        className="inline-block w-[2px] h-4 bg-gray-600 ml-0.5 align-middle animate-pulse"
        aria-hidden="true"
      />
    </div>
  )
}
