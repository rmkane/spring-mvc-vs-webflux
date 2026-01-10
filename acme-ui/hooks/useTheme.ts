import { useEffect, useMemo, useState } from 'react'

import { useLocalStorage } from '@/hooks/useLocalStorage'
import { applyTheme, type Theme } from '@/lib/theme'

const THEME_STORAGE_KEY = 'acme-ui-theme'

/**
 * Hook for managing theme state with system preference support
 * @returns Theme state and controls
 */
export function useTheme() {
  const [theme, setTheme] = useLocalStorage<Theme>(THEME_STORAGE_KEY, 'system')
  // Always start with 'light' to match SSR, then update in useEffect
  const [systemPreference, setSystemPreference] = useState<'light' | 'dark'>('light')
  const [mounted, setMounted] = useState(false)

  // Calculate effective theme based on current theme and system preference
  // During SSR and initial render, always return 'light' to prevent hydration mismatch
  const effectiveTheme = useMemo(() => {
    if (!mounted) {
      return 'light' // Match SSR
    }
    if (theme === 'system') {
      return systemPreference
    }
    return theme
  }, [theme, systemPreference, mounted])

  // Initialize system preference listener on mount
  useEffect(() => {
    // Mark as mounted to enable theme calculations (prevents hydration mismatch)
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMounted(true)
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    const updateSystemPreference = () => {
      const newPreference = mediaQuery.matches ? 'dark' : 'light'
      setSystemPreference(newPreference)
    }

    // Set initial preference
    updateSystemPreference()

    // Listen for system theme changes
    mediaQuery.addEventListener('change', updateSystemPreference)
    return () => mediaQuery.removeEventListener('change', updateSystemPreference)
  }, [])

  // Apply theme when it or system preference changes
  // The script in layout.tsx handles initial application, this keeps it in sync
  useEffect(() => {
    if (mounted) {
      applyTheme(theme)
    }
  }, [theme, systemPreference, mounted])

  return {
    theme,
    setTheme,
    effectiveTheme,
  }
}
