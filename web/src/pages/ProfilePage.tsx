import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Avatar } from '@/components/ui/Avatar'
import { useAuthStore } from '@/store/authStore'
import { useThemeStore, type ThemeMode } from '@/store/themeStore'
import * as leaguesApi from '@/api/leagues'
import * as scoringApi from '@/api/scoring'
import * as predictionsApi from '@/api/predictions'
import * as f1dataApi from '@/api/f1data'
import apiClient from '@/api/client'
import { cn } from '@/lib/utils'

// ---------------------------------------------------------------------------
// Notification preferences — stored in localStorage
// ---------------------------------------------------------------------------
const NOTIF_KEY = 'f1_notification_prefs'

interface NotifPrefs {
  raceReminder: boolean
  results: boolean
  amendments: boolean
}

function loadNotifPrefs(): NotifPrefs {
  try {
    const raw = localStorage.getItem(NOTIF_KEY)
    if (raw) return JSON.parse(raw) as NotifPrefs
  } catch {
    // ignore
  }
  return { raceReminder: true, results: true, amendments: true }
}

function saveNotifPrefs(prefs: NotifPrefs) {
  localStorage.setItem(NOTIF_KEY, JSON.stringify(prefs))
}

// ---------------------------------------------------------------------------
// Stats
// ---------------------------------------------------------------------------
interface Stats {
  totalPoints: number
  leagueCount: number
  predictionsCount: number
}

async function fetchStats(userId: string): Promise<Stats> {
  const leagues = await leaguesApi.getLeagues()
  let totalPoints = 0

  // Sum balance across all leagues
  const balanceResults = await Promise.allSettled(
    leagues.map((l) => scoringApi.getBalance(userId, l.id)),
  )
  for (const r of balanceResults) {
    if (r.status === 'fulfilled') totalPoints += r.value.totalPoints
  }

  // Count predictions submitted across finished races
  let predictionsCount = 0
  try {
    const calendar = await f1dataApi.getCalendar()
    const finishedRaces = calendar.races.filter((r) => r.status === 'finished')
    const predResults = await Promise.allSettled(
      finishedRaces.map((r) => predictionsApi.getPrediction(r.id)),
    )
    for (const r of predResults) {
      if (r.status === 'fulfilled' && r.value.status !== 'draft') predictionsCount++
    }
  } catch {
    // ignore — best-effort
  }

  return {
    totalPoints,
    leagueCount: leagues.length,
    predictionsCount,
  }
}

// ---------------------------------------------------------------------------
// Toast
// ---------------------------------------------------------------------------
interface ToastState {
  message: string
  variant: 'success' | 'error'
}

function Toast({ toast, onDismiss }: { toast: ToastState; onDismiss: () => void }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={cn(
        'fixed bottom-4 right-4 z-50 flex items-center gap-3 rounded-lg border px-4 py-3 shadow-lg text-sm font-medium transition-all',
        toast.variant === 'success'
          ? 'border-green-500/20 bg-green-500/10 text-green-400'
          : 'border-red-500/20 bg-red-500/10 text-red-400',
      )}
    >
      {toast.message}
      <button
        type="button"
        onClick={onDismiss}
        className="ml-2 opacity-60 hover:opacity-100"
        aria-label="Dismiss"
      >
        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
          <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Section header
// ---------------------------------------------------------------------------
function SectionHeader({ title }: { title: string }) {
  return (
    <h2 className="mb-3 text-xs font-semibold uppercase tracking-wider text-text-muted">
      {title}
    </h2>
  )
}

// ---------------------------------------------------------------------------
// Toggle switch
// ---------------------------------------------------------------------------
interface ToggleProps {
  checked: boolean
  onChange: (v: boolean) => void
  label: string
  description?: string
}

function Toggle({ checked, onChange, label, description }: ToggleProps) {
  return (
    <label className="flex cursor-pointer items-start justify-between gap-4 py-2">
      <div>
        <p className="text-sm font-medium text-text-primary">{label}</p>
        {description && (
          <p className="text-xs text-text-muted mt-0.5">{description}</p>
        )}
      </div>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        onClick={() => onChange(!checked)}
        className={cn(
          'relative mt-0.5 h-5 w-9 shrink-0 rounded-full transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-f1-red',
          checked ? 'bg-f1-red' : 'bg-surface-raised',
        )}
      >
        <span
          className={cn(
            'absolute top-0.5 left-0.5 h-4 w-4 rounded-full bg-white shadow transition-transform duration-200',
            checked ? 'translate-x-4' : 'translate-x-0',
          )}
        />
      </button>
    </label>
  )
}

// ---------------------------------------------------------------------------
// Theme selector
// ---------------------------------------------------------------------------
const THEME_OPTIONS: { value: ThemeMode; label: string }[] = [
  { value: 'light', label: 'Light' },
  { value: 'dark', label: 'Dark' },
  { value: 'system', label: 'System' },
]

function ThemeSelector() {
  const { theme, setTheme } = useThemeStore()

  return (
    <div className="flex gap-2 flex-wrap">
      {THEME_OPTIONS.map((opt) => (
        <button
          key={opt.value}
          type="button"
          onClick={() => setTheme(opt.value)}
          className={cn(
            'rounded-md border px-4 py-2 text-sm font-medium transition-colors',
            theme === opt.value
              ? 'border-f1-red bg-f1-red/10 text-f1-red'
              : 'border-border bg-surface text-text-secondary hover:text-text-primary hover:bg-surface-raised',
          )}
        >
          {opt.label}
        </button>
      ))}
    </div>
  )
}

// ---------------------------------------------------------------------------
// ProfilePage
// ---------------------------------------------------------------------------
export function ProfilePage() {
  const { user, setUser, logout } = useAuthStore()
  const navigate = useNavigate()

  // User info edit state
  const [editing, setEditing] = useState(false)
  const [editName, setEditName] = useState(user?.displayName ?? '')
  const [saving, setSaving] = useState(false)
  const editInputRef = useRef<HTMLInputElement>(null)

  // Stats
  const [stats, setStats] = useState<Stats | null>(null)
  const [statsLoading, setStatsLoading] = useState(true)

  // Notification prefs
  const [notifPrefs, setNotifPrefs] = useState<NotifPrefs>(loadNotifPrefs)

  // Toast
  const [toast, setToast] = useState<ToastState | null>(null)
  const toastTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  function showToast(message: string, variant: 'success' | 'error') {
    setToast({ message, variant })
    if (toastTimerRef.current) clearTimeout(toastTimerRef.current)
    toastTimerRef.current = setTimeout(() => setToast(null), 4000)
  }

  // Load stats
  useEffect(() => {
    if (!user) return
    let cancelled = false

    fetchStats(user.id)
      .then((s) => { if (!cancelled) setStats(s) })
      .catch(() => { if (!cancelled) setStats({ totalPoints: 0, leagueCount: 0, predictionsCount: 0 }) })
      .finally(() => { if (!cancelled) setStatsLoading(false) })

    return () => { cancelled = true }
  }, [user])

  // Focus edit input when entering edit mode
  useEffect(() => {
    if (editing) {
      editInputRef.current?.focus()
    }
  }, [editing])

  function updateNotif(key: keyof NotifPrefs, value: boolean) {
    const updated = { ...notifPrefs, [key]: value }
    setNotifPrefs(updated)
    saveNotifPrefs(updated)
  }

  async function handleSaveName() {
    if (!user || !editName.trim()) return
    setSaving(true)
    try {
      await apiClient.patch('/auth/profile', { displayName: editName.trim() })
      setUser({ ...user, displayName: editName.trim() })
      setEditing(false)
      showToast('Display name updated.', 'success')
    } catch {
      showToast('Failed to update display name. Please try again.', 'error')
    } finally {
      setSaving(false)
    }
  }

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <main className="mx-auto max-w-2xl px-4 py-8 space-y-6">
      <h1 className="text-2xl font-bold text-text-primary">Profile &amp; Settings</h1>

      {/* ---------------------------------------------------------------- */}
      {/* User info                                                         */}
      {/* ---------------------------------------------------------------- */}
      <section aria-label="User information">
        <SectionHeader title="Account" />
        <Card>
          <div className="flex items-center gap-4">
            <Avatar name={editing ? editName : user?.displayName} size="lg" />
            <div className="flex-1 min-w-0">
              {editing ? (
                <input
                  ref={editInputRef}
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') void handleSaveName()
                    if (e.key === 'Escape') { setEditing(false); setEditName(user?.displayName ?? '') }
                  }}
                  aria-label="Display name"
                  className={cn(
                    'w-full rounded-md border border-border bg-surface px-3 py-2 text-base text-text-primary',
                    'placeholder:text-text-muted transition-colors duration-150',
                    'focus:outline-none focus:ring-2 focus:ring-f1-red focus:border-f1-red',
                  )}
                />
              ) : (
                <p className="text-lg font-semibold text-text-primary truncate">
                  {user?.displayName}
                </p>
              )}
              <p className="text-sm text-text-muted mt-0.5 truncate">{user?.email}</p>
            </div>
            <div className="flex gap-2 shrink-0">
              {editing ? (
                <>
                  <Button size="sm" variant="primary" loading={saving} onClick={() => void handleSaveName()}>
                    Save
                  </Button>
                  <Button size="sm" variant="secondary" disabled={saving} onClick={() => { setEditing(false); setEditName(user?.displayName ?? '') }}>
                    Cancel
                  </Button>
                </>
              ) : (
                <Button size="sm" variant="secondary" onClick={() => setEditing(true)}>
                  Edit
                </Button>
              )}
            </div>
          </div>
        </Card>
      </section>

      {/* ---------------------------------------------------------------- */}
      {/* Stats                                                             */}
      {/* ---------------------------------------------------------------- */}
      <section aria-label="Statistics">
        <SectionHeader title="Stats" />
        <Card>
          {statsLoading ? (
            <div className="grid grid-cols-3 gap-4 animate-pulse">
              {[1, 2, 3].map((n) => (
                <div key={n} className="space-y-2">
                  <div className="h-8 w-16 rounded bg-surface-raised" />
                  <div className="h-3 w-24 rounded bg-surface-raised" />
                </div>
              ))}
            </div>
          ) : (
            <div className="grid grid-cols-3 gap-4">
              {[
                { value: stats?.totalPoints ?? 0, label: 'Total Points' },
                { value: stats?.leagueCount ?? 0, label: 'Leagues' },
                { value: stats?.predictionsCount ?? 0, label: 'Predictions' },
              ].map(({ value, label }) => (
                <div key={label} className="text-center">
                  <p className="text-2xl font-bold text-text-primary tabular-nums">{value}</p>
                  <p className="text-xs text-text-muted mt-0.5">{label}</p>
                </div>
              ))}
            </div>
          )}
        </Card>
      </section>

      {/* ---------------------------------------------------------------- */}
      {/* Notification preferences                                         */}
      {/* ---------------------------------------------------------------- */}
      <section aria-label="Notification preferences">
        <SectionHeader title="Notifications" />
        <Card>
          <div className="divide-y divide-border">
            <Toggle
              checked={notifPrefs.raceReminder}
              onChange={(v) => updateNotif('raceReminder', v)}
              label="Race Reminders"
              description="Get notified before qualifying so you can submit your prediction."
            />
            <Toggle
              checked={notifPrefs.results}
              onChange={(v) => updateNotif('results', v)}
              label="Race Results"
              description="Be notified when race results are posted and your score is updated."
            />
            <Toggle
              checked={notifPrefs.amendments}
              onChange={(v) => updateNotif('amendments', v)}
              label="Result Amendments"
              description="Alerts when stewards amend results and your points are recalculated."
            />
          </div>
        </Card>
      </section>

      {/* ---------------------------------------------------------------- */}
      {/* Appearance                                                        */}
      {/* ---------------------------------------------------------------- */}
      <section aria-label="Appearance settings">
        <SectionHeader title="Appearance" />
        <Card>
          <p className="mb-3 text-sm text-text-secondary">Choose your preferred colour scheme.</p>
          <ThemeSelector />
        </Card>
      </section>

      {/* ---------------------------------------------------------------- */}
      {/* Danger zone                                                       */}
      {/* ---------------------------------------------------------------- */}
      <section aria-label="Danger zone">
        <SectionHeader title="Danger Zone" />
        <Card>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-text-primary">Sign out</p>
              <p className="text-xs text-text-muted mt-0.5">
                You will be redirected to the login page.
              </p>
            </div>
            <Button variant="danger" size="sm" onClick={() => void handleLogout()}>
              Sign out
            </Button>
          </div>
        </Card>
      </section>

      {/* Toast */}
      {toast && (
        <Toast toast={toast} onDismiss={() => setToast(null)} />
      )}
    </main>
  )
}

export default ProfilePage
