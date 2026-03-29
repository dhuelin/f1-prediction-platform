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
      <View style={styles.profileHeader}>
        <Avatar name={profile?.displayName ?? 'User'} size={72} />
        <Text style={[styles.displayName, { color: c.textPrimary }]}>{profile?.displayName}</Text>
        <Text style={[styles.email, { color: c.textMuted }]}>{profile?.email}</Text>
      </View>

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
  settingRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: spacing.sm, minHeight: 44 },
  settingLabel: { fontSize: typography.sizes.base },
})
