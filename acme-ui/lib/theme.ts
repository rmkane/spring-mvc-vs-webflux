/**
 * Theme utilities for managing light/dark mode
 */

export type Theme = 'light' | 'dark' | 'system'

const THEME_STORAGE_KEY = 'acme-ui-theme'

/**
 * Get the saved theme preference from localStorage
 */
export function getSavedTheme(): Theme | null {
  if (typeof window === 'undefined') {
    return null
  }
  const saved = localStorage.getItem(THEME_STORAGE_KEY)
  if (saved === 'light' || saved === 'dark' || saved === 'system') {
    return saved
  }
  return null
}

/**
 * Save theme preference to localStorage
 */
export function saveTheme(theme: Theme): void {
  if (typeof window === 'undefined') {
    return
  }
  localStorage.setItem(THEME_STORAGE_KEY, theme)
}

/**
 * Get the effective theme (resolves 'system' to actual light/dark)
 */
export function getEffectiveTheme(theme: Theme): 'light' | 'dark' {
  if (theme === 'system') {
    if (typeof window === 'undefined') {
      return 'light' // Default to light during SSR
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }
  return theme
}

/**
 * Apply theme to document
 */
export function applyTheme(theme: Theme): void {
  if (typeof document === 'undefined') {
    return
  }
  const effective = getEffectiveTheme(theme)
  const root = document.documentElement
  if (effective === 'dark') {
    root.classList.add('dark')
  } else {
    root.classList.remove('dark')
  }
}

/**
 * Get initial theme (saved preference or system)
 */
export function getInitialTheme(): Theme {
  if (typeof window === 'undefined') {
    return 'system'
  }
  return getSavedTheme() || 'system'
}
