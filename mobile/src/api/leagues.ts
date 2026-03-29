import apiClient from './client'

export async function getMyLeagues(): Promise<any[]> {
  const { data } = await apiClient.get('/leagues/me')
  return data
}

export async function getLeague(leagueId: string): Promise<any> {
  const { data } = await apiClient.get(`/leagues/${leagueId}`)
  return data
}

export async function createLeague(req: { name: string }): Promise<any> {
  const { data } = await apiClient.post('/leagues', req)
  return data
}

export async function joinLeague(inviteCode: string): Promise<any> {
  const { data } = await apiClient.post('/leagues/join', { inviteCode })
  return data
}

export async function getStandings(leagueId: string): Promise<any> {
  const { data } = await apiClient.get(`/leagues/${leagueId}/standings`)
  return data
}
