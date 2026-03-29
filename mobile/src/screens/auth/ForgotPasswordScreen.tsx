import React, { useState } from 'react'
import { Alert, StyleSheet, Text, View } from 'react-native'
import ScreenWrapper from '@/components/layout/ScreenWrapper'
import { Button, TextInput } from '@/components/ui'
import { useTheme } from '@/hooks/useTheme'
import { forgotPassword } from '@/api/auth'

export default function ForgotPasswordScreen() {
  const { colors } = useTheme()
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [sent, setSent] = useState(false)

  const handleSubmit = async () => {
    if (!email) { Alert.alert('Error', 'Enter your email address'); return }
    setLoading(true)
    try {
      await forgotPassword(email.trim().toLowerCase())
      setSent(true)
    } catch {
      Alert.alert('Error', 'Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <ScreenWrapper padded>
      <View style={styles.header}>
        <Text style={[styles.title, { color: colors.textPrimary }]}>Reset Password</Text>
        <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
          {sent
            ? 'Check your email for a reset link.'
            : "We'll send you a link to reset your password."}
        </Text>
      </View>
      {!sent && (
        <>
          <TextInput label="Email" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" placeholder="you@example.com" />
          <Button label="Send Reset Link" onPress={handleSubmit} loading={loading} fullWidth />
        </>
      )}
    </ScreenWrapper>
  )
}

const styles = StyleSheet.create({
  header: { marginBottom: 32, marginTop: 24 },
  title: { fontSize: 24, fontWeight: '700', marginBottom: 8 },
  subtitle: { fontSize: 15, lineHeight: 22 },
})
