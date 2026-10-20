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
  expiresIn: number
  /** Populated client-side by fetching /auth/me after token issuance */
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
  /** Same value as driverCode — the API keys drivers by their 3-letter code. */
  driverId: string
  driverCode: string
  /** Finish time, gap, or a status like "DNF". */
  timeOrStatus: string
  fastestLap: boolean
}

export interface Calendar {
  season: number
  races: Race[]
}

// --- Predictions ---
// Mirrors PredictionResponse / BonusBetResponse. The previous shapes described a
// different design — a nested `topN.positions`, a `status` enum, a `leagueId` and
// an inline `bets` array — none of which the API returns.

export type SessionType = 'RACE' | 'SPRINT'

export type BetType = 'FASTEST_LAP' | 'DNF_DSQ_DNS' | 'SC_DEPLOYED' | 'SC_COUNT'

/** GET|POST|PUT /predictions/{raceId} */
export interface Prediction {
  id: string
  userId: string
  raceId: string
  sessionType: SessionType
  /** True once the qualifying deadline has passed; the prediction is read-only. */
  locked: boolean
  rankedDriverCodes: string[]
}

/** POST|PUT /predictions/{raceId} */
export interface SubmitPredictionRequest {
  rankedDriverCodes: string[]
  /** Defaults to RACE server-side. */
  sessionType?: SessionType
}

/** GET /predictions/{raceId}/bets, and the POST response. Bets are a separate call. */
export interface BonusBet {
  id: string
  betType: BetType
  stake: number
  /** Driver code, "true"/"false", or a count — depends on betType. */
  betValue: string
}

/** POST /predictions/{raceId}/bets */
export interface SubmitBetRequest {
  betType: BetType
  stake: number
  betValue: string
}

// --- Leagues ---
// These mirror the API exactly. They previously described a different design
// entirely — League carried a nested `config` and `createdAt`, neither of which
// LeagueResponse returns, and LeagueConfig shared no field at all with
// ScoringConfigResponse. TypeScript could not catch that: the types were
// self-consistent, just not consistent with the server.

/** GET /leagues, GET /leagues/mine, GET /leagues/{id}, POST /leagues, POST /leagues/{id}/join */
export interface League {
  id: string
  name: string
  visibility: LeagueVisibility
  /** Only returned when the caller is the league admin; null otherwise. */
  inviteCode: string | null
  adminUserId: string
  memberCount: number
}

export type LeagueVisibility = 'PUBLIC' | 'PRIVATE'

/** POST /leagues */
export interface CreateLeagueRequest {
  name: string
  visibility?: LeagueVisibility
}

/** POST /leagues/{id}/join */
export interface JoinLeagueRequest {
  inviteCode: string | null
}

/** GET /leagues/{id}/members */
export interface LeagueMember {
  userId: string
  username: string
  isAdmin: boolean
  joinedAt: string
}

/** GET|PUT /leagues/{id}/config — the league's scoring rules. */
export interface LeagueScoringConfig {
  id: string
  effectiveFromRace: number
  predictionDepth: number
  exactPositionPoints: number
  /** JSON string, e.g. '[{"offset":1,"points":7}]' */
  offsetTiers: string
  inRangePoints: number
  betMultiplier: number
  /** JSON string, e.g. '{"fastestLap":true,...}' */
  activeBets: string
  sprintScoringEnabled: boolean
  maxStakePerBet: number | null
}

/** PUT /leagues/{id}/config — every field optional except the three the API requires. */
export interface UpdateScoringConfigRequest {
  predictionDepth: number
  exactPositionPoints: number
  inRangePoints: number
  offsetTiers?: string
  betMultiplier?: number
  activeBets?: string
  sprintScoringEnabled?: boolean
  maxStakePerBet?: number | null
}

// --- Standings & Scoring ---

/** One row of GET /scores/leagues/{id}/standings. */
export interface Standing {
  userId: string
  totalPoints: number
  rank: number
}

/** GET /scores/races/{raceId}/projected */
export interface ProjectedStanding {
  userId: string
  projectedRacePoints: number
  currentLeaguePoints: number
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
