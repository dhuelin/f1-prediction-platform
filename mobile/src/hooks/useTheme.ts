import { colors } from '@/theme/tokens'; export function useTheme() { return { isDark: true, colors: { ...colors, surfaceElevated: '#242424' }, toggleScheme: () => {} } }
