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
