import http from 'k6/http'
import { check, sleep } from 'k6'
import { Counter, Rate } from 'k6/metrics'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'

export const options = {
  stages: [
    { duration: '30s', target: 20 },  // ramp up
    { duration: '1m',  target: 20 },  // sustained
    { duration: '15s', target: 0  },  // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed:   ['rate<0.01'],
  },
}

const errors = new Counter('auth_errors')
const successRate = new Rate('auth_success_rate')

export default function () {
  const uniqueSuffix = `${__VU}-${__ITER}-${Date.now()}`
  const email = `perf-${uniqueSuffix}@test.invalid`

  // Register
  const registerRes = http.post(
    `${BASE_URL}/auth/register`,
    JSON.stringify({ email, username: `u${uniqueSuffix}`, password: 'Perf1234!' }),
    { headers: { 'Content-Type': 'application/json' } },
  )
  const registered = check(registerRes, {
    'register 201': (r) => r.status === 201,
    'has accessToken': (r) => !!r.json('accessToken'),
  })
  successRate.add(registered)
  if (!registered) {
    errors.add(1)
    sleep(1)
    return
  }

  const accessToken = registerRes.json('accessToken')
  const refreshToken = registerRes.json('refreshToken')

  // Login
  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email, password: 'Perf1234!' }),
    { headers: { 'Content-Type': 'application/json' } },
  )
  check(loginRes, { 'login 200': (r) => r.status === 200 })

  // Token refresh
  const refreshRes = http.post(
    `${BASE_URL}/auth/refresh`,
    JSON.stringify({ refreshToken }),
    { headers: { 'Content-Type': 'application/json' } },
  )
  check(refreshRes, { 'refresh 200': (r) => r.status === 200 })

  sleep(1)
}
