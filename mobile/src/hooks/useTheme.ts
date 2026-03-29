import { useThemeStore } from '@/store/themeStore'
import { colors } from '@/theme/tokens'

export function useTheme() {
  const { scheme, toggleScheme } = useThemeStore()
  const isDark = scheme === 'dark'

  const c = {
    background:      isDark ? colors.background      : colors.backgroundLight,
    surface:         isDark ? colors.surface          : colors.surfaceLight,
    surfaceElevated: isDark ? colors.surfaceElevated  : colors.surfaceElevatedLight,
    border:          isDark ? colors.border           : colors.borderLight,
    textPrimary:     isDark ? colors.textPrimary      : colors.textPrimaryLight,
    textSecondary:   isDark ? colors.textSecondary    : colors.textSecondaryLight,
    textMuted:       isDark ? colors.textMuted        : colors.textMutedLight,
    primary:         colors.primary,
    accent:          colors.accent,
    success:         colors.success,
    warning:         colors.warning,
    error:           colors.error,
  }

  return { isDark, colors: c, toggleScheme }
}
