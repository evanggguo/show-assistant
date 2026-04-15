/**
 * 游客会话本地存储管理
 * 对应 TDD 4.3 游客模式 localStorage 持久化
 *
 * 存储策略：
 * - key: guest_messages_{ownerUsername} — 各 owner 隔离存储，最多保存 MAX_MESSAGES 条
 */

import type { Message } from './types'

const MAX_MESSAGES = 20

function messagesKey(ownerUsername: string): string {
  return `guest_messages_${ownerUsername}`
}

/**
 * 从 localStorage 读取指定 owner 的游客历史消息
 */
export function loadGuestMessages(ownerUsername: string): Message[] {
  if (typeof window === 'undefined') return []
  try {
    const raw = localStorage.getItem(messagesKey(ownerUsername))
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed as Message[]
  } catch {
    return []
  }
}

/**
 * 将消息列表写入 localStorage（按 owner 隔离）
 */
export function saveGuestMessages(ownerUsername: string, messages: Message[]): void {
  if (typeof window === 'undefined') return
  try {
    const trimmed =
      messages.length > MAX_MESSAGES
        ? messages.slice(messages.length - MAX_MESSAGES)
        : messages
    localStorage.setItem(messagesKey(ownerUsername), JSON.stringify(trimmed))
  } catch {
    // 写入失败（隐私模式/存储满）时静默忽略
  }
}

/**
 * 清除指定 owner 的游客历史消息
 */
export function clearGuestMessages(ownerUsername: string): void {
  if (typeof window === 'undefined') return
  try {
    localStorage.removeItem(messagesKey(ownerUsername))
  } catch {
    // 静默忽略
  }
}
