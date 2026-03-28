import React from 'react'
import { cn } from '@/lib/utils'

export interface CardProps {
  children: React.ReactNode
  header?: React.ReactNode
  footer?: React.ReactNode
  className?: string
  headerClassName?: string
  bodyClassName?: string
  footerClassName?: string
  noPadding?: boolean
}

export function Card({
  children,
  header,
  footer,
  className,
  headerClassName,
  bodyClassName,
  footerClassName,
  noPadding = false,
}: CardProps) {
  return (
    <div
      className={cn(
        'rounded-lg border border-border bg-surface shadow-sm',
        className,
      )}
    >
      {header && (
        <div
          className={cn(
            'border-b border-border px-5 py-4',
            headerClassName,
          )}
        >
          {header}
        </div>
      )}

      <div className={cn(!noPadding && 'px-5 py-4', bodyClassName)}>
        {children}
      </div>

      {footer && (
        <div
          className={cn(
            'border-t border-border px-5 py-4',
            footerClassName,
          )}
        >
          {footer}
        </div>
      )}
    </div>
  )
}

export default Card
