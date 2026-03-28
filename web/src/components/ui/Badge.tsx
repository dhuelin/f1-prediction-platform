import React from 'react'
import { cn } from '@/lib/utils'

export type BadgeVariant =
  | 'default'
  | 'upcoming'
  | 'live'
  | 'finished'
  | 'locked'
  | 'primary'
  | 'success'
  | 'warning'
  | 'danger'

export type BadgeSize = 'sm' | 'md'

export interface BadgeProps {
  variant?: BadgeVariant
  size?: BadgeSize
  children: React.ReactNode
  className?: string
  dot?: boolean
}

const variantClasses: Record<BadgeVariant, string> = {
  default: 'bg-surface-raised text-text-secondary border-border',
  upcoming: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  live: 'bg-green-500/10 text-green-400 border-green-500/20',
  finished: 'bg-gray-500/10 text-gray-400 border-gray-500/20',
  locked: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  primary: 'bg-f1-red/10 text-f1-red border-f1-red/20',
  success: 'bg-green-500/10 text-green-400 border-green-500/20',
  warning: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  danger: 'bg-red-500/10 text-red-400 border-red-500/20',
}

const dotColours: Record<BadgeVariant, string> = {
  default: 'bg-text-muted',
  upcoming: 'bg-blue-400',
  live: 'bg-green-400 animate-pulse',
  finished: 'bg-gray-400',
  locked: 'bg-amber-400',
  primary: 'bg-f1-red',
  success: 'bg-green-400',
  warning: 'bg-amber-400',
  danger: 'bg-red-400',
}

const sizeClasses: Record<BadgeSize, string> = {
  sm: 'px-1.5 py-0.5 text-xs gap-1',
  md: 'px-2.5 py-1 text-sm gap-1.5',
}

export function Badge({
  variant = 'default',
  size = 'md',
  children,
  className,
  dot = false,
}: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border font-medium',
        variantClasses[variant],
        sizeClasses[size],
        className,
      )}
    >
      {dot && (
        <span
          className={cn('h-1.5 w-1.5 rounded-full flex-shrink-0', dotColours[variant])}
          aria-hidden="true"
        />
      )}
      {children}
    </span>
  )
}

export default Badge
