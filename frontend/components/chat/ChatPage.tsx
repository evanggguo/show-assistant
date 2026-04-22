'use client'

/**
 * ChatPage — Main conversation container.
 * Corresponds to TDD 4.3 frontend main page logic.
 *
 * Responsibilities:
 * 1. Fetch /api/owner/profile and /api/suggestions/initial on mount
 * 2. Manage home screen (no messages) vs. active conversation views
 * 3. Coordinate useChatStream, MessageList, ChatInput, and SuggestionCards
 * 4. Display error alerts
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

/** Fallback initial suggestions when the backend is unavailable */
const FALLBACK_SUGGESTIONS = [
  'What are your areas of expertise?',
  'Tell me about yourself',
  'What can you help me with?',
  'Any recent projects you\'d like to share?',
]

/** Fallback owner profile when the backend is unavailable */
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

  // ── Fetch owner profile and initial suggestions ───────────
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

  // ── Streaming chat hook ───────────────────────────────────
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

  /** Clicking a suggestion card sends its text as a message */
  const handleSuggestionSelect = useCallback(
    (text: string) => {
      sendMessage(text)
    },
    [sendMessage]
  )

  // Home screen: no messages and not currently streaming
  const isHomePage = messages.length === 0 && !isStreaming

  return (
    <div className="flex flex-col h-screen bg-gray-50">
      {/* ── Owner profile bar (compact mode during conversation) ── */}
      {!isHomePage && !profileLoading && (
        <OwnerProfile profile={ownerProfile} variant="compact" />
      )}

      {/* ── Main content area ───────────────────────────────── */}
      {isHomePage ? (
        // Home screen: centered welcome content
        <div className="flex-1 flex flex-col items-center justify-center px-4 overflow-y-auto">
          <div className="w-full max-w-[800px] flex flex-col items-center gap-6">
            {/* Owner hero avatar */}
            {!profileLoading && (
              <OwnerProfile profile={ownerProfile} ownerUsername={ownerUsername} variant="hero" />
            )}
            {profileLoading && (
              <div className="w-20 h-20 rounded-full bg-gray-200 animate-pulse" />
            )}

            {/* Welcome message */}
            <div className="text-center">
              <p className="text-xl font-medium text-gray-700 mb-1">
                I am {ownerUsername}&apos;s assistant.
              </p>
              <h2 className="text-xl font-medium text-gray-700">
                How can I help you?
              </h2>
              <p className="text-sm text-gray-400 mt-1">
                Click a card below or type a message to start
              </p>
            </div>

            {/* Initial suggestion cards */}
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
        // Active conversation: message list
        <MessageList
          messages={messages}
          streamingText={streamingText}
          isStreaming={isStreaming}
          onSuggestionSelect={handleSuggestionSelect}
        />
      )}

      {/* ── Error alert ─────────────────────────────────────── */}
      {error && (
        <div className="px-4 py-2 max-w-[800px] mx-auto w-full">
          <ErrorAlert
            message={error}
            onDismiss={clearError}
            onRetry={retryLastMessage}
          />
        </div>
      )}

      {/* ── Bottom input bar ────────────────────────────────── */}
      <ChatInput
        onSend={sendMessage}
        disabled={isStreaming}
        isLoading={isStreaming}
      />
    </div>
  )
}
