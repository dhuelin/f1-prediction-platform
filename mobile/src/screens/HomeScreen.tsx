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
import type { Race, League } from '@/api/types'

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
      const snapshots = await Promise.all(
        leagues.slice(0, 3).map(async (league: League) => {
          try {
            const s = await getStandings(league.id)
            const me = s.standings.find((st: any) => st.userId === user?.id)
            return { league, myRank: me?.rank ?? null, totalPoints: me?.totalPoints ?? 0 }
          } catch {
            return { league, myRank: null, totalPoints: 0 }
          }
        })
      )
      setLeagueSnapshots(snapshots)
    } catch {
      // fail silently
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
      <View style={styles.headerRow}>
        <Text style={[styles.greeting, { color: c.textPrimary }]}>
          Hey, {user?.displayName?.split(' ')[0] ?? 'Racer'}!
        </Text>
      </View>

      {nextRace && (
        <Card style={styles.raceCard}>
          <Text style={[styles.cardLabel, { color: c.textMuted }]}>NEXT RACE</Text>
          <Text style={[styles.raceName, { color: c.textPrimary }]}>{nextRace.name}</Text>
          <Text style={[styles.raceDetail, { color: c.textSecondary }]}>
            {nextRace.circuitName} · {nextRace.country}
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
                  return (
                    <Text style={{ color: c.textPrimary, fontSize: 13, fontWeight: '700', textAlign: 'center' }}>
                      {d > 0 ? `${d}d ${h}h` : `${h}h`}
                    </Text>
                  )
                }}
              </CountdownCircleTimer>
            </View>

            <View style={styles.countdownItem}>
              <Text style={[styles.countdownLabel, { color: c.textMuted }]}>DEADLINE</Text>
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
                  return (
                    <Text style={{ color: c.textPrimary, fontSize: 13, fontWeight: '700', textAlign: 'center' }}>
                      {d > 0 ? `${d}d ${h}h` : h > 0 ? `${h}h ${m}m` : `${m}m`}
                    </Text>
                  )
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

      {leagueSnapshots.length > 0 && (
        <View style={{ marginTop: spacing.md }}>
          <Text style={[styles.cardLabel, { color: c.textMuted, marginBottom: spacing.sm }]}>MY LEAGUES</Text>
          {leagueSnapshots.map(({ league, myRank, totalPoints }) => (
            <Card key={league.id} style={{ marginBottom: spacing.sm }}>
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
                <View>
                  <Text style={{ fontSize: typography.sizes.base, fontWeight: '700', color: c.textPrimary }}>
                    {league.name}
                  </Text>
                  <Text style={{ fontSize: typography.sizes.sm, color: c.textMuted }}>
                    {league.memberCount} members
                  </Text>
                </View>
                <View style={{ alignItems: 'flex-end' }}>
                  <Text style={{ fontSize: typography.sizes.xl, fontWeight: '800', color: colors.primary }}>
                    {totalPoints} pts
                  </Text>
                  {myRank != null && (
                    <Text style={{ fontSize: typography.sizes.sm, color: c.textMuted }}>
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
  raceDetail: { fontSize: typography.sizes.sm, marginBottom: spacing.md },
  countdownRow: { flexDirection: 'row', justifyContent: 'space-around', marginTop: spacing.md },
  countdownItem: { alignItems: 'center', gap: 8 },
  countdownLabel: { fontSize: typography.sizes.xs, fontWeight: '600', letterSpacing: 1 },
  emptyText: { textAlign: 'center', paddingVertical: spacing.md },
})
