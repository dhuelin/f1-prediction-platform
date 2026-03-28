import React from 'react'
import { cn } from '@/lib/utils'

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  hint?: string
  leftIcon?: React.ReactNode
  rightIcon?: React.ReactNode
  wrapperClassName?: string
}

export function Input({
  label,
  error,
  hint,
  leftIcon,
  rightIcon,
  wrapperClassName,
  className,
  id,
  ...props
}: InputProps) {
  const inputId = id ?? (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined)

  return (
    <div className={cn('flex flex-col gap-1.5', wrapperClassName)}>
      {label && (
        <label
          htmlFor={inputId}
          className="text-sm font-medium text-text-primary"
        >
          {label}
          {props.required && (
            <span className="ml-1 text-f1-red" aria-hidden="true">*</span>
          )}
        </label>
      )}

      <div className="relative flex items-center">
        {leftIcon && (
          <span className="pointer-events-none absolute left-3 flex items-center text-text-muted">
            {leftIcon}
          </span>
        )}

        <input
          id={inputId}
          className={cn(
            'w-full rounded-md border bg-surface px-3 py-2 text-base text-text-primary',
            'placeholder:text-text-muted',
            'transition-colors duration-150',
            'focus:outline-none focus:ring-2 focus:ring-f1-red focus:border-f1-red',
            error
              ? 'border-red-500 focus:ring-red-500 focus:border-red-500'
              : 'border-border hover:border-border',
            leftIcon && 'pl-10',
            rightIcon && 'pr-10',
            props.disabled && 'opacity-50 cursor-not-allowed',
            className,
          )}
          aria-invalid={!!error}
          aria-describedby={
            error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined
          }
          {...props}
        />

        {rightIcon && (
          <span className="absolute right-3 flex items-center text-text-muted">
            {rightIcon}
          </span>
        )}
      </div>

      {error && (
        <p id={`${inputId}-error`} className="text-sm text-red-500" role="alert">
          {error}
        </p>
      )}

      {!error && hint && (
        <p id={`${inputId}-hint`} className="text-sm text-text-muted">
          {hint}
        </p>
      )}
    </div>
  )
}

export default Input
