'use client'

/**
 * OwnerProfile — Owner 简介组件
 * 对应 TDD 1.4 首屏 Owner 信息展示
 *
 * variant:
 * - "hero"    首屏大尺寸（居中，头像 80px，名字大字体）
 * - "compact" 对话中顶部小尺寸（头像 32px，行内展示）
 */

import React from 'react'
import Image from 'next/image'
import type { OwnerProfile as OwnerProfileType } from '@/lib/types'

interface OwnerProfileProps {
  profile: OwnerProfileType
  variant?: 'hero' | 'compact'
}

export default function OwnerProfile({
  profile,
  variant = 'hero',
}: OwnerProfileProps) {
  if (variant === 'compact') {
    return (
      <div className="flex items-center gap-2 px-4 py-2 border-b border-gray-100 bg-white">
        <div className="max-w-[800px] mx-auto flex items-center gap-2 w-full">
          {/* 头像（compact：32px） */}
          <Avatar name={profile.name} avatarUrl={profile.avatarUrl} size={32} />
          <div className="flex items-baseline gap-2">
            <span className="text-sm font-semibold text-gray-800">{profile.name}</span>
            <span className="text-xs text-gray-400 hidden sm:inline">{profile.tagline}</span>
          </div>
        </div>
      </div>
    )
  }

  // hero 模式
  return (
    <div className="flex flex-col items-center gap-3 py-6">
      {/* 头像（hero：80px） */}
      <Avatar name={profile.name} avatarUrl={profile.avatarUrl} size={80} />
      <div className="text-center">
        <h1 className="text-2xl font-bold text-gray-900">{profile.name}</h1>
        <p className="text-sm text-gray-500 mt-1">{profile.tagline}</p>
      </div>
    </div>
  )
}

// ─── 内部子组件 ────────────────────────────────────────────────

interface AvatarProps {
  name: string
  avatarUrl?: string
  size: number
}

/**
 * Avatar — 头像组件
 * 有 avatarUrl 时展示图片，否则展示首字母占位符
 */
function Avatar({ name, avatarUrl, size }: AvatarProps) {
  const initial = name ? name.charAt(0).toUpperCase() : '?'

  if (avatarUrl) {
    return (
      <Image
        src={avatarUrl}
        alt={`${name} 的头像`}
        width={size}
        height={size}
        className="rounded-full object-cover"
        style={{ width: size, height: size }}
      />
    )
  }

  // 无图片时：渐变背景 + 首字母
  const fontSize = size >= 60 ? 'text-2xl' : 'text-sm'
  return (
    <div
      className={`rounded-full bg-gradient-to-br from-blue-400 to-indigo-500 flex items-center justify-center text-white font-bold ${fontSize} flex-shrink-0`}
      style={{ width: size, height: size }}
    >
      {initial}
    </div>
  )
}
