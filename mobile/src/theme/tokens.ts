import { Platform } from 'react-native'

// ---- Colours (dark mode defaults) ----
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
  // Light mode equivalents
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
