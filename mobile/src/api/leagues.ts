import apiClient from './client'
import type { League, CreateLeagueRequest, LeagueStandings } from './types'

export async function getMyLeagues(): Promise<League[]> {
  const { data } = await apiClient.get<League[]>('/leagues/me')
  return data
}

export async function getLeague(leagueId: string): Promise<League> {
  const { data } = await apiClient.get<League>(`/leagues/${leagueId}`)
  return data
}

export async function createLeague(req: CreateLeagueRequest): Promise<League> {
  const { data } = await apiClient.post<League>('/leagues', req)
  return data
}

export async function joinLeague(inviteCode: string): Promise<League> {
  const { data } = await apiClient.post<League>('/leagues/join', { inviteCode })
  return data
}

export async function getStandings(leagueId: string): Promise<LeagueStandings> {
  const { data } = await apiClient.get<LeagueStandings>(`/leagues/${leagueId}/standings`)
  return data
}
