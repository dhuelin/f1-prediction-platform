import React from 'react'
import {
  View,
  Text,
  ScrollView,
  StyleSheet,
  ActivityIndicator,
} from 'react-native'
import type { NativeStackScreenProps } from '@react-navigation/native-stack'
import type { HomeStackParamList } from '@/navigation/AppNavigator'
import { useLiveRacePolling } from '@/hooks/useLiveRacePolling'
import { useAuthStore } from '@/store/authStore'
import { colors, spacing, typography, radius } from '@/theme/tokens'
import type { LiveRaceState } from '@/api/f1data'
import type { ProjectedEntry } from '@/api/scoring'

type Props = NativeStackScreenProps<HomeStackParamList, 'LiveRace'>

// ----------------------------------------------------------------
// Helpers
// ----------------------------------------------------------------

function highlightStyle(predictedPos: number, actualPos: number | null) {
  if (actualPos === null) return null
  const diff = Math.abs(predictedPos - actualPos)
  if (diff === 0) return styles.rowExact
  if (diff <= 2) return styles.rowClose
  return null
}

function deltaLabel(predictedPos: number, actualPos: number | null): string {
  if (actualPos === null) return '—'
  const d = actualPos - predictedPos
  if (d === 0) return '✓'
  return d > 0 ? `+${d}` : String(d)
}

// ----------------------------------------------------------------
// Sub-components
// ----------------------------------------------------------------

function PositionRow({
  predictedPos,
  driverCode,
  actualPos,
}: {
  predictedPos: number
  driverCode: string
  actualPos: number | null
}) {
  const hl = highlightStyle(predictedPos, actualPos)
  const delta = deltaLabel(predictedPos, actualPos)
  const deltaColor =
    actualPos === null
      ? colors.textMuted
      : Math.abs(actualPos - predictedPos) === 0
        ? colors.success
        : Math.abs(actualPos - predictedPos) <= 2
          ? colors.accent
          : colors.textMuted

  return (
    <View style={[styles.row, hl]}>
      <Text style={[styles.cell, styles.cellPos]}>{predictedPos}</Text>
      <Text style={[styles.cell, styles.cellCode]}>{driverCode}</Text>
      <Text style={[styles.cell, styles.cellLive]}>
        {actualPos !== null ? `P${actualPos}` : '—'}
      </Text>
      <Text style={[styles.cell, styles.cellDelta, { color: deltaColor }]}>{delta}</Text>
    </View>
  )
}

function LiveLeaderboard({
  entries,
  currentUserId,
}: {
  entries: ProjectedEntry[]
  currentUserId: string | undefined
}) {
  const sorted = [...entries].sort(
    (a, b) =>
      b.currentLeaguePoints + b.projectedRacePoints -
      (a.currentLeaguePoints + a.projectedRacePoints),
  )
  return (
    <View style={styles.card}>
      <Text style={styles.cardTitle}>Projected Leaderboard</Text>
      {sorted.map((e, i) => {
        const isMe = e.userId === currentUserId
        return (
          <View key={e.userId} style={[styles.leaderboardRow, isMe && styles.leaderboardRowMe]}>
            <Text style={[styles.leaderboardRank, isMe && styles.meText]}>{i + 1}</Text>
            <Text style={[styles.leaderboardName, isMe && styles.meText]}>
              {isMe ? 'You' : e.userId.slice(0, 8) + '…'}
            </Text>
            <Text style={[styles.leaderboardPoints, isMe && styles.meText]}>
              {e.currentLeaguePoints + e.projectedRacePoints}
              <Text style={styles.projectedDelta}>  (+{e.projectedRacePoints})</Text>
            </Text>
          </View>
        )
      })}
    </View>
  )
}

function BonusBetTrackers({ state }: { state: LiveRaceState }) {
  return (
    <View style={styles.card}>
      <Text style={styles.cardTitle}>Bonus Bet Trackers</Text>
      <View style={styles.trackerGrid}>
        <View style={styles.trackerCell}>
          <Text style={styles.trackerLabel}>Safety Cars</Text>
          <Text style={styles.trackerValue}>{state.scCount}</Text>
        </View>
        <View style={styles.trackerCell}>
          <Text style={styles.trackerLabel}>Virtual SC</Text>
          <Text style={styles.trackerValue}>{state.vscCount}</Text>
        </View>
      </View>
      <View style={styles.trackerCellWide}>
        <Text style={styles.trackerLabel}>Fastest Lap</Text>
        <Text style={styles.trackerCode}>
          {state.fastestLapDriverCode ?? '—'}
          {state.fastestLapDuration !== null && (
            <Text style={styles.trackerLapTime}>  {state.fastestLapDuration.toFixed(3)}s</Text>
          )}
        </Text>
      </View>
      {state.dnfDriverCodes.length > 0 && (
        <View style={styles.trackerCellWide}>
          <Text style={styles.trackerLabel}>DNF / DSQ / DNS</Text>
          <Text style={styles.trackerCode}>{state.dnfDriverCodes.join(' · ')}</Text>
        </View>
      )}
    </View>
  )
}

// ----------------------------------------------------------------
// Main screen (#120)
// ----------------------------------------------------------------
export default function LiveRaceScreen({ route }: Props) {
  const { raceId, leagueId, raceNumber, rankedDriverCodes = [] } = route.params
  const { user } = useAuthStore()

  const { positions, raceState, projected, lastUpdatedAt, isLoading } = useLiveRacePolling({
    raceId,
    leagueId,
    raceNumber,
  })

  const liveMap = new Map<string, number>(positions.map((p) => [p.driverCode, p.position]))
  const myProjected = projected.find((e) => e.userId === user?.id)

  if (isLoading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator color={colors.primary} size="large" />
      </View>
    )
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.title}>Live Race</Text>
        <View style={[styles.livePill, positions.length > 0 ? styles.livePillActive : styles.livePillWaiting]}>
          <Text style={styles.livePillText}>{positions.length > 0 ? 'LIVE' : 'Waiting…'}</Text>
        </View>
      </View>

      {/* Highlight legend (#122) */}
      <View style={styles.legend}>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: colors.success }]} />
          <Text style={styles.legendLabel}>Exact</Text>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: colors.accent }]} />
          <Text style={styles.legendLabel}>Within 2</Text>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: colors.border }]} />
          <Text style={styles.legendLabel}>Out of range</Text>
        </View>
      </View>

      {/* Side-by-side position grid (#120 + #122) */}
      <View style={styles.card}>
        <View style={styles.columnHeaders}>
          <Text style={[styles.columnHeader, styles.cellPos]}>Pred</Text>
          <Text style={[styles.columnHeader, styles.cellCode]}>Driver</Text>
          <Text style={[styles.columnHeader, styles.cellLive]}>Live</Text>
          <Text style={[styles.columnHeader, styles.cellDelta]}>Δ</Text>
        </View>
        {rankedDriverCodes.length === 0 ? (
          <Text style={styles.empty}>No prediction for this race.</Text>
        ) : (
          rankedDriverCodes.map((code, idx) => (
            <PositionRow
              key={code}
              predictedPos={idx + 1}
              driverCode={code}
              actualPos={liveMap.get(code) ?? null}
            />
          ))
        )}
      </View>

      {/* Projected score (own user) */}
      {myProjected !== undefined && (
        <View style={styles.card}>
          <View style={styles.scoreRow}>
            <Text style={styles.scoreLabel}>Projected race pts</Text>
            <Text style={styles.scoreValue}>{myProjected.projectedRacePoints}</Text>
          </View>
          <View style={styles.scoreRow}>
            <Text style={styles.scoreLabel}>League total (projected)</Text>
            <Text style={[styles.scoreValue, { color: colors.textSecondary, fontSize: typography.sizes.lg }]}>
              {myProjected.currentLeaguePoints + myProjected.projectedRacePoints}
            </Text>
          </View>
        </View>
      )}

      {/* Live leaderboard (#120) */}
      {projected.length > 0 && (
        <LiveLeaderboard entries={projected} currentUserId={user?.id} />
      )}

      {/* Bonus bet trackers (#122) */}
      <BonusBetTrackers state={raceState} />

      {/* Last update */}
      {lastUpdatedAt && (
        <Text style={styles.timestamp}>
          Updated {lastUpdatedAt.toLocaleTimeString()}
        </Text>
      )}
    </ScrollView>
  )
}

// ----------------------------------------------------------------
// Styles
// ----------------------------------------------------------------
const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  content: { padding: spacing.md, gap: spacing.md },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.background },

  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  title: { color: colors.textPrimary, fontSize: typography.sizes['2xl'], fontWeight: typography.weights.bold },
  livePill: { paddingHorizontal: spacing.sm, paddingVertical: spacing.xs, borderRadius: radius.full },
  livePillActive: { backgroundColor: colors.primary },
  livePillWaiting: { backgroundColor: colors.surface },
  livePillText: { color: colors.textPrimary, fontSize: typography.sizes.xs, fontWeight: typography.weights.bold },

  legend: { flexDirection: 'row', gap: spacing.md },
  legendItem: { flexDirection: 'row', alignItems: 'center', gap: spacing.xs },
  legendDot: { width: 10, height: 10, borderRadius: radius.sm },
  legendLabel: { color: colors.textSecondary, fontSize: typography.sizes.xs },

  card: { backgroundColor: colors.surface, borderRadius: radius.lg, padding: spacing.md, gap: spacing.xs },
  cardTitle: { color: colors.textSecondary, fontSize: typography.sizes.xs, fontWeight: typography.weights.semibold, textTransform: 'uppercase', letterSpacing: 1, marginBottom: spacing.xs },

  columnHeaders: { flexDirection: 'row', paddingBottom: spacing.xs, borderBottomWidth: 1, borderBottomColor: colors.border, marginBottom: spacing.xs },
  columnHeader: { color: colors.textMuted, fontSize: typography.sizes.xs, textTransform: 'uppercase' },

  row: { flexDirection: 'row', alignItems: 'center', paddingVertical: spacing.xs, paddingHorizontal: spacing.xs, borderRadius: radius.sm, borderLeftWidth: 3, borderLeftColor: 'transparent' },
  rowExact: { backgroundColor: `${colors.success}20`, borderLeftColor: colors.success },
  rowClose: { backgroundColor: `${colors.accent}20`, borderLeftColor: colors.accent },

  cell: { color: colors.textPrimary, fontSize: typography.sizes.sm },
  cellPos: { width: 28, textAlign: 'right', color: colors.textSecondary },
  cellCode: { flex: 1, fontFamily: 'monospace', fontWeight: typography.weights.semibold, marginLeft: spacing.sm, letterSpacing: 2 },
  cellLive: { width: 36, textAlign: 'right' },
  cellDelta: { width: 36, textAlign: 'right', fontSize: typography.sizes.xs },

  empty: { color: colors.textMuted, textAlign: 'center', paddingVertical: spacing.lg },

  scoreRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  scoreLabel: { color: colors.textSecondary, fontSize: typography.sizes.sm },
  scoreValue: { color: colors.textPrimary, fontSize: typography.sizes['2xl'], fontWeight: typography.weights.bold },

  leaderboardRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: spacing.xs, gap: spacing.sm },
  leaderboardRowMe: { backgroundColor: `${colors.primary}15`, borderRadius: radius.sm, paddingHorizontal: spacing.xs },
  leaderboardRank: { color: colors.textMuted, width: 20, textAlign: 'right', fontSize: typography.sizes.sm },
  leaderboardName: { flex: 1, color: colors.textPrimary, fontSize: typography.sizes.xs, fontFamily: 'monospace' },
  leaderboardPoints: { color: colors.textPrimary, fontSize: typography.sizes.sm, fontWeight: typography.weights.semibold },
  projectedDelta: { color: colors.textMuted, fontSize: typography.sizes.xs, fontWeight: typography.weights.normal },
  meText: { color: colors.primary },

  trackerGrid: { flexDirection: 'row', gap: spacing.sm },
  trackerCell: { flex: 1, backgroundColor: colors.surfaceElevated, borderRadius: radius.md, padding: spacing.sm, alignItems: 'center' },
  trackerCellWide: { backgroundColor: colors.surfaceElevated, borderRadius: radius.md, padding: spacing.sm, marginTop: spacing.xs },
  trackerLabel: { color: colors.textSecondary, fontSize: typography.sizes.xs, marginBottom: spacing.xs },
  trackerValue: { color: colors.textPrimary, fontSize: typography.sizes['2xl'], fontWeight: typography.weights.bold },
  trackerCode: { color: colors.textPrimary, fontFamily: 'monospace', fontSize: typography.sizes.base, fontWeight: typography.weights.bold, letterSpacing: 2 },
  trackerLapTime: { color: colors.textSecondary, fontSize: typography.sizes.sm, fontWeight: typography.weights.normal },

  timestamp: { textAlign: 'center', color: colors.textMuted, fontSize: typography.sizes.xs, paddingVertical: spacing.sm },
})
