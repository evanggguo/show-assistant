/**
 * Guest session local storage management.
 * Corresponds to TDD 4.3 guest mode localStorage persistence.
 *
 * Storage strategy:
 * - key: guest_messages_{ownerUsername} — isolated per owner, capped at MAX_MESSAGES entries
 */

import type { Message } from './types'

const MAX_MESSAGES = 20

function messagesKey(ownerUsername: string): string {
  return `guest_messages_${ownerUsername}`
}

/**
 * Load guest message history for the given owner from localStorage.
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
 * Persist the message list to localStorage (isolated per owner).
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
    // Silently ignore write failures (private mode / storage full)
  }
}

/**
 * Clear guest message history for the given owner.
 */
export function clearGuestMessages(ownerUsername: string): void {
  if (typeof window === 'undefined') return
  try {
    localStorage.removeItem(messagesKey(ownerUsername))
  } catch {
    // Silently ignore
  }
}
