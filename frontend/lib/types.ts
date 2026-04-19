/**
 * Core type definitions.
 * Corresponds to TDD section 4.3 Dossier frontend data structures.
 */

/**
 * A single message supporting user and assistant roles.
 */
export interface Message {
  id?: number
  role: 'user' | 'assistant'
  content: string
  /** Dynamic follow-up suggestions for this message (populated on assistant messages after the done event) */
  suggestions?: string[]
  /** Whether this message is currently being streamed */
  isStreaming?: boolean
}

/**
 * Owner public profile (fetched from backend /api/owner/profile).
 */
export interface OwnerProfile {
  name: string
  tagline: string
  avatarUrl?: string
}

/**
 * Internal state structure of the useChatStream hook.
 * Corresponds to TDD 4.3.3.
 */
export interface ChatStreamState {
  messages: Message[]
  /** Text currently being streamed (cleared when streaming ends) */
  streamingText: string
  isStreaming: boolean
  /** Suggestions returned by the most recent done event */
  suggestions: string[]
  error: string | null
}

/**
 * Request body structure for sending a message.
 * Corresponds to TDD 4.3 ChatRequest.
 */
export interface ChatRequest {
  /** Conversation ID; omitted in guest mode */
  conversationId?: number
  message: string
  /** Full message history (guest mode) */
  history?: { role: string; content: string }[]
}

/**
 * Data payload of the SSE done event.
 */
export interface SseDoneData {
  messageId: number
  suggestions?: string[]
}

/**
 * Data payload of the SSE error event.
 */
export interface SseErrorData {
  code: string
  message: string
}

/**
 * Data payload of the SSE token event.
 */
export interface SseTokenData {
  text: string
}
