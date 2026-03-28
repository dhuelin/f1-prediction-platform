import { RouterProvider } from 'react-router-dom'
import { router } from '@/router'
import { useTheme } from '@/hooks/useTheme'

/**
 * ThemeInitialiser applies the theme class to <html> on first render.
 * It renders nothing — side-effects only.
 */
function ThemeInitialiser() {
  useTheme() // attaches side-effects (class on <html>, media listener)
  return null
}

function App() {
  return (
    <>
      <ThemeInitialiser />
      <RouterProvider router={router} />
    </>
  )
}

export default App
