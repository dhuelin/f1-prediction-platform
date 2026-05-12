import http from 'k6/http'
import { check, sleep } from 'k6'
import { Rate, Trend } from 'k6/metrics'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || 'replace-with-valid-jwt'
const RACE_ID = __ENV.RACE_ID || 'perf-race-001'
const LEAGUE_ID = __ENV.LEAGUE_ID || 'perf-league-001'
const RACE_NUMBER = __ENV.RACE_NUMBER || '1'

export const options = {
  // Simulate 100 concurrent users polling at the mobile app's 5-second cadence.
  scenarios: {
    live_polling: {
      executor: 'constant-vus',
      vus: 100,
      duration: '2m',
    },
  },
  thresholds: {
    http_req_duration:      ['p(95)<500'],
    http_req_failed:        ['rate<0.01'],
    live_positions_latency: ['p(95)<300'],
  },
}

const successRate = new Rate('live_success_rate')
const positionsLatency = new Trend('live_positions_latency')

function authHeaders() {
  return {
    Authorization: `Bearer ${ACCESS_TOKEN}`,
  }
}

export default function () {
  // GET /live/positions
  const posRes = http.get(`${BASE_URL}/live/positions`, { headers: authHeaders() })
  const posOk = check(posRes, { 'positions 200': (r) => r.status === 200 })
  successRate.add(posOk)
  positionsLatency.add(posRes.timings.duration)

  // GET /live/race-state
  const stateRes = http.get(`${BASE_URL}/live/race-state`, { headers: authHeaders() })
  check(stateRes, { 'race-state 200': (r) => r.status === 200 })

  // GET /scores/races/{raceId}/projected
  const projRes = http.get(
    `${BASE_URL}/scores/races/${RACE_ID}/projected?leagueId=${LEAGUE_ID}&raceNumber=${RACE_NUMBER}`,
    { headers: authHeaders() },
  )
  check(projRes, { 'projected 200 or 404': (r) => r.status === 200 || r.status === 404 })

  sleep(5) // mimic the mobile 5-second polling interval
}
