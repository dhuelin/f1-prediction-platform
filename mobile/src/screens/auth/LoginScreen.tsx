import React, { useState } from 'react'
import { Alert, Platform, StyleSheet, Text, TouchableOpacity, View } from 'react-native'
import { NativeStackNavigationProp } from '@react-navigation/native-stack'
import * as AppleAuthentication from 'expo-apple-authentication'
import * as Google from 'expo-auth-session/providers/google'
import * as WebBrowser from 'expo-web-browser'
import ScreenWrapper from '@/components/layout/ScreenWrapper'
import { Button, TextInput } from '@/components/ui'
import { useTheme } from '@/hooks/useTheme'
import { useAuthStore } from '@/store/authStore'
import { login, loginWithApple, loginWithGoogle } from '@/api/auth'
import { typography, spacing } from '@/theme/tokens'
import type { AuthStackParamList } from '@/navigation/AuthNavigator'

// Required for Google OAuth redirect handling on Android/iOS
WebBrowser.maybeCompleteAuthSession()

type Props = { navigation: NativeStackNavigationProp<AuthStackParamList, 'Login'> }

export default function LoginScreen({ navigation }: Props) {
  const { colors } = useTheme()
  const { setAuth } = useAuthStore()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)

  // Google OAuth
  const [, , promptGoogleAsync] = Google.useAuthRequest({
    iosClientId: process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID,
    androidClientId: process.env.EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID,
    webClientId: process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID,
  })

  const handleLogin = async () => {
    if (!email || !password) { Alert.alert('Error', 'Email and password are required'); return }
    setLoading(true)
    try {
      const resp = await login(email.trim().toLowerCase(), password)
      await setAuth(resp.user, resp.accessToken, resp.refreshToken)
    } catch (e: any) {
      Alert.alert('Login Failed', e?.response?.data?.message ?? 'Please check your credentials')
    } finally {
      setLoading(false)
    }
  }

  const handleGoogleLogin = async () => {
    try {
      const result = await promptGoogleAsync()
      if (result?.type !== 'success' || !result.authentication?.idToken) return
      const resp = await loginWithGoogle(result.authentication.idToken)
      await setAuth(resp.user, resp.accessToken, resp.refreshToken)
    } catch (e: any) {
      Alert.alert('Google Sign-In Failed', e?.message ?? 'Please try again')
    }
  }

  const handleAppleLogin = async () => {
    try {
      const cred = await AppleAuthentication.signInAsync({
        requestedScopes: [
          AppleAuthentication.AppleAuthenticationScope.FULL_NAME,
          AppleAuthentication.AppleAuthenticationScope.EMAIL,
        ],
      })
      const fullName = cred.fullName
        ? `${cred.fullName.givenName ?? ''} ${cred.fullName.familyName ?? ''}`.trim()
        : undefined
      const resp = await loginWithApple(cred.identityToken!, fullName)
      await setAuth(resp.user, resp.accessToken, resp.refreshToken)
    } catch (e: any) {
      if (e.code !== 'ERR_CANCELED') {
        Alert.alert('Apple Sign-In Failed', e.message)
      }
    }
  }

  return (
    <ScreenWrapper scrollable padded>
      <View style={styles.header}>
        <Text style={[styles.title, { color: colors.primary }]}>F1 PREDICT</Text>
        <Text style={[styles.subtitle, { color: colors.textSecondary }]}>Sign in to your account</Text>
      </View>

      <TextInput
        label="Email"
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
        autoComplete="email"
        placeholder="you@example.com"
      />
      <TextInput
        label="Password"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        autoComplete="password"
        placeholder="••••••••"
      />

      <TouchableOpacity
        onPress={() => navigation.navigate('ForgotPassword')}
        style={{ alignSelf: 'flex-end', marginBottom: spacing.lg }}
      >
        <Text style={{ color: colors.primary, fontSize: typography.sizes.sm }}>Forgot password?</Text>
      </TouchableOpacity>

      <Button label="Sign In" onPress={handleLogin} loading={loading} fullWidth />

      {Platform.OS === 'ios' && (
        <AppleAuthentication.AppleAuthenticationButton
          buttonType={AppleAuthentication.AppleAuthenticationButtonType.SIGN_IN}
          buttonStyle={AppleAuthentication.AppleAuthenticationButtonStyle.BLACK}
          cornerRadius={8}
          style={{ height: 48, marginTop: spacing.md }}
          onPress={handleAppleLogin}
        />
      )}

      <Button
        label="Continue with Google"
        onPress={handleGoogleLogin}
        variant="outline"
        fullWidth
        style={{ marginTop: spacing.sm }}
      />

      <TouchableOpacity
        onPress={() => navigation.navigate('Register')}
        style={{ marginTop: spacing.xl, alignItems: 'center' }}
      >
        <Text style={{ color: colors.textSecondary, fontSize: typography.sizes.sm }}>
          Don't have an account?{' '}
          <Text style={{ color: colors.primary, fontWeight: '600' }}>Register</Text>
        </Text>
      </TouchableOpacity>
    </ScreenWrapper>
  )
}

const styles = StyleSheet.create({
  header: { marginBottom: 32, marginTop: 24 },
  title: { fontSize: 32, fontWeight: '800', letterSpacing: 2, textAlign: 'center' },
  subtitle: { fontSize: 15, textAlign: 'center', marginTop: 8 },
})
