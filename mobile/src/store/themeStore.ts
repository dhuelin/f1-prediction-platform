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
