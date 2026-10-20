import apiClient from './client'
import type {
  BonusBet,
  Prediction,
  SessionType,
  SubmitBetRequest,
  SubmitPredictionRequest,
} from './types'

/** Returns null when the user has no prediction for this race yet (404). */
export async function getPrediction(
  raceId: string,
  sessionType: SessionType = 'RACE',
): Promise<Prediction | null> {
  try {
    const { data } = await apiClient.get<Prediction>(`/predictions/${raceId}`, {
      params: { sessionType },
    })
    return data
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } }).response?.status
    if (status === 404) return null
    throw e
  }
}

export async function submitPrediction(
  raceId: string,
  payload: SubmitPredictionRequest,
): Promise<Prediction> {
  const { data } = await apiClient.post<Prediction>(`/predictions/${raceId}`, payload)
  return data
}

export async function updatePrediction(
  raceId: string,
  payload: SubmitPredictionRequest,
): Promise<Prediction> {
  const { data } = await apiClient.put<Prediction>(`/predictions/${raceId}`, payload)
  return data
}

/** Bonus bets are a separate resource — GET /predictions/{raceId} omits them. */
export async function getBets(
  raceId: string,
  sessionType: SessionType = 'RACE',
): Promise<BonusBet[]> {
  const { data } = await apiClient.get<BonusBet[]>(`/predictions/${raceId}/bets`, {
    params: { sessionType },
  })
  return data
}

export async function submitBet(
  raceId: string,
  payload: SubmitBetRequest,
  leagueId?: string,
  sessionType: SessionType = 'RACE',
): Promise<BonusBet> {
  const { data } = await apiClient.post<BonusBet>(
    `/predictions/${raceId}/bets`,
    payload,
    { params: { sessionType, ...(leagueId ? { leagueId } : {}) } },
  )
  return data
}
