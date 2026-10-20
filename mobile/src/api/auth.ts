import apiClient from './client'
import type { AuthResponse, UserProfile } from './types'

export async function login(email: string, password: string): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', { email, password })
  return data
}

/**
 * Register a new account.
 *
 * The API field is `username`; this previously sent `displayName`, which failed
 * @NotBlank validation on the server — registration from mobile could not succeed.
 */
export async function register(email: string, password: string, username: string): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/register', { email, password, username })
  return data
}

export async function forgotPassword(email: string): Promise<void> {
  await apiClient.post('/auth/forgot-password', { email })
}

// The OAuth endpoints are /oauth/{provider}/callback and both take { idToken }.
export async function loginWithGoogle(idToken: string): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/oauth/google/callback', { idToken })
  return data
}

export async function loginWithApple(identityToken: string): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/oauth/apple/callback', { idToken: identityToken })
  return data
}

export async function getMe(): Promise<UserProfile> {
  const { data } = await apiClient.get<UserProfile>('/auth/me')
  return data
}

/** Revoke the refresh token server-side so the session cannot be resumed. */
export async function logout(refreshToken: string): Promise<void> {
  await apiClient.post('/auth/logout', { refreshToken })
}
