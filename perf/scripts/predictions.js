import http from 'k6/http'
import { check, sleep } from 'k6'
import { Rate } from 'k6/metrics'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
// Provide a pre-seeded JWT via env var for prediction tests, or fall back to a placeholder.
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || 'replace-with-valid-jwt'
const RACE_ID = __ENV.RACE_ID || 'perf-race-001'

const TOP10 = ['VER', 'NOR', 'LEC', 'PIA', 'SAI', 'RUS', 'HAM', 'ALO', 'GAS', 'HUL']

export const options = {
  stages: [
    { duration: '20s', target: 10 },
    { duration: '1m',  target: 50 },
    { duration: '20s', target: 0  },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed:   ['rate<0.01'],
  },
}

const successRate = new Rate('prediction_success_rate')

function authHeaders() {
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${ACCESS_TOKEN}`,
  }
}

export default function () {
  // POST prediction
  const postRes = http.post(
    `${BASE_URL}/predictions/${RACE_ID}`,
    JSON.stringify({ rankedDriverCodes: TOP10 }),
    { headers: authHeaders() },
  )
  const ok = check(postRes, {
    'submit 201 or 409': (r) => r.status === 201 || r.status === 409,
  })
  successRate.add(ok)

  // GET predictions for race
  const getRes = http.get(
    `${BASE_URL}/predictions/${RACE_ID}`,
    { headers: authHeaders() },
  )
  check(getRes, { 'get 200': (r) => r.status === 200 })

  sleep(0.5)
}
