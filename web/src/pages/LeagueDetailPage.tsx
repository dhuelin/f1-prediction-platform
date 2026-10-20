import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { Avatar } from '@/components/ui/Avatar'
import { Modal } from '@/components/ui/Modal'
import { Loader } from '@/components/ui/Loader'
import { useAuthStore } from '@/store/authStore'
import * as leaguesApi from '@/api/leagues'
import type { League, LeagueMember, LeagueScoringConfig, Standing } from '@/api/types'
import { cn } from '@/lib/utils'

// ---------------------------------------------------------------------------
// Placeholder data
// ---------------------------------------------------------------------------
const PLACEHOLDER_LEAGUE: League = {
  id: 'league-1',
  name: 'Paddock Pundits',
  visibility: 'PRIVATE',
  adminUserId: 'user-admin',
  inviteCode: 'PUNDITS42',
  memberCount: 5,
}

// Standings are a flat array ordered by rank; usernames are not part of this
// payload and are joined in from the members endpoint.
const PLACEHOLDER_STANDINGS: Standing[] = [
  { rank: 1, userId: 'user-1', totalPoints: 148 },
  { rank: 2, userId: 'user-2', totalPoints: 132 },
  { rank: 3, userId: 'user-3', totalPoints: 119 },
  { rank: 4, userId: 'user-4', totalPoints: 101 },
  { rank: 5, userId: 'user-admin', totalPoints: 88 },
]

const PLACEHOLDER_MEMBERS: LeagueMember[] = [
  { userId: 'user-1', username: 'Max V', isAdmin: false, joinedAt: '2026-01-02T00:00:00Z' },
  { userId: 'user-2', username: 'Lewis H', isAdmin: false, joinedAt: '2026-01-03T00:00:00Z' },
  { userId: 'user-3', username: 'Charles L', isAdmin: false, joinedAt: '2026-01-04T00:00:00Z' },
  { userId: 'user-4', username: 'Lando N', isAdmin: false, joinedAt: '2026-01-05T00:00:00Z' },
  { userId: 'user-admin', username: 'You', isAdmin: true, joinedAt: '2026-01-01T00:00:00Z' },
]

// ---------------------------------------------------------------------------
// Tabs
// ---------------------------------------------------------------------------
function Tabs({
  tabs,
  active,
  onChange,
}: {
  tabs: string[]
  active: number
  onChange: (i: number) => void
}) {
  return (
    <div className="flex gap-1 rounded-lg border border-border bg-surface p-1 w-fit">
      {tabs.map((tab, i) => (
        <button
          key={tab}
          type="button"
          onClick={() => onChange(i)}
          className={cn(
            'rounded-md px-4 py-1.5 text-sm font-medium transition-colors',
            active === i
              ? 'bg-f1-red text-white'
              : 'text-text-secondary hover:text-text-primary hover:bg-surface-raised',
          )}
        >
          {tab}
        </button>
      ))}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Standings row
// ---------------------------------------------------------------------------
function StandingRow({
  standing,
  username,
  isCurrentUser,
}: {
  standing: Standing
  /** Joined in from GET /leagues/{id}/members — standings carry only user ids. */
  username: string
  isCurrentUser: boolean
}) {
  const rankBadge =
    standing.rank === 1
      ? 'text-yellow-400'
      : standing.rank === 2
      ? 'text-gray-300'
      : standing.rank === 3
      ? 'text-amber-600'
      : 'text-text-muted'

  return (
    <div
      className={cn(
        'flex items-center gap-3 rounded-md px-3 py-2.5 transition-colors',
        isCurrentUser ? 'bg-f1-red/10 border border-f1-red/20' : 'hover:bg-surface-raised',
      )}
    >
      <span className={cn('w-6 text-right text-sm font-bold', rankBadge)}>
        {standing.rank}
      </span>
      <Avatar name={username} size="sm" />
      <span className={cn('flex-1 text-sm font-medium', isCurrentUser ? 'text-f1-red' : 'text-text-primary')}>
        {username}
        {isCurrentUser && (
          <span className="ml-1.5 text-xs text-text-muted">(you)</span>
        )}
      </span>
      <span className="text-sm font-semibold text-text-primary tabular-nums">
        {standing.totalPoints}
      </span>
      <span className="w-14 text-right text-xs text-text-muted">pts</span>
    </div>
  )
}

// ---------------------------------------------------------------------------
// LeagueDetailPage
// ---------------------------------------------------------------------------
export function LeagueDetailPage() {
  const { leagueId } = useParams<{ leagueId: string }>()
  const navigate = useNavigate()
  const { user } = useAuthStore()

  const [league, setLeague] = useState<League | null>(null)
  const [standings, setStandings] = useState<Standing[] | null>(null)
  const [members, setMembers] = useState<LeagueMember[] | null>(null)
  const [config, setConfig] = useState<LeagueScoringConfig | null>(null)
  const [loading, setLoading] = useState(true)
  const [error] = useState<string | null>(null)

  // Leave / remove member
  const [confirmLeaveOpen, setConfirmLeaveOpen] = useState(false)
  const [confirmRemoveUserId, setConfirmRemoveUserId] = useState<string | null>(null)
  const [leaving, setLeaving] = useState(false)
  const [removingId, setRemovingId] = useState<string | null>(null)

  const [activeTab, setActiveTab] = useState(0)

  // Standings carry user ids only; names come from the members endpoint.
  const usernameById = new Map((members ?? []).map((m) => [m.userId, m.username]))
  const usernameFor = (userId: string) =>
    usernameById.get(userId) ?? `User ${userId.slice(0, 8)}`

  const isAdmin = user?.id === league?.adminUserId

  const availableTabs = ['Standings', ...(isAdmin ? ['Settings', 'Members'] : [])]

  useEffect(() => {
    if (!leagueId) return
    let cancelled = false

    async function load() {
      if (!leagueId) return
      try {
        // Members supply the usernames that standings omit; config is a separate
        // endpoint rather than a field on the league.
        const [leagueData, standingsData, membersData, configData] = await Promise.allSettled([
          leaguesApi.getLeague(leagueId),
          leaguesApi.getStandings(leagueId),
          leaguesApi.getMembers(leagueId),
          leaguesApi.getConfig(leagueId),
        ])

        if (cancelled) return

        setLeague(leagueData.status === 'fulfilled' ? leagueData.value : PLACEHOLDER_LEAGUE)
        setStandings(standingsData.status === 'fulfilled' ? standingsData.value : PLACEHOLDER_STANDINGS)
        setMembers(membersData.status === 'fulfilled' ? membersData.value : PLACEHOLDER_MEMBERS)
        // No fallback for config: showing invented scoring rules would be worse
        // than showing none.
        setConfig(configData.status === 'fulfilled' ? configData.value : null)
      } catch {
        if (!cancelled) {
          setLeague(PLACEHOLDER_LEAGUE)
          setStandings(PLACEHOLDER_STANDINGS)
          setMembers(PLACEHOLDER_MEMBERS)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void load()
    return () => { cancelled = true }
  }, [leagueId])

  async function handleLeave() {
    if (!leagueId || !user) return
    setLeaving(true)
    try {
      await leaguesApi.removeMember(leagueId, user.id)
      navigate('/leagues', { replace: true })
    } catch {
      // TODO: surface error
    } finally {
      setLeaving(false)
      setConfirmLeaveOpen(false)
    }
  }

  async function handleRemoveMember(userId: string) {
    if (!leagueId) return
    setRemovingId(userId)
    try {
      await leaguesApi.removeMember(leagueId, userId)
      setStandings((prev) => (prev ? prev.filter((s) => s.userId !== userId) : prev))
      setMembers((prev) => (prev ? prev.filter((m) => m.userId !== userId) : prev))
      setLeague((prev) => prev ? { ...prev, memberCount: prev.memberCount - 1 } : prev)
    } catch {
      // TODO: surface error
    } finally {
      setRemovingId(null)
      setConfirmRemoveUserId(null)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-96 items-center justify-center">
        <Loader size="lg" label="Loading league…" />
      </div>
    )
  }

  if (error || !league) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-8">
        <p className="text-red-400">{error ?? 'League not found.'}</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      {/* Header */}
      <div className="mb-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <div className="flex flex-wrap items-center gap-2 mb-1">
              <h1 className="text-2xl font-bold text-text-primary">{league.name}</h1>
              <Badge variant="default">{league.visibility}</Badge>
            </div>
            <p className="text-sm text-text-muted">
              {league.memberCount} member{league.memberCount !== 1 ? 's' : ''}
              {config && ` · Top ${config.predictionDepth}`}
            </p>
            {isAdmin && (
              <p className="mt-1 text-xs text-text-muted">
                Invite code:{' '}
                <span className="font-mono text-text-primary">{league.inviteCode}</span>
              </p>
            )}
          </div>

          {!isAdmin && (
            <Button
              variant="danger"
              size="sm"
              onClick={() => setConfirmLeaveOpen(true)}
            >
              Leave League
            </Button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="mb-5">
        <Tabs tabs={availableTabs} active={activeTab} onChange={setActiveTab} />
      </div>

      {/* Tab: Standings */}
      {activeTab === 0 && (
        <Card
          header={
            <div className="flex items-center justify-between">
              <span className="font-semibold text-text-primary">Leaderboard</span>
              {standings && standings.length > 0 && (
                <span className="text-xs text-text-muted">
                  {standings.length} ranked
                </span>
              )}
            </div>
          }
          noPadding
        >
          <div className="p-3 space-y-1">
            {!standings || standings.length === 0 ? (
              <p className="py-6 text-center text-sm text-text-secondary">
                No standings yet. Race results pending.
              </p>
            ) : (
              standings.map((standing) => (
                <StandingRow
                  key={standing.userId}
                  standing={standing}
                  username={usernameFor(standing.userId)}
                  isCurrentUser={standing.userId === user?.id}
                />
              ))
            )}
          </div>
        </Card>
      )}

      {/* Tab: Settings (admin only) */}
      {activeTab === 1 && isAdmin && (
        <Card header={<span className="font-semibold text-text-primary">Scoring Config</span>}>
          {!config ? (
            <p className="py-6 text-center text-sm text-text-secondary">
              Scoring config unavailable.
            </p>
          ) : (
            <dl className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {[
                { label: 'Prediction Depth', value: config.predictionDepth },
                { label: 'Exact Position', value: `${config.exactPositionPoints} pts` },
                { label: 'Within Range', value: `${config.inRangePoints} pts` },
                { label: 'Bet Multiplier', value: `${config.betMultiplier}x` },
                { label: 'Sprint Scoring', value: config.sprintScoringEnabled ? 'Enabled' : 'Disabled' },
                {
                  label: 'Max Stake / Bet',
                  value: config.maxStakePerBet !== null ? `${config.maxStakePerBet} pts` : 'Unlimited',
                },
                { label: 'Effective From Race', value: config.effectiveFromRace },
              ].map(({ label, value }) => (
                <div key={label} className="rounded-md border border-border px-4 py-3">
                  <dt className="text-xs text-text-muted mb-1">{label}</dt>
                  <dd className="text-sm font-semibold text-text-primary">{String(value)}</dd>
                </div>
              ))}
            </dl>
          )}
          <p className="mt-4 text-xs text-text-muted">
            Config changes are not yet supported from the web UI. Contact the platform admin.
          </p>
        </Card>
      )}

      {/* Tab: Members (admin only) */}
      {activeTab === (isAdmin ? 2 : -1) && isAdmin && (
        <Card header={<span className="font-semibold text-text-primary">Members</span>} noPadding>
          <div className="p-3 space-y-1">
            {!members || members.length === 0 ? (
              <p className="py-6 text-center text-sm text-text-secondary">No members.</p>
            ) : (
              members.map((m) => (
                <div
                  key={m.userId}
                  className="flex items-center gap-3 rounded-md px-3 py-2.5 hover:bg-surface-raised transition-colors"
                >
                  <Avatar name={m.username} size="sm" />
                  <span className="flex-1 text-sm font-medium text-text-primary">
                    {m.username}
                    {m.isAdmin && (
                      <span className="ml-2 text-xs text-text-muted">(admin)</span>
                    )}
                    {m.userId === user?.id && (
                      <span className="ml-1 text-xs text-text-muted">(you)</span>
                    )}
                  </span>
                  {!m.isAdmin && m.userId !== user?.id && (
                    <Button
                      variant="danger"
                      size="sm"
                      loading={removingId === m.userId}
                      onClick={() => setConfirmRemoveUserId(m.userId)}
                    >
                      Remove
                    </Button>
                  )}
                </div>
              ))
            )}
          </div>
        </Card>
      )}

      {/* Confirm leave modal */}
      <Modal
        open={confirmLeaveOpen}
        onClose={() => setConfirmLeaveOpen(false)}
        title="Leave League"
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-sm text-text-secondary">
            Are you sure you want to leave <strong className="text-text-primary">{league.name}</strong>?
            Your scores will be removed.
          </p>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" size="sm" onClick={() => setConfirmLeaveOpen(false)}>
              Cancel
            </Button>
            <Button variant="danger" size="sm" loading={leaving} onClick={() => void handleLeave()}>
              Leave
            </Button>
          </div>
        </div>
      </Modal>

      {/* Confirm remove member modal */}
      <Modal
        open={confirmRemoveUserId !== null}
        onClose={() => setConfirmRemoveUserId(null)}
        title="Remove Member"
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-sm text-text-secondary">
            Remove{' '}
            <strong className="text-text-primary">
              {confirmRemoveUserId ? usernameFor(confirmRemoveUserId) : 'this member'}
            </strong>{' '}
            from the league?
          </p>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" size="sm" onClick={() => setConfirmRemoveUserId(null)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              size="sm"
              loading={removingId === confirmRemoveUserId}
              onClick={() => {
                if (confirmRemoveUserId) void handleRemoveMember(confirmRemoveUserId)
              }}
            >
              Remove
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}

export default LeagueDetailPage
