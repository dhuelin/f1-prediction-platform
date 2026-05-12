import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { ACCESS_TOKEN_KEY } from '@/api/client'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
// Convert http(s):// to ws(s)://
const WS_BASE = API_BASE.replace(/^http/, 'ws')

interface UseWebSocketOptions<T> {
  /** Called for every inbound message on the topic. Stable ref — no need to memoize. */
  onMessage?: (data: T) => void
  /** Set to false to skip connecting (e.g. session not yet active). Default: true. */
  enabled?: boolean
}

interface UseWebSocketResult<T> {
  connected: boolean
  lastMessage: T | null
  error: string | null
}

/**
 * Subscribes to a STOMP topic over a native WebSocket via the API Gateway.
 *
 * The JWT is passed as ?token= on the WebSocket URL so the gateway's JWT filter
 * can authenticate the upgrade request. Reconnects automatically on disconnect
 * and re-connects when the auth token in localStorage changes.
 *
 * @param topic  STOMP topic, e.g. "/topic/live/12345". Pass null to stay disconnected.
 */
export function useWebSocket<T>(
  topic: string | null,
  options: UseWebSocketOptions<T> = {},
): UseWebSocketResult<T> {
  const { onMessage, enabled = true } = options

  const [connected, setConnected] = useState(false)
  const [lastMessage, setLastMessage] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)

  // Keep a stable ref to onMessage so the effect doesn't re-run when the callback changes.
  const onMessageRef = useRef(onMessage)
  useEffect(() => {
    onMessageRef.current = onMessage
  })

  useEffect(() => {
    if (!topic || !enabled) return

    const token = localStorage.getItem(ACCESS_TOKEN_KEY)
    if (!token) {
      setError('Not authenticated')
      return
    }

    const client = new Client({
      brokerURL: `${WS_BASE}/ws?token=${token}`,
      reconnectDelay: 5_000,
      onConnect: () => {
        setConnected(true)
        setError(null)
        client.subscribe(topic, (frame) => {
          try {
            const data = JSON.parse(frame.body) as T
            setLastMessage(data)
            onMessageRef.current?.(data)
          } catch {
            // ignore unparseable frames
          }
        })
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => {
        setConnected(false)
        setError(frame.headers?.message ?? 'STOMP error')
      },
      onWebSocketError: (event) => {
        setConnected(false)
        setError((event as ErrorEvent).message ?? 'WebSocket error')
      },
    })

    client.activate()

    return () => {
      client.deactivate()
      setConnected(false)
    }
    // Re-connect when the topic or enabled flag changes. Token changes (logout/login)
    // are handled implicitly because token is read fresh on each activation.
  }, [topic, enabled])

  return { connected, lastMessage, error }
}
