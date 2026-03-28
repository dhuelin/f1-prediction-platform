import React, { useState } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Card } from '@/components/ui/Card'
import { ThemeToggle } from '@/components/ui/ThemeToggle'

interface FieldErrors {
  email?: string
  password?: string
}

function validate(email: string, password: string): FieldErrors {
  const errors: FieldErrors = {}
  if (!email) errors.email = 'Email is required.'
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = 'Enter a valid email address.'
  if (!password) errors.password = 'Password is required.'
  return errors
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login, isLoading, error, clearError } = useAuthStore()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [forgotToast, setForgotToast] = useState(false)

  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/'

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const errors = validate(email, password)
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }
    setFieldErrors({})
    clearError()
    try {
      await login(email, password)
      navigate(from, { replace: true })
    } catch {
      // error is set in store
    }
  }

  function handleForgot(e: React.MouseEvent) {
    e.preventDefault()
    setForgotToast(true)
    setTimeout(() => setForgotToast(false), 3000)
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-bg px-4 py-8">
      {/* Theme toggle — top right */}
      <div className="absolute right-4 top-4">
        <ThemeToggle />
      </div>

      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold tracking-tight text-text-primary">
            <span className="text-f1-red">F1</span> Predict
          </h1>
          <p className="mt-2 text-text-secondary">Sign in to your account</p>
        </div>

        <Card>
          {/* API error banner */}
          {error && (
            <div
              className="mb-4 rounded-md border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-400"
              role="alert"
            >
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
            <Input
              label="Email"
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value)
                setFieldErrors((prev) => ({ ...prev, email: undefined }))
              }}
              error={fieldErrors.email}
              placeholder="verstappen@redbull.com"
              autoComplete="email"
              required
            />
            <Input
              label="Password"
              type="password"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value)
                setFieldErrors((prev) => ({ ...prev, password: undefined }))
              }}
              error={fieldErrors.password}
              placeholder="••••••••"
              autoComplete="current-password"
              required
            />

            <div className="flex justify-end">
              <a
                href="#"
                onClick={handleForgot}
                className="text-sm text-text-secondary hover:text-f1-red transition-colors"
              >
                Forgot password?
              </a>
            </div>

            {forgotToast && (
              <p className="text-sm text-text-secondary" role="status">
                Password reset coming soon — contact your league admin.
              </p>
            )}

            <Button type="submit" variant="primary" loading={isLoading} className="w-full mt-1">
              Sign in
            </Button>
          </form>
        </Card>

        <p className="mt-4 text-center text-sm text-text-secondary">
          Don&apos;t have an account?{' '}
          <Link to="/register" className="text-f1-red hover:underline">
            Create one
          </Link>
        </p>
      </div>
    </div>
  )
}

export default LoginPage
