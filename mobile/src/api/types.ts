// ============================================================
// Shared TypeScript interfaces for the F1 Prediction Platform
// ============================================================

// --- Auth ---

/** GET /auth/me — mirrors UserProfileResponse. */
export interface User {
  id: string
  email: string
  username: string
  emailVerified: boolean
  createdAt: string
}

/** Alias used by the auth client for clarity at call sites. */
export type UserProfile = User

/**
 * POST /auth/login, /auth/register and the OAuth callbacks.
 *
 * The API returns tokens only — there is no nested `user`. Callers fetch the
 * profile with getMe() after storing the token, which is what the token is for.
 */
export interface AuthResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
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
// These mirror the API. They previously described a different design: League
// carried a nested `config` and `createdAt` that LeagueResponse does not return,
// and LeagueConfig shared no field with ScoringConfigResponse.

export type LeagueVisibility = 'PUBLIC' | 'PRIVATE'

export interface League {
  id: string
  name: string
  visibility: LeagueVisibility
  /** Only returned to the league admin; null otherwise. */
  inviteCode: string | null
  adminUserId: string
  memberCount: number
}

export interface CreateLeagueRequest {
  name: string
  visibility?: LeagueVisibility
}

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

/** GET|PUT /leagues/{id}/config */
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

// --- Standings & Scoring ---

/**
 * One row of GET /scores/leagues/{id}/standings.
 *
 * The endpoint returns a flat array of these — not an object with a `standings`
 * field — and carries no username: those come from the members endpoint.
 */
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
