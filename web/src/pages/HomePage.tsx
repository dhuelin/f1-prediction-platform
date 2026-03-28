import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import * as f1Api from '@/api/f1data'
import * as predictionsApi from '@/api/predictions'
import * as leaguesApi from '@/api/leagues'
import * as scoringApi from '@/api/scoring'
import type { Race, Prediction, League, UserBalance } from '@/api/types'
import { cn } from '@/lib/utils'

// ---------------------------------------------------------------------------
// Placeholder data shown when the API is unavailable
// ---------------------------------------------------------------------------
const PLACEHOLDER_RACE: Race = {
  id: 'race-2026-01',
  name: 'Bahrain Grand Prix',
  circuitName: 'Bahrain International Circuit',
  country: 'Bahrain',
  city: 'Sakhir',
  raceDateTime: new Date(Date.now() + 4 * 24 * 60 * 60 * 1000).toISOString(),
  qualifyingDateTime: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString(),
  round: 1,
  season: 2026,
  status: 'upcoming',
}

const COUNTRY_FLAGS: Record<string, string> = {
  Bahrain: '🇧🇭',
  'Saudi Arabia': '🇸🇦',
  Australia: '🇦🇺',
  Japan: '🇯🇵',
  China: '🇨🇳',
  USA: '🇺🇸',
  'United States': '🇺🇸',
  Italy: '🇮🇹',
  Monaco: '🇲🇨',
  Canada: '🇨🇦',
  Spain: '🇪🇸',
  Austria: '🇦🇹',
  'United Kingdom': '🇬🇧',
  Hungary: '🇭🇺',
  Belgium: '🇧🇪',
  Netherlands: '🇳🇱',
  Singapore: '🇸🇬',
  Mexico: '🇲🇽',
  Brazil: '🇧🇷',
  'United Arab Emirates': '🇦🇪',
  Azerbaijan: '🇦🇿',
  France: '🇫🇷',
  Portugal: '🇵🇹',
  Qatar: '🇶🇦',
}

function flagFor(country: string): string {
  return COUNTRY_FLAGS[country] ?? '🏁'
}

// ---------------------------------------------------------------------------
// Countdown hook
// ---------------------------------------------------------------------------
interface Countdown {
  days: number
  hours: number
  minutes: number
  seconds: number
  expired: boolean
}

function useCountdown(targetIso: string): Countdown {
  function calc(): Countdown {
    const diff = new Date(targetIso).getTime() - Date.now()
    if (diff <= 0) return { days: 0, hours: 0, minutes: 0, seconds: 0, expired: true }
    const totalSeconds = Math.floor(diff / 1000)
    return {
      days: Math.floor(totalSeconds / 86400),
      hours: Math.floor((totalSeconds % 86400) / 3600),
      minutes: Math.floor((totalSeconds % 3600) / 60),
      seconds: totalSeconds % 60,
      expired: false,
    }
  }

  const [countdown, setCountdown] = useState<Countdown>(calc)

  useEffect(() => {
    const id = setInterval(() => setCountdown(calc()), 1000)
    return () => clearInterval(id)
  })

  return countdown
}

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------
function CountdownBlock({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex flex-col items-center">
      <span className="text-2xl font-bold text-text-primary tabular-nums">
        {String(value).padStart(2, '0')}
      </span>
      <span className="text-xs text-text-muted uppercase tracking-wide">{label}</span>
    </div>
  )
}

interface LeagueRowProps {
  league: League
  balance: UserBalance | null
}

function LeagueRow({ league, balance }: LeagueRowProps) {
  return (
    <Link
      to={`/leagues/${league.id}`}
      className="flex items-center justify-between rounded-md px-3 py-2 hover:bg-surface-raised transition-colors"
    >
      <span className="text-sm font-medium text-text-primary">{league.name}</span>
      <span className="text-sm text-text-secondary">
        {balance !== null ? (
          <span className="font-semibold text-f1-red">{balance.totalPoints} pts</span>
        ) : (
          <span className="text-text-muted">—</span>
        )}
      </span>
    </Link>
  )
}

function SkeletonBlock({ className }: { className?: string }) {
  return (
    <div className={cn('animate-pulse rounded-md bg-surface-raised', className)} />
  )
}

// ---------------------------------------------------------------------------
// HomePage
// ---------------------------------------------------------------------------
export function HomePage() {
  const { user } = useAuthStore()

  const [nextRace, setNextRace] = useState<Race | null>(null)
  const [prediction, setPrediction] = useState<Prediction | null>(null)
  const [predictionStatus, setPredictionStatus] = useState<'submitted' | 'not-submitted' | 'locked' | 'loading'>('loading')
  const [leagues, setLeagues] = useState<League[]>([])
  const [balances, setBalances] = useState<Map<string, UserBalance>>(new Map())
  const [loading, setLoading] = useState(true)
  const [error] = useState<string | null>(null)

  // Fetch next race
  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        const calendar = await f1Api.getCalendar()
        const upcoming = calendar.races
          .filter((r) => r.status === 'upcoming' || r.status === 'live')
          .sort((a, b) => new Date(a.raceDateTime).getTime() - new Date(b.raceDateTime).getTime())
        const race = upcoming[0] ?? null
        if (!cancelled) setNextRace(race ?? PLACEHOLDER_RACE)
      } catch {
        if (!cancelled) setNextRace(PLACEHOLDER_RACE)
      }
    }

    void load()
    return () => { cancelled = true }
  }, [])

  // Fetch prediction status
  useEffect(() => {
    if (!nextRace) return
    let cancelled = false

    async function load() {
      if (!nextRace) return
      try {
        const p = await predictionsApi.getPrediction(nextRace.id)
        if (!cancelled) {
          setPrediction(p)
          setPredictionStatus(p.status === 'locked' ? 'locked' : 'submitted')
        }
      } catch (err: unknown) {
        if (!cancelled) {
          // 404 = not submitted
          const status = (err as { response?: { status?: number } })?.response?.status
          setPredictionStatus(status === 404 ? 'not-submitted' : 'not-submitted')
          setPrediction(null)
        }
      }
    }

    void load()
    return () => { cancelled = true }
  }, [nextRace])

  // Fetch leagues + balances
  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        const leagueList = await leaguesApi.getLeagues()
        if (cancelled) return
        setLeagues(leagueList)

        if (user) {
          const balanceEntries = await Promise.allSettled(
            leagueList.map((l) => scoringApi.getBalance(user.id, l.id)),
          )
          if (cancelled) return
          const map = new Map<string, UserBalance>()
          balanceEntries.forEach((result, i) => {
            if (result.status === 'fulfilled') {
              map.set(leagueList[i].id, result.value)
            }
          })
          setBalances(map)
        }
      } catch {
        // leagues fail silently — show empty state
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void load()
    return () => { cancelled = true }
  }, [user])

  // Treat initial load as complete once race is set
  useEffect(() => {
    if (nextRace !== null) setLoading(false)
  }, [nextRace])

  const countdown = useCountdown(nextRace?.qualifyingDateTime ?? new Date(0).toISOString())

  if (error) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-8">
        <p className="text-red-400">{error}</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      {/* Welcome header */}
      <header className="mb-8">
        <h1 className="text-2xl font-bold text-text-primary">
          Welcome back, {user?.displayName ?? 'Driver'}
        </h1>
        <p className="mt-1 text-text-secondary">2026 Formula 1 Season</p>
      </header>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Next race card — spans 2 cols on large screens */}
        <div className="lg:col-span-2 space-y-4">
          {/* Race info */}
          <Card
            header={
              <div className="flex items-center justify-between">
                <span className="font-semibold text-text-primary">Next Race</span>
                <Badge variant="upcoming" dot>Upcoming</Badge>
              </div>
            }
          >
            {loading || !nextRace ? (
              <div className="space-y-2">
                <SkeletonBlock className="h-6 w-48" />
                <SkeletonBlock className="h-4 w-64" />
              </div>
            ) : (
              <>
                <div className="flex items-center gap-2">
                  <span className="text-2xl" aria-label={nextRace.country}>
                    {flagFor(nextRace.country)}
                  </span>
                  <p className="text-xl font-bold text-text-primary">{nextRace.name}</p>
                </div>
                <p className="mt-1 text-sm text-text-secondary">
                  {nextRace.circuitName} · {nextRace.city}
                </p>
                <p className="mt-1 text-xs text-text-muted">
                  Round {nextRace.round} · {new Date(nextRace.raceDateTime).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })}
                </p>
              </>
            )}
          </Card>

          {/* Countdown card */}
          <Card
            header={
              <span className="font-semibold text-text-primary">
                Qualifying in (predictions lock)
              </span>
            }
          >
            {loading || !nextRace ? (
              <div className="flex gap-6">
                {[...Array(4)].map((_, i) => (
                  <SkeletonBlock key={i} className="h-10 w-12" />
                ))}
              </div>
            ) : countdown.expired ? (
              <p className="text-sm font-medium text-amber-400">Predictions are locked.</p>
            ) : (
              <div className="flex items-center gap-4">
                <CountdownBlock label="days" value={countdown.days} />
                <span className="text-text-muted text-xl font-bold">:</span>
                <CountdownBlock label="hrs" value={countdown.hours} />
                <span className="text-text-muted text-xl font-bold">:</span>
                <CountdownBlock label="min" value={countdown.minutes} />
                <span className="text-text-muted text-xl font-bold">:</span>
                <CountdownBlock label="sec" value={countdown.seconds} />
              </div>
            )}
          </Card>

          {/* Prediction status + CTA */}
          <Card
            header={<span className="font-semibold text-text-primary">Your Prediction</span>}
          >
            {predictionStatus === 'loading' ? (
              <SkeletonBlock className="h-8 w-40" />
            ) : predictionStatus === 'submitted' ? (
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-green-400">Submitted ✓</p>
                  {prediction?.submittedAt && (
                    <p className="text-xs text-text-muted mt-0.5">
                      {new Date(prediction.submittedAt).toLocaleString()}
                    </p>
                  )}
                </div>
                {nextRace && (
                  <Link to={`/predict/${nextRace.id}`}>
                    <Button variant="secondary" size="sm">Edit</Button>
                  </Link>
                )}
              </div>
            ) : predictionStatus === 'locked' ? (
              <div className="flex items-center gap-3">
                <Badge variant="locked">Locked</Badge>
                <span className="text-sm text-text-secondary">Predictions are closed.</span>
              </div>
            ) : (
              <div className="flex items-center justify-between">
                <p className="text-text-secondary">No prediction submitted yet.</p>
                {nextRace && !countdown.expired && (
                  <Link to={`/predict/${nextRace.id}`}>
                    <Button variant="primary" size="sm">Submit Prediction</Button>
                  </Link>
                )}
              </div>
            )}
          </Card>
        </div>

        {/* League snapshots */}
        <div>
          <Card
            header={
              <div className="flex items-center justify-between">
                <span className="font-semibold text-text-primary">My Leagues</span>
                <Link to="/leagues" className="text-xs text-f1-red hover:underline">
                  View all
                </Link>
              </div>
            }
          >
            {loading ? (
              <div className="space-y-2">
                {[...Array(3)].map((_, i) => (
                  <SkeletonBlock key={i} className="h-8 w-full" />
                ))}
              </div>
            ) : leagues.length === 0 ? (
              <div className="text-center py-4">
                <p className="text-sm text-text-secondary mb-3">Join a league to start scoring.</p>
                <Link to="/leagues">
                  <Button variant="secondary" size="sm">Browse Leagues</Button>
                </Link>
              </div>
            ) : (
              <div className="flex flex-col gap-1">
                {leagues.slice(0, 5).map((league) => (
                  <LeagueRow
                    key={league.id}
                    league={league}
                    balance={balances.get(league.id) ?? null}
                  />
                ))}
              </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  )
}

export default HomePage
