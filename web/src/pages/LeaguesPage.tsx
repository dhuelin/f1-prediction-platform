import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Card } from '@/components/ui/Card'
import { Modal } from '@/components/ui/Modal'
import { Loader } from '@/components/ui/Loader'
import { useAuthStore } from '@/store/authStore'
import * as leaguesApi from '@/api/leagues'
import * as scoringApi from '@/api/scoring'
import type { League, UserBalance } from '@/api/types'
import { cn } from '@/lib/utils'

// ---------------------------------------------------------------------------
// Placeholder data
// ---------------------------------------------------------------------------
const PLACEHOLDER_LEAGUES: League[] = [
  {
    id: 'league-1',
    name: 'Paddock Pundits',
    adminUserId: 'user-1',
    inviteCode: 'PUNDITS42',
    memberCount: 12,
    config: { topNSize: 10, scoringMode: 'PROXIMITY', bonusBetsEnabled: true, stakingEnabled: true, maxStakePerRace: 100, maxBetsPerRace: 4, catchUpPoints: null },
    createdAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 'league-2',
    name: 'F1 Fanatics Global',
    adminUserId: 'user-2',
    inviteCode: 'FANATICS',
    memberCount: 48,
    config: { topNSize: 10, scoringMode: 'PROXIMITY', bonusBetsEnabled: false, stakingEnabled: false, maxStakePerRace: 0, maxBetsPerRace: 0, catchUpPoints: null },
    createdAt: '2026-01-15T00:00:00Z',
  },
]

// ---------------------------------------------------------------------------
// Tab component
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
// League card
// ---------------------------------------------------------------------------
interface LeagueCardProps {
  league: League
  balance: UserBalance | null
  isMyLeague: boolean
  onJoin?: (league: League) => void
  joining?: boolean
}

function LeagueCard({ league, balance, isMyLeague, onJoin, joining }: LeagueCardProps) {
  return (
    <div className="flex items-center justify-between rounded-lg border border-border bg-surface px-5 py-4 transition-colors hover:bg-surface-raised">
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <Link
            to={`/leagues/${league.id}`}
            className="font-semibold text-text-primary hover:text-f1-red transition-colors truncate"
          >
            {league.name}
          </Link>
        </div>
        <p className="mt-0.5 text-sm text-text-muted">
          {league.memberCount} member{league.memberCount !== 1 ? 's' : ''}
          {league.config.bonusBetsEnabled && ' · Bonus bets'}
        </p>
      </div>

      <div className="flex items-center gap-3 ml-4 flex-shrink-0">
        {isMyLeague && balance !== null && (
          <span className="text-sm font-semibold text-f1-red">{balance.totalPoints} pts</span>
        )}
        {!isMyLeague && onJoin && (
          <Button
            variant="secondary"
            size="sm"
            loading={joining}
            onClick={() => onJoin(league)}
          >
            Join
          </Button>
        )}
        {isMyLeague && (
          <Link to={`/leagues/${league.id}`}>
            <Button variant="ghost" size="sm">View</Button>
          </Link>
        )}
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// LeaguesPage
// ---------------------------------------------------------------------------
export function LeaguesPage() {
  const { user } = useAuthStore()

  const [activeTab, setActiveTab] = useState(0)
  const [myLeagues, setMyLeagues] = useState<League[]>([])
  const [publicLeagues, setPublicLeagues] = useState<League[]>([])
  const [balances, setBalances] = useState<Map<string, UserBalance>>(new Map())
  const [loading, setLoading] = useState(true)
  const [error] = useState<string | null>(null)

  // Join by code modal
  const [joinModalOpen, setJoinModalOpen] = useState(false)
  const [joinCode, setJoinCode] = useState('')
  const [joinError, setJoinError] = useState<string | null>(null)
  const [joining, setJoining] = useState(false)

  // Create modal
  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [createName, setCreateName] = useState('')
  const [createDesc, setCreateDesc] = useState('')
  const [createPublic, setCreatePublic] = useState(true)
  const [createError, setCreateError] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)

  // Joining via browse
  const [joiningId, setJoiningId] = useState<string | null>(null)

  async function fetchLeagues() {
    try {
      const list = await leaguesApi.getLeagues()
      setMyLeagues(list.length > 0 ? list : PLACEHOLDER_LEAGUES.slice(0, 1))
      setPublicLeagues(list.length > 0 ? list : PLACEHOLDER_LEAGUES)

      if (user) {
        const entries = await Promise.allSettled(
          list.map((l) => scoringApi.getBalance(user.id, l.id)),
        )
        const map = new Map<string, UserBalance>()
        entries.forEach((r, i) => {
          if (r.status === 'fulfilled') map.set(list[i].id, r.value)
        })
        setBalances(map)
      }
    } catch {
      setMyLeagues(PLACEHOLDER_LEAGUES.slice(0, 1))
      setPublicLeagues(PLACEHOLDER_LEAGUES)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void fetchLeagues()
  }, [user])

  async function handleJoinByCode() {
    const code = joinCode.trim()
    if (!code) {
      setJoinError('Please enter an invite code.')
      return
    }
    setJoining(true)
    setJoinError(null)
    try {
      // Find league by code — we attempt joining the first matching league
      // The API joinLeague expects (leagueId, code). We use code as a stand-in
      // for league ID when no ID is known; real implementation would search first.
      await leaguesApi.joinLeague(code, code)
      setJoinModalOpen(false)
      setJoinCode('')
      void fetchLeagues()
    } catch {
      setJoinError('Invalid invite code or league not found.')
    } finally {
      setJoining(false)
    }
  }

  async function handleJoinLeague(league: League) {
    setJoiningId(league.id)
    try {
      await leaguesApi.joinLeague(league.id, league.inviteCode)
      void fetchLeagues()
    } catch {
      // silently fail — show in UI later
    } finally {
      setJoiningId(null)
    }
  }

  async function handleCreate() {
    if (!createName.trim()) {
      setCreateError('League name is required.')
      return
    }
    setCreating(true)
    setCreateError(null)
    try {
      await leaguesApi.createLeague({
        name: createName.trim(),
        config: createPublic ? undefined : { topNSize: 10, scoringMode: 'PROXIMITY', bonusBetsEnabled: true, stakingEnabled: true, maxStakePerRace: 100, maxBetsPerRace: 4, catchUpPoints: null },
      })
      setCreateModalOpen(false)
      setCreateName('')
      setCreateDesc('')
      setCreatePublic(true)
      void fetchLeagues()
    } catch {
      setCreateError('Failed to create league. Please try again.')
    } finally {
      setCreating(false)
    }
  }

  if (error) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-8">
        <p className="text-red-400">{error}</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      {/* Header */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-text-primary">Leagues</h1>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" onClick={() => setJoinModalOpen(true)}>
            Join by code
          </Button>
          <Button variant="primary" size="sm" onClick={() => setCreateModalOpen(true)}>
            Create League
          </Button>
        </div>
      </div>

      {/* Tabs */}
      <div className="mb-5">
        <Tabs tabs={['My Leagues', 'Browse']} active={activeTab} onChange={setActiveTab} />
      </div>

      {loading ? (
        <div className="flex justify-center py-12">
          <Loader size="lg" label="Loading leagues…" />
        </div>
      ) : activeTab === 0 ? (
        /* My Leagues */
        <div className="space-y-3">
          {myLeagues.length === 0 ? (
            <Card>
              <div className="py-6 text-center">
                <p className="text-text-secondary mb-4">You haven&apos;t joined any leagues yet.</p>
                <Button variant="primary" size="sm" onClick={() => setActiveTab(1)}>
                  Browse Leagues
                </Button>
              </div>
            </Card>
          ) : (
            myLeagues.map((league) => (
              <LeagueCard
                key={league.id}
                league={league}
                balance={balances.get(league.id) ?? null}
                isMyLeague
              />
            ))
          )}
        </div>
      ) : (
        /* Browse public leagues */
        <div className="space-y-3">
          {publicLeagues.length === 0 ? (
            <Card>
              <p className="text-text-secondary text-center py-6">No public leagues found.</p>
            </Card>
          ) : (
            publicLeagues.map((league) => (
              <LeagueCard
                key={league.id}
                league={league}
                balance={null}
                isMyLeague={false}
                onJoin={handleJoinLeague}
                joining={joiningId === league.id}
              />
            ))
          )}
        </div>
      )}

      {/* Join by code modal */}
      <Modal
        open={joinModalOpen}
        onClose={() => {
          setJoinModalOpen(false)
          setJoinCode('')
          setJoinError(null)
        }}
        title="Join League by Invite Code"
        size="sm"
      >
        <div className="space-y-4">
          {joinError && (
            <div className="rounded-md border border-red-500/30 bg-red-500/10 px-4 py-2 text-sm text-red-400" role="alert">
              {joinError}
            </div>
          )}
          <Input
            label="Invite Code"
            value={joinCode}
            onChange={(e) => setJoinCode(e.target.value)}
            placeholder="e.g. PUNDITS42"
            autoFocus
          />
          <div className="flex justify-end gap-2">
            <Button variant="secondary" size="sm" onClick={() => setJoinModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" size="sm" loading={joining} onClick={() => void handleJoinByCode()}>
              Join
            </Button>
          </div>
        </div>
      </Modal>

      {/* Create league modal */}
      <Modal
        open={createModalOpen}
        onClose={() => {
          setCreateModalOpen(false)
          setCreateName('')
          setCreateDesc('')
          setCreatePublic(true)
          setCreateError(null)
        }}
        title="Create League"
        size="md"
      >
        <div className="space-y-4">
          {createError && (
            <div className="rounded-md border border-red-500/30 bg-red-500/10 px-4 py-2 text-sm text-red-400" role="alert">
              {createError}
            </div>
          )}
          <Input
            label="League Name"
            value={createName}
            onChange={(e) => setCreateName(e.target.value)}
            placeholder="e.g. Paddock Pundits"
            required
            autoFocus
          />
          <Input
            label="Description (optional)"
            value={createDesc}
            onChange={(e) => setCreateDesc(e.target.value)}
            placeholder="A short description…"
          />
          <div className="flex items-center gap-3">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={createPublic}
                onChange={(e) => setCreatePublic(e.target.checked)}
                className="h-4 w-4 rounded border-border accent-f1-red"
              />
              <span className="text-sm font-medium text-text-primary">Public league</span>
            </label>
            <span className="text-xs text-text-muted">
              {createPublic ? 'Visible in Browse' : 'Invite-only'}
            </span>
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" size="sm" onClick={() => setCreateModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" size="sm" loading={creating} onClick={() => void handleCreate()}>
              Create
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}

export default LeaguesPage
