import { createBrowserRouter, Navigate } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'

import { LoginPage } from '@/pages/LoginPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { PredictPage } from '@/pages/PredictPage'
import { LeaguesPage } from '@/pages/LeaguesPage'
import { LeagueDetailPage } from '@/pages/LeagueDetailPage'
import { HistoryPage } from '@/pages/HistoryPage'
import { ProfilePage } from '@/pages/ProfilePage'

export const router = createBrowserRouter([
  // --- Public routes ---
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/register',
    element: <RegisterPage />,
  },

  // --- Protected routes ---
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <DashboardPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/predict/:raceId',
    element: (
      <ProtectedRoute>
        <PredictPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/leagues',
    element: (
      <ProtectedRoute>
        <LeaguesPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/leagues/:leagueId',
    element: (
      <ProtectedRoute>
        <LeagueDetailPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/history',
    element: (
      <ProtectedRoute>
        <HistoryPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/profile',
    element: (
      <ProtectedRoute>
        <ProfilePage />
      </ProtectedRoute>
    ),
  },

  // --- Catch-all ---
  {
    path: '*',
    element: <Navigate to="/" replace />,
  },
])

export default router
