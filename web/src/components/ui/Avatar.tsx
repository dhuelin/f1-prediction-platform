import React from 'react'
import { cn } from '@/lib/utils'

export type AvatarSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl'

export interface AvatarProps {
  src?: string
  alt?: string
  name?: string
  size?: AvatarSize
  className?: string
}

const sizeClasses: Record<AvatarSize, string> = {
  xs: 'h-6 w-6 text-xs',
  sm: 'h-8 w-8 text-sm',
  md: 'h-10 w-10 text-base',
  lg: 'h-12 w-12 text-lg',
  xl: 'h-16 w-16 text-xl',
}

/**
 * Generate initials from a display name.
 * e.g. "Max Verstappen" → "MV", "Lewis" → "L"
 */
function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/)
  if (parts.length === 1) return parts[0].charAt(0).toUpperCase()
  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase()
}

/**
 * Deterministic hue from a string (for consistent avatar colours).
 */
function stringToHue(str: string): number {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash)
  }
  return Math.abs(hash) % 360
}

export function Avatar({ src, alt, name, size = 'md', className }: AvatarProps) {
  const [imgError, setImgError] = React.useState(false)

  const showImage = src && !imgError
  const initials = name ? getInitials(name) : '?'
  const hue = name ? stringToHue(name) : 0
  const bgStyle = !showImage
    ? { backgroundColor: `hsl(${hue}, 60%, 35%)` }
    : undefined

  return (
    <span
      className={cn(
        'inline-flex items-center justify-center rounded-full flex-shrink-0 font-semibold text-white overflow-hidden',
        sizeClasses[size],
        className,
      )}
      style={bgStyle}
      aria-label={alt ?? name}
      role="img"
    >
      {showImage ? (
        <img
          src={src}
          alt={alt ?? name ?? ''}
          className="h-full w-full object-cover"
          onError={() => setImgError(true)}
        />
      ) : (
        <span aria-hidden="true">{initials}</span>
      )}
    </span>
  )
}

export default Avatar
