import { Card } from '@/components/ui/Card'

export function HistoryPage() {
  return (
    <main className="mx-auto max-w-5xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-bold text-text-primary">Prediction History</h1>
      <Card>
        <p className="text-text-secondary">Your past predictions will appear here.</p>
      </Card>
    </main>
  )
}

export default HistoryPage
