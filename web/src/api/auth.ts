import apiClient, { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY } from './client'
import type { AuthResponse, RefreshTokenResponse, User } from './types'

/** Shape returned by GET /auth/me */
interface ProfileResponse {
  id: string
  email: string
  username: string
  emailVerified: boolean
  createdAt: string
}

async function fetchProfile(): Promise<User> {
  const { data } = await apiClient.get<ProfileResponse>('/auth/me')
  return {
    id: data.id,
    email: data.email,
    displayName: data.username,
  }
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const { data } = await apiClient.post<{ accessToken: string; refreshToken: string; expiresIn: number }>(
    '/auth/login',
    { email, password },
  )
  localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
  const user = await fetchProfile()
  return { ...data, user }
}

export async function register(
  email: string,
  password: string,
  displayName: string,
): Promise<AuthResponse> {
  const { data } = await apiClient.post<{ accessToken: string; refreshToken: string; expiresIn: number }>(
    '/auth/register',
    { email, password, username: displayName },
  )
  localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
  const user = await fetchProfile()
  return { ...data, user }
}

export async function logout(): Promise<void> {
  try {
    await apiClient.post('/auth/logout')
  } finally {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }
}

export async function refreshToken(): Promise<RefreshTokenResponse> {
  const token = localStorage.getItem(REFRESH_TOKEN_KEY)
  const { data } = await apiClient.post<RefreshTokenResponse>('/auth/refresh', {
    refreshToken: token,
  })
  localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
  return data
}

export { fetchProfile }
