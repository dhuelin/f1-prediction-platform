import apiClient from './client'
import type {
  Prediction,
  SubmitPredictionRequest,
  UpdatePredictionRequest,
  SubmitBetRequest,
  BonusBet,
} from './types'

export async function getPrediction(raceId: string): Promise<Prediction> {
  const { data } = await apiClient.get<Prediction>(`/predictions/${raceId}`)
  return data
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
  payload: UpdatePredictionRequest,
): Promise<Prediction> {
  const { data } = await apiClient.put<Prediction>(`/predictions/${raceId}`, payload)
  return data
}

export async function submitBet(
  raceId: string,
  payload: SubmitBetRequest,
): Promise<BonusBet> {
  const { data } = await apiClient.post<BonusBet>(
    `/predictions/${raceId}/bets`,
    payload,
  )
  return data
}
