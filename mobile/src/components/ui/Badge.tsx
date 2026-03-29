import React from 'react'
import { StyleSheet, Text, View } from 'react-native'
import { useTheme } from '@/hooks/useTheme'
import { colors, radius, typography } from '@/theme/tokens'

type BadgeVariant = 'default' | 'primary' | 'success' | 'warning' | 'error' | 'live'

export default function Badge({ label, variant = 'default' }: { label: string; variant?: BadgeVariant }) {
  const { colors: c } = useTheme()

  // Semantic variants use fixed brand/status colors that work in both themes.
  // 'default' uses a theme-aware neutral so it reads correctly in light mode.
  const bg: Record<BadgeVariant, string> = {
    default:  c.textMuted,
    primary:  colors.primary,
    success:  colors.success,
    warning:  colors.warning,
    error:    colors.error,
    live:     colors.accent,
  }

  return (
    <View style={[styles.badge, { backgroundColor: bg[variant] }]}>
      <Text style={styles.text}>{label}</Text>
    </View>
  )
}

const styles = StyleSheet.create({
  badge: { paddingHorizontal: 8, paddingVertical: 2, borderRadius: radius.full, alignSelf: 'flex-start' },
  text: { color: '#fff', fontSize: typography.sizes.xs, fontWeight: '600' },
})
