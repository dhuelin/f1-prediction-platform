import { useAuthStore } from '@/store/authStore'
import { Card } from '@/components/ui/Card'
import { Avatar } from '@/components/ui/Avatar'
import { Button } from '@/components/ui/Button'

export function ProfilePage() {
  const { user, logout } = useAuthStore()

  return (
    <main className="mx-auto max-w-2xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-bold text-text-primary">Profile</h1>
      <Card>
        <div className="flex items-center gap-4">
          <Avatar name={user?.displayName} size="lg" />
          <div>
            <p className="text-lg font-semibold text-text-primary">{user?.displayName}</p>
            <p className="text-sm text-text-secondary">{user?.email}</p>
          </div>
        </div>
        <div className="mt-6">
          <Button variant="danger" onClick={() => void logout()}>
            Sign out
          </Button>
        </div>
      </Card>
    </main>
  )
}

export default ProfilePage
