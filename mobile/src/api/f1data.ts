import apiClient from './client'
import type { Race, Driver, Calendar, RaceResult } from './types'

// f1-data-service is served under /f1 (its context-path) and the gateway routes
// /f1/** to it. These calls previously used /f1data/** and /live/**, which no
// gateway route matches — so they fell through to the catch-all that serves the
// launch page and came back as HTTP 200 full of marketing HTML, not an error.

export async function getNextRace(): Promise<Race | null> {
  try {
    const { data } = await apiClient.get<Race | null>('/f1/races/next')
    return data
  } catch {
    return null
  }
}

export async function getCurrentSeasonCalendar(): Promise<Calendar> {
  const { data } = await apiClient.get<Calendar>('/f1/calendar/current')
  return data
}

export async function getDrivers(): Promise<Driver[]> {
  const { data } = await apiClient.get<Driver[]>('/f1/drivers')
  return data
}

export async function getRaceResults(raceId: string): Promise<RaceResult[]> {
  const { data } = await apiClient.get<RaceResult[]>(`/f1/races/${raceId}/results`)
  return data
}

export interface LiveDriverPosition {
  driverNumber: number
  driverCode: string
  position: number
}

export interface LiveRaceState {
  scCount: number
  vscCount: number
  fastestLapDriverCode: string | null
  fastestLapDuration: number | null
  dnfDriverCodes: string[]
}

export async function getLivePositions(): Promise<LiveDriverPosition[]> {
  try {
    const { data } = await apiClient.get<{ positions: LiveDriverPosition[] }>('/f1/live/positions')
    return data.positions ?? []
  } catch {
    return []
  }
}

export async function getLiveRaceState(): Promise<LiveRaceState> {
  try {
    const { data } = await apiClient.get<LiveRaceState>('/f1/live/race-state')
    return data
  } catch {
    return { scCount: 0, vscCount: 0, fastestLapDriverCode: null, fastestLapDuration: null, dnfDriverCodes: [] }
  }
}
