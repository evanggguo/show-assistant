'use client'

/**
 * ChatInput — Fixed bottom input bar.
 * Corresponds to TDD 1.4 input area design.
 *
 * Features:
 * - Textarea supports multi-line input
 * - Enter to send, Shift+Enter for new line
 * - Disabled during streaming; send button shows loading state
 * - Input border highlights on focus
 * - Mobile-friendly (sticky bottom, does not overlap messages)
 */

import React, { useState, useRef, useCallback, KeyboardEvent } from 'react'
import { SendHorizontal, Loader2 } from 'lucide-react'

interface ChatInputProps {
  onSend: (text: string) => void
  disabled?: boolean
  isLoading?: boolean
  placeholder?: string
}

export default function ChatInput({
  onSend,
  disabled = false,
  isLoading = false,
  placeholder = 'Send a message...',
}: ChatInputProps) {
  const [value, setValue] = useState('')
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const canSend = value.trim().length > 0 && !disabled && !isLoading

  /** Send the message and clear the input */
  const handleSend = useCallback(() => {
    if (!canSend) return
    onSend(value.trim())
    setValue('')
    // Reset textarea height
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
    }
  }, [canSend, onSend, value])

  /** Enter to send, Shift+Enter for new line */
  const handleKeyDown = useCallback(
    (e: KeyboardEvent<HTMLTextAreaElement>) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        handleSend()
      }
    },
    [handleSend]
  )

  /** Auto-resize textarea height (max 6 lines) */
  const handleChange = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setValue(e.target.value)
    const el = e.target
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 144) + 'px' // 6 lines ≈ 144px
  }, [])

  return (
    <div className="sticky bottom-0 bg-white border-t border-gray-100 px-4 py-3 shadow-[0_-1px_8px_rgba(0,0,0,0.04)]">
      <div className="max-w-[800px] mx-auto">
        <div
          className={[
            'flex items-end gap-2 rounded-2xl border bg-white px-4 py-2',
            'transition-colors duration-150',
            disabled || isLoading
              ? 'border-gray-200 bg-gray-50'
              : 'border-gray-300 focus-within:border-blue-400 focus-within:ring-2 focus-within:ring-blue-100',
          ].join(' ')}
        >
          <textarea
            ref={textareaRef}
            value={value}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            disabled={disabled || isLoading}
            placeholder={isLoading ? 'AI is typing...' : placeholder}
            rows={1}
            className={[
              'flex-1 resize-none bg-transparent text-sm text-gray-800',
              'outline-none placeholder-gray-400 py-1.5',
              'max-h-36 leading-relaxed',
              (disabled || isLoading) && 'cursor-not-allowed opacity-60',
            ]
              .filter(Boolean)
              .join(' ')}
            style={{ scrollbarWidth: 'thin' }}
          />

          {/* Send button */}
          <button
            onClick={handleSend}
            disabled={!canSend}
            aria-label="Send message"
            className={[
              'flex-shrink-0 w-8 h-8 flex items-center justify-center',
              'rounded-full transition-colors duration-150 mb-0.5',
              canSend
                ? 'bg-blue-500 hover:bg-blue-600 text-white cursor-pointer'
                : 'bg-gray-200 text-gray-400 cursor-not-allowed',
            ].join(' ')}
          >
            {isLoading ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <SendHorizontal className="w-4 h-4" />
            )}
          </button>
        </div>

        {/* Keyboard hint */}
        <p className="text-xs text-gray-400 mt-1.5 text-center select-none">
          Enter to send &nbsp;·&nbsp; Shift+Enter for new line
        </p>
      </div>
    </div>
  )
}
