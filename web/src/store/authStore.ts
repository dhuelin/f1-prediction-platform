import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { User } from '@/api/types'
import * as authApi from '@/api/auth'

interface AuthState {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  error: string | null
}

interface AuthActions {
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, displayName: string) => Promise<void>
  logout: () => Promise<void>
  setUser: (user: User | null) => void
  clearError: () => void
}

type AuthStore = AuthState & AuthActions

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      // State
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,

      // Actions
      login: async (email, password) => {
        set({ isLoading: true, error: null })
        try {
          const response = await authApi.login(email, password)
          set({ user: response.user, isAuthenticated: true, isLoading: false })
        } catch (err: unknown) {
          const message =
            err instanceof Error ? err.message : 'Login failed. Please try again.'
          set({ isLoading: false, error: message, isAuthenticated: false, user: null })
          throw err
        }
      },

      register: async (email, password, displayName) => {
        set({ isLoading: true, error: null })
        try {
          const response = await authApi.register(email, password, displayName)
          set({ user: response.user, isAuthenticated: true, isLoading: false })
        } catch (err: unknown) {
          const message =
            err instanceof Error ? err.message : 'Registration failed. Please try again.'
          set({ isLoading: false, error: message, isAuthenticated: false, user: null })
          throw err
        }
      },

      logout: async () => {
        set({ isLoading: true })
        try {
          await authApi.logout()
        } finally {
          set({ user: null, isAuthenticated: false, isLoading: false, error: null })
        }
      },

      setUser: (user) => {
        set({ user, isAuthenticated: user !== null })
      },

      clearError: () => set({ error: null }),
    }),
    {
      name: 'f1_auth',
      // Only persist user identity — tokens live in localStorage directly
      partialize: (state) => ({
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
)
