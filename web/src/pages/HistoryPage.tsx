import { useEffect, useState } from 'react'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { useAuthStore } from '@/store/authStore'
import * as f1dataApi from '@/api/f1data'
import * as predictionsApi from '@/api/predictions'
import * as scoringApi from '@/api/scoring'
import * as leaguesApi from '@/api/leagues'
import type { Race, RaceResult, Prediction, UserBalance } from '@/api/types'
import { cn } from '@/lib/utils'

// ---------------------------------------------------------------------------
// Placeholder data
// ---------------------------------------------------------------------------
const PLACEHOLDER_RACES: Race[] = [
  {
    id: 'race-1',
    name: 'Bahrain Grand Prix',
    circuitName: 'Bahrain International Circuit',
    country: 'Bahrain',
    city: 'Sakhir',
    raceDateTime: '2026-03-01T15:00:00Z',
    qualifyingDateTime: '2026-02-28T15:00:00Z',
    round: 1,
    season: 2026,
    status: 'finished',
  },
  {
    id: 'race-2',
    name: 'Saudi Arabian Grand Prix',
    circuitName: 'Jeddah Corniche Circuit',
    country: 'Saudi Arabia',
    city: 'Jeddah',
    raceDateTime: '2026-03-08T17:00:00Z',
    qualifyingDateTime: '2026-03-07T17:00:00Z',
    round: 2,
    season: 2026,
    status: 'finished',
  },
  {
    id: 'race-3',
    name: 'Australian Grand Prix',
    circuitName: 'Albert Park Circuit',
    country: 'Australia',
    city: 'Melbourne',
    raceDateTime: '2026-03-22T05:00:00Z',
    qualifyingDateTime: '2026-03-21T05:00:00Z',
    round: 3,
    season: 2026,
    status: 'finished',
  },
]

const PLACEHOLDER_RESULTS: RaceResult[] = [
  { raceId: 'race-1', position: 1, driverId: 'd-ver', driverName: 'Max Verstappen', driverCode: 'VER', team: 'Red Bull Racing', lapsCompleted: 57, timeOrStatus: '1:31:44.742', fastestLap: false },
  { raceId: 'race-1', position: 2, driverId: 'd-nor', driverName: 'Lando Norris', driverCode: 'NOR', team: 'McLaren', lapsCompleted: 57, timeOrStatus: '+2.513s', fastestLap: true },
  { raceId: 'race-1', position: 3, driverId: 'd-lec', driverName: 'Charles Leclerc', driverCode: 'LEC', team: 'Ferrari', lapsCompleted: 57, timeOrStatus: '+4.101s', fastestLap: false },
  { raceId: 'race-1', position: 4, driverId: 'd-ham', driverName: 'Lewis Hamilton', driverCode: 'HAM', team: 'Ferrari', lapsCompleted: 57, timeOrStatus: '+8.241s', fastestLap: false },
  { raceId: 'race-1', position: 5, driverId: 'd-pia', driverName: 'Oscar Piastri', driverCode: 'PIA', team: 'McLaren', lapsCompleted: 57, timeOrStatus: '+12.553s', fastestLap: false },
]

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
function formatRaceDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------
interface RaceDetail {
  race: Race
  prediction: Prediction | null
  results: RaceResult[]
  balance: UserBalance | null
}

// ---------------------------------------------------------------------------
// Skeleton
// ---------------------------------------------------------------------------
function SkeletonRow() {
  return (
    <div className="animate-pulse rounded-lg border border-border bg-surface p-4">
      <div className="flex items-center justify-between">
        <div className="space-y-2">
          <div className="h-4 w-48 rounded bg-surface-raised" />
          <div className="h-3 w-32 rounded bg-surface-raised" />
        </div>
        <div className="h-6 w-20 rounded-full bg-surface-raised" />
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Race accordion item
// ---------------------------------------------------------------------------
interface RaceAccordionProps {
  detail: RaceDetail
  open: boolean
  onToggle: () => void
}

function RaceAccordion({ detail, open, onToggle }: RaceAccordionProps) {
  const { race, prediction, results, balance } = detail

  return (
    <div className="rounded-lg border border-border bg-surface overflow-hidden">
      {/* Header (always visible) */}
      <button
        type="button"
        className="w-full flex items-center justify-between px-5 py-4 text-left hover:bg-surface-raised transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-f1-red"
        onClick={onToggle}
        aria-expanded={open}
      >
        <div className="flex items-center gap-4 min-w-0">
          <span className="shrink-0 w-7 h-7 rounded-full bg-f1-red/10 text-f1-red text-xs font-bold flex items-center justify-center">
            {race.round}
          </span>
          <div className="min-w-0">
            <p className="font-semibold text-text-primary truncate">{race.name}</p>
            <p className="text-sm text-text-muted">{formatRaceDate(race.raceDateTime)}</p>
          </div>
        </div>
        <div className="flex items-center gap-3 shrink-0 ml-4">
          {balance !== null && (
            <span className="text-sm font-semibold text-text-primary tabular-nums">
              +{balance.totalPoints} pts
            </span>
          )}
          <Badge variant="finished" size="sm" dot>Finished</Badge>
          <svg
            className={cn('h-4 w-4 text-text-muted transition-transform duration-200', open && 'rotate-180')}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
            aria-hidden="true"
          >
            <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
          </svg>
        </div>
      </button>

      {/* Expanded body */}
      {open && (
        <div className="border-t border-border px-5 py-4 grid grid-cols-1 gap-5 sm:grid-cols-2">
          {/* Your prediction */}
          <div>
            <h3 className="mb-3 text-xs font-semibold uppercase tracking-wider text-text-muted">
              Your Prediction
            </h3>
            {!prediction ? (
              <p className="text-sm text-text-secondary italic">No prediction submitted.</p>
            ) : (
              <ol className="space-y-1.5">
                {prediction.topN.positions.map((driverId, idx) => (
                  <li key={driverId} className="flex items-center gap-2 text-sm">
                    <span className="w-5 text-right text-xs font-bold text-text-muted tabular-nums">
                      {idx + 1}.
                    </span>
                    <span className="font-mono text-xs bg-surface-raised text-text-secondary px-1.5 py-0.5 rounded">
                      {driverId}
                    </span>
                  </li>
                ))}
              </ol>
            )}
          </div>

          {/* Actual result */}
          <div>
            <h3 className="mb-3 text-xs font-semibold uppercase tracking-wider text-text-muted">
              Actual Result
            </h3>
            {results.length === 0 ? (
              <p className="text-sm text-text-secondary italic">Results not yet available.</p>
            ) : (
              <ol className="space-y-1.5">
                {results.slice(0, 10).map((r) => (
                  <li key={r.driverId} className="flex items-center gap-2 text-sm">
                    <span className="w-5 text-right text-xs font-bold text-text-muted tabular-nums">
                      {r.position}.
                    </span>
                    <span className="font-mono text-xs bg-surface-raised text-text-secondary px-1.5 py-0.5 rounded">
                      {r.driverCode}
                    </span>
                    <span className="flex-1 text-text-primary truncate">{r.driverName}</span>
                    {r.fastestLap && (
                      <span className="text-xs text-purple-400" title="Fastest Lap">FL</span>
                    )}
                  </li>
                ))}
              </ol>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// HistoryPage
// ---------------------------------------------------------------------------
export function HistoryPage() {
  const { user } = useAuthStore()

  const [details, setDetails] = useState<RaceDetail[]>([])
  const [totalPoints, setTotalPoints] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [openRaceId, setOpenRaceId] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        // 1. Calendar — filter finished races
        const calendarResult = await f1dataApi.getCalendar().catch(() => ({
          season: 2026,
          races: PLACEHOLDER_RACES,
        }))

        const finishedRaces = calendarResult.races.filter((r) => r.status === 'finished')

        if (finishedRaces.length === 0) {
          if (!cancelled) {
            setDetails([])
            setLoading(false)
          }
          return
        }

        // 2. Fetch user's league (to get leagueId for balance calls)
        let leagueId: string | null = null
        try {
          const leagues = await leaguesApi.getLeagues()
          if (leagues.length > 0) leagueId = leagues[0].id
        } catch {
          // no leagues — balance will be null
        }

        // 3. For each finished race: fetch prediction + results + balance in parallel
        const raceDetails: RaceDetail[] = await Promise.all(
          finishedRaces.map(async (race) => {
            const [predResult, resultsResult, balanceResult] = await Promise.allSettled([
              predictionsApi.getPrediction(race.id),
              f1dataApi.getRaceResults(race.id),
              leagueId && user
                ? scoringApi.getBalance(user.id, leagueId)
                : Promise.reject(new Error('no league')),
            ])

            const prediction = predResult.status === 'fulfilled' ? predResult.value : null
            const results =
              resultsResult.status === 'fulfilled'
                ? resultsResult.value
                : race.id === 'race-1'
                ? PLACEHOLDER_RESULTS
                : []
            const balance =
              balanceResult.status === 'fulfilled' ? balanceResult.value : null

            return { race, prediction, results, balance }
          }),
        )

        if (cancelled) return

        // 4. Compute total points from balances (de-duped per league)
        const seenLeagues = new Set<string>()
        let pts = 0
        for (const d of raceDetails) {
          if (d.balance && !seenLeagues.has(d.balance.leagueId)) {
            pts += d.balance.totalPoints
            seenLeagues.add(d.balance.leagueId)
          }
        }

        setDetails(raceDetails)
        setTotalPoints(pts > 0 ? pts : null)
      } catch {
        if (!cancelled) {
          // Fall back to placeholder data
          setDetails(
            PLACEHOLDER_RACES.map((race) => ({
              race,
              prediction: null,
              results: race.id === 'race-1' ? PLACEHOLDER_RESULTS : [],
              balance: null,
            })),
          )
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void load()
    return () => { cancelled = true }
  }, [user])

  function toggleRace(raceId: string) {
    setOpenRaceId((prev) => (prev === raceId ? null : raceId))
  }

  return (
    <main className="mx-auto max-w-5xl px-4 py-8">
      {/* Page header */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-text-primary">Prediction History</h1>
        {totalPoints !== null && (
          <div className="rounded-lg border border-f1-red/20 bg-f1-red/5 px-4 py-2">
            <span className="text-sm text-text-muted">Season total</span>
            <span className="ml-2 text-lg font-bold text-f1-red tabular-nums">
              {totalPoints} pts
            </span>
          </div>
        )}
      </div>

      {/* Content */}
      {loading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((n) => (
            <SkeletonRow key={n} />
          ))}
        </div>
      ) : details.length === 0 ? (
        <Card>
          <div className="py-12 text-center">
            <p className="text-lg font-medium text-text-secondary">No completed races yet.</p>
            <p className="mt-1 text-sm text-text-muted">
              Race history will appear here once results are posted.
            </p>
          </div>
        </Card>
      ) : (
        <div className="space-y-3">
          {details.map(({ race, prediction, results, balance }) => (
            <RaceAccordion
              key={race.id}
              detail={{ race, prediction, results, balance }}
              open={openRaceId === race.id}
              onToggle={() => toggleRace(race.id)}
            />
          ))}
        </div>
      )}
    </main>
  )
}

export default HistoryPage
