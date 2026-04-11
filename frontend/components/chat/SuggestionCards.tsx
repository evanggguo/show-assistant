'use client'

/**
 * SuggestionCards — 提示词卡片组
 * 对应 TDD 1.4 首屏初始提示词 & 动态提示词（done 事件后展示）
 *
 * 横向可滚动，点击卡片触发 onSelect 回调（发送给 ChatInput/ChatPage）
 */

import React from 'react'

interface SuggestionCardsProps {
  suggestions: string[]
  /** 点击某张卡片时的回调 */
  onSelect: (text: string) => void
  /** 是否禁用（流式输出中） */
  disabled?: boolean
}

export default function SuggestionCards({
  suggestions,
  onSelect,
  disabled = false,
}: SuggestionCardsProps) {
  if (!suggestions || suggestions.length === 0) return null

  return (
    <div
      className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide"
      // 隐藏滚动条但保留滚动功能（Tailwind 插件或手动样式）
      style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
    >
      {suggestions.map((text, idx) => (
        <button
          key={idx}
          onClick={() => !disabled && onSelect(text)}
          disabled={disabled}
          className={[
            'flex-shrink-0 max-w-[220px] text-left',
            'rounded-2xl border border-gray-200 bg-white px-4 py-3',
            'text-sm text-gray-700 leading-snug',
            'transition-colors duration-150',
            disabled
              ? 'cursor-not-allowed opacity-50'
              : 'hover:bg-gray-50 hover:border-gray-300 cursor-pointer',
          ].join(' ')}
        >
          {text}
        </button>
      ))}
    </div>
  )
}
