import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'

interface LocationState { locality: string; setLocality: (l: string) => void }
const LocationContext = createContext<LocationState | null>(null)
const KEY = 'gharse.locality'

function read(): string {
  try { return localStorage.getItem(KEY) ?? '' } catch { return '' }
}

/** The customer's chosen locality. Empty string means "all of Pune". */
export function LocationProvider({ children }: { children: ReactNode }) {
  const [locality, setState] = useState(read)
  const value = useMemo<LocationState>(() => ({
    locality,
    setLocality: (l) => {
      setState(l)
      try { localStorage.setItem(KEY, l) } catch { /* ignore */ }
    },
  }), [locality])
  return <LocationContext.Provider value={value}>{children}</LocationContext.Provider>
}

export function useLocation2(): LocationState {
  const ctx = useContext(LocationContext)
  if (!ctx) throw new Error('useLocation2 must be used inside LocationProvider')
  return ctx
}
