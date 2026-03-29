import React from 'react'
import { StyleSheet, Text, View } from 'react-native'
import { colors, radius, typography } from '@/theme/tokens'

type BadgeVariant = 'default' | 'primary' | 'success' | 'warning' | 'error' | 'live'

const BG: Record<BadgeVariant, string> = {
  default: colors.border,
  primary: colors.primary,
  success: colors.success,
  warning: colors.warning,
  error:   colors.error,
  live:    colors.accent,
}

export default function Badge({ label, variant = 'default' }: { label: string; variant?: BadgeVariant }) {
  return (
    <View style={[styles.badge, { backgroundColor: BG[variant] }]}>
      <Text style={styles.text}>{label}</Text>
    </View>
  )
}

const styles = StyleSheet.create({
  badge: { paddingHorizontal: 8, paddingVertical: 2, borderRadius: radius.full, alignSelf: 'flex-start' },
  text: { color: '#fff', fontSize: typography.sizes.xs, fontWeight: '600' },
})
