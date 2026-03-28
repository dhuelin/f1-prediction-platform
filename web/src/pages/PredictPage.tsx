import { useParams } from 'react-router-dom'
import { Card } from '@/components/ui/Card'

export function PredictPage() {
  const { raceId } = useParams<{ raceId: string }>()

  return (
    <main className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-bold text-text-primary">Submit Prediction</h1>
      <Card>
        <p className="text-text-secondary">
          Prediction form for race <span className="font-mono text-text-primary">{raceId}</span> coming soon.
        </p>
      </Card>
    </main>
  )
}

export default PredictPage
