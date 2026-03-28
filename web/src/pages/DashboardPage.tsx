import { useAuthStore } from '@/store/authStore'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'

export function DashboardPage() {
  const { user } = useAuthStore()

  return (
    <main className="mx-auto max-w-5xl px-4 py-8">
      <header className="mb-8">
        <h1 className="text-2xl font-bold text-text-primary">
          Welcome back, {user?.displayName ?? 'Driver'}
        </h1>
        <p className="mt-1 text-text-secondary">2026 Formula 1 Season</p>
      </header>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Card
          header={
            <div className="flex items-center justify-between">
              <span className="font-semibold text-text-primary">Next Race</span>
              <Badge variant="upcoming" dot>Upcoming</Badge>
            </div>
          }
        >
          <p className="text-lg font-bold text-text-primary">Bahrain Grand Prix</p>
          <p className="text-sm text-text-secondary">Bahrain International Circuit</p>
          <p className="mt-2 text-sm text-text-muted">Round 1 · 16 Mar 2026</p>
        </Card>

        <Card
          header={
            <span className="font-semibold text-text-primary">Your Prediction</span>
          }
        >
          <p className="text-text-secondary">No prediction submitted yet.</p>
          <p className="mt-2 text-sm text-text-muted">Qualifying locks in 3 days.</p>
        </Card>

        <Card
          header={
            <span className="font-semibold text-text-primary">Points Balance</span>
          }
        >
          <p className="text-3xl font-bold text-f1-red">—</p>
          <p className="mt-1 text-sm text-text-secondary">Join a league to start scoring.</p>
        </Card>
      </div>
    </main>
  )
}

export default DashboardPage
