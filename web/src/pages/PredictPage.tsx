import React, { useEffect, useState, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import {
  DragDropContext,
  Droppable,
  Draggable,
  type DropResult,
} from '@hello-pangea/dnd'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { Loader } from '@/components/ui/Loader'
import * as f1Api from '@/api/f1data'
import * as predictionsApi from '@/api/predictions'
import * as leaguesApi from '@/api/leagues'
import type { Driver, Prediction, Race } from '@/api/types'
import { cn } from '@/lib/utils'

// ---------------------------------------------------------------------------
// Placeholder data
// ---------------------------------------------------------------------------
const PLACEHOLDER_DRIVERS: Driver[] = [
  { id: 'd-ver', name: 'Max Verstappen', code: 'VER', team: 'Red Bull Racing', number: 1, nationality: 'Dutch' },
  { id: 'd-nor', name: 'Lando Norris', code: 'NOR', team: 'McLaren', number: 4, nationality: 'British' },
  { id: 'd-lec', name: 'Charles Leclerc', code: 'LEC', team: 'Ferrari', number: 16, nationality: 'Monégasque' },
  { id: 'd-ham', name: 'Lewis Hamilton', code: 'HAM', team: 'Ferrari', number: 44, nationality: 'British' },
  { id: 'd-rus', name: 'George Russell', code: 'RUS', team: 'Mercedes', number: 63, nationality: 'British' },
  { id: 'd-pia', name: 'Oscar Piastri', code: 'PIA', team: 'McLaren', number: 81, nationality: 'Australian' },
  { id: 'd-sai', name: 'Carlos Sainz', code: 'SAI', team: 'Williams', number: 55, nationality: 'Spanish' },
  { id: 'd-per', name: 'Sergio Perez', code: 'PER', team: 'Red Bull Racing', number: 11, nationality: 'Mexican' },
  { id: 'd-alo', name: 'Fernando Alonso', code: 'ALO', team: 'Aston Martin', number: 14, nationality: 'Spanish' },
  { id: 'd-str', name: 'Lance Stroll', code: 'STR', team: 'Aston Martin', number: 18, nationality: 'Canadian' },
  { id: 'd-gas', name: 'Pierre Gasly', code: 'GAS', team: 'Alpine', number: 10, nationality: 'French' },
  { id: 'd-oco', name: 'Esteban Ocon', code: 'OCO', team: 'Haas', number: 31, nationality: 'French' },
  { id: 'd-tsu', name: 'Yuki Tsunoda', code: 'TSU', team: 'RB', number: 22, nationality: 'Japanese' },
  { id: 'd-bot', name: 'Valtteri Bottas', code: 'BOT', team: 'Sauber', number: 77, nationality: 'Finnish' },
  { id: 'd-hul', name: 'Nico Hülkenberg', code: 'HUL', team: 'Sauber', number: 27, nationality: 'German' },
  { id: 'd-mag', name: 'Kevin Magnussen', code: 'MAG', team: 'Haas', number: 20, nationality: 'Danish' },
  { id: 'd-alb', name: 'Alexander Albon', code: 'ALB', team: 'Williams', number: 23, nationality: 'Thai' },
  { id: 'd-dev', name: 'Nyck de Vries', code: 'DEV', team: 'RB', number: 21, nationality: 'Dutch' },
  { id: 'd-ric', name: 'Daniel Ricciardo', code: 'RIC', team: 'RB', number: 3, nationality: 'Australian' },
  { id: 'd-zhou', name: 'Zhou Guanyu', code: 'ZHO', team: 'Sauber', number: 24, nationality: 'Chinese' },
]

const TOP_N = 10

type BetType = 'FASTEST_LAP' | 'DNF_DSQ_DNS' | 'SC_DEPLOYED' | 'SC_COUNT'

const BET_LABELS: Record<BetType, string> = {
  FASTEST_LAP: 'Fastest Lap',
  DNF_DSQ_DNS: 'DNF / DSQ / DNS',
  SC_DEPLOYED: 'Safety Car Deployed',
  SC_COUNT: 'Safety Car Count',
}

const BET_PLACEHOLDERS: Record<BetType, string> = {
  FASTEST_LAP: 'Driver code, e.g. VER',
  DNF_DSQ_DNS: 'Driver code, e.g. HAM',
  SC_DEPLOYED: 'YES or NO',
  SC_COUNT: 'Number, e.g. 2',
}

interface BetEntry {
  type: BetType
  enabled: boolean
  value: string
  stake: string
}

// ---------------------------------------------------------------------------
// Countdown for header bar
// ---------------------------------------------------------------------------
function useSecondsLeft(targetIso: string): number {
  const [secs, setSecs] = useState(() =>
    Math.max(0, Math.floor((new Date(targetIso).getTime() - Date.now()) / 1000)),
  )
  useEffect(() => {
    const id = setInterval(
      () => setSecs(Math.max(0, Math.floor((new Date(targetIso).getTime() - Date.now()) / 1000))),
      1000,
    )
    return () => clearInterval(id)
  })
  return secs
}

// ---------------------------------------------------------------------------
// Driver chip
// ---------------------------------------------------------------------------
function DriverChip({ driver, compact }: { driver: Driver; compact?: boolean }) {
  return (
    <div className={cn('flex items-center gap-2', compact && 'text-sm')}>
      <span className="font-mono text-xs text-text-muted w-8">{driver.code}</span>
      <span className="font-medium text-text-primary truncate">{driver.name}</span>
      {!compact && (
        <span className="ml-auto text-xs text-text-muted hidden sm:block">{driver.team}</span>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Toast
// ---------------------------------------------------------------------------
function Toast({ message, type }: { message: string; type: 'success' | 'error' }) {
  return (
    <div
      className={cn(
        'fixed bottom-6 right-6 z-50 rounded-lg border px-5 py-3 text-sm font-medium shadow-lg',
        'animate-in fade-in-0 slide-in-from-bottom-4 duration-300',
        type === 'success'
          ? 'border-green-500/30 bg-green-500/10 text-green-400'
          : 'border-red-500/30 bg-red-500/10 text-red-400',
      )}
      role="status"
    >
      {message}
    </div>
  )
}

// ---------------------------------------------------------------------------
// PredictPage
// ---------------------------------------------------------------------------
export function PredictPage() {
  const { raceId } = useParams<{ raceId: string }>()

  const [drivers, setDrivers] = useState<Driver[]>([])
  const [race, setRace] = useState<Race | null>(null)
  const [existingPrediction, setExistingPrediction] = useState<Prediction | null>(null)
  const [isUpdateMode, setIsUpdateMode] = useState(false)
  const [isLocked, setIsLocked] = useState(false)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null)
  const [leagueId, setLeagueId] = useState<string>('')

  // Drag state: ranked slots (driverId | null), pool of remaining drivers
  const [ranked, setRanked] = useState<Array<string | null>>(Array(TOP_N).fill(null))
  const [pool, setPool] = useState<string[]>([])

  // Bonus bets
  const [bets, setBets] = useState<BetEntry[]>([
    { type: 'FASTEST_LAP', enabled: false, value: '', stake: '10' },
    { type: 'DNF_DSQ_DNS', enabled: false, value: '', stake: '10' },
    { type: 'SC_DEPLOYED', enabled: false, value: '', stake: '10' },
    { type: 'SC_COUNT', enabled: false, value: '', stake: '10' },
  ])

  // Fetch data
  useEffect(() => {
    if (!raceId) return
    let cancelled = false

    async function load() {
      if (!raceId) return
      try {
        const [driversData, calendarData] = await Promise.allSettled([
          f1Api.getDrivers(),
          f1Api.getCalendar(),
        ])

        const driverList =
          driversData.status === 'fulfilled' ? driversData.value : PLACEHOLDER_DRIVERS
        if (cancelled) return
        setDrivers(driverList)
        setPool(driverList.map((d) => d.id))

        if (calendarData.status === 'fulfilled') {
          const r = calendarData.value.races.find((x) => x.id === raceId) ?? null
          if (!cancelled) setRace(r)
        }

        // Try to fetch league for submission
        try {
          const leagueList = await leaguesApi.getLeagues()
          if (!cancelled && leagueList.length > 0) {
            setLeagueId(leagueList[0].id)
          }
        } catch {
          // no league — will fail gracefully on submit
        }

        // Try to load existing prediction
        try {
          const p = await predictionsApi.getPrediction(raceId)
          if (cancelled) return
          setExistingPrediction(p)
          if (p.status === 'locked') {
            setIsLocked(true)
          } else {
            setIsUpdateMode(true)
            // Pre-populate ranked
            const positions = p.topN.positions
            const newRanked: Array<string | null> = Array(TOP_N).fill(null)
            positions.forEach((id, i) => {
              if (i < TOP_N) newRanked[i] = id
            })
            setRanked(newRanked)
            const usedSet = new Set(positions)
            setPool(driverList.map((d) => d.id).filter((id) => !usedSet.has(id)))

            // Pre-populate bets
            if (p.bets.length > 0) {
              setBets((prev) =>
                prev.map((b) => {
                  const existing = p.bets.find((pb) => pb.type === b.type)
                  if (existing) {
                    return { ...b, enabled: true, value: existing.value, stake: String(existing.stake) }
                  }
                  return b
                }),
              )
            }
          }
        } catch {
          // 404 = no prediction yet — stay in submit mode
        }
      } catch {
        if (!cancelled) {
          setDrivers(PLACEHOLDER_DRIVERS)
          setPool(PLACEHOLDER_DRIVERS.map((d) => d.id))
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void load()
    return () => { cancelled = true }
  }, [raceId])

  function showToast(message: string, type: 'success' | 'error') {
    setToast({ message, type })
    setTimeout(() => setToast(null), 3500)
  }

  const driverMap = React.useMemo(() => {
    const m = new Map<string, Driver>()
    drivers.forEach((d) => m.set(d.id, d))
    return m
  }, [drivers])

  const onDragEnd = useCallback(
    (result: DropResult) => {
      const { source, destination } = result
      if (!destination) return

      const srcId = source.droppableId
      const dstId = destination.droppableId

      if (srcId === dstId && srcId === 'pool') {
        // reorder within pool
        const next = [...pool]
        const [moved] = next.splice(source.index, 1)
        next.splice(destination.index, 0, moved)
        setPool(next)
        return
      }

      if (srcId === 'pool' && dstId === 'ranked') {
        const driverId = pool[source.index]
        const nextPool = [...pool]
        nextPool.splice(source.index, 1)

        const nextRanked = [...ranked]
        // if target slot is occupied, push displaced driver back to pool
        const displaced = nextRanked[destination.index]
        nextRanked[destination.index] = driverId
        if (displaced !== null) nextPool.push(displaced)
        setPool(nextPool)
        setRanked(nextRanked)
        return
      }

      if (srcId === 'ranked' && dstId === 'pool') {
        const driverId = ranked[source.index]
        if (driverId === null) return
        const nextRanked = [...ranked]
        nextRanked[source.index] = null
        const nextPool = [...pool]
        nextPool.splice(destination.index, 0, driverId)
        setRanked(nextRanked)
        setPool(nextPool)
        return
      }

      if (srcId === 'ranked' && dstId === 'ranked') {
        // Swap the two slots
        const nextRanked = [...ranked]
        const a = nextRanked[source.index]
        nextRanked[source.index] = nextRanked[destination.index]
        nextRanked[destination.index] = a
        setRanked(nextRanked)
      }
    },
    [pool, ranked],
  )

  async function handleSubmit() {
    if (!raceId) return
    const positions = ranked.filter((id): id is string => id !== null)
    if (positions.length < TOP_N) {
      showToast(`Please rank all ${TOP_N} positions before submitting.`, 'error')
      return
    }

    setSubmitting(true)
    try {
      if (isUpdateMode && existingPrediction) {
        await predictionsApi.updatePrediction(raceId, { topN: { positions } })
      } else {
        await predictionsApi.submitPrediction(raceId, {
          leagueId,
          topN: { positions },
        })
      }

      // Submit enabled bets
      const enabledBets = bets.filter((b) => b.enabled && b.value.trim())
      await Promise.allSettled(
        enabledBets.map((b) =>
          predictionsApi.submitBet(raceId, {
            type: b.type,
            value: b.value.trim(),
            stake: Number(b.stake) || 10,
          }),
        ),
      )

      showToast(isUpdateMode ? 'Prediction updated!' : 'Prediction submitted!', 'success')
      setIsUpdateMode(true)
    } catch {
      showToast('Failed to submit prediction. Please try again.', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  // Deadline countdown
  const deadlineIso = race?.qualifyingDateTime ?? new Date(0).toISOString()
  const secsLeft = useSecondsLeft(deadlineIso)
  const deadlineExpired = secsLeft === 0
  const deadlineUrgent = secsLeft > 0 && secsLeft < 3600

  if (loading) {
    return (
      <div className="flex min-h-96 items-center justify-center">
        <Loader size="lg" label="Loading prediction form…" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-6">
      {/* Header bar with deadline countdown */}
      <div
        className={cn(
          'mb-6 flex flex-wrap items-center justify-between gap-3 rounded-lg border px-5 py-3',
          deadlineUrgent
            ? 'border-red-500/40 bg-red-500/10'
            : deadlineExpired
            ? 'border-amber-500/30 bg-amber-500/10'
            : 'border-border bg-surface',
        )}
      >
        <div>
          <h1 className="text-xl font-bold text-text-primary">
            {race?.name ?? `Race ${raceId ?? ''}`}
          </h1>
          {isUpdateMode && !isLocked && (
            <p className="text-sm text-blue-400 mt-0.5">Editing existing prediction</p>
          )}
          {isLocked && <Badge variant="locked" className="mt-1">Locked</Badge>}
        </div>
        <div
          className={cn(
            'text-sm font-medium tabular-nums',
            deadlineUrgent ? 'text-red-400' : 'text-text-secondary',
          )}
        >
          {deadlineExpired ? (
            'Predictions closed'
          ) : (
            <>
              Closes in{' '}
              {secsLeft >= 3600
                ? `${Math.floor(secsLeft / 3600)}h ${Math.floor((secsLeft % 3600) / 60)}m`
                : `${Math.floor(secsLeft / 60)}m ${secsLeft % 60}s`}
            </>
          )}
        </div>
      </div>

      {isLocked ? (
        // Read-only view
        <Card header={<span className="font-semibold text-text-primary">Locked Prediction</span>}>
          <div className="space-y-2">
            {existingPrediction?.topN.positions.map((driverId, i) => {
              const driver = driverMap.get(driverId)
              return (
                <div
                  key={driverId}
                  className="flex items-center gap-3 rounded-md border border-border px-3 py-2"
                >
                  <span className="w-6 text-sm font-bold text-text-muted text-right">
                    {i + 1}
                  </span>
                  {driver ? (
                    <DriverChip driver={driver} />
                  ) : (
                    <span className="text-text-muted text-sm">{driverId}</span>
                  )}
                </div>
              )
            })}
          </div>
        </Card>
      ) : (
        <div className="grid gap-6 lg:grid-cols-2">
          {/* Left panel: drag-to-rank */}
          <DragDropContext onDragEnd={onDragEnd}>
            <div className="space-y-4">
              <Card
                header={
                  <span className="font-semibold text-text-primary">
                    Top {TOP_N} Finishing Order
                  </span>
                }
                noPadding
              >
                <Droppable droppableId="ranked">
                  {(provided, snapshot) => (
                    <div
                      ref={provided.innerRef}
                      {...provided.droppableProps}
                      className={cn(
                        'min-h-[20rem] p-3 space-y-1.5 transition-colors',
                        snapshot.isDraggingOver && 'bg-f1-red/5',
                      )}
                    >
                      {ranked.map((driverId, idx) => {
                        const driver = driverId ? driverMap.get(driverId) : null
                        if (!driverId) {
                          return (
                            <div
                              key={`empty-${idx}`}
                              className="flex items-center gap-3 rounded-md border border-dashed border-border px-3 py-2 opacity-50"
                            >
                              <span className="w-6 text-right text-sm font-bold text-text-muted">
                                {idx + 1}
                              </span>
                              <span className="text-xs text-text-muted">Drag a driver here</span>
                            </div>
                          )
                        }
                        return (
                          <Draggable key={driverId} draggableId={driverId} index={idx}>
                            {(drag, dragSnapshot) => (
                              <div
                                ref={drag.innerRef}
                                {...drag.draggableProps}
                                {...drag.dragHandleProps}
                                className={cn(
                                  'flex items-center gap-3 rounded-md border px-3 py-2 cursor-grab active:cursor-grabbing transition-colors',
                                  dragSnapshot.isDragging
                                    ? 'border-f1-red/60 bg-f1-red/10 shadow-md'
                                    : 'border-border bg-surface hover:bg-surface-raised',
                                )}
                              >
                                <span className="w-6 text-right text-sm font-bold text-f1-red">
                                  {idx + 1}
                                </span>
                                {driver ? (
                                  <DriverChip driver={driver} />
                                ) : (
                                  <span className="text-text-muted text-sm font-mono">{driverId}</span>
                                )}
                                <svg
                                  className="ml-auto h-4 w-4 text-text-muted flex-shrink-0"
                                  fill="none"
                                  viewBox="0 0 24 24"
                                  stroke="currentColor"
                                  aria-hidden="true"
                                >
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8h16M4 16h16" />
                                </svg>
                              </div>
                            )}
                          </Draggable>
                        )
                      })}
                      {provided.placeholder}
                    </div>
                  )}
                </Droppable>
              </Card>

              {/* Driver pool */}
              <Card
                header={
                  <span className="font-semibold text-text-primary">
                    Available Drivers ({pool.length})
                  </span>
                }
                noPadding
              >
                <Droppable droppableId="pool">
                  {(provided, snapshot) => (
                    <div
                      ref={provided.innerRef}
                      {...provided.droppableProps}
                      className={cn(
                        'max-h-72 overflow-y-auto p-3 space-y-1 transition-colors',
                        snapshot.isDraggingOver && 'bg-blue-500/5',
                      )}
                    >
                      {pool.length === 0 ? (
                        <p className="py-2 text-center text-sm text-text-muted">
                          All drivers ranked
                        </p>
                      ) : (
                        pool.map((driverId, idx) => {
                          const driver = driverMap.get(driverId)
                          return (
                            <Draggable key={driverId} draggableId={driverId} index={idx}>
                              {(drag, dragSnapshot) => (
                                <div
                                  ref={drag.innerRef}
                                  {...drag.draggableProps}
                                  {...drag.dragHandleProps}
                                  className={cn(
                                    'flex items-center gap-3 rounded-md border px-3 py-2 cursor-grab active:cursor-grabbing transition-colors',
                                    dragSnapshot.isDragging
                                      ? 'border-blue-500/60 bg-blue-500/10 shadow-md'
                                      : 'border-border bg-surface hover:bg-surface-raised',
                                  )}
                                >
                                  {driver ? (
                                    <DriverChip driver={driver} />
                                  ) : (
                                    <span className="text-text-muted text-sm font-mono">{driverId}</span>
                                  )}
                                </div>
                              )}
                            </Draggable>
                          )
                        })
                      )}
                      {provided.placeholder}
                    </div>
                  )}
                </Droppable>
              </Card>
            </div>
          </DragDropContext>

          {/* Right panel: bonus bets */}
          <div className="space-y-4">
            <Card header={<span className="font-semibold text-text-primary">Bonus Bets</span>}>
              <div className="space-y-5">
                {bets.map((bet, i) => (
                  <div key={bet.type} className="space-y-2">
                    <label className="flex items-center gap-2 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={bet.enabled}
                        onChange={(e) => {
                          const next = [...bets]
                          next[i] = { ...next[i], enabled: e.target.checked }
                          setBets(next)
                        }}
                        className="h-4 w-4 rounded border-border accent-f1-red"
                      />
                      <span className="text-sm font-medium text-text-primary">
                        {BET_LABELS[bet.type]}
                      </span>
                    </label>

                    {bet.enabled && (
                      <div className="ml-6 grid grid-cols-2 gap-2">
                        <Input
                          label="Value"
                          value={bet.value}
                          onChange={(e) => {
                            const next = [...bets]
                            next[i] = { ...next[i], value: e.target.value }
                            setBets(next)
                          }}
                          placeholder={BET_PLACEHOLDERS[bet.type]}
                          size={undefined}
                        />
                        <Input
                          label="Stake (pts)"
                          type="number"
                          min={1}
                          value={bet.stake}
                          onChange={(e) => {
                            const next = [...bets]
                            next[i] = { ...next[i], stake: e.target.value }
                            setBets(next)
                          }}
                          placeholder="10"
                        />
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </Card>

            {/* Submit */}
            <Button
              variant="primary"
              size="lg"
              loading={submitting}
              disabled={deadlineExpired}
              onClick={() => void handleSubmit()}
              className="w-full"
            >
              {deadlineExpired
                ? 'Deadline passed'
                : isUpdateMode
                ? 'Update Prediction'
                : 'Submit Prediction'}
            </Button>
            {deadlineExpired && (
              <p className="text-center text-xs text-text-muted">
                The prediction deadline has passed.
              </p>
            )}
          </div>
        </div>
      )}

      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  )
}

export default PredictPage
