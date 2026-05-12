import apiClient from './client'
import type { LeagueStandings, UserBalance } from './types'

export async function getLeagueStandings(leagueId: string): Promise<LeagueStandings> {
  const { data } = await apiClient.get<LeagueStandings>(`/scoring/leagues/${leagueId}/standings`)
  return data
}

export async function getUserBalance(leagueId: string): Promise<UserBalance> {
  const { data } = await apiClient.get<UserBalance>(`/scoring/leagues/${leagueId}/balance`)
  return data
}

export interface ProjectedEntry {
  userId: string
  projectedRacePoints: number
  currentLeaguePoints: number
}

export async function getProjectedScores(
  raceId: string,
  leagueId: string,
  raceNumber: number,
): Promise<ProjectedEntry[]> {
  try {
    const { data } = await apiClient.get<ProjectedEntry[]>(
      `/scores/races/${raceId}/projected`,
      { params: { leagueId, raceNumber } },
    )
    return data
  } catch {
    return []
  }
}
