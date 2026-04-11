/**
 * 核心类型定义
 * 对应 TDD 4.3 节 Show Assistant 前端数据结构
 */

/**
 * 单条消息，支持 user 和 assistant 角色
 */
export interface Message {
  id?: number
  role: 'user' | 'assistant'
  content: string
  /** 该消息关联的动态提示词（仅 assistant 消息在 done 事件后填充） */
  suggestions?: string[]
  /** 是否正在流式输出（流式气泡专用） */
  isStreaming?: boolean
}

/**
 * Owner 简介（从后端 /api/owner/profile 获取）
 */
export interface OwnerProfile {
  name: string
  tagline: string
  avatarUrl?: string
}

/**
 * useChatStream Hook 内部状态结构
 * 对应 TDD 4.3.3
 */
export interface ChatStreamState {
  messages: Message[]
  /** 当前正在流式输出的文本（流结束后清空） */
  streamingText: string
  isStreaming: boolean
  /** 最近一次 done 事件带回的建议提示词 */
  suggestions: string[]
  error: string | null
}

/**
 * 发送消息的请求体结构
 * 对应 TDD 4.3 ChatRequest
 */
export interface ChatRequest {
  /** 会话 ID，游客 MVP 阶段暂不传 */
  conversationId?: number
  message: string
  /** 携带历史消息（游客模式） */
  history?: { role: string; content: string }[]
}

/**
 * SSE done 事件的数据结构
 */
export interface SseDoneData {
  messageId: number
  suggestions?: string[]
}

/**
 * SSE error 事件的数据结构
 */
export interface SseErrorData {
  code: string
  message: string
}

/**
 * SSE token 事件的数据结构
 */
export interface SseTokenData {
  text: string
}
