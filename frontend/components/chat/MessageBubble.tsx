'use client'

/**
 * MessageBubble — 单条消息气泡
 * 对应 TDD 1.4 消息界面设计
 *
 * - role=user：右对齐，灰色背景
 * - role=assistant：左对齐，白色背景，Markdown 渲染
 * - assistant 消息下方展示动态提示词卡片（有 suggestions 时）
 */

import React from 'react'
import ReactMarkdown from 'react-markdown'
import type { Message } from '@/lib/types'
import SuggestionCards from './SuggestionCards'

interface MessageBubbleProps {
  message: Message
  /** 点击提示词卡片的回调 */
  onSuggestionSelect?: (text: string) => void
  /** 是否禁用提示词卡片（流式输出中） */
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
          {/* 用户消息：右对齐，灰色背景圆角气泡 */}
          <div className="bg-gray-100 rounded-2xl rounded-tr-sm px-4 py-3">
            <p className="text-sm text-gray-800 whitespace-pre-wrap break-words">
              {message.content}
            </p>
          </div>
        </div>
      </div>
    )
  }

  // assistant 消息
  return (
    <div className="flex flex-col items-start mb-4">
      {/* AI 气泡：左对齐，白色背景，Markdown 渲染 */}
      <div className="max-w-[85%] sm:max-w-[75%]">
        <div className="bg-white border border-gray-100 rounded-2xl rounded-tl-sm px-4 py-3 shadow-sm">
          <div className="prose prose-sm max-w-none text-gray-800">
            <ReactMarkdown>{message.content}</ReactMarkdown>
          </div>
        </div>
      </div>

      {/* 动态提示词卡片（done 事件后展示） */}
      {message.suggestions && message.suggestions.length > 0 && onSuggestionSelect && (
        <div className="mt-2 w-full max-w-[85%] sm:max-w-[75%]">
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
