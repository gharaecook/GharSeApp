import type { AuthResponse, User } from './types'

const BASE = ((import.meta.env.VITE_API_URL as string | undefined) ?? 'http://localhost:8080').replace(/\/+$/, '')
export const API = `${BASE}/api/v1`

export class ApiError extends Error {
  status: number
  code: string
  fieldErrors: { field: string; message: string }[]
  constructor(status: number, code: string, message: string, fieldErrors: { field: string; message: string }[] = []) {
    super(message)
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

const STORAGE_KEY = 'gharse.session'
interface Session { accessToken: string; refreshToken: string; user: User }

export function loadSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as Session) : null
  } catch {
    return null
  }
}
export function saveSession(s: Session | null) {
  try {
    if (s) localStorage.setItem(STORAGE_KEY, JSON.stringify(s))
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    /* storage unavailable: session lives only in memory for this tab */
  }
  window.dispatchEvent(new Event('gharse:session'))
}

let refreshing: Promise<boolean> | null = null
async function refreshTokens(): Promise<boolean> {
  const s = loadSession()
  if (!s) return false
  refreshing ??= fetch(`${API}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: s.refreshToken }),
  })
    .then(async (r) => {
      if (!r.ok) return false
      const data = (await r.json()) as AuthResponse
      saveSession({ accessToken: data.accessToken, refreshToken: data.refreshToken, user: data.user })
      return true
    })
    .catch(() => false)
    .finally(() => { refreshing = null })
  return refreshing
}

type Query = Record<string, string | number | boolean | undefined | null>

export function qs(params: Query): string {
  const p = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') p.set(k, String(v))
  })
  const s = p.toString()
  return s ? `?${s}` : ''
}

async function request<T>(method: string, path: string, body?: unknown, retry = true): Promise<T> {
  const s = loadSession()
  const headers: Record<string, string> = {}
  if (body !== undefined && !(body instanceof FormData)) headers['Content-Type'] = 'application/json'
  if (s) headers.Authorization = `Bearer ${s.accessToken}`
  let res: Response
  try {
    res = await fetch(`${API}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : body instanceof FormData ? body : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the server. Please check your connection.')
  }
  if (res.status === 401 && retry && s && !path.startsWith('/auth/')) {
    if (await refreshTokens()) return request<T>(method, path, body, false)
    saveSession(null)
  }
  if (res.status === 204) return undefined as T
  const text = await res.text()
  const data = text ? JSON.parse(text) : undefined
  if (!res.ok) {
    throw new ApiError(res.status, data?.code ?? 'ERROR', data?.message ?? 'Something went wrong', data?.errors ?? [])
  }
  return data as T
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body ?? {}),
  put: <T>(path: string, body: unknown) => request<T>('PUT', path, body),
  del: <T>(path: string) => request<T>('DELETE', path),
  upload: async (file: File, folder: 'profiles' | 'dishes' | 'kitchens' | 'documents') => {
    const fd = new FormData()
    fd.append('file', file)
    const r = await request<{ url: string }>('POST', `/files?folder=${folder}`, fd)
    return r.url
  },
}
