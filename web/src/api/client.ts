import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'
import type { RefreshTokenResponse } from './types'

const ACCESS_TOKEN_KEY = 'f1_access_token'
const REFRESH_TOKEN_KEY = 'f1_refresh_token'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15_000,
})

// ---------------------------------------------------------------
// Request interceptor — attach Bearer token
// ---------------------------------------------------------------
apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ---------------------------------------------------------------
// Response interceptor — handle 401 with token refresh
// ---------------------------------------------------------------
let isRefreshing = false
let refreshSubscribers: Array<(token: string) => void> = []

function subscribeToTokenRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb)
}

function notifyRefreshSubscribers(token: string) {
  refreshSubscribers.forEach((cb) => cb(token))
  refreshSubscribers = []
}

function clearAuth() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

function redirectToLogin() {
  // Use location.replace so the current page isn't added to history
  window.location.replace('/login')
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean
    }

    // Only attempt refresh on 401 and not on the refresh endpoint itself
    const isAuthEndpoint =
      originalRequest.url?.includes('/auth/login') ||
      originalRequest.url?.includes('/auth/register') ||
      originalRequest.url?.includes('/auth/refresh')

    if (error.response?.status === 401 && !originalRequest._retry && !isAuthEndpoint) {
      if (isRefreshing) {
        // Queue this request until refresh completes
        return new Promise((resolve) => {
          subscribeToTokenRefresh((token) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`
            }
            resolve(apiClient(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)

      if (!refreshToken) {
        isRefreshing = false
        clearAuth()
        redirectToLogin()
        return Promise.reject(error)
      }

      try {
        const { data } = await axios.post<RefreshTokenResponse>(
          `${import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'}/auth/refresh`,
          { refreshToken },
        )

        localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken)
        localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)

        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
        }

        notifyRefreshSubscribers(data.accessToken)
        isRefreshing = false

        return apiClient(originalRequest)
      } catch {
        isRefreshing = false
        clearAuth()
        redirectToLogin()
        return Promise.reject(error)
      }
    }

    return Promise.reject(error)
  },
)

export { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY }
export default apiClient
