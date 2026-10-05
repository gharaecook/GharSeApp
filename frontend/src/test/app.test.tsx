import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { AuthProvider } from '../lib/auth'
import { LocationProvider } from '../lib/location'
import { formatTime, rupees, trackerSteps } from '../lib/format'
import { registerSchema } from '../pages/AuthPages'

function renderAt(path: string) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[path]}>
        <AuthProvider><LocationProvider><App /></LocationProvider></AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

afterEach(() => { localStorage.clear(); vi.restoreAllMocks() })

describe('formatting', () => {
  it('formats rupees and time windows', () => {
    expect(rupees(1200)).toBe('₹1,200')
    expect(formatTime('12:00:00')).toBe('12 PM')
    expect(formatTime('13:30:00')).toBe('1:30 PM')
  })
  it('uses pickup vs delivery steps', () => {
    expect(trackerSteps('PICKUP')).toContain('READY_FOR_PICKUP')
    expect(trackerSteps('COOK_DELIVERY')).toContain('OUT_FOR_DELIVERY')
  })
})

describe('registration schema', () => {
  const ok = { name: 'Priya', email: 'p@x.in', phone: '9876543210', password: 'longenough', role: 'CUSTOMER' as const }
  it('accepts valid input', () => expect(registerSchema.safeParse(ok).success).toBe(true))
  it('rejects a short password, bad email and bad phone', () => {
    const r = registerSchema.safeParse({ ...ok, password: 'short', email: 'nope', phone: 'abc' })
    expect(r.success).toBe(false)
    expect(r.error?.issues.length).toBe(3)
  })
  it('does not allow registering as admin', () => expect(registerSchema.safeParse({ ...ok, role: 'ADMIN' }).success).toBe(false))
})

describe('routing and guards', () => {
  it('shows the login form', () => {
    renderAt('/login')
    expect(screen.getByRole('heading', { name: /welcome back/i })).toBeInTheDocument()
  })
  it('redirects anonymous visitors from protected pages to login', async () => {
    renderAt('/cart')
    await waitFor(() => expect(screen.getByRole('heading', { name: /welcome back/i })).toBeInTheDocument())
  })
  it('redirects a customer away from the admin area', async () => {
    localStorage.setItem('gharse.session', JSON.stringify({ accessToken: 'a', refreshToken: 'r', user: { id: '1', name: 'Priya K', email: 'p@x.in', phone: '1', role: 'CUSTOMER', status: 'ACTIVE', createdAt: '' } }))
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ content: [], totalElements: 0, totalPages: 0, items: [], itemCount: 0, unread: 0 }), { status: 200 }))
    renderAt('/admin')
    await waitFor(() => expect(screen.getByRole('heading', { name: /discover amazing/i })).toBeInTheDocument())
  })
  it('renders a friendly 404', () => {
    renderAt('/nothing-here')
    expect(screen.getByText(/page not found/i)).toBeInTheDocument()
  })
})
