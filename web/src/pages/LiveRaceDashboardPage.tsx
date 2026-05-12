import { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { Loader } from '@/components/ui/Loader'
import { useWebSocket } from '@/hooks/useWebSocket'
import { useAuthStore } from '@/store/authStore'
import { getPrediction } from '@/api/predictions'
import { getProjectedScores, type ProjectedEntry } from '@/api/scoring'
import type { Prediction } from '@/api/types'

// Shape of each driver entry in the STOMP live position event
interface DriverPosition {
  driverNumber: number
  driverCode: string
  position: number
}

interface LivePositionEvent {
  sessionKey: number
  raceId: string | null
  timestamp: string
  positions: DriverPosition[]
}

// ----------------------------------------------------------------
// Proximity class: how many positions off vs the prediction
// ----------------------------------------------------------------
function proximityClass(predictedPos: number, actualPos: number): string {
  const diff = Math.abs(predictedPos - actualPos)
  if (diff === 0) return 'bg-green-500/20 text-green-400 font-bold'
  if (diff <= 2) return 'bg-orange-500/20 text-orange-400'
  return ''
}

// ----------------------------------------------------------------
// Sub-component: one row in the side-by-side grid
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
  const rowClass = actualPos !== null ? proximityClass(predictedPos, actualPos) : ''
  return (
    <div className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-colors ${rowClass}`}>
      <span className="w-7 text-right tabular-nums text-sm text-secondary">{predictedPos}</span>
      <span className="flex-1 font-mono font-semibold tracking-widest">{driverCode}</span>
      <span className="w-7 text-right tabular-nums text-sm">
        {actualPos !== null ? `P${actualPos}` : '—'}
      </span>
      {actualPos !== null && (
        <span className="w-8 text-right tabular-nums text-xs text-secondary">
          {actualPos - predictedPos > 0 ? `+${actualPos - predictedPos}` : actualPos - predictedPos}
        </span>
      )}
    </div>
  )
}

// ----------------------------------------------------------------
// Main page
// ----------------------------------------------------------------
export function LiveRaceDashboardPage() {
  const { raceId } = useParams<{ raceId: string }>()
  const [searchParams] = useSearchParams()
  const leagueId = searchParams.get('leagueId') ?? ''
  const raceNumber = Number(searchParams.get('raceNumber') ?? '0')
  const { user } = useAuthStore()

  const [prediction, setPrediction] = useState<Prediction | null>(null)
  const [projected, setProjected] = useState<ProjectedEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Live positions from WebSocket
  const topic = raceId ? `/topic/live/race/${raceId}` : null
  const { connected, lastMessage: liveEvent } = useWebSocket<LivePositionEvent>(topic)

  // Build a lookup: driverCode → live position number
  const livePositionMap = new Map<string, number>(
    (liveEvent?.positions ?? []).map((p) => [p.driverCode, p.position]),
  )

  // Fetch prediction on mount
  useEffect(() => {
    if (!raceId) return
    setLoading(true)
    getPrediction(raceId)
      .then((p) => setPrediction(p))
      .catch(() => setError('Could not load your prediction.'))
      .finally(() => setLoading(false))
  }, [raceId])

  // Poll projected scores whenever live positions update
  useEffect(() => {
    if (!raceId || !leagueId || !liveEvent || raceNumber === 0) return
    getProjectedScores(raceId, leagueId, raceNumber)
      .then(setProjected)
      .catch(() => {/* fail silently — projection is best-effort */})
  }, [raceId, leagueId, raceNumber, liveEvent])

  if (loading) return <div className="flex justify-center py-20"><Loader /></div>
  if (error) return <p className="text-center py-10 text-red-400">{error}</p>

  const rankedDrivers: string[] = prediction?.topN?.positions ?? []
  const myProjected = projected.find((e) => e.userId === user?.id)

  return (
    <div className="max-w-3xl mx-auto px-4 py-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Live Race</h1>
        <Badge variant={connected ? 'success' : 'warning'}>
          {connected ? 'Live' : 'Connecting…'}
        </Badge>
      </div>

      {/* Side-by-side grid */}
      <Card>
        <div className="p-4 space-y-1">
          {/* Column headers */}
          <div className="flex items-center gap-3 px-3 pb-2 border-b border-white/10 text-xs text-secondary uppercase tracking-wide">
            <span className="w-7 text-right">Pred</span>
            <span className="flex-1">Driver</span>
            <span className="w-7 text-right">Live</span>
            <span className="w-8 text-right">Δ</span>
          </div>

          {rankedDrivers.length === 0 ? (
            <p className="text-center py-6 text-secondary text-sm">
              No prediction submitted for this race.
            </p>
          ) : (
            rankedDrivers.map((code, idx) => (
              <PositionRow
                key={code}
                predictedPos={idx + 1}
                driverCode={code}
                actualPos={livePositionMap.get(code) ?? null}
              />
            ))
          )}
        </div>
      </Card>

      {/* Projected score */}
      {myProjected !== undefined && (
        <Card>
          <div className="p-4 flex items-center justify-between">
            <span className="text-sm text-secondary">Projected race points</span>
            <span className="text-2xl font-bold tabular-nums">
              {myProjected.projectedRacePoints}
            </span>
          </div>
          <div className="px-4 pb-4 flex items-center justify-between">
            <span className="text-sm text-secondary">Current league total</span>
            <span className="text-lg font-semibold tabular-nums text-secondary">
              {myProjected.currentLeaguePoints}
            </span>
          </div>
        </Card>
      )}

      {/* Timestamp */}
      {liveEvent && (
        <p className="text-center text-xs text-secondary">
          Last update: {new Date(liveEvent.timestamp).toLocaleTimeString()}
        </p>
      )}
    </div>
  )
}
