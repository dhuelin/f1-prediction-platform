import apiClient from './client'
import type { BonusBet, Prediction, SessionType, SubmitBetRequest } from './types'

export async function getPrediction(raceId: string, sessionType: SessionType = 'RACE'): Promise<Prediction | null> {
  try {
    const { data } = await apiClient.get<Prediction>(`/predictions/${raceId}`, { params: { sessionType } })
    return data
  } catch (e: any) {
    if (e.response?.status === 404) return null
    throw e
  }
}

export async function submitPrediction(raceId: string, rankedDriverCodes: string[], sessionType: SessionType = 'RACE'): Promise<Prediction> {
  const { data } = await apiClient.post<Prediction>(`/predictions/${raceId}`, { rankedDriverCodes, sessionType })
  return data
}

export async function updatePrediction(raceId: string, rankedDriverCodes: string[], sessionType: SessionType = 'RACE'): Promise<Prediction> {
  const { data } = await apiClient.put<Prediction>(`/predictions/${raceId}`, { rankedDriverCodes, sessionType })
  return data
}

export async function submitBet(raceId: string, bet: SubmitBetRequest, leagueId: string, sessionType: SessionType = 'RACE'): Promise<BonusBet> {
  const { data } = await apiClient.post<BonusBet>(`/predictions/${raceId}/bets`, bet, {
    params: { sessionType, leagueId },
  })
  return data
}

/** Existing bonus bets for a race — GET /predictions/{raceId} does not include them. */
export async function getBets(raceId: string, sessionType: SessionType = 'RACE'): Promise<BonusBet[]> {
  const { data } = await apiClient.get<BonusBet[]>(`/predictions/${raceId}/bets`, {
    params: { sessionType },
  })
  return data
}
