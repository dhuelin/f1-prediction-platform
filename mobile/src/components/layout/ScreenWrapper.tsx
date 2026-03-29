import React from 'react'
import {
  KeyboardAvoidingView, Platform, ScrollView,
  StyleSheet, View, ViewStyle
} from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { useTheme } from '@/hooks/useTheme'

interface Props {
  children: React.ReactNode
  scrollable?: boolean
  style?: ViewStyle
  padded?: boolean
}

export default function ScreenWrapper({ children, scrollable = false, style, padded = true }: Props) {
  const { colors } = useTheme()
  const inner = (
    <View style={[styles.inner, padded && styles.padded, style]}>
      {children}
    </View>
  )
  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={{ flex: 1 }}>
        {scrollable
          ? <ScrollView showsVerticalScrollIndicator={false}>{inner}</ScrollView>
          : inner}
      </KeyboardAvoidingView>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe:   { flex: 1 },
  inner:  { flex: 1 },
  padded: { padding: 16 },
})
