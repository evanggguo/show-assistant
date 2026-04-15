/**
 * 客户端后端 REST API 请求封装
 *
 * 注意：SSE 流式对话不在此封装，
 * 直接在 useChatStream Hook 内通过 fetch + ReadableStream 处理。
 */

import type { OwnerProfile } from './types'

/** 后端 API 地址，优先读取环境变量 */
const API_BASE =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

async function apiFetch<T>(path: string): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { Accept: 'application/json' },
  })
  if (!res.ok) {
    throw new Error(`API ${path} 返回 ${res.status}: ${res.statusText}`)
  }
  const body = await res.json()
  // 后端统一包装格式 { success, data }
  if (body && typeof body === 'object' && 'data' in body) {
    return body.data as T
  }
  return body as T
}

/**
 * 获取指定 owner 的公开简介
 * GET /api/owners/{ownerUsername}/profile
 */
export async function fetchOwnerProfile(ownerUsername: string): Promise<OwnerProfile> {
  return apiFetch<OwnerProfile>(`/api/owners/${ownerUsername}/profile`)
}

/**
 * 获取指定 owner 的初始建议提示词列表
 * GET /api/owners/{ownerUsername}/suggestions
 */
export async function fetchInitialSuggestions(ownerUsername: string): Promise<string[]> {
  return apiFetch<string[]>(`/api/owners/${ownerUsername}/suggestions`)
}
