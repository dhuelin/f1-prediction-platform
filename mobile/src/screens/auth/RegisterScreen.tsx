import React, { useState } from 'react'
import { Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native'
import { NativeStackNavigationProp } from '@react-navigation/native-stack'
import ScreenWrapper from '@/components/layout/ScreenWrapper'
import { Button, TextInput } from '@/components/ui'
import { useTheme } from '@/hooks/useTheme'
import { useAuthStore } from '@/store/authStore'
import { register } from '@/api/auth'
import { typography } from '@/theme/tokens'
import type { AuthStackParamList } from '@/navigation/AuthNavigator'

type Props = { navigation: NativeStackNavigationProp<AuthStackParamList, 'Register'> }

export default function RegisterScreen({ navigation }: Props) {
  const { colors } = useTheme()
  const { setAuth } = useAuthStore()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [loading, setLoading] = useState(false)

  const handleRegister = async () => {
    if (!displayName || !email || !password || !confirm) { Alert.alert('Error', 'All fields required'); return }
    if (password !== confirm) { Alert.alert('Error', 'Passwords do not match'); return }
    if (password.length < 8) { Alert.alert('Error', 'Password must be at least 8 characters'); return }
    setLoading(true)
    try {
      const resp = await register(email.trim().toLowerCase(), password, displayName.trim())
      await setAuth(resp.user, resp.accessToken, resp.refreshToken)
    } catch (e: any) {
      Alert.alert('Registration Failed', e?.response?.data?.message ?? 'Please try again')
    } finally {
      setLoading(false)
    }
  }

  return (
    <ScreenWrapper scrollable padded>
      <View style={styles.header}>
        <Text style={[styles.title, { color: colors.primary }]}>CREATE ACCOUNT</Text>
      </View>

      <TextInput label="Display Name" value={displayName} onChangeText={setDisplayName} autoComplete="name" placeholder="Max Verstappen" />
      <TextInput label="Email" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" autoComplete="email" placeholder="you@example.com" />
      <TextInput label="Password" value={password} onChangeText={setPassword} secureTextEntry autoComplete="new-password" placeholder="••••••••" />
      <TextInput label="Confirm Password" value={confirm} onChangeText={setConfirm} secureTextEntry placeholder="••••••••" />

      <Button label="Create Account" onPress={handleRegister} loading={loading} fullWidth />

      <TouchableOpacity onPress={() => navigation.navigate('Login')} style={{ marginTop: 24, alignItems: 'center' }}>
        <Text style={{ color: colors.textSecondary, fontSize: typography.sizes.sm }}>
          Already have an account? <Text style={{ color: colors.primary, fontWeight: '600' }}>Sign In</Text>
        </Text>
      </TouchableOpacity>
    </ScreenWrapper>
  )
}

const styles = StyleSheet.create({
  header: { marginBottom: 32, marginTop: 24 },
  title: { fontSize: 28, fontWeight: '800', letterSpacing: 1.5, textAlign: 'center' },
})
