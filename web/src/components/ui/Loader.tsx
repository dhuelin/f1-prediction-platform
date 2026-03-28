import { cn } from '@/lib/utils'

export type LoaderSize = 'sm' | 'md' | 'lg' | 'xl'

export interface LoaderProps {
  size?: LoaderSize
  className?: string
  label?: string
}

export interface FullScreenLoaderProps {
  label?: string
}

const sizeClasses: Record<LoaderSize, string> = {
  sm: 'h-4 w-4 border-2',
  md: 'h-6 w-6 border-2',
  lg: 'h-8 w-8 border-[3px]',
  xl: 'h-12 w-12 border-4',
}

export function Loader({ size = 'md', className, label = 'Loading…' }: LoaderProps) {
  return (
    <span
      role="status"
      aria-label={label}
      className={cn('inline-block', className)}
    >
      <span
        className={cn(
          'block animate-spin rounded-full border-border border-t-f1-red',
          sizeClasses[size],
        )}
        aria-hidden="true"
      />
      <span className="sr-only">{label}</span>
    </span>
  )
}

export function FullScreenLoader({ label = 'Loading…' }: FullScreenLoaderProps) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-bg/80 backdrop-blur-sm"
      role="status"
      aria-label={label}
    >
      <div className="flex flex-col items-center gap-4">
        <span
          className="block h-12 w-12 animate-spin rounded-full border-4 border-border border-t-f1-red"
          aria-hidden="true"
        />
        <p className="text-sm text-text-secondary">{label}</p>
      </div>
    </div>
  )
}

export default Loader
