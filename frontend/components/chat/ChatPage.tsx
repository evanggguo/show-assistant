'use client'

/**
 * ChatPage — 主对话容器
 * 对应 TDD 4.3 前端主页面逻辑
 *
 * 职责：
 * 1. 挂载时请求 /api/owner/profile 和 /api/suggestions/initial
 * 2. 管理首屏（无消息）vs 对话中两种视图
 * 3. 协调 useChatStream、MessageList、ChatInput、SuggestionCards
 * 4. 错误提示展示
 */

import React, { useEffect, useState, useCallback } from 'react'
import type { OwnerProfile as OwnerProfileType } from '@/lib/types'
import { fetchOwnerProfile, fetchInitialSuggestions } from '@/lib/api'
import { useChatStream } from '@/hooks/useChatStream'
import OwnerProfile from '@/components/OwnerProfile'
import ErrorAlert from '@/components/ErrorAlert'
import MessageList from './MessageList'
import SuggestionCards from './SuggestionCards'
import ChatInput from './ChatInput'

/** 后端不可用时的降级初始提示词 */
const FALLBACK_SUGGESTIONS = [
  'What are your areas of expertise?',
  'Tell me about yourself',
  'What can you help me with?',
  'Any recent projects you\'d like to share?',
]

/** 后端不可用时的降级 Owner 简介 */
const FALLBACK_PROFILE: OwnerProfileType = {
  name: 'Dossier',
  tagline: 'Welcome',
}

interface ChatPageProps {
  ownerUsername: string
}

export default function ChatPage({ ownerUsername }: ChatPageProps) {
  const [ownerProfile, setOwnerProfile] = useState<OwnerProfileType>(FALLBACK_PROFILE)
  const [initialSuggestions, setInitialSuggestions] =
    useState<string[]>(FALLBACK_SUGGESTIONS)
  const [profileLoading, setProfileLoading] = useState(true)

  // ── 获取 Owner 简介和初始提示词 ────────────────────────────
  useEffect(() => {
    let cancelled = false

    async function loadInitialData() {
      try {
        const [profile, suggestions] = await Promise.allSettled([
          fetchOwnerProfile(ownerUsername),
          fetchInitialSuggestions(ownerUsername),
        ])

        if (cancelled) return

        if (profile.status === 'fulfilled') {
          setOwnerProfile(profile.value)
        }
        if (suggestions.status === 'fulfilled' && suggestions.value.length > 0) {
          setInitialSuggestions(suggestions.value)
        }
      } finally {
        if (!cancelled) setProfileLoading(false)
      }
    }

    loadInitialData()
    return () => {
      cancelled = true
    }
  }, [ownerUsername])

  // ── 流式对话 Hook ──────────────────────────────────────────
  const {
    messages,
    streamingText,
    isStreaming,
    currentSuggestions,
    error,
    sendMessage,
    retryLastMessage,
    clearError,
  } = useChatStream(ownerUsername, initialSuggestions)

  /**
   * 点击提示词卡片：直接发送该文本
   */
  const handleSuggestionSelect = useCallback(
    (text: string) => {
      sendMessage(text)
    },
    [sendMessage]
  )

  // 是否处于首屏（无消息且未在流式中）
  const isHomePage = messages.length === 0 && !isStreaming

  return (
    <div className="flex flex-col h-screen bg-gray-50">
      {/* ── 顶部 Owner 简介（对话中显示 compact 模式） ─────── */}
      {!isHomePage && !profileLoading && (
        <OwnerProfile profile={ownerProfile} variant="compact" />
      )}

      {/* ── 主内容区 ────────────────────────────────────────── */}
      {isHomePage ? (
        // 首屏：居中展示欢迎内容
        <div className="flex-1 flex flex-col items-center justify-center px-4 overflow-y-auto">
          <div className="w-full max-w-[800px] flex flex-col items-center gap-6">
            {/* Owner 大头像 */}
            {!profileLoading && (
              <OwnerProfile profile={ownerProfile} variant="hero" />
            )}
            {profileLoading && (
              <div className="w-20 h-20 rounded-full bg-gray-200 animate-pulse" />
            )}

            {/* 欢迎语 */}
            <div className="text-center">
              <h2 className="text-xl font-medium text-gray-700">
                How can I help you?
              </h2>
              <p className="text-sm text-gray-400 mt-1">
                Click a card below or type a message to start
              </p>
            </div>

            {/* 初始提示词卡片 */}
            <div className="w-full">
              <SuggestionCards
                suggestions={initialSuggestions}
                onSelect={handleSuggestionSelect}
                disabled={isStreaming}
              />
            </div>
          </div>
        </div>
      ) : (
        // 对话中：消息列表
        <MessageList
          messages={messages}
          streamingText={streamingText}
          isStreaming={isStreaming}
          onSuggestionSelect={handleSuggestionSelect}
        />
      )}

      {/* ── 错误提示 ─────────────────────────────────────────── */}
      {error && (
        <div className="px-4 py-2 max-w-[800px] mx-auto w-full">
          <ErrorAlert
            message={error}
            onDismiss={clearError}
            onRetry={retryLastMessage}
          />
        </div>
      )}

      {/* ── 底部输入框 ────────────────────────────────────────── */}
      <ChatInput
        onSend={sendMessage}
        disabled={isStreaming}
        isLoading={isStreaming}
      />
    </div>
  )
}
