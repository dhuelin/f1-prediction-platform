import React from 'react'
import { ActivityIndicator, StyleSheet, View } from 'react-native'
import { useTheme } from '@/hooks/useTheme'
import { colors } from '@/theme/tokens'

export default function Loader({ size = 'large' }: { size?: 'small' | 'large' }) {
  const { colors: c } = useTheme()
  return (
    <View style={[styles.wrapper, { backgroundColor: c.background }]}>
      <ActivityIndicator color={colors.primary} size={size} />
    </View>
  )
}

const styles = StyleSheet.create({
  wrapper: { flex: 1, justifyContent: 'center', alignItems: 'center' },
})
