import apiClient from './client'
import type { AuthResponse } from './types'

export async function login(email: string, password: string): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', { email, password })
  return data
}

export async function register(email: string, password: string, displayName: string): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/register', { email, password, displayName })
  return data
}

export async function forgotPassword(email: string): Promise<void> {
  await apiClient.post('/auth/forgot-password', { email })
}

export async function loginWithGoogle(idToken: string): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/oauth/google', { idToken })
  return data
}

export async function loginWithApple(identityToken: string, fullName?: string): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/oauth/apple', { identityToken, fullName })
  return data
}

export async function getMe(): Promise<{ id: string; email: string; displayName: string }> {
  const { data } = await apiClient.get('/auth/me')
  return data
}
