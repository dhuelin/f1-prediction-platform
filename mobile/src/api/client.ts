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

let isRefreshing = false
let refreshSubscribers: Array<(token: string) => void> = []

function subscribeToRefresh(cb: (t: string) => void) { refreshSubscribers.push(cb) }
function notifyRefreshSubscribers(t: string) { refreshSubscribers.forEach(cb => cb(t)); refreshSubscribers = [] }

async function clearAuth() {
  await SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY)
  await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY)
}

apiClient.interceptors.response.use(
  r => r,
  async (error: AxiosError) => {
    const req = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
    const isAuthEndpoint = req.url?.match(/\/auth\/(login|register|refresh)/)

    if (error.response?.status === 401 && !req._retry && !isAuthEndpoint) {
      if (isRefreshing) {
        return new Promise(resolve => {
          subscribeToRefresh(token => {
            if (req.headers) req.headers.Authorization = `Bearer ${token}`
            resolve(apiClient(req))
          })
        })
      }
      req._retry = true
      isRefreshing = true
      const refreshToken = await SecureStore.getItemAsync(REFRESH_TOKEN_KEY)
      if (!refreshToken) {
        isRefreshing = false
        await clearAuth()
        return Promise.reject(error)
      }
      try {
        const { data } = await axios.post<RefreshTokenResponse>(`${BASE_URL}/auth/refresh`, { refreshToken })
        await SecureStore.setItemAsync(ACCESS_TOKEN_KEY, data.accessToken)
        await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, data.refreshToken)
        if (req.headers) req.headers.Authorization = `Bearer ${data.accessToken}`
        notifyRefreshSubscribers(data.accessToken)
        isRefreshing = false
        return apiClient(req)
      } catch {
        isRefreshing = false
        await clearAuth()
        return Promise.reject(error)
      }
    }
    return Promise.reject(error)
  },
)

export default apiClient
