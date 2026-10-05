import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, loadSession, saveSession } from './api'
import type { AuthResponse, Role, User } from './types'

interface AuthState {
  user: User | null
  login: (email: string, password: string) => Promise<User>
  register: (input: { name: string; phone: string; email: string; password: string; role: Role; locality?: string }) => Promise<User>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => loadSession()?.user ?? null)

  useEffect(() => {
    const sync = () => setUser(loadSession()?.user ?? null)
    window.addEventListener('gharse:session', sync)
    window.addEventListener('storage', sync)
    return () => {
      window.removeEventListener('gharse:session', sync)
      window.removeEventListener('storage', sync)
    }
  }, [])

  const accept = (r: AuthResponse) => {
    saveSession({ accessToken: r.accessToken, refreshToken: r.refreshToken, user: r.user })
    setUser(r.user)
    return r.user
  }
  const login = useCallback(async (email: string, password: string) => accept(await api.post<AuthResponse>('/auth/login', { email, password })), [])
  const register = useCallback(async (input: Parameters<AuthState['register']>[0]) => accept(await api.post<AuthResponse>('/auth/register', input)), [])
  const logout = useCallback(() => {
    saveSession(null)
    setUser(null)
  }, [])

  const value = useMemo(() => ({ user, login, register, logout }), [user, login, register, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

export function homeFor(role: Role): string {
  return role === 'ADMIN' ? '/admin' : role === 'COOK' ? '/cook' : '/home'
}
