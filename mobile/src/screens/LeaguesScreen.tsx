import React, { useEffect, useState } from 'react'
import {
  Alert, FlatList, Modal, StyleSheet, Text,
  TouchableOpacity, View,
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
