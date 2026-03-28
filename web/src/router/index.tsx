import React from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'
import { AppLayout } from '@/components/layout/AppLayout'

import { LoginPage } from '@/pages/LoginPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { HomePage } from '@/pages/HomePage'
import { PredictPage } from '@/pages/PredictPage'
import { LeaguesPage } from '@/pages/LeaguesPage'
import { LeagueDetailPage } from '@/pages/LeagueDetailPage'
import { HistoryPage } from '@/pages/HistoryPage'
import { ProfilePage } from '@/pages/ProfilePage'

function Protected({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute>
      <AppLayout>{children}</AppLayout>
    </ProtectedRoute>
  )
}

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

  // --- Protected routes (wrapped in AppLayout) ---
  {
    path: '/',
    element: (
      <Protected>
        <HomePage />
      </Protected>
    ),
  },
  {
    path: '/predict/:raceId',
    element: (
      <Protected>
        <PredictPage />
      </Protected>
    ),
  },
  {
    path: '/leagues',
    element: (
      <Protected>
        <LeaguesPage />
      </Protected>
    ),
  },
  {
    path: '/leagues/:leagueId',
    element: (
      <Protected>
        <LeagueDetailPage />
      </Protected>
    ),
  },
  {
    path: '/history',
    element: (
      <Protected>
        <HistoryPage />
      </Protected>
    ),
  },
  {
    path: '/profile',
    element: (
      <Protected>
        <ProfilePage />
      </Protected>
    ),
  },

  // --- Catch-all ---
  {
    path: '*',
    element: <Navigate to="/" replace />,
  },
])

export default router
