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
