import * as Notifications from 'expo-notifications'
import * as Linking from 'expo-linking'
import Constants from 'expo-constants'
import { Platform } from 'react-native'
import apiClient from '@/api/client'

// ---- Foreground notification behaviour ----
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
    shouldShowBanner: true,
    shouldShowList: true,
  }),
})

// ---- Deep link config (maps notification payloads to routes) ----
export const linking = {
  prefixes: [Linking.createURL('/'), 'f1predict://'],
  config: {
    screens: {
      LeaguesTab: {
        screens: {
          LeagueDetail: 'league/:leagueId',
        },
      },
      Predict: 'predict',
      Home: 'home',
    },
  },
}

/**
 * Request push permission and register token with the notification service.
 * Call this once after the user is authenticated.
 */
export async function registerForPushNotifications(): Promise<void> {
  const { status: existingStatus } = await Notifications.getPermissionsAsync()
  let finalStatus = existingStatus

  if (existingStatus !== 'granted') {
    const { status } = await Notifications.requestPermissionsAsync()
    finalStatus = status
  }

  if (finalStatus !== 'granted') {
    console.log('Push notification permission not granted')
    return
  }

  const tokenData = await Notifications.getExpoPushTokenAsync({
    projectId: Constants.expoConfig?.extra?.eas?.projectId,
  })
  const token = tokenData.data

  try {
    await apiClient.post('/notifications/devices', {
      token,
      platform: Platform.OS === 'ios' ? 'APNS' : 'FCM',
    })
  } catch (e) {
    console.warn('Failed to register push token:', e)
  }

  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('default', {
      name: 'F1 Predict',
      importance: Notifications.AndroidImportance.HIGH,
      vibrationPattern: [0, 250, 250, 250],
      lightColor: '#E10600',
    })
  }
}

/**
 * Handle a notification tap — navigate to the relevant screen.
 */
export function handleNotificationResponse(
  response: Notifications.NotificationResponse,
  navigate: (screen: string, params?: Record<string, unknown>) => void,
): void {
  const data = response.notification.request.content.data as Record<string, string> | undefined
  if (!data) return

  switch (data.type) {
    case 'PREDICTION_REMINDER':
      navigate('Predict')
      break
    case 'RESULTS_PUBLISHED':
    case 'SCORE_AMENDED':
      if (data.leagueId) {
        navigate('LeagueDetail', { leagueId: data.leagueId })
      } else {
        navigate('Home')
      }
      break
    case 'RACE_START':
      navigate('Home')
      break
    default:
      navigate('Home')
  }
}
