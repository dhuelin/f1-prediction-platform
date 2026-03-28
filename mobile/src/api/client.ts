import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'
import * as SecureStore from 'expo-secure-store'
import type { RefreshTokenResponse } from './types'

export const ACCESS_TOKEN_KEY = 'f1_access_token'
export const REFRESH_TOKEN_KEY = 'f1_refresh_token'

const BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080'

export const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
})

apiClient.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  const token = await SecureStore.getItemAsync(ACCESS_TOKEN_KEY)
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

type Subscriber = { resolve: (token: string) => void; reject: (err: unknown) => void }
let isRefreshing = false
let refreshSubscribers: Subscriber[] = []

function subscribeToRefresh(resolve: (t: string) => void, reject: (e: unknown) => void) {
  refreshSubscribers.push({ resolve, reject })
}

function notifyRefreshSubscribers(token: string) {
  refreshSubscribers.forEach(s => s.resolve(token))
  refreshSubscribers = []
}

function rejectRefreshSubscribers(err: unknown) {
  refreshSubscribers.forEach(s => s.reject(err))
  refreshSubscribers = []
}

async function clearAuth() {
  await SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY)
  await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY)
}

apiClient.interceptors.response.use(
  r => r,
  async (error: AxiosError) => {
    const req = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined

    // Guard: network-level errors may have no config
    if (!req) return Promise.reject(error)

    const isAuthEndpoint = req.url?.match(/\/auth\/(login|register|refresh)/)

    if (error.response?.status === 401 && !req._retry && !isAuthEndpoint) {
      if (isRefreshing) {
        // Queue this request until the in-flight refresh settles
        return new Promise<string>((resolve, reject) => {
          subscribeToRefresh(resolve, reject)
        }).then(token => {
          req._retry = true
          if (req.headers) req.headers.Authorization = `Bearer ${token}`
          return apiClient(req)
        })
      }

      req._retry = true
      isRefreshing = true

      const refreshToken = await SecureStore.getItemAsync(REFRESH_TOKEN_KEY)
      if (!refreshToken) {
        isRefreshing = false
        rejectRefreshSubscribers(error)
        await clearAuth()
        return Promise.reject(error)
      }

      try {
        const { data } = await axios.post<RefreshTokenResponse>(`${BASE_URL}/auth/refresh`, { refreshToken })
        await SecureStore.setItemAsync(ACCESS_TOKEN_KEY, data.accessToken)
        await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, data.refreshToken)
        if (req.headers) req.headers.Authorization = `Bearer ${data.accessToken}`

        // Clear flag BEFORE notifying subscribers so replayed requests don't re-queue
        isRefreshing = false
        notifyRefreshSubscribers(data.accessToken)

        return apiClient(req)
      } catch (refreshError) {
        isRefreshing = false
        rejectRefreshSubscribers(refreshError)
        await clearAuth()
        return Promise.reject(refreshError)
      }
    }

    return Promise.reject(error)
  },
)

export default apiClient
