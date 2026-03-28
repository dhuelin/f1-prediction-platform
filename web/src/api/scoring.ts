import apiClient from './client'
import type { UserBalance, AveragePoints } from './types'

export async function getBalance(userId: string, leagueId: string): Promise<UserBalance> {
  const { data } = await apiClient.get<UserBalance>(
    `/scores/balance/${userId}/leagues/${leagueId}`,
  )
  return data
}

export async function getAveragePoints(leagueId: string): Promise<AveragePoints> {
  const { data } = await apiClient.get<AveragePoints>(
    `/scores/leagues/${leagueId}/average-points`,
  )
  return data
}
