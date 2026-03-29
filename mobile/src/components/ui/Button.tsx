import React from 'react'
import {
  ActivityIndicator, Pressable, StyleSheet, Text, ViewStyle
} from 'react-native'
import { colors, radius, typography } from '@/theme/tokens'

type Variant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger'

interface Props {
  label: string
  onPress: () => void
  variant?: Variant
  loading?: boolean
  disabled?: boolean
  fullWidth?: boolean
  style?: ViewStyle
}

export default function Button({
  label, onPress, variant = 'primary', loading = false,
  disabled = false, fullWidth = false, style
}: Props) {
  const bg: Record<Variant, string> = {
    primary:   colors.primary,
    secondary: colors.surface,
    outline:   'transparent',
    ghost:     'transparent',
    danger:    colors.error,
  }
  const fg: Record<Variant, string> = {
    primary:   '#fff',
    secondary: colors.textPrimary,
    outline:   colors.primary,
    ghost:     colors.textSecondary,
    danger:    '#fff',
  }
  const borderColor = variant === 'outline' ? colors.primary : 'transparent'
  const isDisabled = disabled || loading

  return (
    <Pressable
      onPress={onPress}
      disabled={isDisabled}
      style={[
        styles.base,
        { backgroundColor: bg[variant], borderColor, opacity: isDisabled ? 0.5 : 1 },
        fullWidth && { alignSelf: 'stretch' },
        style,
      ]}
    >
      {loading
        ? <ActivityIndicator color={fg[variant]} size="small" />
        : <Text style={[styles.label, { color: fg[variant] }]}>{label}</Text>}
    </Pressable>
  )
}

const styles = StyleSheet.create({
  base: {
    paddingHorizontal: 20,
    paddingVertical: 13,
    borderRadius: radius.md,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 48,
  },
  label: {
    fontSize: typography.sizes.base,
    fontWeight: typography.weights.semibold,
    letterSpacing: 0.3,
  },
})
