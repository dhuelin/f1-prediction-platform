import React from 'react'
import { StyleSheet, Text, View } from 'react-native'
import { colors } from '@/theme/tokens'

interface Props {
  name: string
  size?: number
}

export default function Avatar({ name, size = 40 }: Props) {
  const initials = name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()
  return (
    <View style={[styles.circle, { width: size, height: size, borderRadius: size / 2 }]}>
      <Text style={[styles.text, { fontSize: size * 0.35 }]}>{initials}</Text>
    </View>
  )
}

const styles = StyleSheet.create({
  circle: { backgroundColor: colors.primary, justifyContent: 'center', alignItems: 'center' },
  text: { color: '#fff', fontWeight: '700' },
})
