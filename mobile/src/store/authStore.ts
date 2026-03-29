import { create } from 'zustand'
import * as SecureStore from 'expo-secure-store'
import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY } from '@/api/client'
import type { User } from '@/api/types'

interface AuthState {
  user: User | null
  accessToken: string | null
  isLoading: boolean
  setAuth: (user: User, accessToken: string, refreshToken: string) => Promise<void>
  clearAuth: () => Promise<void>
  loadFromStorage: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  isLoading: true,

  setAuth: async (user, accessToken, refreshToken) => {
    await SecureStore.setItemAsync(ACCESS_TOKEN_KEY, accessToken)
    await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, refreshToken)
    set({ user, accessToken })
    // Register for push after login — non-blocking; imported lazily to avoid circular dep
    import('@/notifications/push').then(m => m.registerForPushNotifications()).catch(console.warn)
  },

  clearAuth: async () => {
    await SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY)
    await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY)
    set({ user: null, accessToken: null })
  },

  loadFromStorage: async () => {
    const token = await SecureStore.getItemAsync(ACCESS_TOKEN_KEY)
    if (token) {
      try {
        const { getMe } = await import('@/api/auth')
        const user = await getMe()
        set({ accessToken: token, user: user as User, isLoading: false })
      } catch {
        await SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY)
        await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY)
        set({ accessToken: null, user: null, isLoading: false })
      }
    } else {
      set({ accessToken: null, isLoading: false })
    }
  },
}))
