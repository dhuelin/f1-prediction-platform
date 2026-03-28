import { useEffect, useCallback } from 'react'
import { useThemeStore, type ThemeMode } from '@/store/themeStore'

/**
 * Returns the current effective theme (resolved from 'system' if needed),
 * and helpers to toggle or set the theme.
 *
 * Side-effect: applies `class="dark"` or `class="light"` to `<html>`.
 */
export function useTheme() {
  const { theme, setTheme } = useThemeStore()

  const getSystemPreference = useCallback((): 'dark' | 'light' => {
    if (typeof window === 'undefined') return 'dark'
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }, [])

  const resolvedTheme: 'dark' | 'light' =
    theme === 'system' ? getSystemPreference() : theme

  // Apply class to <html> whenever resolved theme changes
  useEffect(() => {
    const root = document.documentElement
    root.classList.remove('dark', 'light')
    root.classList.add(resolvedTheme)
  }, [resolvedTheme])

  // Listen to system preference changes when in 'system' mode
  useEffect(() => {
    if (theme !== 'system') return

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    const handleChange = () => {
      const root = document.documentElement
      root.classList.remove('dark', 'light')
      root.classList.add(mediaQuery.matches ? 'dark' : 'light')
    }

    mediaQuery.addEventListener('change', handleChange)
    return () => mediaQuery.removeEventListener('change', handleChange)
  }, [theme])

  const toggle = useCallback(() => {
    if (theme === 'dark') setTheme('light')
    else if (theme === 'light') setTheme('dark')
    else {
      // From 'system', toggle to the opposite of current system preference
      setTheme(getSystemPreference() === 'dark' ? 'light' : 'dark')
    }
  }, [theme, setTheme, getSystemPreference])

  return {
    theme,
    resolvedTheme,
    isDark: resolvedTheme === 'dark',
    toggle,
    setTheme: setTheme as (t: ThemeMode) => void,
  }
}
