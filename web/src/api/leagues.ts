import apiClient from './client'
import type {
  League,
  CreateLeagueRequest,
  LeagueStandings,
  LeagueConfig,
} from './types'

export async function getLeagues(): Promise<League[]> {
  const { data } = await apiClient.get<League[]>('/leagues')
  return data
}

export async function getLeague(id: string): Promise<League> {
  const { data } = await apiClient.get<League>(`/leagues/${id}`)
  return data
}

export async function createLeague(payload: CreateLeagueRequest): Promise<League> {
  const { data } = await apiClient.post<League>('/leagues', payload)
  return data
}

export async function joinLeague(id: string, inviteCode: string): Promise<League> {
  const { data } = await apiClient.post<League>(`/leagues/${id}/join`, { inviteCode })
  return data
}

export async function getStandings(leagueId: string): Promise<LeagueStandings> {
  const { data } = await apiClient.get<LeagueStandings>(
    `/scores/leagues/${leagueId}/standings`,
  )
  return data
}

export async function updateConfig(
  leagueId: string,
  payload: Partial<LeagueConfig>,
): Promise<LeagueConfig> {
  const { data } = await apiClient.post<LeagueConfig>(
    `/leagues/${leagueId}/config`,
    payload,
  )
  return data
}

export async function removeMember(leagueId: string, userId: string): Promise<void> {
  await apiClient.delete(`/leagues/${leagueId}/members/${userId}`)
}
