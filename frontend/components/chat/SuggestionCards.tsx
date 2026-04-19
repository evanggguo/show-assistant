'use client'

/**
 * SuggestionCards — Prompt suggestion card group.
 * Corresponds to TDD 1.4 home-screen initial suggestions & dynamic suggestions (shown after the done event).
 *
 * Horizontally scrollable; clicking a card fires the onSelect callback (forwarded to ChatInput/ChatPage).
 */

import React from 'react'

interface SuggestionCardsProps {
  suggestions: string[]
  /** Callback when a card is clicked */
  onSelect: (text: string) => void
  /** Whether cards are disabled (during streaming) */
  disabled?: boolean
}

export default function SuggestionCards({
  suggestions,
  onSelect,
  disabled = false,
}: SuggestionCardsProps) {
  if (!suggestions || suggestions.length === 0) return null

  return (
    <div className="flex flex-wrap gap-2">
      {suggestions.map((text, idx) => (
        <button
          key={idx}
          onClick={() => !disabled && onSelect(text)}
          disabled={disabled}
          className={[
            'text-left',
            'rounded-xl border border-blue-100 bg-blue-50 px-3 py-1.5',
            'text-xs text-blue-700 leading-snug',
            'transition-colors duration-150',
            disabled
              ? 'cursor-not-allowed opacity-50'
              : 'hover:bg-blue-100 hover:border-blue-200 cursor-pointer',
          ].join(' ')}
        >
          {text}
        </button>
      ))}
    </div>
  )
}
