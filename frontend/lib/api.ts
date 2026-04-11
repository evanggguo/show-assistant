/**
 * 后端 REST API 请求封装
 * 对应 TDD 4.3 后端接口调用
 *
 * 注意：SSE 流式对话（/api/chat）不在此封装，
 * 直接在 useChatStream Hook 内通过 fetch + ReadableStream 处理，
 * 避免经过 Next.js API Route 导致 SSE 被缓冲。
 */

import type { OwnerProfile } from './types'

/** 后端 API 地址，优先读取环境变量 */
const API_BASE =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

/**
 * 通用 GET 请求，附带 JSON 解析和错误处理
 */
async function apiFetch<T>(path: string): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { Accept: 'application/json' },
  })
  if (!res.ok) {
    throw new Error(`API ${path} 返回 ${res.status}: ${res.statusText}`)
  }
  return res.json() as Promise<T>
}

/**
 * 获取 Owner 简介
 * GET /api/owner/profile
 * 对应 TDD 1.4 首屏 Owner 信息展示
 */
export async function fetchOwnerProfile(): Promise<OwnerProfile> {
  return apiFetch<OwnerProfile>('/api/owner/profile')
}

/**
 * 获取初始建议提示词列表（首屏展示）
 * GET /api/suggestions/initial
 * 对应 TDD 4.3 首屏初始提示词卡片
 */
export async function fetchInitialSuggestions(): Promise<string[]> {
  return apiFetch<string[]>('/api/suggestions/initial')
}
