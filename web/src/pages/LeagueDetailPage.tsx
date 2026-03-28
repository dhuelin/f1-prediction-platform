import { useParams } from 'react-router-dom'
import { Card } from '@/components/ui/Card'

export function LeagueDetailPage() {
  const { leagueId } = useParams<{ leagueId: string }>()

  return (
    <main className="mx-auto max-w-5xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-bold text-text-primary">League</h1>
      <Card>
        <p className="text-text-secondary">
          League detail for <span className="font-mono text-text-primary">{leagueId}</span> coming soon.
        </p>
      </Card>
    </main>
  )
}

export default LeagueDetailPage
