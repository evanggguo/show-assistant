/**
 * Client-side REST API request wrappers.
 *
 * Note: SSE streaming chat is not handled here;
 * it is managed directly inside the useChatStream hook via fetch + ReadableStream.
 */

import type { OwnerProfile } from './types'

/** Backend API base URL, reads from environment variable if available */
const API_BASE =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

async function apiFetch<T>(path: string): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { Accept: 'application/json' },
  })
  if (!res.ok) {
    throw new Error(`API ${path} returned ${res.status}: ${res.statusText}`)
  }
  const body = await res.json()
  // Backend uses a unified wrapper format: { success, data }
  if (body && typeof body === 'object' && 'data' in body) {
    return body.data as T
  }
  return body as T
}

/**
 * Fetch the public profile for a given owner.
 * GET /api/owners/{ownerUsername}/profile
 */
export async function fetchOwnerProfile(ownerUsername: string): Promise<OwnerProfile> {
  return apiFetch<OwnerProfile>(`/api/owners/${ownerUsername}/profile`)
}

/**
 * Fetch the initial prompt suggestions for a given owner.
 * GET /api/owners/{ownerUsername}/suggestions
 */
export async function fetchInitialSuggestions(ownerUsername: string): Promise<string[]> {
  return apiFetch<string[]>(`/api/owners/${ownerUsername}/suggestions`)
}
