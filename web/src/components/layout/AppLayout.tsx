import React, { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { Avatar } from '@/components/ui/Avatar'
import { ThemeToggle } from '@/components/ui/ThemeToggle'
import { cn } from '@/lib/utils'

interface NavItem {
  label: string
  to: string
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Home', to: '/' },
  { label: 'Leagues', to: '/leagues' },
  { label: 'History', to: '/history' },
  { label: 'Profile', to: '/profile' },
]

interface AppLayoutProps {
  children: React.ReactNode
}

export function AppLayout({ children }: AppLayoutProps) {
  const { user, logout } = useAuthStore()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="flex min-h-screen flex-col bg-bg">
      {/* Top nav */}
      <header className="sticky top-0 z-40 border-b border-border bg-surface/90 backdrop-blur-sm">
        <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4">
          {/* Logo */}
          <Link
            to="/"
            className="text-lg font-bold tracking-tight text-text-primary hover:opacity-80 transition-opacity"
          >
            <span className="text-f1-red">F1</span> Predict
          </Link>

          {/* Desktop nav links */}
          <nav className="hidden items-center gap-1 md:flex" aria-label="Main navigation">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) =>
                  cn(
                    'rounded-md px-3 py-1.5 text-sm font-medium transition-colors duration-150',
                    isActive
                      ? 'bg-f1-red/10 text-f1-red'
                      : 'text-text-secondary hover:text-text-primary hover:bg-surface-raised',
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

          {/* Right side */}
          <div className="flex items-center gap-2">
            <ThemeToggle />

            {/* User avatar with dropdown */}
            <div className="relative">
              <button
                type="button"
                className={cn(
                  'flex items-center gap-2 rounded-md px-2 py-1 transition-colors',
                  'hover:bg-surface-raised focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-f1-red',
                )}
                onClick={() => setMenuOpen((v) => !v)}
                aria-haspopup="true"
                aria-expanded={menuOpen}
                aria-label="User menu"
              >
                <Avatar name={user?.displayName} size="sm" />
                <span className="hidden text-sm font-medium text-text-primary sm:block">
                  {user?.displayName}
                </span>
              </button>

              {menuOpen && (
                <>
                  {/* Backdrop to close */}
                  <div
                    className="fixed inset-0 z-10"
                    aria-hidden="true"
                    onClick={() => setMenuOpen(false)}
                  />
                  <div className="absolute right-0 z-20 mt-2 w-44 rounded-lg border border-border bg-surface py-1 shadow-lg">
                    <Link
                      to="/profile"
                      className="block px-4 py-2 text-sm text-text-secondary hover:bg-surface-raised hover:text-text-primary transition-colors"
                      onClick={() => setMenuOpen(false)}
                    >
                      Profile
                    </Link>
                    <button
                      type="button"
                      className="w-full px-4 py-2 text-left text-sm text-red-400 hover:bg-surface-raised transition-colors"
                      onClick={() => {
                        setMenuOpen(false)
                        void handleLogout()
                      }}
                    >
                      Sign out
                    </button>
                  </div>
                </>
              )}
            </div>

            {/* Mobile hamburger */}
            <button
              type="button"
              className={cn(
                'inline-flex h-9 w-9 items-center justify-center rounded-md md:hidden',
                'text-text-secondary hover:text-text-primary hover:bg-surface-raised transition-colors',
                'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-f1-red',
              )}
              onClick={() => setMenuOpen((v) => !v)}
              aria-label="Toggle mobile menu"
            >
              {menuOpen ? (
                <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              ) : (
                <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
                </svg>
              )}
            </button>
          </div>
        </div>

        {/* Mobile nav drawer */}
        {menuOpen && (
          <div className="border-t border-border bg-surface px-4 py-3 md:hidden">
            <nav className="flex flex-col gap-1" aria-label="Mobile navigation">
              {NAV_ITEMS.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === '/'}
                  className={({ isActive }) =>
                    cn(
                      'rounded-md px-3 py-2 text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-f1-red/10 text-f1-red'
                        : 'text-text-secondary hover:text-text-primary hover:bg-surface-raised',
                    )
                  }
                  onClick={() => setMenuOpen(false)}
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>
        )}
      </header>

      {/* Page content */}
      <main className="flex-1">{children}</main>
    </div>
  )
}

export default AppLayout
