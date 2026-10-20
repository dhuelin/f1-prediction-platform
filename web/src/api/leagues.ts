import apiClient from './client'
import type {
  League,
  CreateLeagueRequest,
  LeagueMember,
  LeagueScoringConfig,
  Standing,
  UpdateScoringConfigRequest,
} from './types'

/** Public leagues available to browse. */
export async function getLeagues(): Promise<League[]> {
  const { data } = await apiClient.get<League[]>('/leagues')
  return data
}

/** Leagues the signed-in user belongs to, public or private. */
export async function getMyLeagues(): Promise<League[]> {
  const { data } = await apiClient.get<League[]>('/leagues/mine')
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

export async function joinLeague(id: string, inviteCode: string | null): Promise<League> {
  const { data } = await apiClient.post<League>(`/leagues/${id}/join`, { inviteCode })
  return data
}

/** Member list with usernames. Private-league rosters are members-only. */
export async function getMembers(leagueId: string): Promise<LeagueMember[]> {
  const { data } = await apiClient.get<LeagueMember[]>(`/leagues/${leagueId}/members`)
  return data
}

/** Standings come back as a flat array ordered by rank. */
export async function getStandings(leagueId: string): Promise<Standing[]> {
  const { data } = await apiClient.get<Standing[]>(
    `/scores/leagues/${leagueId}/standings`,
  )
  return data
}

export async function getConfig(leagueId: string): Promise<LeagueScoringConfig> {
  const { data } = await apiClient.get<LeagueScoringConfig>(`/leagues/${leagueId}/config`)
  return data
}

/**
 * Update a league's scoring rules.
 *
 * PUT, not POST, and `nextRace` is required: a config change takes effect from a
 * race number rather than immediately, so mid-season edits cannot rescore races
 * already run. The previous version POSTed without it, which the API rejected.
 */
export async function updateConfig(
  leagueId: string,
  nextRace: number,
  payload: UpdateScoringConfigRequest,
): Promise<LeagueScoringConfig> {
  const { data } = await apiClient.put<LeagueScoringConfig>(
    `/leagues/${leagueId}/config`,
    payload,
    { params: { nextRace } },
  )
  return data
}

export async function removeMember(leagueId: string, userId: string): Promise<void> {
  await apiClient.delete(`/leagues/${leagueId}/members/${userId}`)
}
