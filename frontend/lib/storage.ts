/**
 * 游客会话本地存储管理
 * 对应 TDD 4.3 游客模式 localStorage 持久化
 *
 * 存储策略：
 * - key: guest_conversation_id — 游客会话 ID（MVP 暂不持久化）
 * - key: guest_messages — 历史消息列表，最多保存 MAX_MESSAGES 条
 */

import type { Message } from './types'

const MESSAGES_KEY = 'guest_messages'
const MAX_MESSAGES = 20

/**
 * 从 localStorage 读取游客历史消息
 * 解析失败时返回空数组（避免崩溃）
 */
export function loadGuestMessages(): Message[] {
  if (typeof window === 'undefined') return []
  try {
    const raw = localStorage.getItem(MESSAGES_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed as Message[]
  } catch {
    return []
  }
}

/**
 * 将消息列表写入 localStorage
 * 超出 MAX_MESSAGES 条时截断最早的消息
 *
 * @param messages 要保存的消息列表
 */
export function saveGuestMessages(messages: Message[]): void {
  if (typeof window === 'undefined') return
  try {
    const trimmed =
      messages.length > MAX_MESSAGES
        ? messages.slice(messages.length - MAX_MESSAGES)
        : messages
    localStorage.setItem(MESSAGES_KEY, JSON.stringify(trimmed))
  } catch {
    // 写入失败（隐私模式/存储满）时静默忽略
  }
}

/**
 * 清除游客历史消息
 */
export function clearGuestMessages(): void {
  if (typeof window === 'undefined') return
  try {
    localStorage.removeItem(MESSAGES_KEY)
  } catch {
    // 静默忽略
  }
}
