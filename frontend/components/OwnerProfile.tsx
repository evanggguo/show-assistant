'use client'

/**
 * OwnerProfile — Owner profile display component.
 * Corresponds to TDD 1.4 home-screen owner info.
 *
 * variant:
 * - "hero"    Large size on home screen (centered, 80px avatar, large name)
 * - "compact" Small size at top during conversation (32px avatar, inline)
 */

import React from 'react'
import Image from 'next/image'
import type { OwnerProfile as OwnerProfileType } from '@/lib/types'

interface OwnerProfileProps {
  profile: OwnerProfileType
  ownerUsername?: string
  variant?: 'hero' | 'compact'
}

export default function OwnerProfile({
  profile,
  ownerUsername,
  variant = 'hero',
}: OwnerProfileProps) {
  if (variant === 'compact') {
    return (
      <div className="flex items-center gap-2 px-4 py-2 border-b border-gray-100 bg-white">
        <div className="max-w-[800px] mx-auto flex items-center gap-2 w-full">
          {/* Avatar (compact: 32px) */}
          <Avatar name={profile.name} avatarUrl={profile.avatarUrl} size={32} />
          <div className="flex items-baseline gap-2">
            <span className="text-sm font-semibold text-gray-800">{profile.name}</span>
            <span className="text-xs text-gray-400 hidden sm:inline">{profile.tagline}</span>
          </div>
        </div>
      </div>
    )
  }

  // Hero mode
  return (
    <div className="flex flex-col items-center gap-3 py-6">
      <p className="text-2xl font-semibold text-gray-700">
        Welcome to Dossier System
      </p>
      {/* Avatar (hero: 80px) */}
      <Avatar name={profile.name} avatarUrl={profile.avatarUrl} size={80} />
      <div className="text-center">
        <p className="text-base text-gray-600">I am {ownerUsername ?? profile.name}&apos;s assistant</p>
        {profile.tagline && (
          <p className="text-sm text-gray-400 mt-1">{profile.tagline}</p>
        )}
      </div>
    </div>
  )
}

// ─── Internal sub-components ──────────────────────────────────

interface AvatarProps {
  name: string
  avatarUrl?: string
  size: number
}

/**
 * Avatar — Avatar component.
 * Shows an image when avatarUrl is set; otherwise shows an initial-letter placeholder.
 */
function Avatar({ name, avatarUrl, size }: AvatarProps) {
  const initial = name ? name.charAt(0).toUpperCase() : '?'

  if (avatarUrl) {
    return (
      <Image
        src={avatarUrl}
        alt={`${name}'s avatar`}
        width={size}
        height={size}
        className="rounded-full object-cover"
        style={{ width: size, height: size }}
      />
    )
  }

  // No image: gradient background with initial letter
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
