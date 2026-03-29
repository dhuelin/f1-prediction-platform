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
