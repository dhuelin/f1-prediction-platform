import apiClient from './client'
import type { Calendar, Driver, RaceResult } from './types'

export async function getCalendar(): Promise<Calendar> {
  const { data } = await apiClient.get<Calendar>('/f1data/calendar')
  return data
}

export async function getDrivers(): Promise<Driver[]> {
  const { data } = await apiClient.get<Driver[]>('/f1data/drivers')
  return data
}

export async function getRaceResults(raceId: string): Promise<RaceResult[]> {
  const { data } = await apiClient.get<RaceResult[]>(`/f1data/races/${raceId}/results`)
  return data
}

export interface RaceState {
  scCount: number
  vscCount: number
  fastestLapDriverCode: string | null
  fastestLapDuration: number | null
  dnfDriverCodes: string[]
}

export async function getLiveRaceState(): Promise<RaceState> {
  const { data } = await apiClient.get<RaceState>('/live/race-state')
  return data
}
