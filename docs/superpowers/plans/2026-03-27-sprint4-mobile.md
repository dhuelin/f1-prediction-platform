# Sprint 4 — Mobile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the F1 Prediction Platform React Native mobile app (iOS + Android) with full feature parity to the web frontend, push notifications, and App Store / Google Play submission prep.

**Architecture:** Expo managed workflow with TypeScript. React Navigation (native stack + bottom tabs) handles auth-gating. All screens reuse the same API layer types as the web (`src/api/types.ts`). Design tokens are centralised in `src/theme/tokens.ts`; platform components wrap React Native primitives. Push notifications are registered via Expo Notifications, device tokens sent to the Notification Service, and deep-link handlers map payloads to screens.

**Tech Stack:** Expo SDK 52, React Native 0.76, TypeScript 5, React Navigation 6 (native-stack + bottom-tabs), Zustand (with expo-secure-store persistence), Axios, react-native-draggable-flatlist, expo-notifications, expo-auth-session (Google OAuth), expo-apple-authentication (Apple Sign-In), EAS Build for production binaries.

---

## File Structure

```
mobile/
├── app.json                              # Expo config
├── eas.json                              # EAS Build profiles
├── package.json
├── tsconfig.json
├── .env.example
├── assets/
│   ├── icon.png                          # 1024×1024
│   ├── adaptive-icon.png                 # Android adaptive icon foreground
│   └── splash.png                        # 1284×2778 (iPhone 14 Pro Max)
├── src/
│   ├── api/
│   │   ├── client.ts                     # Axios + SecureStore interceptors
│   │   ├── auth.ts
│   │   ├── predictions.ts
│   │   ├── leagues.ts
│   │   ├── f1data.ts
│   │   ├── scoring.ts
│   │   └── types.ts                      # Identical to web/src/api/types.ts
│   ├── store/
│   │   ├── authStore.ts                  # Zustand + SecureStore persistence
│   │   └── themeStore.ts
│   ├── theme/
│   │   └── tokens.ts                     # RN StyleSheet values (colours, spacing, typography)
│   ├── components/
│   │   ├── ui/
│   │   │   ├── Button.tsx
│   │   │   ├── TextInput.tsx
│   │   │   ├── Card.tsx
│   │   │   ├── Badge.tsx
│   │   │   ├── Avatar.tsx
│   │   │   ├── Loader.tsx
│   │   │   └── index.ts
│   │   └── layout/
│   │       └── ScreenWrapper.tsx         # SafeAreaView + KeyboardAvoidingView
│   ├── navigation/
│   │   ├── RootNavigator.tsx             # Switches Auth ↔ App based on auth state
│   │   ├── AuthNavigator.tsx             # Stack: Login → Register → ForgotPassword
│   │   └── AppNavigator.tsx              # Bottom tabs: Home, Predict, Leagues, Profile
│   ├── screens/
│   │   ├── auth/
│   │   │   ├── LoginScreen.tsx
│   │   │   ├── RegisterScreen.tsx
│   │   │   └── ForgotPasswordScreen.tsx
│   │   ├── home/
│   │   │   └── HomeScreen.tsx
│   │   ├── predict/
│   │   │   └── PredictScreen.tsx
│   │   ├── leagues/
│   │   │   ├── LeaguesScreen.tsx
│   │   │   └── LeagueDetailScreen.tsx
│   │   └── profile/
│   │       └── ProfileScreen.tsx
│   ├── hooks/
│   │   └── useTheme.ts
│   └── notifications/
│       └── push.ts                       # Register, getToken, deep-link handler
```

---

## Task 1: Expo Project Setup (#97)

**Closes:** #97

**Files:**
- Create: `mobile/package.json`
- Create: `mobile/tsconfig.json`
- Create: `mobile/.env.example`
- Create: `mobile/src/api/types.ts`

- [ ] **Step 1: Scaffold Expo project**

From the repo root:
```bash
cd "/media/dhuelin/Shared Drive/Codebases/Claude_Apps/f1-prediction-platform"
npx create-expo-app@latest mobile --template blank-typescript
cd mobile
```

- [ ] **Step 2: Install all dependencies**

```bash
npx expo install expo-secure-store expo-notifications expo-auth-session \
  expo-apple-authentication expo-linking expo-constants expo-web-browser \
  react-native-gesture-handler react-native-reanimated \
  react-native-safe-area-context react-native-screens \
  react-native-draggable-flatlist

npm install @react-navigation/native @react-navigation/native-stack \
  @react-navigation/bottom-tabs \
  axios zustand \
  react-native-countdown-circle-timer

npx expo install expo-image
```

- [ ] **Step 3: Write `mobile/tsconfig.json`**

```json
{
  "extends": "expo/tsconfig.base",
  "compilerOptions": {
    "strict": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["**/*.ts", "**/*.tsx", ".expo/types/**/*.d.ts", "expo-env.d.ts"]
}
```

- [ ] **Step 4: Write `mobile/.env.example`**

```
EXPO_PUBLIC_API_BASE_URL=http://localhost:8080
EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID=YOUR_IOS_CLIENT_ID.apps.googleusercontent.com
EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID=YOUR_ANDROID_CLIENT_ID.apps.googleusercontent.com
EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

- [ ] **Step 5: Copy API types from web**

Copy `web/src/api/types.ts` verbatim to `mobile/src/api/types.ts`. The type shapes are identical — both apps talk to the same backend.

- [ ] **Step 6: Create `mobile/src/api/client.ts`**

```typescript
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
```

- [ ] **Step 7: Verify TypeScript compiles**

```bash
cd mobile && npx tsc --noEmit
```
Expected: no errors.

- [ ] **Step 8: Commit**

```bash
git checkout -b feature/97-mobile-expo-setup
git add mobile/
git commit -m "[#97] Add Expo mobile project skeleton with TypeScript and API client"
```

---

## Task 2: app.json Config (#98)

**Closes:** #98

**Files:**
- Modify: `mobile/app.json`
- Create: `mobile/eas.json`
- Create: `mobile/assets/icon.png` (placeholder — 1024×1024 dark red F1-themed)
- Create: `mobile/assets/adaptive-icon.png`
- Create: `mobile/assets/splash.png`

- [ ] **Step 1: Write `mobile/app.json`**

```json
{
  "expo": {
    "name": "F1 Predict",
    "slug": "f1-predict",
    "version": "1.0.0",
    "orientation": "portrait",
    "icon": "./assets/icon.png",
    "userInterfaceStyle": "automatic",
    "splash": {
      "image": "./assets/splash.png",
      "resizeMode": "contain",
      "backgroundColor": "#0A0A0A"
    },
    "assetBundlePatterns": ["**/*"],
    "ios": {
      "supportsTablet": false,
      "bundleIdentifier": "com.f1predict.app",
      "buildNumber": "1",
      "infoPlist": {
        "NSCameraUsageDescription": "Used for profile photo upload.",
        "NSPhotoLibraryUsageDescription": "Used for profile photo upload."
      }
    },
    "android": {
      "adaptiveIcon": {
        "foregroundImage": "./assets/adaptive-icon.png",
        "backgroundColor": "#0A0A0A"
      },
      "package": "com.f1predict.app",
      "versionCode": 1,
      "permissions": ["NOTIFICATIONS", "RECEIVE_BOOT_COMPLETED"]
    },
    "plugins": [
      "expo-notifications",
      "expo-secure-store",
      [
        "expo-build-properties",
        {
          "ios": { "deploymentTarget": "16.0" },
          "android": { "compileSdkVersion": 35, "targetSdkVersion": 35 }
        }
      ]
    ],
    "extra": {
      "eas": { "projectId": "YOUR_EAS_PROJECT_ID" }
    }
  }
}
```

- [ ] **Step 2: Write `mobile/eas.json`**

```json
{
  "cli": { "version": ">= 12.0.0" },
  "build": {
    "development": {
      "developmentClient": true,
      "distribution": "internal",
      "ios": { "simulator": true }
    },
    "preview": {
      "distribution": "internal"
    },
    "production": {
      "autoIncrement": true
    }
  },
  "submit": {
    "production": {}
  }
}
```

- [ ] **Step 3: Create placeholder assets**

Use a Node one-liner to create 1×1 pixel PNG placeholders (they will be replaced before submission):

```bash
cd mobile
# Create minimal valid PNG files (1×1 transparent pixel) as placeholders
node -e "
const fs = require('fs');
// Minimal 1×1 dark-background PNG (valid PNG header + IHDR + IDAT + IEND)
const buf = Buffer.from('89504e470d0a1a0a0000000d4948445200000001000000010802000000907753de0000000c4944415408d76360f8cf000001020017dd8db40000000049454e44ae426082','hex');
fs.mkdirSync('assets', {recursive:true});
['assets/icon.png','assets/adaptive-icon.png','assets/splash.png'].forEach(f => fs.writeFileSync(f, buf));
console.log('Placeholder assets created');
"
```

- [ ] **Step 4: Install expo-build-properties plugin**

```bash
npx expo install expo-build-properties
```

- [ ] **Step 5: Commit**

```bash
git add mobile/app.json mobile/eas.json mobile/assets/
git commit -m "[#98] Configure app.json: bundle IDs, icons, splash, permissions, EAS build"
```

---

## Task 3: Navigation Setup (#99)

**Closes:** #99

**Files:**
- Create: `mobile/src/navigation/RootNavigator.tsx`
- Create: `mobile/src/navigation/AuthNavigator.tsx`
- Create: `mobile/src/navigation/AppNavigator.tsx`
- Create: `mobile/src/store/authStore.ts`
- Create: `mobile/src/store/themeStore.ts`
- Modify: `mobile/App.tsx`

- [ ] **Step 1: Write `mobile/src/store/authStore.ts`**

```typescript
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
  },

  clearAuth: async () => {
    await SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY)
    await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY)
    set({ user: null, accessToken: null })
  },

  loadFromStorage: async () => {
    const token = await SecureStore.getItemAsync(ACCESS_TOKEN_KEY)
    if (token) {
      // Rehydrate user profile — imported lazily to avoid circular dep at module load time
      try {
        const { getMe } = await import('@/api/auth')
        const user = await getMe()
        set({ accessToken: token, user: user as User, isLoading: false })
      } catch {
        // Token may be expired — clear auth so app routes to login
        await SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY)
        await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY)
        set({ accessToken: null, user: null, isLoading: false })
      }
    } else {
      set({ accessToken: null, isLoading: false })
    }
  },
}))
```

- [ ] **Step 2: Write `mobile/src/store/themeStore.ts`**

```typescript
import { create } from 'zustand'
import { Appearance } from 'react-native'

type ColorScheme = 'dark' | 'light'

interface ThemeState {
  scheme: ColorScheme
  setScheme: (s: ColorScheme) => void
  toggleScheme: () => void
}

export const useThemeStore = create<ThemeState>((set, get) => ({
  scheme: (Appearance.getColorScheme() ?? 'dark') as ColorScheme,
  setScheme: (scheme) => set({ scheme }),
  toggleScheme: () => set({ scheme: get().scheme === 'dark' ? 'light' : 'dark' }),
}))
```

- [ ] **Step 3: Write `mobile/src/navigation/AuthNavigator.tsx`**

```typescript
import React from 'react'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import LoginScreen from '@/screens/auth/LoginScreen'
import RegisterScreen from '@/screens/auth/RegisterScreen'
import ForgotPasswordScreen from '@/screens/auth/ForgotPasswordScreen'

export type AuthStackParamList = {
  Login: undefined
  Register: undefined
  ForgotPassword: undefined
}

const Stack = createNativeStackNavigator<AuthStackParamList>()

export default function AuthNavigator() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Login" component={LoginScreen} />
      <Stack.Screen name="Register" component={RegisterScreen} />
      <Stack.Screen name="ForgotPassword" component={ForgotPasswordScreen} />
    </Stack.Navigator>
  )
}
```

- [ ] **Step 4: Write `mobile/src/navigation/AppNavigator.tsx`**

```typescript
import React from 'react'
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import HomeScreen from '@/screens/home/HomeScreen'
import PredictScreen from '@/screens/predict/PredictScreen'
import LeaguesScreen from '@/screens/leagues/LeaguesScreen'
import LeagueDetailScreen from '@/screens/leagues/LeagueDetailScreen'
import ProfileScreen from '@/screens/profile/ProfileScreen'
import { colors } from '@/theme/tokens'

export type AppTabParamList = {
  Home: undefined
  Predict: undefined
  LeaguesTab: undefined
  Profile: undefined
}

export type LeagueStackParamList = {
  Leagues: undefined
  LeagueDetail: { leagueId: string; leagueName: string }
}

const Tab = createBottomTabNavigator<AppTabParamList>()
const LeagueStack = createNativeStackNavigator<LeagueStackParamList>()

function LeaguesStackNavigator() {
  return (
    <LeagueStack.Navigator screenOptions={{ headerShown: false }}>
      <LeagueStack.Screen name="Leagues" component={LeaguesScreen} />
      <LeagueStack.Screen name="LeagueDetail" component={LeagueDetailScreen} />
    </LeagueStack.Navigator>
  )
}

export default function AppNavigator() {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarStyle: { backgroundColor: colors.surface, borderTopColor: colors.border },
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.textMuted,
      }}
    >
      <Tab.Screen name="Home" component={HomeScreen} options={{ tabBarLabel: 'Home' }} />
      <Tab.Screen name="Predict" component={PredictScreen} options={{ tabBarLabel: 'Predict' }} />
      <Tab.Screen name="LeaguesTab" component={LeaguesStackNavigator} options={{ tabBarLabel: 'Leagues' }} />
      <Tab.Screen name="Profile" component={ProfileScreen} options={{ tabBarLabel: 'Profile' }} />
    </Tab.Navigator>
  )
}
```

- [ ] **Step 5: Write `mobile/src/navigation/RootNavigator.tsx`**

```typescript
import React, { useEffect } from 'react'
import { NavigationContainer } from '@react-navigation/native'
import { ActivityIndicator, View } from 'react-native'
import { useAuthStore } from '@/store/authStore'
import AuthNavigator from './AuthNavigator'
import AppNavigator from './AppNavigator'
import { colors } from '@/theme/tokens'
import { linking } from '@/notifications/push'

export default function RootNavigator() {
  const { accessToken, isLoading, loadFromStorage } = useAuthStore()

  useEffect(() => { loadFromStorage() }, [])

  if (isLoading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.background }}>
        <ActivityIndicator color={colors.primary} size="large" />
      </View>
    )
  }

  return (
    <NavigationContainer linking={linking}>
      {accessToken ? <AppNavigator /> : <AuthNavigator />}
    </NavigationContainer>
  )
}
```

- [ ] **Step 6: Update `mobile/App.tsx`**

```typescript
import 'react-native-gesture-handler'
import React from 'react'
import { GestureHandlerRootView } from 'react-native-gesture-handler'
import { StatusBar } from 'expo-status-bar'
import RootNavigator from '@/navigation/RootNavigator'

export default function App() {
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <StatusBar style="light" />
      <RootNavigator />
    </GestureHandlerRootView>
  )
}
```

- [ ] **Step 7: Verify TypeScript**

```bash
cd mobile && npx tsc --noEmit
```
Expected: no errors (stub screens don't exist yet — create empty placeholder files to unblock TS):

For each missing screen, create a placeholder:
```typescript
// e.g. mobile/src/screens/auth/LoginScreen.tsx
import React from 'react'
import { View, Text } from 'react-native'
export default function LoginScreen() { return <View><Text>Login</Text></View> }
```
Do the same for `RegisterScreen`, `ForgotPasswordScreen`, `HomeScreen`, `PredictScreen`, `LeaguesScreen`, `LeagueDetailScreen`, `ProfileScreen`. These are replaced in later tasks.

- [ ] **Step 8: Commit**

```bash
git add mobile/src/navigation/ mobile/src/store/ mobile/App.tsx mobile/src/screens/
git commit -m "[#99] Add React Navigation stack+tab setup with auth flow and placeholder screens"
```

---

## Task 4: Design System (#100)

**Closes:** #100

**Files:**
- Create: `mobile/src/theme/tokens.ts`
- Create: `mobile/src/components/ui/Button.tsx`
- Create: `mobile/src/components/ui/TextInput.tsx`
- Create: `mobile/src/components/ui/Card.tsx`
- Create: `mobile/src/components/ui/Badge.tsx`
- Create: `mobile/src/components/ui/Avatar.tsx`
- Create: `mobile/src/components/ui/Loader.tsx`
- Create: `mobile/src/components/ui/index.ts`
- Create: `mobile/src/components/layout/ScreenWrapper.tsx`
- Create: `mobile/src/hooks/useTheme.ts`

- [ ] **Step 1: Write `mobile/src/theme/tokens.ts`**

```typescript
import { Platform } from 'react-native'

// ---- Colours (dark mode defaults; light mode overrides handled per-component) ----
export const colors = {
  background: '#0A0A0A',
  surface: '#1A1A1A',
  surfaceElevated: '#242424',
  border: '#333333',
  primary: '#E10600',      // F1 red
  primaryHover: '#B30500',
  accent: '#F97316',       // orange — live/alert
  textPrimary: '#FFFFFF',
  textSecondary: '#AAAAAA',
  textMuted: '#666666',
  success: '#22C55E',
  warning: '#F59E0B',
  error: '#EF4444',
  // Light mode
  backgroundLight: '#F8F8F8',
  surfaceLight: '#FFFFFF',
  surfaceElevatedLight: '#F0F0F0',
  borderLight: '#E5E5E5',
  textPrimaryLight: '#111111',
  textSecondaryLight: '#555555',
  textMutedLight: '#999999',
}

export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  '2xl': 48,
}

export const radius = {
  sm: 4,
  md: 8,
  lg: 12,
  xl: 16,
  full: 9999,
}

export const typography = {
  fontFamily: Platform.OS === 'ios' ? 'System' : 'Roboto',
  sizes: {
    xs: 11,
    sm: 13,
    base: 15,
    lg: 17,
    xl: 20,
    '2xl': 24,
    '3xl': 30,
  },
  weights: {
    normal: '400' as const,
    medium: '500' as const,
    semibold: '600' as const,
    bold: '700' as const,
    extrabold: '800' as const,
  },
}
```

- [ ] **Step 2: Write `mobile/src/hooks/useTheme.ts`**

```typescript
import { useThemeStore } from '@/store/themeStore'
import { colors } from '@/theme/tokens'

export function useTheme() {
  const { scheme, toggleScheme } = useThemeStore()
  const isDark = scheme === 'dark'

  const c = {
    background:      isDark ? colors.background      : colors.backgroundLight,
    surface:         isDark ? colors.surface          : colors.surfaceLight,
    surfaceElevated: isDark ? colors.surfaceElevated  : colors.surfaceElevatedLight,
    border:          isDark ? colors.border           : colors.borderLight,
    textPrimary:     isDark ? colors.textPrimary      : colors.textPrimaryLight,
    textSecondary:   isDark ? colors.textSecondary    : colors.textSecondaryLight,
    textMuted:       isDark ? colors.textMuted        : colors.textMutedLight,
    primary:         colors.primary,
    accent:          colors.accent,
    success:         colors.success,
    warning:         colors.warning,
    error:           colors.error,
  }

  return { isDark, colors: c, toggleScheme }
}
```

- [ ] **Step 3: Write `mobile/src/components/layout/ScreenWrapper.tsx`**

```typescript
import React from 'react'
import {
  KeyboardAvoidingView, Platform, ScrollView,
  StyleSheet, View, ViewStyle
} from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { useTheme } from '@/hooks/useTheme'

interface Props {
  children: React.ReactNode
  scrollable?: boolean
  style?: ViewStyle
  padded?: boolean
}

export default function ScreenWrapper({ children, scrollable = false, style, padded = true }: Props) {
  const { colors } = useTheme()
  const inner = (
    <View style={[styles.inner, padded && styles.padded, style]}>
      {children}
    </View>
  )
  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={{ flex: 1 }}>
        {scrollable
          ? <ScrollView showsVerticalScrollIndicator={false}>{inner}</ScrollView>
          : inner}
      </KeyboardAvoidingView>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe:   { flex: 1 },
  inner:  { flex: 1 },
  padded: { padding: 16 },
})
```

- [ ] **Step 4: Write `mobile/src/components/ui/Button.tsx`**

```typescript
import React from 'react'
import {
  ActivityIndicator, Pressable, StyleSheet, Text, ViewStyle
} from 'react-native'
import { colors, radius, typography } from '@/theme/tokens'

type Variant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger'

interface Props {
  label: string
  onPress: () => void
  variant?: Variant
  loading?: boolean
  disabled?: boolean
  fullWidth?: boolean
  style?: ViewStyle
}

export default function Button({
  label, onPress, variant = 'primary', loading = false,
  disabled = false, fullWidth = false, style
}: Props) {
  const bg: Record<Variant, string> = {
    primary:   colors.primary,
    secondary: colors.surface,
    outline:   'transparent',
    ghost:     'transparent',
    danger:    colors.error,
  }
  const fg: Record<Variant, string> = {
    primary:   '#fff',
    secondary: colors.textPrimary,
    outline:   colors.primary,
    ghost:     colors.textSecondary,
    danger:    '#fff',
  }
  const borderColor = variant === 'outline' ? colors.primary : 'transparent'
  const isDisabled = disabled || loading

  return (
    <Pressable
      onPress={onPress}
      disabled={isDisabled}
      style={[
        styles.base,
        { backgroundColor: bg[variant], borderColor, opacity: isDisabled ? 0.5 : 1 },
        fullWidth && { alignSelf: 'stretch' },
        style,
      ]}
    >
      {loading
        ? <ActivityIndicator color={fg[variant]} size="small" />
        : <Text style={[styles.label, { color: fg[variant] }]}>{label}</Text>}
    </Pressable>
  )
}

const styles = StyleSheet.create({
  base: {
    paddingHorizontal: 20,
    paddingVertical: 13,
    borderRadius: radius.md,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 48,
  },
  label: {
    fontSize: typography.sizes.base,
    fontWeight: typography.weights.semibold,
    letterSpacing: 0.3,
  },
})
```

- [ ] **Step 5: Write `mobile/src/components/ui/TextInput.tsx`**

```typescript
import React, { forwardRef } from 'react'
import {
  StyleSheet, Text, TextInput as RNTextInput,
  TextInputProps, View
} from 'react-native'
import { useTheme } from '@/hooks/useTheme'
import { radius, typography } from '@/theme/tokens'

interface Props extends TextInputProps {
  label?: string
  error?: string
}

const TextInput = forwardRef<RNTextInput, Props>(({ label, error, style, ...rest }, ref) => {
  const { colors } = useTheme()
  return (
    <View style={styles.wrapper}>
      {label && <Text style={[styles.label, { color: colors.textSecondary }]}>{label}</Text>}
      <RNTextInput
        ref={ref}
        placeholderTextColor={colors.textMuted}
        style={[
          styles.input,
          {
            backgroundColor: colors.surfaceElevated,
            borderColor: error ? colors.error : colors.border,
            color: colors.textPrimary,
          },
          style,
        ]}
        {...rest}
      />
      {error && <Text style={[styles.error, { color: colors.error }]}>{error}</Text>}
    </View>
  )
})

TextInput.displayName = 'TextInput'
export default TextInput

const styles = StyleSheet.create({
  wrapper: { marginBottom: 12 },
  label: { fontSize: typography.sizes.sm, fontWeight: '500', marginBottom: 6 },
  input: {
    borderWidth: 1,
    borderRadius: radius.md,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: typography.sizes.base,
    minHeight: 48,
  },
  error: { fontSize: typography.sizes.sm, marginTop: 4 },
})
```

- [ ] **Step 6: Write `mobile/src/components/ui/Card.tsx`**

```typescript
import React from 'react'
import { StyleSheet, View, ViewStyle } from 'react-native'
import { useTheme } from '@/hooks/useTheme'
import { radius, spacing } from '@/theme/tokens'

interface Props {
  children: React.ReactNode
  style?: ViewStyle
  elevated?: boolean
}

export default function Card({ children, style, elevated = false }: Props) {
  const { colors } = useTheme()
  return (
    <View style={[
      styles.card,
      { backgroundColor: elevated ? colors.surfaceElevated : colors.surface, borderColor: colors.border },
      style,
    ]}>
      {children}
    </View>
  )
}

const styles = StyleSheet.create({
  card: {
    borderRadius: radius.lg,
    padding: spacing.md,
    borderWidth: 1,
  },
})
```

- [ ] **Step 7: Write remaining UI components**

**`mobile/src/components/ui/Badge.tsx`**
```typescript
import React from 'react'
import { StyleSheet, Text, View } from 'react-native'
import { colors, radius, typography } from '@/theme/tokens'

type BadgeVariant = 'default' | 'primary' | 'success' | 'warning' | 'error' | 'live'

const BG: Record<BadgeVariant, string> = {
  default: colors.border,
  primary: colors.primary,
  success: colors.success,
  warning: colors.warning,
  error:   colors.error,
  live:    colors.accent,
}

export default function Badge({ label, variant = 'default' }: { label: string; variant?: BadgeVariant }) {
  return (
    <View style={[styles.badge, { backgroundColor: BG[variant] }]}>
      <Text style={styles.text}>{label}</Text>
    </View>
  )
}

const styles = StyleSheet.create({
  badge: { paddingHorizontal: 8, paddingVertical: 2, borderRadius: radius.full, alignSelf: 'flex-start' },
  text: { color: '#fff', fontSize: typography.sizes.xs, fontWeight: '600' },
})
```

**`mobile/src/components/ui/Avatar.tsx`**
```typescript
import React from 'react'
import { StyleSheet, Text, View } from 'react-native'
import { colors, radius, typography } from '@/theme/tokens'

interface Props {
  name: string
  size?: number
}

export default function Avatar({ name, size = 40 }: Props) {
  const initials = name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()
  return (
    <View style={[styles.circle, { width: size, height: size, borderRadius: size / 2 }]}>
      <Text style={[styles.text, { fontSize: size * 0.35 }]}>{initials}</Text>
    </View>
  )
}

const styles = StyleSheet.create({
  circle: { backgroundColor: colors.primary, justifyContent: 'center', alignItems: 'center' },
  text: { color: '#fff', fontWeight: '700' },
})
```

**`mobile/src/components/ui/Loader.tsx`**
```typescript
import React from 'react'
import { ActivityIndicator, StyleSheet, View } from 'react-native'
import { colors } from '@/theme/tokens'

export default function Loader({ size = 'large' }: { size?: 'small' | 'large' }) {
  return (
    <View style={styles.wrapper}>
      <ActivityIndicator color={colors.primary} size={size} />
    </View>
  )
}

const styles = StyleSheet.create({
  wrapper: { flex: 1, justifyContent: 'center', alignItems: 'center' },
})
```

- [ ] **Step 8: Write `mobile/src/components/ui/index.ts`**

```typescript
export { default as Button } from './Button'
export { default as TextInput } from './TextInput'
export { default as Card } from './Card'
export { default as Badge } from './Badge'
export { default as Avatar } from './Avatar'
export { default as Loader } from './Loader'
```

- [ ] **Step 9: Verify TypeScript**

```bash
cd mobile && npx tsc --noEmit
```
Expected: no errors.

- [ ] **Step 10: Commit**

```bash
git add mobile/src/theme/ mobile/src/components/ mobile/src/hooks/
git commit -m "[#100] Add design system: tokens, ScreenWrapper, Button, TextInput, Card, Badge, Avatar, Loader"
```

---

## Task 5: Auth Screens (#101)

**Closes:** #101

**Files:**
- Create: `mobile/src/api/auth.ts`
- Modify: `mobile/src/screens/auth/LoginScreen.tsx` (replace placeholder)
- Modify: `mobile/src/screens/auth/RegisterScreen.tsx`
- Modify: `mobile/src/screens/auth/ForgotPasswordScreen.tsx`

- [ ] **Step 1: Write `mobile/src/api/auth.ts`**

```typescript
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
```

- [ ] **Step 2: Write `mobile/src/screens/auth/LoginScreen.tsx`**

```typescript
import React, { useState } from 'react'
import { Alert, Platform, StyleSheet, Text, TouchableOpacity, View } from 'react-native'
import { NativeStackNavigationProp } from '@react-navigation/native-stack'
import * as AppleAuthentication from 'expo-apple-authentication'
import * as Google from 'expo-auth-session/providers/google'
import * as WebBrowser from 'expo-web-browser'
import ScreenWrapper from '@/components/layout/ScreenWrapper'
import { Button, TextInput } from '@/components/ui'
import { useTheme } from '@/hooks/useTheme'
import { useAuthStore } from '@/store/authStore'
import { login, loginWithApple, loginWithGoogle } from '@/api/auth'
import { typography, spacing } from '@/theme/tokens'
import type { AuthStackParamList } from '@/navigation/AuthNavigator'

// Required for Google OAuth redirect handling on Android/iOS
WebBrowser.maybeCompleteAuthSession()

type Props = { navigation: NativeStackNavigationProp<AuthStackParamList, 'Login'> }

export default function LoginScreen({ navigation }: Props) {
  const { colors } = useTheme()
  const { setAuth } = useAuthStore()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)

  // Google OAuth — clientId values set in app.json > extra or EAS secrets
  const [, , promptGoogleAsync] = Google.useAuthRequest({
    iosClientId: process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID,
    androidClientId: process.env.EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID,
    webClientId: process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID,
  })

  const handleLogin = async () => {
    if (!email || !password) { Alert.alert('Error', 'Email and password are required'); return }
    setLoading(true)
    try {
      const resp = await login(email.trim().toLowerCase(), password)
      await setAuth(resp.user, resp.accessToken, resp.refreshToken)
    } catch (e: any) {
      Alert.alert('Login Failed', e?.response?.data?.message ?? 'Please check your credentials')
    } finally {
      setLoading(false)
    }
  }

  const handleGoogleLogin = async () => {
    try {
      const result = await promptGoogleAsync()
      if (result?.type !== 'success' || !result.authentication?.idToken) return
      const resp = await loginWithGoogle(result.authentication.idToken)
      await setAuth(resp.user, resp.accessToken, resp.refreshToken)
    } catch (e: any) {
      Alert.alert('Google Sign-In Failed', e?.message ?? 'Please try again')
    }
  }

  const handleAppleLogin = async () => {
    try {
      const cred = await AppleAuthentication.signInAsync({
        requestedScopes: [
          AppleAuthentication.AppleAuthenticationScope.FULL_NAME,
          AppleAuthentication.AppleAuthenticationScope.EMAIL,
        ],
      })
      const fullName = cred.fullName
        ? `${cred.fullName.givenName ?? ''} ${cred.fullName.familyName ?? ''}`.trim()
        : undefined
      const resp = await loginWithApple(cred.identityToken!, fullName)
      await setAuth(resp.user, resp.accessToken, resp.refreshToken)
    } catch (e: any) {
      if (e.code !== 'ERR_CANCELED') {
        Alert.alert('Apple Sign-In Failed', e.message)
      }
    }
  }

  return (
    <ScreenWrapper scrollable padded>
      <View style={styles.header}>
        <Text style={[styles.title, { color: colors.primary }]}>F1 PREDICT</Text>
        <Text style={[styles.subtitle, { color: colors.textSecondary }]}>Sign in to your account</Text>
      </View>

      <TextInput
        label="Email"
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
        autoComplete="email"
        placeholder="you@example.com"
      />
      <TextInput
        label="Password"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        autoComplete="password"
        placeholder="••••••••"
      />

      <TouchableOpacity
        onPress={() => navigation.navigate('ForgotPassword')}
        style={{ alignSelf: 'flex-end', marginBottom: spacing.lg }}
      >
        <Text style={{ color: colors.primary, fontSize: typography.sizes.sm }}>Forgot password?</Text>
      </TouchableOpacity>

      <Button label="Sign In" onPress={handleLogin} loading={loading} fullWidth />

      <AppleAuthentication.AppleAuthenticationButton
        buttonType={AppleAuthentication.AppleAuthenticationButtonType.SIGN_IN}
        buttonStyle={AppleAuthentication.AppleAuthenticationButtonStyle.BLACK}
        cornerRadius={8}
        style={{ height: 48, marginTop: spacing.md }}
        onPress={handleAppleLogin}
      />

      <Button
        label="Continue with Google"
        onPress={handleGoogleLogin}
        variant="outline"
        fullWidth
        style={{ marginTop: spacing.sm }}
      />

      <TouchableOpacity
        onPress={() => navigation.navigate('Register')}
        style={{ marginTop: spacing.xl, alignItems: 'center' }}
      >
        <Text style={{ color: colors.textSecondary, fontSize: typography.sizes.sm }}>
          Don't have an account?{' '}
          <Text style={{ color: colors.primary, fontWeight: '600' }}>Register</Text>
        </Text>
      </TouchableOpacity>
    </ScreenWrapper>
  )
}

const styles = StyleSheet.create({
  header: { marginBottom: 32, marginTop: 24 },
  title: { fontSize: 32, fontWeight: '800', letterSpacing: 2, textAlign: 'center' },
  subtitle: { fontSize: 15, textAlign: 'center', marginTop: 8 },
})
```

- [ ] **Step 3: Write `mobile/src/screens/auth/RegisterScreen.tsx`**

```typescript
import React, { useState } from 'react'
import { Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native'
import { NativeStackNavigationProp } from '@react-navigation/native-stack'
import ScreenWrapper from '@/components/layout/ScreenWrapper'
import { Button, TextInput } from '@/components/ui'
import { useTheme } from '@/hooks/useTheme'
import { useAuthStore } from '@/store/authStore'
import { register } from '@/api/auth'
import { typography } from '@/theme/tokens'
import type { AuthStackParamList } from '@/navigation/AuthNavigator'

type Props = { navigation: NativeStackNavigationProp<AuthStackParamList, 'Register'> }

export default function RegisterScreen({ navigation }: Props) {
  const { colors } = useTheme()
  const { setAuth } = useAuthStore()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [loading, setLoading] = useState(false)

  const handleRegister = async () => {
    if (!displayName || !email || !password) { Alert.alert('Error', 'All fields required'); return }
    if (password !== confirm) { Alert.alert('Error', 'Passwords do not match'); return }
    if (password.length < 8) { Alert.alert('Error', 'Password must be at least 8 characters'); return }
    setLoading(true)
    try {
      const resp = await register(email.trim().toLowerCase(), password, displayName.trim())
      await setAuth(resp.user, resp.accessToken, resp.refreshToken)
    } catch (e: any) {
      Alert.alert('Registration Failed', e?.response?.data?.message ?? 'Please try again')
    } finally {
      setLoading(false)
    }
  }

  return (
    <ScreenWrapper scrollable padded>
      <View style={styles.header}>
        <Text style={[styles.title, { color: colors.primary }]}>CREATE ACCOUNT</Text>
      </View>

      <TextInput label="Display Name" value={displayName} onChangeText={setDisplayName} autoComplete="name" placeholder="Max Verstappen" />
      <TextInput label="Email" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" autoComplete="email" placeholder="you@example.com" />
      <TextInput label="Password" value={password} onChangeText={setPassword} secureTextEntry autoComplete="new-password" placeholder="••••••••" />
      <TextInput label="Confirm Password" value={confirm} onChangeText={setConfirm} secureTextEntry placeholder="••••••••" />

      <Button label="Create Account" onPress={handleRegister} loading={loading} fullWidth />

      <TouchableOpacity onPress={() => navigation.navigate('Login')} style={{ marginTop: 24, alignItems: 'center' }}>
        <Text style={{ color: colors.textSecondary, fontSize: typography.sizes.sm }}>
          Already have an account? <Text style={{ color: colors.primary, fontWeight: '600' }}>Sign In</Text>
        </Text>
      </TouchableOpacity>
    </ScreenWrapper>
  )
}

const styles = StyleSheet.create({
  header: { marginBottom: 32, marginTop: 24 },
  title: { fontSize: 28, fontWeight: '800', letterSpacing: 1.5, textAlign: 'center' },
})
```

- [ ] **Step 4: Write `mobile/src/screens/auth/ForgotPasswordScreen.tsx`**

```typescript
import React, { useState } from 'react'
import { Alert, StyleSheet, Text, View } from 'react-native'
import ScreenWrapper from '@/components/layout/ScreenWrapper'
import { Button, TextInput } from '@/components/ui'
import { useTheme } from '@/hooks/useTheme'
import { forgotPassword } from '@/api/auth'

export default function ForgotPasswordScreen() {
  const { colors } = useTheme()
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [sent, setSent] = useState(false)

  const handleSubmit = async () => {
    if (!email) { Alert.alert('Error', 'Enter your email address'); return }
    setLoading(true)
    try {
      await forgotPassword(email.trim().toLowerCase())
      setSent(true)
    } catch {
      Alert.alert('Error', 'Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <ScreenWrapper padded>
      <View style={styles.header}>
        <Text style={[styles.title, { color: colors.textPrimary }]}>Reset Password</Text>
        <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
          {sent
            ? 'Check your email for a reset link.'
            : "We'll send you a link to reset your password."}
        </Text>
      </View>
      {!sent && (
        <>
          <TextInput label="Email" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" placeholder="you@example.com" />
          <Button label="Send Reset Link" onPress={handleSubmit} loading={loading} fullWidth />
        </>
      )}
    </ScreenWrapper>
  )
}

const styles = StyleSheet.create({
  header: { marginBottom: 32, marginTop: 24 },
  title: { fontSize: 24, fontWeight: '700', marginBottom: 8 },
  subtitle: { fontSize: 15, lineHeight: 22 },
})
```

- [ ] **Step 5: Verify TypeScript**

```bash
cd mobile && npx tsc --noEmit
```
Expected: no errors.

- [ ] **Step 6: Commit**

```bash
git add mobile/src/api/auth.ts mobile/src/screens/auth/
git commit -m "[#101] Add auth screens: Login (+ Apple Sign-In), Register, ForgotPassword"
```

---

## Task 6: Home Dashboard Screen (#102)

**Closes:** #102

**Files:**
- Create: `mobile/src/api/f1data.ts`
- Create: `mobile/src/api/scoring.ts`
- Modify: `mobile/src/screens/home/HomeScreen.tsx`

- [ ] **Step 1: Write `mobile/src/api/f1data.ts`**

```typescript
import apiClient from './client'
import type { Race, Driver, Calendar, RaceResult } from './types'

export async function getNextRace(): Promise<Race | null> {
  const { data } = await apiClient.get<Race | null>('/f1data/races/next')
  return data
}

export async function getCurrentSeasonCalendar(): Promise<Calendar> {
  const { data } = await apiClient.get<Calendar>('/f1data/calendar/current')
  return data
}

export async function getDrivers(): Promise<Driver[]> {
  const { data } = await apiClient.get<Driver[]>('/f1data/drivers/current')
  return data
}

export async function getRaceResults(raceId: string): Promise<RaceResult[]> {
  const { data } = await apiClient.get<RaceResult[]>(`/f1data/races/${raceId}/results`)
  return data
}
```

- [ ] **Step 2: Write `mobile/src/api/scoring.ts`**

```typescript
import apiClient from './client'
import type { LeagueStandings, UserBalance } from './types'

export async function getLeagueStandings(leagueId: string): Promise<LeagueStandings> {
  const { data } = await apiClient.get<LeagueStandings>(`/scoring/leagues/${leagueId}/standings`)
  return data
}

export async function getUserBalance(leagueId: string): Promise<UserBalance> {
  const { data } = await apiClient.get<UserBalance>(`/scoring/leagues/${leagueId}/balance`)
  return data
}
```

- [ ] **Step 3: Write `mobile/src/screens/home/HomeScreen.tsx`**

```typescript
import React, { useEffect, useState } from 'react'
import { StyleSheet, Text, View } from 'react-native'
import { CountdownCircleTimer } from 'react-native-countdown-circle-timer'
import ScreenWrapper from '@/components/layout/ScreenWrapper'
import { Card, Loader } from '@/components/ui'
import { useTheme } from '@/hooks/useTheme'
import { useAuthStore } from '@/store/authStore'
import { getNextRace } from '@/api/f1data'
import { getMyLeagues, getStandings } from '@/api/leagues'
import { colors, spacing, typography } from '@/theme/tokens'
import type { Race, League, LeagueStandings } from '@/api/types'

export default function HomeScreen() {
  const { colors: c } = useTheme()
  const { user } = useAuthStore()
  const [nextRace, setNextRace] = useState<Race | null>(null)
  const [leagueSnapshots, setLeagueSnapshots] = useState<
    Array<{ league: League; myRank: number | null; totalPoints: number }>
  >([])
  const [loading, setLoading] = useState(true)

  const load = async () => {
    try {
      const [race, leagues] = await Promise.all([getNextRace(), getMyLeagues().catch(() => [])])
      setNextRace(race)
      // Fetch standings for each league to build rank snapshots (parallel, fail-open per league)
      const snapshots = await Promise.all(
        leagues.slice(0, 3).map(async (league) => {
          try {
            const s = await getStandings(league.id)
            const me = s.standings.find(st => st.userId === user?.id)
            return { league, myRank: me?.rank ?? null, totalPoints: me?.totalPoints ?? 0 }
          } catch {
            return { league, myRank: null, totalPoints: 0 }
          }
        })
      )
      setLeagueSnapshots(snapshots)
    } catch {
      // fail silently — show empty state
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const secondsUntilRace = nextRace
    ? Math.max(0, Math.floor((new Date(nextRace.raceDateTime).getTime() - Date.now()) / 1000))
    : 0

  const secondsUntilQualifying = nextRace
    ? Math.max(0, Math.floor((new Date(nextRace.qualifyingDateTime).getTime() - Date.now()) / 1000))
    : 0

  if (loading) return <Loader />

  return (
    <ScreenWrapper scrollable padded>
      {/* Header */}
      <View style={styles.headerRow}>
        <Text style={[styles.greeting, { color: c.textPrimary }]}>
          Hey, {user?.displayName?.split(' ')[0] ?? 'Racer'}!
        </Text>
      </View>

      {/* Next Race Card */}
      {nextRace && (
        <Card style={styles.raceCard}>
          <Text style={[styles.cardLabel, { color: c.textMuted }]}>NEXT RACE</Text>
          <Text style={[styles.raceName, { color: c.textPrimary }]}>{nextRace.name}</Text>
          <Text style={[styles.raceDetail, { color: c.textSecondary }]}>
            {nextRace.circuitName} · {nextRace.country}
          </Text>
          <Text style={[styles.raceDate, { color: c.textMuted }]}>
            {new Date(nextRace.raceDateTime).toLocaleDateString('en-GB', {
              weekday: 'long', day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit'
            })}
          </Text>

          <View style={styles.countdownRow}>
            <View style={styles.countdownItem}>
              <Text style={[styles.countdownLabel, { color: c.textMuted }]}>RACE IN</Text>
              <CountdownCircleTimer
                isPlaying
                duration={secondsUntilRace}
                initialRemainingTime={secondsUntilRace}
                colors={colors.primary as any}
                size={80}
                strokeWidth={6}
              >
                {({ remainingTime }) => {
                  const d = Math.floor(remainingTime / 86400)
                  const h = Math.floor((remainingTime % 86400) / 3600)
                  return <Text style={{ color: c.textPrimary, fontSize: 13, fontWeight: '700', textAlign: 'center' }}>
                    {d > 0 ? `${d}d ${h}h` : `${h}h`}
                  </Text>
                }}
              </CountdownCircleTimer>
            </View>

            <View style={styles.countdownItem}>
              <Text style={[styles.countdownLabel, { color: c.textMuted }]}>QUALI DEADLINE</Text>
              <CountdownCircleTimer
                isPlaying
                duration={secondsUntilQualifying}
                initialRemainingTime={secondsUntilQualifying}
                colors={secondsUntilQualifying < 3600 ? colors.accent as any : colors.primary as any}
                size={80}
                strokeWidth={6}
              >
                {({ remainingTime }) => {
                  const d = Math.floor(remainingTime / 86400)
                  const h = Math.floor((remainingTime % 86400) / 3600)
                  const m = Math.floor((remainingTime % 3600) / 60)
                  return <Text style={{ color: c.textPrimary, fontSize: 13, fontWeight: '700', textAlign: 'center' }}>
                    {d > 0 ? `${d}d ${h}h` : h > 0 ? `${h}h ${m}m` : `${m}m`}
                  </Text>
                }}
              </CountdownCircleTimer>
            </View>
          </View>
        </Card>
      )}

      {!nextRace && (
        <Card>
          <Text style={[styles.emptyText, { color: c.textMuted }]}>No upcoming races found.</Text>
        </Card>
      )}

      {/* League rank snapshots */}
      {leagueSnapshots.length > 0 && (
        <View style={{ marginTop: spacing.md }}>
          <Text style={[styles.cardLabel, { color: c.textMuted, marginBottom: spacing.sm }]}>MY LEAGUES</Text>
          {leagueSnapshots.map(({ league, myRank, totalPoints }) => (
            <Card key={league.id} style={{ marginBottom: spacing.sm }}>
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
                <View>
                  <Text style={[{ fontSize: typography.sizes.base, fontWeight: '700' }, { color: c.textPrimary }]}>
                    {league.name}
                  </Text>
                  <Text style={[{ fontSize: typography.sizes.sm }, { color: c.textMuted }]}>
                    {league.memberCount} members
                  </Text>
                </View>
                <View style={{ alignItems: 'flex-end' }}>
                  <Text style={[{ fontSize: typography.sizes.xl, fontWeight: '800' }, { color: colors.primary }]}>
                    {totalPoints} pts
                  </Text>
                  {myRank && (
                    <Text style={[{ fontSize: typography.sizes.sm }, { color: c.textMuted }]}>
                      Rank #{myRank}
                    </Text>
                  )}
                </View>
              </View>
            </Card>
          ))}
        </View>
      )}
    </ScreenWrapper>
  )
}

const styles = StyleSheet.create({
  headerRow: { marginBottom: spacing.lg },
  greeting: { fontSize: typography.sizes['2xl'], fontWeight: '700' },
  raceCard: { marginBottom: spacing.md },
  cardLabel: { fontSize: typography.sizes.xs, fontWeight: '700', letterSpacing: 1.5, marginBottom: 4 },
  raceName: { fontSize: typography.sizes.xl, fontWeight: '700', marginBottom: 4 },
  raceDetail: { fontSize: typography.sizes.sm, marginBottom: 4 },
  raceDate: { fontSize: typography.sizes.sm, marginBottom: spacing.md },
  countdownRow: { flexDirection: 'row', justifyContent: 'space-around', marginTop: spacing.md },
  countdownItem: { alignItems: 'center', gap: 8 },
  countdownLabel: { fontSize: typography.sizes.xs, fontWeight: '600', letterSpacing: 1 },
  emptyText: { textAlign: 'center', paddingVertical: spacing.md },
})
```

- [ ] **Step 4: Verify TypeScript**

```bash
cd mobile && npx tsc --noEmit
```

- [ ] **Step 5: Commit**

```bash
git add mobile/src/api/f1data.ts mobile/src/api/scoring.ts mobile/src/screens/home/
git commit -m "[#102] Add home dashboard screen: next race countdown, qualifying deadline timer"
```

---

## Task 7: Predict Screen (#103)

**Closes:** #103

**Files:**
- Create: `mobile/src/api/predictions.ts`
- Create: `mobile/src/api/leagues.ts`
- Modify: `mobile/src/screens/predict/PredictScreen.tsx`

- [ ] **Step 1: Write `mobile/src/api/predictions.ts`**

```typescript
import apiClient from './client'
import type { Prediction, SubmitBetRequest, BonusBet } from './types'

export async function getPrediction(raceId: string, sessionType = 'RACE'): Promise<Prediction | null> {
  try {
    const { data } = await apiClient.get<Prediction>(`/predictions/${raceId}`, { params: { sessionType } })
    return data
  } catch (e: any) {
    if (e.response?.status === 404) return null
    throw e
  }
}

export async function submitPrediction(raceId: string, rankedDriverCodes: string[], sessionType = 'RACE'): Promise<Prediction> {
  const { data } = await apiClient.post<Prediction>(`/predictions/${raceId}`, { rankedDriverCodes, sessionType })
  return data
}

export async function updatePrediction(raceId: string, rankedDriverCodes: string[], sessionType = 'RACE'): Promise<Prediction> {
  const { data } = await apiClient.put<Prediction>(`/predictions/${raceId}`, { rankedDriverCodes, sessionType })
  return data
}

export async function submitBet(raceId: string, bet: SubmitBetRequest, leagueId: string, sessionType = 'RACE'): Promise<BonusBet> {
  const { data } = await apiClient.post<BonusBet>(`/predictions/${raceId}/bets`, bet, {
    params: { sessionType, leagueId },
  })
  return data
}
```

- [ ] **Step 2: Write `mobile/src/api/leagues.ts`**

```typescript
import apiClient from './client'
import type { League, CreateLeagueRequest, LeagueStandings } from './types'

export async function getMyLeagues(): Promise<League[]> {
  const { data } = await apiClient.get<League[]>('/leagues/me')
  return data
}

export async function getLeague(leagueId: string): Promise<League> {
  const { data } = await apiClient.get<League>(`/leagues/${leagueId}`)
  return data
}

export async function createLeague(req: CreateLeagueRequest): Promise<League> {
  const { data } = await apiClient.post<League>('/leagues', req)
  return data
}

export async function joinLeague(inviteCode: string): Promise<League> {
  const { data } = await apiClient.post<League>('/leagues/join', { inviteCode })
  return data
}

export async function getStandings(leagueId: string): Promise<LeagueStandings> {
  const { data } = await apiClient.get<LeagueStandings>(`/leagues/${leagueId}/standings`)
  return data
}
```

- [ ] **Step 3: Write `mobile/src/screens/predict/PredictScreen.tsx`**

```typescript
import React, { useCallback, useEffect, useState } from 'react'
import { Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native'
import DraggableFlatList, { RenderItemParams, ScaleDecorator } from 'react-native-draggable-flatlist'
import ScreenWrapper from '@/components/layout/ScreenWrapper'
import { Badge, Button, Card, Loader } from '@/components/ui'
import { useTheme } from '@/hooks/useTheme'
import { getDrivers, getNextRace } from '@/api/f1data'
import { getPrediction, submitPrediction, updatePrediction } from '@/api/predictions'
import { getMyLeagues } from '@/api/leagues'
import { spacing, typography, colors as rawColors } from '@/theme/tokens'
import type { Driver, Race, League } from '@/api/types'

interface DriverItem {
  key: string
  code: string
  name: string
  team: string
  number: number
}

export default function PredictScreen() {
  const { colors: c } = useTheme()
  const [race, setRace] = useState<Race | null>(null)
  const [drivers, setDrivers] = useState<DriverItem[]>([])
  const [ranked, setRanked] = useState<DriverItem[]>([])
  const [leagues, setLeagues] = useState<League[]>([])
  const [selectedLeagueId, setSelectedLeagueId] = useState<string | null>(null)
  const [isLocked, setIsLocked] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [existingPredictionId, setExistingPredictionId] = useState<string | null>(null)

  useEffect(() => {
    const init = async () => {
      const [r, d, l] = await Promise.all([getNextRace(), getDrivers(), getMyLeagues()])
      setRace(r)
      setLeagues(l)
      if (l.length > 0) setSelectedLeagueId(l[0].id)
      const items: DriverItem[] = d.map(drv => ({
        key: drv.code, code: drv.code, name: drv.name, team: drv.team, number: drv.number
      }))
      if (r) {
        const existing = await getPrediction(r.id)
        if (existing) {
          setExistingPredictionId(existing.id)
          setIsLocked(existing.status === 'locked')
          const orderedItems = existing.topN.positions
            .map(code => items.find(i => i.code === code))
            .filter(Boolean) as DriverItem[]
          const remaining = items.filter(i => !existing.topN.positions.includes(i.code))
          setRanked([...orderedItems, ...remaining])
        } else {
          setRanked(items)
        }
      } else {
        setRanked(items)
      }
      setDrivers(items)
      setLoading(false)
    }
    init().catch(() => setLoading(false))
  }, [])

  const handleSubmit = async () => {
    if (!race) return
    const depth = leagues.find(l => l.id === selectedLeagueId)?.config?.topNSize ?? 10
    const codes = ranked.slice(0, depth).map(d => d.code)
    setSaving(true)
    try {
      if (existingPredictionId) {
        await updatePrediction(race.id, codes)
      } else {
        await submitPrediction(race.id, codes)
      }
      Alert.alert('Saved', 'Your prediction has been submitted!')
    } catch (e: any) {
      Alert.alert('Error', e?.response?.data?.message ?? 'Failed to save prediction')
    } finally {
      setSaving(false)
    }
  }

  const renderItem = useCallback(({ item, drag, isActive }: RenderItemParams<DriverItem>) => {
    const index = ranked.findIndex(d => d.key === item.key)
    const depth = leagues.find(l => l.id === selectedLeagueId)?.config?.topNSize ?? 10
    const inPrediction = index < depth
    return (
      <ScaleDecorator>
        <TouchableOpacity
          onLongPress={isLocked ? undefined : drag}
          disabled={isActive || isLocked}
          activeOpacity={0.9}
          style={[
            styles.driverRow,
            {
              backgroundColor: isActive ? c.surfaceElevated : c.surface,
              borderColor: inPrediction ? rawColors.primary : c.border,
            }
          ]}
        >
          <Text style={[styles.posNum, { color: inPrediction ? rawColors.primary : c.textMuted }]}>
            {index + 1}
          </Text>
          <View style={styles.driverInfo}>
            <Text style={[styles.driverCode, { color: c.textPrimary }]}>{item.code}</Text>
            <Text style={[styles.driverName, { color: c.textSecondary }]}>{item.name}</Text>
          </View>
          <Text style={[styles.teamName, { color: c.textMuted }]} numberOfLines={1}>{item.team}</Text>
          <Text style={{ color: c.textMuted, fontSize: 18 }}>≡</Text>
        </TouchableOpacity>
      </ScaleDecorator>
    )
  }, [ranked, isLocked, selectedLeagueId, leagues, c])

  if (loading) return <Loader />

  const isDeadlinePassed = race ? new Date(race.qualifyingDateTime) < new Date() : false

  return (
    <View style={{ flex: 1, backgroundColor: c.background }}>
      {/* Header */}
      <View style={[styles.header, { backgroundColor: c.surface, borderBottomColor: c.border }]}>
        <Text style={[styles.headerTitle, { color: c.textPrimary }]}>
          {race ? race.name : 'No Upcoming Race'}
        </Text>
        {isLocked && <Badge label="LOCKED" variant="error" />}
        {isDeadlinePassed && !isLocked && <Badge label="DEADLINE PASSED" variant="warning" />}
      </View>

      <DraggableFlatList
        data={ranked}
        onDragEnd={({ data }) => setRanked(data)}
        keyExtractor={item => item.key}
        renderItem={renderItem}
        containerStyle={{ flex: 1 }}
      />

      {!isLocked && !isDeadlinePassed && race && (
        <View style={[styles.footer, { backgroundColor: c.surface, borderTopColor: c.border }]}>
          <Button
            label="Save Prediction"
            onPress={handleSubmit}
            loading={saving}
            fullWidth
          />
        </View>
      )}
    </View>
  )
}

const styles = StyleSheet.create({
  header: {
    padding: spacing.md,
    borderBottomWidth: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: 60,
  },
  headerTitle: { fontSize: typography.sizes.lg, fontWeight: '700', flex: 1 },
  driverRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm + 2,
    borderLeftWidth: 3,
    borderTopWidth: 0,
    borderRightWidth: 0,
    borderBottomWidth: 0,
    marginHorizontal: spacing.sm,
    marginVertical: 2,
    borderRadius: 8,
    borderColor: 'transparent',
    gap: spacing.sm,
  },
  posNum: { width: 28, fontSize: typography.sizes.base, fontWeight: '700', textAlign: 'center' },
  driverInfo: { flex: 1 },
  driverCode: { fontSize: typography.sizes.base, fontWeight: '700', letterSpacing: 0.5 },
  driverName: { fontSize: typography.sizes.xs },
  teamName: { fontSize: typography.sizes.xs, maxWidth: 100, textAlign: 'right', marginRight: 8 },
  footer: { padding: spacing.md, borderTopWidth: 1, paddingBottom: 32 },
})
```

- [ ] **Step 4: Verify TypeScript**

```bash
cd mobile && npx tsc --noEmit
```

- [ ] **Step 5: Commit**

```bash
git add mobile/src/api/predictions.ts mobile/src/api/leagues.ts mobile/src/screens/predict/
git commit -m "[#103] Add predict screen with drag-to-rank DraggableFlatList and submit logic"
```

---

## Task 8: Leagues Screen (#104)

**Closes:** #104

**Files:**
- Modify: `mobile/src/screens/leagues/LeaguesScreen.tsx`

- [ ] **Step 1: Write `mobile/src/screens/leagues/LeaguesScreen.tsx`**

```typescript
import React, { useEffect, useState } from 'react'
import {
  Alert, FlatList, Modal, StyleSheet, Text,
  TouchableOpacity, View
} from 'react-native'
import { useNavigation } from '@react-navigation/native'
import { NativeStackNavigationProp } from '@react-navigation/native-stack'
import ScreenWrapper from '@/components/layout/ScreenWrapper'
import { Button, Card, Loader, TextInput } from '@/components/ui'
import { useTheme } from '@/hooks/useTheme'
import { createLeague, getMyLeagues, joinLeague } from '@/api/leagues'
import { spacing, typography } from '@/theme/tokens'
import type { League } from '@/api/types'
import type { LeagueStackParamList } from '@/navigation/AppNavigator'

type Nav = NativeStackNavigationProp<LeagueStackParamList, 'Leagues'>

export default function LeaguesScreen() {
  const { colors: c } = useTheme()
  const navigation = useNavigation<Nav>()
  const [leagues, setLeagues] = useState<League[]>([])
  const [loading, setLoading] = useState(true)
  const [showJoin, setShowJoin] = useState(false)
  const [showCreate, setShowCreate] = useState(false)
  const [inviteCode, setInviteCode] = useState('')
  const [newLeagueName, setNewLeagueName] = useState('')
  const [actionLoading, setActionLoading] = useState(false)

  const load = async () => {
    try { setLeagues(await getMyLeagues()) }
    catch { /* ignore */ }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const handleJoin = async () => {
    if (!inviteCode.trim()) { Alert.alert('Error', 'Enter an invite code'); return }
    setActionLoading(true)
    try {
      const league = await joinLeague(inviteCode.trim().toUpperCase())
      setLeagues(prev => [...prev, league])
      setShowJoin(false)
      setInviteCode('')
    } catch (e: any) {
      Alert.alert('Error', e?.response?.data?.message ?? 'Invalid invite code')
    } finally { setActionLoading(false) }
  }

  const handleCreate = async () => {
    if (!newLeagueName.trim()) { Alert.alert('Error', 'League name required'); return }
    setActionLoading(true)
    try {
      const league = await createLeague({ name: newLeagueName.trim() })
      setLeagues(prev => [...prev, league])
      setShowCreate(false)
      setNewLeagueName('')
    } catch (e: any) {
      Alert.alert('Error', e?.response?.data?.message ?? 'Failed to create league')
    } finally { setActionLoading(false) }
  }

  if (loading) return <Loader />

  return (
    <ScreenWrapper>
      <View style={[styles.header, { backgroundColor: c.surface, borderBottomColor: c.border }]}>
        <Text style={[styles.title, { color: c.textPrimary }]}>My Leagues</Text>
        <View style={styles.headerActions}>
          <TouchableOpacity onPress={() => setShowJoin(true)} style={[styles.iconBtn, { backgroundColor: c.surfaceElevated }]}>
            <Text style={{ color: c.textPrimary, fontSize: 20 }}>+</Text>
          </TouchableOpacity>
        </View>
      </View>

      <FlatList
        data={leagues}
        keyExtractor={l => l.id}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => (
          <TouchableOpacity onPress={() => navigation.navigate('LeagueDetail', { leagueId: item.id, leagueName: item.name })}>
            <Card style={styles.leagueCard}>
              <Text style={[styles.leagueName, { color: c.textPrimary }]}>{item.name}</Text>
              <Text style={[styles.leagueMeta, { color: c.textMuted }]}>{item.memberCount} members · Code: {item.inviteCode}</Text>
            </Card>
          </TouchableOpacity>
        )}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text style={[styles.emptyText, { color: c.textMuted }]}>You're not in any leagues yet.</Text>
            <Button label="Join a League" onPress={() => setShowJoin(true)} variant="outline" />
            <Button label="Create a League" onPress={() => setShowCreate(true)} style={{ marginTop: 8 }} />
          </View>
        }
      />

      {/* Join Modal */}
      <Modal visible={showJoin} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={[styles.modalSheet, { backgroundColor: c.surface }]}>
            <Text style={[styles.modalTitle, { color: c.textPrimary }]}>Join League</Text>
            <TextInput label="Invite Code" value={inviteCode} onChangeText={setInviteCode} autoCapitalize="characters" placeholder="ABC123" />
            <Button label="Join" onPress={handleJoin} loading={actionLoading} fullWidth />
            <Button label="Cancel" onPress={() => setShowJoin(false)} variant="ghost" fullWidth style={{ marginTop: 8 }} />
          </View>
        </View>
      </Modal>

      {/* Create Modal */}
      <Modal visible={showCreate} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={[styles.modalSheet, { backgroundColor: c.surface }]}>
            <Text style={[styles.modalTitle, { color: c.textPrimary }]}>Create League</Text>
            <TextInput label="League Name" value={newLeagueName} onChangeText={setNewLeagueName} placeholder="Red Bull Friends" />
            <Button label="Create" onPress={handleCreate} loading={actionLoading} fullWidth />
            <Button label="Cancel" onPress={() => setShowCreate(false)} variant="ghost" fullWidth style={{ marginTop: 8 }} />
          </View>
        </View>
      </Modal>
    </ScreenWrapper>
  )
}

const styles = StyleSheet.create({
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: spacing.md, paddingTop: 60, borderBottomWidth: 1 },
  title: { fontSize: typography.sizes['2xl'], fontWeight: '700' },
  headerActions: { flexDirection: 'row', gap: 8 },
  iconBtn: { width: 36, height: 36, borderRadius: 18, justifyContent: 'center', alignItems: 'center' },
  list: { padding: spacing.md, gap: spacing.sm },
  leagueCard: {},
  leagueName: { fontSize: typography.sizes.lg, fontWeight: '700', marginBottom: 4 },
  leagueMeta: { fontSize: typography.sizes.sm },
  empty: { flex: 1, alignItems: 'center', paddingTop: 60, gap: 12, paddingHorizontal: spacing.lg },
  emptyText: { fontSize: typography.sizes.base, marginBottom: spacing.sm },
  modalOverlay: { flex: 1, justifyContent: 'flex-end', backgroundColor: 'rgba(0,0,0,0.6)' },
  modalSheet: { borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: spacing.lg, paddingBottom: 40 },
  modalTitle: { fontSize: typography.sizes.xl, fontWeight: '700', marginBottom: spacing.md },
})
```

- [ ] **Step 2: Verify TypeScript**

```bash
cd mobile && npx tsc --noEmit
```

- [ ] **Step 3: Commit**

```bash
git add mobile/src/screens/leagues/LeaguesScreen.tsx
git commit -m "[#104] Add leagues screen: list, join-by-code, and create league modals"
```

---

## Task 9: League Detail Screen (#105)

**Closes:** #105

**Files:**
- Modify: `mobile/src/screens/leagues/LeagueDetailScreen.tsx`

- [ ] **Step 1: Write `mobile/src/screens/leagues/LeagueDetailScreen.tsx`**

```typescript
import React, { useEffect, useState } from 'react'
import { FlatList, StyleSheet, Text, TouchableOpacity, View } from 'react-native'
import { RouteProp, useRoute } from '@react-navigation/native'
import ScreenWrapper from '@/components/layout/ScreenWrapper'
import { Avatar, Badge, Card, Loader } from '@/components/ui'
import { useTheme } from '@/hooks/useTheme'
import { getLeague, getStandings } from '@/api/leagues'
import { spacing, typography, colors as rawColors } from '@/theme/tokens'
import type { League, LeagueStandings, Standing } from '@/api/types'
import type { LeagueStackParamList } from '@/navigation/AppNavigator'

type RoutePropType = RouteProp<LeagueStackParamList, 'LeagueDetail'>

export default function LeagueDetailScreen() {
  const { colors: c } = useTheme()
  const { params } = useRoute<RoutePropType>()
  const [league, setLeague] = useState<League | null>(null)
  const [standings, setStandings] = useState<LeagueStandings | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      try {
        const [l, s] = await Promise.all([
          getLeague(params.leagueId),
          getStandings(params.leagueId),
        ])
        setLeague(l)
        setStandings(s)
      } catch { /* ignore */ }
      finally { setLoading(false) }
    }
    load()
  }, [params.leagueId])

  if (loading) return <Loader />

  const renderStanding = ({ item, index }: { item: Standing; index: number }) => {
    const medal = index === 0 ? '🥇' : index === 1 ? '🥈' : index === 2 ? '🥉' : null
    return (
      <View style={[styles.row, { borderBottomColor: c.border }]}>
        <Text style={[styles.rank, { color: index < 3 ? rawColors.primary : c.textMuted }]}>
          {medal ?? item.rank}
        </Text>
        <Avatar name={item.displayName} size={36} />
        <View style={styles.rowInfo}>
          <Text style={[styles.displayName, { color: c.textPrimary }]}>{item.displayName}</Text>
          <Text style={[styles.races, { color: c.textMuted }]}>{item.racesScored} races</Text>
        </View>
        <Text style={[styles.points, { color: rawColors.primary }]}>{item.totalPoints} pts</Text>
      </View>
    )
  }

  return (
    <ScreenWrapper>
      {/* Header */}
      <View style={[styles.header, { backgroundColor: c.surface, borderBottomColor: c.border }]}>
        <View>
          <Text style={[styles.leagueName, { color: c.textPrimary }]}>{league?.name}</Text>
          <Text style={[styles.leagueMeta, { color: c.textMuted }]}>
            {league?.memberCount} members · Invite: {league?.inviteCode}
          </Text>
        </View>
      </View>

      <FlatList
        data={standings?.standings ?? []}
        keyExtractor={s => s.userId}
        renderItem={renderStanding}
        ListHeaderComponent={
          <Text style={[styles.sectionTitle, { color: c.textMuted }]}>STANDINGS</Text>
        }
        contentContainerStyle={styles.list}
        ListEmptyComponent={
          <Text style={[styles.empty, { color: c.textMuted }]}>No standings yet.</Text>
        }
      />
    </ScreenWrapper>
  )
}

const styles = StyleSheet.create({
  header: { padding: spacing.md, paddingTop: 60, borderBottomWidth: 1, marginBottom: spacing.sm },
  leagueName: { fontSize: typography.sizes.xl, fontWeight: '700' },
  leagueMeta: { fontSize: typography.sizes.sm, marginTop: 4 },
  sectionTitle: { fontSize: typography.sizes.xs, fontWeight: '700', letterSpacing: 1.5, marginBottom: spacing.sm, paddingHorizontal: spacing.md },
  list: { paddingBottom: 40 },
  row: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: spacing.md, paddingVertical: spacing.sm + 2, borderBottomWidth: 1, gap: spacing.sm },
  rank: { width: 28, fontSize: typography.sizes.base, fontWeight: '700', textAlign: 'center' },
  rowInfo: { flex: 1 },
  displayName: { fontSize: typography.sizes.base, fontWeight: '600' },
  races: { fontSize: typography.sizes.xs },
  points: { fontSize: typography.sizes.lg, fontWeight: '800' },
  empty: { textAlign: 'center', paddingTop: 40 },
})
```

- [ ] **Step 2: Verify TypeScript**

```bash
cd mobile && npx tsc --noEmit
```

- [ ] **Step 3: Commit**

```bash
git add mobile/src/screens/leagues/LeagueDetailScreen.tsx
git commit -m "[#105] Add league detail screen: standings leaderboard and invite code"
```

---

## Task 10: Profile and Settings Screen (#106)

**Closes:** #106

**Files:**
- Modify: `mobile/src/screens/profile/ProfileScreen.tsx`

- [ ] **Step 1: Write `mobile/src/screens/profile/ProfileScreen.tsx`**

```typescript
import React, { useEffect, useState } from 'react'
import { Alert, StyleSheet, Switch, Text, TouchableOpacity, View } from 'react-native'
import ScreenWrapper from '@/components/layout/ScreenWrapper'
import { Avatar, Card, Loader } from '@/components/ui'
import { useTheme } from '@/hooks/useTheme'
import { useAuthStore } from '@/store/authStore'
import { useThemeStore } from '@/store/themeStore'
import { getMe } from '@/api/auth'
import apiClient from '@/api/client'
import { spacing, typography } from '@/theme/tokens'
import type { User } from '@/api/types'

interface NotificationPrefs {
  predictionReminder: boolean
  raceStart: boolean
  resultsPublished: boolean
  scoreAmended: boolean
}

async function getNotificationPrefs(): Promise<NotificationPrefs> {
  const { data } = await apiClient.get<NotificationPrefs>('/notifications/preferences')
  return data
}

async function updateNotificationPrefs(prefs: Partial<NotificationPrefs>): Promise<void> {
  await apiClient.put('/notifications/preferences', prefs)
}

export default function ProfileScreen() {
  const { colors: c } = useTheme()
  const { user, clearAuth } = useAuthStore()
  const { scheme, toggleScheme } = useThemeStore()
  const [profile, setProfile] = useState<User | null>(user)
  const [loading, setLoading] = useState(!user)
  const [notifPrefs, setNotifPrefs] = useState<NotificationPrefs>({
    predictionReminder: true, raceStart: true, resultsPublished: true, scoreAmended: true,
  })

  useEffect(() => {
    const init = async () => {
      if (!user) {
        const me = await getMe().catch(() => null)
        if (me) setProfile(me as User)
      }
      const prefs = await getNotificationPrefs().catch(() => null)
      if (prefs) setNotifPrefs(prefs)
      setLoading(false)
    }
    init()
  }, [user])

  const toggleNotifPref = async (key: keyof NotificationPrefs) => {
    const updated = { ...notifPrefs, [key]: !notifPrefs[key] }
    setNotifPrefs(updated)
    updateNotificationPrefs({ [key]: updated[key] }).catch(() => {
      // Revert on failure
      setNotifPrefs(notifPrefs)
    })
  }

  const handleSignOut = () => {
    Alert.alert('Sign Out', 'Are you sure?', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Sign Out', style: 'destructive', onPress: () => clearAuth() },
    ])
  }

  if (loading) return <Loader />

  return (
    <ScreenWrapper scrollable padded>
      {/* Avatar + Name */}
      <View style={styles.profileHeader}>
        <Avatar name={profile?.displayName ?? 'User'} size={72} />
        <Text style={[styles.displayName, { color: c.textPrimary }]}>{profile?.displayName}</Text>
        <Text style={[styles.email, { color: c.textMuted }]}>{profile?.email}</Text>
      </View>

      {/* Settings */}
      <Card style={styles.section}>
        <Text style={[styles.sectionTitle, { color: c.textMuted }]}>APPEARANCE</Text>
        <View style={styles.settingRow}>
          <Text style={[styles.settingLabel, { color: c.textPrimary }]}>Dark Mode</Text>
          <Switch
            value={scheme === 'dark'}
            onValueChange={toggleScheme}
            trackColor={{ false: c.border, true: c.primary }}
            thumbColor="#fff"
          />
        </View>
      </Card>

      <Card style={styles.section}>
        <Text style={[styles.sectionTitle, { color: c.textMuted }]}>NOTIFICATIONS</Text>
        {(
          [
            ['predictionReminder', 'Pre-qualifying reminder'],
            ['raceStart', 'Race start alert'],
            ['resultsPublished', 'Results published'],
            ['scoreAmended', 'Score amended'],
          ] as [keyof NotificationPrefs, string][]
        ).map(([key, label]) => (
          <View key={key} style={styles.settingRow}>
            <Text style={[styles.settingLabel, { color: c.textPrimary }]}>{label}</Text>
            <Switch
              value={notifPrefs[key]}
              onValueChange={() => toggleNotifPref(key)}
              trackColor={{ false: c.border, true: c.primary }}
              thumbColor="#fff"
            />
          </View>
        ))}
      </Card>

      <Card style={styles.section}>
        <Text style={[styles.sectionTitle, { color: c.textMuted }]}>ACCOUNT</Text>
        <TouchableOpacity style={styles.settingRow} onPress={handleSignOut}>
          <Text style={[styles.settingLabel, { color: c.error }]}>Sign Out</Text>
        </TouchableOpacity>
      </Card>
    </ScreenWrapper>
  )
}

const styles = StyleSheet.create({
  profileHeader: { alignItems: 'center', paddingVertical: spacing.xl, gap: 8 },
  displayName: { fontSize: typography.sizes['2xl'], fontWeight: '700' },
  email: { fontSize: typography.sizes.sm },
  section: { marginBottom: spacing.md },
  sectionTitle: { fontSize: typography.sizes.xs, fontWeight: '700', letterSpacing: 1.5, marginBottom: spacing.sm },
  settingRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: spacing.sm },
  settingLabel: { fontSize: typography.sizes.base },
})
```

- [ ] **Step 2: Verify TypeScript**

```bash
cd mobile && npx tsc --noEmit
```

- [ ] **Step 3: Commit**

```bash
git add mobile/src/screens/profile/ProfileScreen.tsx
git commit -m "[#106] Add profile and settings screen: avatar, dark mode toggle, sign out"
```

---

## Task 11: Push Notifications (#107)

**Closes:** #107

**Files:**
- Create: `mobile/src/notifications/push.ts`

- [ ] **Step 1: Write `mobile/src/notifications/push.ts`**

```typescript
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
 * Request push permission and register token with Notification Service.
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
 * Pass the navigation ref (from RootNavigator) to navigate programmatically.
 */
export function handleNotificationResponse(
  response: Notifications.NotificationResponse,
  navigate: (screen: string, params?: object) => void,
) {
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
```

- [ ] **Step 2: Call `registerForPushNotifications` after login**

In `mobile/src/store/authStore.ts`, update `setAuth` to trigger registration:

```typescript
import { registerForPushNotifications } from '@/notifications/push'

// inside setAuth:
setAuth: async (user, accessToken, refreshToken) => {
  await SecureStore.setItemAsync(ACCESS_TOKEN_KEY, accessToken)
  await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, refreshToken)
  set({ user, accessToken })
  // Register for push after login — non-blocking
  registerForPushNotifications().catch(console.warn)
},
```

- [ ] **Step 3: Verify TypeScript**

```bash
cd mobile && npx tsc --noEmit
```

- [ ] **Step 4: Commit**

```bash
git add mobile/src/notifications/push.ts mobile/src/store/authStore.ts
git commit -m "[#107] Add push notification setup: APNs/FCM via Expo Notifications, token registration"
```

---

## Task 12: Deep Link Handlers (#108)

**Closes:** #108

**Files:**
- Modify: `mobile/src/navigation/RootNavigator.tsx`

- [ ] **Step 1: Add notification response listener to RootNavigator**

Update `mobile/src/navigation/RootNavigator.tsx` to add the notification listener using a navigation ref:

```typescript
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
      handleNotificationResponse(response, (screen, params) => {
        navRef.current?.navigate(screen as never, params as never)
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
```

- [ ] **Step 2: Verify TypeScript**

```bash
cd mobile && npx tsc --noEmit
```

- [ ] **Step 3: Commit**

```bash
git add mobile/src/navigation/RootNavigator.tsx
git commit -m "[#108] Add deep link handlers: notification tap routes to Predict, LeagueDetail, or Home"
```

---

## Task 13: App Store Submission Prep (#109)

**Closes:** #109

**Files:**
- Create: `mobile/docs/app-store-metadata.md`
- Create: `mobile/docs/privacy-policy.md`

> Note: Actual 1024×1024 production-quality icon and screenshots are created by a human designer using the real app running on a device/simulator. This task sets up the metadata and policy text; placeholder assets from Task 2 remain until the designer delivers final assets.

- [ ] **Step 1: Write `mobile/docs/app-store-metadata.md`**

```markdown
# App Store Metadata — F1 Predict (iOS)

## App Name
F1 Predict

## Subtitle
Formula 1 Predictions & Leagues

## Category
Sports

## Age Rating
4+

## Description
F1 Predict lets you compete with friends by predicting Formula 1 race outcomes.

**How it works:**
- Predict the top 10 finishing order before each race
- Place bonus bets on fastest lap, safety cars, and retirements
- Earn points based on accuracy — exact positions score highest
- Create or join private leagues with custom scoring rules
- Track live race positions in real-time during the race

**Features:**
- Drag-to-rank prediction interface
- Customisable scoring per league
- Full 2025 F1 calendar including sprint weekends
- Live race dashboard with projected scores
- Push notifications for race reminders and results
- Dark and light mode

## Keywords
F1, Formula 1, Formula One, prediction, fantasy, racing, MotoGP, motorsport, leagues, friends

## Support URL
https://f1predict.app/support

## Marketing URL
https://f1predict.app

## Privacy Policy URL
https://f1predict.app/privacy

## Screenshots Required
- 6.9" iPhone (iPhone 16 Pro Max): 1320×2868 — 3 required, up to 10
- 6.5" iPhone (iPhone 14 Plus): 1284×2778 — 3 required
- iPad Pro 12.9" (6th gen): 2048×2732 — 3 required (if tablet supported)

### Screenshot suggestions
1. Home screen — race countdown
2. Predict screen — drag-to-rank in action
3. Leagues screen — leaderboard
4. Live race screen (Sprint 5)
5. Profile / settings

## App Review Notes
Test account for review:
- Email: reviewer@f1predict.app
- Password: [set before submission]
- The app requires an active F1 race season to show race data. During off-season, the next race card shows "No Upcoming Race".
```

- [ ] **Step 2: Write `mobile/docs/privacy-policy.md`**

```markdown
# F1 Predict — Privacy Policy

*Last updated: 2026-03-27*

## Data We Collect
- **Account data:** email address, display name, encrypted password (if using email registration)
- **OAuth tokens:** Google and Apple Sign-In tokens (not stored — used only to create a session)
- **Prediction data:** your race predictions, bonus bets, and scores
- **Device tokens:** push notification token (to send race reminders and results)
- **Usage analytics:** prediction submissions, league joins, live dashboard views — no PII in raw events

## Data We Do Not Collect
- Location data
- Contacts
- Payment information (the app is free with no in-app purchases)
- Advertising identifiers

## How We Use Your Data
- To operate the prediction and league service
- To send you push notifications you have opted into (race reminders, results)
- To display your anonymised rank in league leaderboards

## Data Sharing
We do not sell or share your personal data with third parties for advertising purposes. Push notifications are delivered via Apple APNs (iOS) and Google FCM (Android) — these providers receive only your device token.

## Data Deletion
You may request deletion of your account and all associated data by emailing privacy@f1predict.app. Deletion is processed within 30 days.

## Contact
privacy@f1predict.app
```

- [ ] **Step 3: Commit**

```bash
git add mobile/docs/
git commit -m "[#109] Add App Store metadata: description, keywords, screenshot specs, privacy policy"
```

---

## Task 14: Google Play Submission Prep (#110)

**Closes:** #110

**Files:**
- Create: `mobile/docs/play-store-metadata.md`

- [ ] **Step 1: Write `mobile/docs/play-store-metadata.md`**

```markdown
# Google Play Metadata — F1 Predict (Android)

## App Name
F1 Predict

## Short Description (80 chars max)
Predict F1 race results and compete in friends leagues.

## Full Description (4000 chars max)
F1 Predict lets you compete with friends by predicting Formula 1 race outcomes.

**How it works:**
Predict the top 10 finishing order before each qualifying session closes. The closer your predictions, the more points you score. Exact position = 10 points. One position off = 7 points. Two positions off = 2 points.

**Bonus Bets:**
Go further with bonus bets — stake your points on fastest lap driver, safety car deployments, or who will retire from the race. Win your stake × multiplier; lose it if you're wrong.

**Private Leagues:**
Create a private league with friends and customise the scoring to your group's preference. Set your own prediction depth, offset tiers, and bonus bet multiplier. The league admin controls all settings.

**Live Race Dashboard:**
During the race, watch live positions update in real-time. See your projected score if the race ended right now. An orange highlight shows any driver within 2 positions of your predicted slot — a live battle that could change your score.

**Supports:**
- Full 2025 F1 calendar including sprint race weekends
- Real-time race positions via the official OpenF1 API
- Push notifications for race reminders and results
- Dark and light mode

## Category
Sports

## Content Rating
Everyone (PEGI 3 / Everyone)
No violence, no gambling (no real money), no user-generated content moderation issues.

## Tags
F1, Formula 1, racing, fantasy sports, predictions, sports league

## Contact Email
support@f1predict.app

## Privacy Policy URL
https://f1predict.app/privacy

## Screenshots Required
- Phone: 1080×1920 minimum, up to 8 screenshots
- 7-inch tablet: optional
- 10-inch tablet: optional
- Feature graphic: 1024×500

### Screenshot suggestions (same as iOS)
1. Home screen — race countdown
2. Predict screen — drag-to-rank
3. Leagues leaderboard
4. Live race dashboard (Sprint 5)
5. Dark/light mode comparison

## Data Safety Section (Google Play Console)
- Data collected: Name, Email Address, App interactions, App activity
- Data shared: None
- All data encrypted in transit: Yes
- User can request deletion: Yes — via privacy@f1predict.app
```

- [ ] **Step 2: Commit**

```bash
git add mobile/docs/play-store-metadata.md
git commit -m "[#110] Add Google Play metadata: description, content rating, data safety, screenshots spec"
```

---

## Final Steps: Push Branch and Open PR

- [ ] **Push branch**

```bash
git push -u origin feature/97-mobile-expo-setup
```

(All mobile commits are on this one branch; the branch was created in Task 1.)

- [ ] **Verify CI passes**

```bash
gh run watch
```
Expected: CI passes. The CI workflow runs `npx tsc --noEmit` in the `mobile/` directory (add this to the CI YAML if not already present — see below).

- [ ] **Update CI YAML to include mobile TypeScript check**

In `.github/workflows/ci.yml`, add under the build step:

```yaml
- name: Mobile TypeScript check
  working-directory: mobile
  run: |
    npm install
    npx tsc --noEmit
```

- [ ] **Open PR**

```bash
gh pr create \
  --title "feat: Sprint 4 — Mobile app (React Native + Expo)" \
  --body "$(cat <<'EOF'
## Summary

- Expo + TypeScript project scaffold (#97)
- app.json: iOS bundle ID `com.f1predict.app`, Android package `com.f1predict.app`, icons, splash (#98)
- React Navigation native-stack + bottom tabs with auth-gating (#99)
- Design system: tokens, ScreenWrapper, Button, TextInput, Card, Badge, Avatar, Loader (#100)
- Auth screens: Login (Apple Sign-In), Register, ForgotPassword (#101)
- Home dashboard: next race countdown + qualifying deadline timers (#102)
- Predict screen: drag-to-rank with `react-native-draggable-flatlist` (#103)
- Leagues screen: list, join by code, create league (#104)
- League detail: standings leaderboard (#105)
- Profile & settings: dark mode toggle, sign out (#106)
- Push notifications: Expo Notifications, APNs/FCM, device token registration (#107)
- Deep link handlers: notification tap → correct screen (#108)
- App Store metadata and privacy policy (#109)
- Google Play metadata and data safety (#110)

## Test Plan
- [ ] `npx tsc --noEmit` in `mobile/` — zero errors
- [ ] CI passes
- [ ] `npx expo start` launches in iOS Simulator — auth flow navigates correctly
- [ ] Drag reorders drivers on Predict screen
- [ ] League join/create modals open and dismiss
- [ ] Sign out clears auth and shows Login screen

Closes #97, Closes #98, Closes #99, Closes #100, Closes #101, Closes #102, Closes #103, Closes #104, Closes #105, Closes #106, Closes #107, Closes #108, Closes #109, Closes #110
EOF
)"
```
