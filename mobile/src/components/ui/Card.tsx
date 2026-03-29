import React from 'react'
import { StyleSheet, View, ViewStyle } from 'react-native'
import { useTheme } from '@/hooks/useTheme'
import { radius, spacing } from '@/theme/tokens'

interface Props {
  children: React.ReactNode
  style?: ViewStyle
  elevated?: boolean
}

export default function Card({ children, style, elevated = false }: Props) {
  const { colors } = useTheme()
  return (
    <View style={[
      styles.card,
      { backgroundColor: elevated ? colors.surfaceElevated : colors.surface, borderColor: colors.border },
      style,
    ]}>
      {children}
    </View>
  )
}

const styles = StyleSheet.create({
  card: {
    borderRadius: radius.lg,
    padding: spacing.md,
    borderWidth: 1,
  },
})
