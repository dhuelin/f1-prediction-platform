import React, { useEffect, useRef } from 'react'
import { NavigationContainer, NavigationContainerRef } from '@react-navigation/native'
import { ActivityIndicator, View } from 'react-native'
import * as Notifications from 'expo-notifications'
import { useAuthStore } from '@/store/authStore'
import AuthNavigator from './AuthNavigator'
import AppNavigator from './AppNavigator'
import { colors } from '@/theme/tokens'
import { linking, handleNotificationResponse } from '@/notifications/push'

export default function RootNavigator() {
  const { accessToken, isLoading, loadFromStorage } = useAuthStore()
  const navRef = useRef<NavigationContainerRef<any>>(null)

  useEffect(() => { loadFromStorage() }, [])

  useEffect(() => {
    const sub = Notifications.addNotificationResponseReceivedListener(response => {
      handleNotificationResponse(response, (screen: string, params: Record<string, unknown>) => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        ;(navRef.current as any)?.navigate(screen, params)
      })
    })
    return () => sub.remove()
  }, [])

  if (isLoading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.background }}>
        <ActivityIndicator color={colors.primary} size="large" />
      </View>
    )
  }

  return (
    <NavigationContainer ref={navRef} linking={linking}>
      {accessToken ? <AppNavigator /> : <AuthNavigator />}
    </NavigationContainer>
  )
}
