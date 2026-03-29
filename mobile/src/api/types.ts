// ============================================================
// Shared TypeScript interfaces for the F1 Prediction Platform
// ============================================================

// --- Auth ---

export interface User {
  id: string
  email: string
  displayName: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: User
}

export interface RefreshTokenResponse {
  accessToken: string
  refreshToken: string
}

// --- Drivers ---

export interface Driver {
  id: string
  /** Full name, e.g. "Max Verstappen" */
  name: string
  /** 3-letter code, e.g. "VER" */
  code: string
  /** Constructor name, e.g. "Red Bull Racing" */
  team: string
  /** Permanent race number */
  number: number
  nationality: string
}

// --- Races & Calendar ---

export type RaceStatus = 'upcoming' | 'live' | 'finished' | 'cancelled'

export interface Race {
  id: string
  name: string
  circuitName: string
  country: string
  city: string
  /** ISO-8601 date string of race start */
  raceDateTime: string
  /** ISO-8601 date string — predictions lock at qualifying start */
  qualifyingDateTime: string
  round: number
  season: number
  status: RaceStatus
}

export interface RaceResult {
  raceId: string
  position: number
  driverId: string
  driverName: string
  driverCode: string
  team: string
  lapsCompleted: number
  timeOrStatus: string
  fastestLap: boolean
}

export interface Calendar {
  season: number
  races: Race[]
}

// --- Predictions ---

export type PredictionStatus = 'draft' | 'submitted' | 'locked'

export interface TopNPrediction {
  positions: string[]  // driverId[], length = N (typically 10)
}

export interface BonusBet {
  id: string
  type: string   // e.g. "FASTEST_LAP", "SAFETY_CAR", "DNF"
  value: string  // e.g. driverId or "YES" / "NO"
  stake: number  // points wagered
}

export interface Prediction {
  id: string
  userId: string
  raceId: string
  leagueId: string
  topN: TopNPrediction
  bets: BonusBet[]
  status: PredictionStatus
  submittedAt: string | null
  lockedAt: string | null
}

export interface SubmitPredictionRequest {
  leagueId: string
  topN: TopNPrediction
}

export interface UpdatePredictionRequest {
  topN?: TopNPrediction
}

export interface SubmitBetRequest {
  type: string
  value: string
  stake: number
}

// --- Leagues ---

export type LeagueScoringMode = 'PROXIMITY' | 'EXACT'

export interface LeagueConfig {
  topNSize: number
  scoringMode: LeagueScoringMode
  bonusBetsEnabled: boolean
  stakingEnabled: boolean
  maxStakePerRace: number
  maxBetsPerRace: number
  catchUpPoints: number | null
}

export interface League {
  id: string
  name: string
  adminUserId: string
  inviteCode: string
  memberCount: number
  config: LeagueConfig
  createdAt: string
}

export interface CreateLeagueRequest {
  name: string
  config?: Partial<LeagueConfig>
}

export interface JoinLeagueRequest {
  inviteCode: string
}

// --- Standings & Scoring ---

export interface Standing {
  rank: number
  userId: string
  displayName: string
  totalPoints: number
  pointsBalance: number
  racesScored: number
}

export interface LeagueStandings {
  leagueId: string
  leagueName: string
  season: number
  standings: Standing[]
  updatedAt: string
}

export interface UserBalance {
  userId: string
  leagueId: string
  totalPoints: number
  pointsBalance: number
  stakingBalance: number
  racesScored: number
}

export interface AveragePoints {
  leagueId: string
  averagePoints: number
  raceCount: number
}

// --- Generic API wrappers ---

export interface ApiError {
  status: number
  code: string
  message: string
  timestamp: string
}

export interface PagedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}
