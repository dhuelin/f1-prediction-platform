import { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { Loader } from '@/components/ui/Loader'
import { useWebSocket } from '@/hooks/useWebSocket'
import { useAuthStore } from '@/store/authStore'
import { getPrediction } from '@/api/predictions'
import { getProjectedScores, type ProjectedEntry } from '@/api/scoring'
import { getLiveRaceState, type RaceState } from '@/api/f1data'
import type { Prediction } from '@/api/types'

// ----------------------------------------------------------------
// Types for WebSocket payload
// ----------------------------------------------------------------
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
// Highlight: colour row by proximity to predicted position
// ----------------------------------------------------------------
function rowHighlight(predictedPos: number, actualPos: number): string {
  const diff = Math.abs(predictedPos - actualPos)
  if (diff === 0) return 'bg-green-500/15 border-l-2 border-green-500'
  if (diff <= 2) return 'bg-orange-500/15 border-l-2 border-orange-500'
  return 'border-l-2 border-transparent'
}

// ----------------------------------------------------------------
// Position row in the side-by-side grid (#116 + #118)
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
  const highlight = actualPos !== null ? rowHighlight(predictedPos, actualPos) : 'border-l-2 border-transparent'
  const delta = actualPos !== null ? actualPos - predictedPos : null

  return (
    <div className={`flex items-center gap-3 px-3 py-2 rounded-r-lg transition-colors ${highlight}`}>
      <span className="w-6 text-right tabular-nums text-sm text-secondary">{predictedPos}</span>
      <span className="flex-1 font-mono font-semibold tracking-widest text-sm">{driverCode}</span>
      <span className="w-8 text-right tabular-nums text-sm font-medium">
        {actualPos !== null ? `P${actualPos}` : '—'}
      </span>
      <span className={`w-8 text-right tabular-nums text-xs ${delta === 0 ? 'text-green-400' : delta !== null && Math.abs(delta) <= 2 ? 'text-orange-400' : 'text-secondary'}`}>
        {delta === null ? '' : delta === 0 ? '✓' : delta > 0 ? `+${delta}` : delta}
      </span>
    </div>
  )
}

// ----------------------------------------------------------------
// Live leaderboard widget (#117)
// ----------------------------------------------------------------
function LiveLeaderboardWidget({
  entries,
  currentUserId,
}: {
  entries: ProjectedEntry[]
  currentUserId: string | undefined
}) {
  const sorted = [...entries].sort(
    (a, b) => b.currentLeaguePoints + b.projectedRacePoints - (a.currentLeaguePoints + a.projectedRacePoints),
  )

  return (
    <Card>
      <div className="p-4">
        <h2 className="text-sm font-semibold text-secondary uppercase tracking-wide mb-3">
          Projected Leaderboard
        </h2>
        {sorted.length === 0 ? (
          <p className="text-sm text-secondary text-center py-3">No data yet</p>
        ) : (
          <div className="space-y-1">
            {sorted.map((entry, idx) => {
              const isMe = entry.userId === currentUserId
              return (
                <div
                  key={entry.userId}
                  className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm ${isMe ? 'bg-f1-red/10 font-semibold' : ''}`}
                >
                  <span className="w-5 text-right text-secondary tabular-nums">{idx + 1}</span>
                  <span className="flex-1 truncate font-mono text-xs">
                    {isMe ? 'You' : entry.userId.slice(0, 8) + '…'}
                  </span>
                  <span className="tabular-nums">
                    {entry.currentLeaguePoints + entry.projectedRacePoints}
                    <span className="text-xs text-secondary ml-1">
                      (+{entry.projectedRacePoints})
                    </span>
                  </span>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </Card>
  )
}

// ----------------------------------------------------------------
// Bonus bet trackers widget (#119)
// ----------------------------------------------------------------
function BonusBetTrackersWidget({ state }: { state: RaceState | null }) {
  if (!state) return null
  return (
    <Card>
      <div className="p-4">
        <h2 className="text-sm font-semibold text-secondary uppercase tracking-wide mb-3">
          Bonus Bet Trackers
        </h2>
        <div className="grid grid-cols-2 gap-3">
          <div className="bg-surface/50 rounded-lg p-3 text-center">
            <p className="text-xs text-secondary mb-1">Safety Cars</p>
            <p className="text-2xl font-bold tabular-nums">{state.scCount}</p>
          </div>
          <div className="bg-surface/50 rounded-lg p-3 text-center">
            <p className="text-xs text-secondary mb-1">Virtual SC</p>
            <p className="text-2xl font-bold tabular-nums">{state.vscCount}</p>
          </div>
          <div className="col-span-2 bg-surface/50 rounded-lg p-3">
            <p className="text-xs text-secondary mb-1">Fastest Lap</p>
            <p className="text-lg font-bold font-mono tracking-widest">
              {state.fastestLapDriverCode ?? '—'}
              {state.fastestLapDuration !== null && (
                <span className="text-sm text-secondary ml-2 font-normal">
                  {state.fastestLapDuration.toFixed(3)}s
                </span>
              )}
            </p>
          </div>
          {state.dnfDriverCodes.length > 0 && (
            <div className="col-span-2 bg-surface/50 rounded-lg p-3">
              <p className="text-xs text-secondary mb-1">DNF / DSQ / DNS</p>
              <p className="text-sm font-mono tracking-wider">
                {state.dnfDriverCodes.join(' · ')}
              </p>
            </div>
          )}
        </div>
      </div>
    </Card>
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
  const [raceState, setRaceState] = useState<RaceState | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Live positions from WebSocket
  const topic = raceId ? `/topic/live/race/${raceId}` : null
  const { connected, lastMessage: liveEvent } = useWebSocket<LivePositionEvent>(topic)

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

  // Poll projected standings + race state on each live update
  useEffect(() => {
    if (!raceId || !liveEvent) return
    if (leagueId && raceNumber > 0) {
      getProjectedScores(raceId, leagueId, raceNumber).then(setProjected).catch(() => {})
    }
    getLiveRaceState().then(setRaceState).catch(() => {})
  }, [raceId, leagueId, raceNumber, liveEvent])

  if (loading) return <div className="flex justify-center py-20"><Loader /></div>
  if (error) return <p className="text-center py-10 text-red-400">{error}</p>

  const rankedDrivers: string[] = prediction?.topN?.positions ?? []
  const myProjected = projected.find((e) => e.userId === user?.id)

  return (
    <div className="max-w-3xl mx-auto px-4 py-6 space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Live Race</h1>
        <Badge variant={connected ? 'success' : 'warning'}>
          {connected ? 'Live' : 'Connecting…'}
        </Badge>
      </div>

      {/* Highlight legend (#118) */}
      <div className="flex items-center gap-4 text-xs text-secondary px-1">
        <span className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded-sm bg-green-500/40 inline-block" />
          Exact
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded-sm bg-orange-500/40 inline-block" />
          Within 2
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded-sm bg-white/10 inline-block" />
          Out of range
        </span>
      </div>

      {/* Side-by-side grid (#116 + #118) */}
      <Card>
        <div className="p-4 space-y-0.5">
          <div className="flex items-center gap-3 px-3 pb-2 border-b border-white/10 text-xs text-secondary uppercase tracking-wide">
            <span className="w-6 text-right">Pred</span>
            <span className="flex-1">Driver</span>
            <span className="w-8 text-right">Live</span>
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

      {/* Projected score (current user) */}
      {myProjected !== undefined && (
        <Card>
          <div className="p-4 flex items-center justify-between">
            <span className="text-sm text-secondary">Projected race points</span>
            <span className="text-2xl font-bold tabular-nums">{myProjected.projectedRacePoints}</span>
          </div>
          <div className="px-4 pb-4 flex items-center justify-between">
            <span className="text-sm text-secondary">League total (projected)</span>
            <span className="text-lg font-semibold tabular-nums text-secondary">
              {myProjected.currentLeaguePoints + myProjected.projectedRacePoints}
            </span>
          </div>
        </Card>
      )}

      {/* Live leaderboard (#117) */}
      {projected.length > 0 && (
        <LiveLeaderboardWidget entries={projected} currentUserId={user?.id} />
      )}

      {/* Bonus bet trackers (#119) */}
      <BonusBetTrackersWidget state={raceState} />

      {/* Last update timestamp */}
      {liveEvent && (
        <p className="text-center text-xs text-secondary pt-1">
          Last update: {new Date(liveEvent.timestamp).toLocaleTimeString()}
        </p>
      )}
    </div>
  )
}
