/**
 * 管理端 TypeScript 类型定义
 */

export interface OwnerProfileData {
  id: number
  name: string
  tagline: string
  avatarUrl?: string
  contact?: Record<string, string>
  customPrompt?: string
}

export interface UpdateOwnerData {
  name?: string
  tagline?: string
  avatarUrl?: string
  contact?: Record<string, string>
  customPrompt?: string
}

export interface SuggestionData {
  id: number
  text: string
  sortOrder: number
  enabled: boolean
}

export interface CreateSuggestionData {
  text: string
  sortOrder: number
  enabled?: boolean
}

export interface UpdateSuggestionData {
  text?: string
  sortOrder?: number
  enabled?: boolean
}

export interface KnowledgeData {
  id: number
  type: string
  title?: string
  content: string
  createdAt: string
}

export interface CreateKnowledgeData {
  type: string
  title?: string
  content: string
}

export interface UpdateKnowledgeData {
  title?: string
  content?: string
}

export interface OwnerSummaryData {
  id: number
  username: string
  name: string
  createdAt: string
}

export interface DocumentData {
  id: number
  filename: string
  fileType: string
  fileSize: number
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  createdAt: string
}
