import apiClient from './client'
import type { CreateLeagueRequest, League, LeagueMember, LeagueScoringConfig, Standing } from './types'

export async function getMyLeagues(): Promise<League[]> {
  const { data } = await apiClient.get<League[]>('/leagues/mine')
  return data
}

/** Public leagues available to browse. */
export async function getPublicLeagues(): Promise<League[]> {
  const { data } = await apiClient.get<League[]>('/leagues')
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

/** Join using only an invite code — the server resolves which league owns it. */
export async function joinLeague(inviteCode: string): Promise<League> {
  const { data } = await apiClient.post<League>('/leagues/join', { inviteCode })
  return data
}

/** Join a league already known by id. Private leagues still require the code. */
export async function joinLeagueById(leagueId: string, inviteCode: string | null): Promise<League> {
  const { data } = await apiClient.post<League>(`/leagues/${leagueId}/join`, { inviteCode })
  return data
}

export async function getMembers(leagueId: string): Promise<LeagueMember[]> {
  const { data } = await apiClient.get<LeagueMember[]>(`/leagues/${leagueId}/members`)
  return data
}

export async function getConfig(leagueId: string): Promise<LeagueScoringConfig> {
  const { data } = await apiClient.get<LeagueScoringConfig>(`/leagues/${leagueId}/config`)
  return data
}

/** Standings live on the scoring service and come back as a flat array. */
export async function getStandings(leagueId: string): Promise<Standing[]> {
  const { data } = await apiClient.get<Standing[]>(`/scores/leagues/${leagueId}/standings`)
  return data
}

export async function removeMember(leagueId: string, userId: string): Promise<void> {
  await apiClient.delete(`/leagues/${leagueId}/members/${userId}`)
}
