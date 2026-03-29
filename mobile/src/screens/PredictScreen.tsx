import React, { useCallback, useEffect, useState } from 'react'
import { Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import DraggableFlatList, { RenderItemParams, ScaleDecorator } from 'react-native-draggable-flatlist'
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
  const insets = useSafeAreaInsets()
  const [race, setRace] = useState<Race | null>(null)
  const [ranked, setRanked] = useState<DriverItem[]>([])
  const [leagues, setLeagues] = useState<League[]>([])
  const [selectedLeagueId, setSelectedLeagueId] = useState<string | null>(null)
  const [isLocked, setIsLocked] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [existingPredictionId, setExistingPredictionId] = useState<string | null>(null)

  useEffect(() => {
    const init = async () => {
      const [r, d, l] = await Promise.all([getNextRace(), getDrivers(), getMyLeagues().catch(() => [] as League[])])
      setRace(r)
      setLeagues(l)
      if (l.length > 0) setSelectedLeagueId(l[0].id)
      const items: DriverItem[] = d.map((drv: Driver) => ({
        key: drv.code, code: drv.code, name: drv.name, team: drv.team, number: drv.number,
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
              // Dim rows when locked so the drag handle affordance isn't misleading
              opacity: isLocked ? 0.55 : 1,
            },
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
          {!isLocked && <Text style={{ color: c.textMuted, fontSize: 18 }}>≡</Text>}
        </TouchableOpacity>
      </ScaleDecorator>
    )
  }, [ranked, isLocked, selectedLeagueId, leagues, c])

  if (loading) return <Loader />

  // No leagues: guide the user rather than letting them submit into the void
  if (leagues.length === 0) {
    return (
      <View style={[styles.container, { backgroundColor: c.background, paddingTop: insets.top }]}>
        <View style={[styles.header, { backgroundColor: c.surface, borderBottomColor: c.border }]}>
          <Text style={[styles.headerTitle, { color: c.textPrimary }]}>Predict</Text>
        </View>
        <View style={styles.emptyState}>
          <Card>
            <Text style={[styles.emptyTitle, { color: c.textPrimary }]}>No league joined yet</Text>
            <Text style={[styles.emptyBody, { color: c.textMuted }]}>
              Join or create a league from the Leagues tab before submitting a prediction.
            </Text>
          </Card>
        </View>
      </View>
    )
  }

  const isDeadlinePassed = race ? new Date(race.qualifyingDateTime) < new Date() : false

  return (
    <View style={[styles.container, { backgroundColor: c.background, paddingTop: insets.top }]}>
      <View style={[styles.header, { backgroundColor: c.surface, borderBottomColor: c.border }]}>
        <Text style={[styles.headerTitle, { color: c.textPrimary }]}>
          {race ? race.name : 'No Upcoming Race'}
        </Text>
        {isLocked && <Badge label="LOCKED" variant="error" />}
        {isDeadlinePassed && !isLocked && <Badge label="DEADLINE PASSED" variant="warning" />}
      </View>

      {/* No race: still show the driver list context-free is confusing — show empty state */}
      {!race ? (
        <View style={styles.emptyState}>
          <Card>
            <Text style={[styles.emptyTitle, { color: c.textPrimary }]}>Off-season</Text>
            <Text style={[styles.emptyBody, { color: c.textMuted }]}>
              No upcoming race found. Check back when the next race weekend is announced.
            </Text>
          </Card>
        </View>
      ) : (
        <>
          <DraggableFlatList
            data={ranked}
            onDragEnd={({ data }) => setRanked(data)}
            keyExtractor={item => item.key}
            renderItem={renderItem}
            containerStyle={{ flex: 1 }}
          />

          {!isLocked && !isDeadlinePassed && (
            <View style={[styles.footer, { backgroundColor: c.surface, borderTopColor: c.border, paddingBottom: insets.bottom + spacing.md }]}>
              <Button label="Save Prediction" onPress={handleSubmit} loading={saving} fullWidth />
            </View>
          )}
        </>
      )}
    </View>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    padding: spacing.md,
    borderBottomWidth: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  headerTitle: { fontSize: typography.sizes.lg, fontWeight: '700', flex: 1 },
  driverRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm + 2,
    minHeight: 44,
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
  footer: { padding: spacing.md, borderTopWidth: 1 },
  emptyState: { flex: 1, justifyContent: 'center', padding: spacing.lg },
  emptyTitle: { fontSize: typography.sizes.lg, fontWeight: '700', marginBottom: spacing.sm },
  emptyBody: { fontSize: typography.sizes.sm, lineHeight: 20 },
})
