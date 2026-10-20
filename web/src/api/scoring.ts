import apiClient from './client'
import type { ProjectedStanding } from './types'

/**
 * A user's total points balance in a league.
 *
 * The endpoint returns a bare integer, not an object — this was previously typed
 * as a six-field `UserBalance`, so every property read on the result was
 * undefined at runtime.
 */
export async function getBalance(userId: string, leagueId: string): Promise<number> {
  const { data } = await apiClient.get<number>(
    `/scores/balance/${userId}/leagues/${leagueId}`,
  )
  return data
}

/** Average total points across a league's members. Also a bare integer. */
export async function getAveragePoints(leagueId: string): Promise<number> {
  const { data } = await apiClient.get<number>(
    `/scores/leagues/${leagueId}/average-points`,
  )
  return data
}

export async function getProjectedScores(
  raceId: string,
  leagueId: string,
  raceNumber: number,
): Promise<ProjectedStanding[]> {
  const { data } = await apiClient.get<ProjectedStanding[]>(
    `/scores/races/${raceId}/projected`,
    { params: { leagueId, raceNumber } },
  )
  return data
}
