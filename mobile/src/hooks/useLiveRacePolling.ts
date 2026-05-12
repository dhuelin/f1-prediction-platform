import { useEffect, useRef, useState, useCallback } from 'react'
import { AppState, type AppStateStatus } from 'react-native'
import {
  getLivePositions,
  getLiveRaceState,
  type LiveDriverPosition,
  type LiveRaceState,
} from '@/api/f1data'
import { getProjectedScores, type ProjectedEntry } from '@/api/scoring'

const POLL_INTERVAL_MS = 5_000

interface UseLiveRacePollingOptions {
  raceId: string
  leagueId?: string
  raceNumber?: number
  enabled?: boolean
}

interface LiveRaceData {
  positions: LiveDriverPosition[]
  raceState: LiveRaceState
  projected: ProjectedEntry[]
  lastUpdatedAt: Date | null
  isLoading: boolean
}

const DEFAULT_RACE_STATE: LiveRaceState = {
  scCount: 0,
  vscCount: 0,
  fastestLapDriverCode: null,
  fastestLapDuration: null,
  dnfDriverCodes: [],
}

/**
 * Polls the live positions, race state, and projected scores every 5 seconds.
 * Automatically pauses polling when the app goes to the background (via AppState),
 * and throttles state updates when the data hasn't changed.
 */
export function useLiveRacePolling({
  raceId,
  leagueId,
  raceNumber,
  enabled = true,
}: UseLiveRacePollingOptions): LiveRaceData {
  const [data, setData] = useState<LiveRaceData>({
    positions: [],
    raceState: DEFAULT_RACE_STATE,
    projected: [],
    lastUpdatedAt: null,
    isLoading: true,
  })

  // Track last serialized positions to skip no-op updates.
  const lastPositionsRef = useRef<string>('')
  const appStateRef = useRef<AppStateStatus>(AppState.currentState)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const poll = useCallback(async () => {
    if (!enabled || appStateRef.current !== 'active') return

    const [positions, raceState, projected] = await Promise.all([
      getLivePositions(),
      getLiveRaceState(),
      leagueId && raceNumber ? getProjectedScores(raceId, leagueId, raceNumber) : Promise.resolve([]),
    ])

    const positionsKey = JSON.stringify(positions)
    const changed = positionsKey !== lastPositionsRef.current
    lastPositionsRef.current = positionsKey

    setData((prev) => ({
      positions,
      // Only update raceState / projected when positions changed — avoids flicker.
      raceState: changed ? raceState : prev.raceState,
      projected: changed ? projected : prev.projected,
      lastUpdatedAt: changed ? new Date() : prev.lastUpdatedAt,
      isLoading: false,
    }))
  }, [enabled, raceId, leagueId, raceNumber])

  useEffect(() => {
    if (!enabled) return

    // Initial fetch immediately
    poll()

    timerRef.current = setInterval(poll, POLL_INTERVAL_MS)

    const subscription = AppState.addEventListener('change', (nextState) => {
      const wasBackground = appStateRef.current !== 'active'
      appStateRef.current = nextState
      // Resume polling immediately when coming back to foreground
      if (wasBackground && nextState === 'active') {
        poll()
      }
    })

    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
      subscription.remove()
    }
  }, [enabled, poll])

  return data
}
