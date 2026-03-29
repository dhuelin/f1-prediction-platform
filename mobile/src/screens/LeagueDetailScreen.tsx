import React, { useEffect, useState } from 'react'
import { FlatList, StyleSheet, Text, View } from 'react-native'
import { RouteProp, useRoute } from '@react-navigation/native'
import ScreenWrapper from '@/components/layout/ScreenWrapper'
import { Avatar, Loader } from '@/components/ui'
import { useTheme } from '@/hooks/useTheme'
import { getLeague, getStandings } from '@/api/leagues'
import { spacing, typography } from '@/theme/tokens'
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
        <Text style={[styles.rank, { color: index < 3 ? c.primary : c.textMuted }]}>
          {medal ?? item.rank}
        </Text>
        <Avatar name={item.displayName} size={36} />
        <View style={styles.rowInfo}>
          <Text style={[styles.displayName, { color: c.textPrimary }]}>{item.displayName}</Text>
          <Text style={[styles.races, { color: c.textMuted }]}>{item.racesScored} races</Text>
        </View>
        <Text style={[styles.points, { color: c.primary }]}>{item.totalPoints} pts</Text>
      </View>
    )
  }

  return (
    <ScreenWrapper>
      <View style={[styles.header, { backgroundColor: c.surface, borderBottomColor: c.border }]}>
        <Text style={[styles.leagueName, { color: c.textPrimary }]}>{league?.name}</Text>
        <Text style={[styles.leagueMeta, { color: c.textMuted }]}>
          {league?.memberCount} members · Invite: {league?.inviteCode}
        </Text>
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
  header: { padding: spacing.md, borderBottomWidth: 1, marginBottom: spacing.sm },
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
