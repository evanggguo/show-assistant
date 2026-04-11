'use client'

/**
 * StreamingBubble — 流式输出中的 AI 气泡
 * 对应 TDD 4.3 流式渲染
 *
 * 特性：
 * - 末尾显示光标 ▍（CSS animate-pulse 闪烁）
 * - 使用 react-markdown 边流边渲染 Markdown
 */

import React from 'react'
import ReactMarkdown from 'react-markdown'

interface StreamingBubbleProps {
  text: string
}

export default function StreamingBubble({ text }: StreamingBubbleProps) {
  return (
    <div className="flex justify-start mb-4">
      <div className="max-w-[85%] sm:max-w-[75%]">
        {/* AI 气泡：左对齐，白色背景 */}
        <div className="bg-white border border-gray-100 rounded-2xl rounded-tl-sm px-4 py-3 shadow-sm">
          <div className="prose prose-sm max-w-none text-gray-800">
            <ReactMarkdown>{text || ''}</ReactMarkdown>
          </div>
          {/* 光标闪烁动画 */}
          <span
            className="inline-block w-[2px] h-4 bg-gray-600 ml-0.5 align-middle animate-pulse"
            aria-hidden="true"
          />
        </div>
      </div>
    </div>
  )
}
