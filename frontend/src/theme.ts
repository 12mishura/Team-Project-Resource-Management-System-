import { useSyncExternalStore } from 'react'

export type ThemePreference = 'light' | 'dark' | 'system'

const storageKey = 'tasktracker.theme'
const media = window.matchMedia('(prefers-color-scheme: dark)')
const listeners = new Set<() => void>()

function readPreference(): ThemePreference {
  try {
    const saved = localStorage.getItem(storageKey)
    return saved === 'dark' || saved === 'system' ? saved : 'light'
  } catch {
    return 'light'
  }
}

let preference = readPreference()

export const resolvedTheme = () => (preference === 'system' ? (media.matches ? 'dark' : 'light') : preference)

function apply() {
  document.documentElement.dataset.theme = resolvedTheme()
  listeners.forEach(listener => listener())
}

export function setThemePreference(next: ThemePreference) {
  preference = next
  try {
    localStorage.setItem(storageKey, next)
  } catch {
    return apply()
  }
  apply()
}

export function toggleTheme() {
  setThemePreference(resolvedTheme() === 'dark' ? 'light' : 'dark')
}

export function initTheme() {
  apply()
  media.addEventListener('change', () => {
    if (preference === 'system') apply()
  })
}

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function useTheme() {
  const theme = useSyncExternalStore(subscribe, resolvedTheme)
  const choice = useSyncExternalStore(subscribe, () => preference)
  return { theme, preference: choice }
}
