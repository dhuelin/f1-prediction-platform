import apiClient from './client'
import type { Race, Driver, Calendar, RaceResult } from './types'

export async function getNextRace(): Promise<Race | null> {
  try {
    const { data } = await apiClient.get<Race | null>('/f1data/races/next')
    return data
  } catch {
    return null
  }
}

export async function getCurrentSeasonCalendar(): Promise<Calendar> {
  const { data } = await apiClient.get<Calendar>('/f1data/calendar/current')
  return data
}

export async function getDrivers(): Promise<Driver[]> {
  const { data } = await apiClient.get<Driver[]>('/f1data/drivers/current')
  return data
}

export async function getRaceResults(raceId: string): Promise<RaceResult[]> {
  const { data } = await apiClient.get<RaceResult[]>(`/f1data/races/${raceId}/results`)
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
    const { data } = await apiClient.get<{ positions: LiveDriverPosition[] }>('/live/positions')
    return data.positions ?? []
  } catch {
    return []
  }
}

export async function getLiveRaceState(): Promise<LiveRaceState> {
  try {
    const { data } = await apiClient.get<LiveRaceState>('/live/race-state')
    return data
  } catch {
    return { scCount: 0, vscCount: 0, fastestLapDriverCode: null, fastestLapDuration: null, dnfDriverCodes: [] }
  }
}
