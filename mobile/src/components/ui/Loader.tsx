import React from 'react'
import { ActivityIndicator, StyleSheet, View } from 'react-native'
import { colors } from '@/theme/tokens'

export default function Loader({ size = 'large' }: { size?: 'small' | 'large' }) {
  return (
    <View style={styles.wrapper}>
      <ActivityIndicator color={colors.primary} size={size} />
    </View>
  )
}

const styles = StyleSheet.create({
  wrapper: { flex: 1, justifyContent: 'center', alignItems: 'center' },
})
