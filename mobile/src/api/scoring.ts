import apiClient from './client'
import type { ProjectedStanding, Standing } from './types'

// Scoring is served under /scores, not /scoring — the old paths matched no
// gateway route and returned the launch page with a 200.

export async function getLeagueStandings(leagueId: string): Promise<Standing[]> {
  const { data } = await apiClient.get<Standing[]>(`/scores/leagues/${leagueId}/standings`)
  return data
}

/**
 * A user's points balance in a league. The endpoint is keyed on both ids and
 * returns a bare integer, not an object.
 */
export async function getUserBalance(userId: string, leagueId: string): Promise<number> {
  const { data } = await apiClient.get<number>(`/scores/balance/${userId}/leagues/${leagueId}`)
  return data
}

export async function getProjectedScores(
  raceId: string,
  leagueId: string,
  raceNumber: number,
): Promise<ProjectedStanding[]> {
  try {
    const { data } = await apiClient.get<ProjectedStanding[]>(
      `/scores/races/${raceId}/projected`,
      { params: { leagueId, raceNumber } },
    )
    return data
  } catch {
    return []
  }
}
