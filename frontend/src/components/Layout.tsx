import { useState, type FormEvent } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { homeFor, useAuth } from '../lib/auth'
import { useLocation2 } from '../lib/location'
import { PUNE_LOCALITIES } from '../lib/format'
import type { Cart, NotificationItem } from '../lib/types'

export function Logo({ className = '' }: { className?: string }) {
  return (
    <Link to="/" className={`flex items-center ${className}`} aria-label="GharSe home">
      <img src="/gharse-logo.png" alt="" aria-hidden="true" className="h-14 w-auto object-contain sm:h-16" />
    </Link>
  )
}

function Navbar() {
  const { user, logout } = useAuth()
  const { locality, setLocality } = useLocation2()
  const navigate = useNavigate()
  const [q, setQ] = useState('')
  const [menu, setMenu] = useState(false)

  const cart = useQuery({ queryKey: ['cart'], queryFn: () => api.get<Cart>('/cart'), enabled: user?.role === 'CUSTOMER' })
  const notes = useQuery({
    queryKey: ['notifications'], queryFn: () => api.get<{ unread: number; items: NotificationItem[] }>('/notifications'),
    enabled: !!user, refetchInterval: 30_000,
  })

  const onSearch = (e: FormEvent) => {
    e.preventDefault()
    navigate(`/cooks${q.trim() ? `?q=${encodeURIComponent(q.trim())}` : ''}`)
  }
  const link = ({ isActive }: { isActive: boolean }) => `rounded-lg px-3 py-1.5 text-sm font-bold ${isActive ? 'bg-brand-100 text-brand-700' : 'text-ink-700 hover:bg-cream-200'}`

  return (
    <header className="sticky top-0 z-40 border-b border-cream-300 bg-cream-50/95 backdrop-blur">
      <div className="container-page flex flex-wrap items-center gap-x-4 gap-y-2 py-3">
        <Logo />
        <label className="order-3 flex items-center gap-1 text-sm font-bold text-ink-700 sm:order-none">
          <span aria-hidden>📍</span>
          <span className="sr-only">Location</span>
          <select aria-label="Choose your locality" value={locality} onChange={(e) => setLocality(e.target.value)} className="rounded-lg border border-cream-300 bg-white px-2 py-1 text-sm">
            <option value="">All of Pune</option>
            {PUNE_LOCALITIES.map((l) => <option key={l}>{l}</option>)}
          </select>
        </label>
        <form onSubmit={onSearch} role="search" className="order-last w-full sm:order-none sm:w-auto sm:flex-1">
          <input value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search cooks and dishes" placeholder="What are you craving?" className="input !rounded-full" />
        </form>
        <nav className="ml-auto flex items-center gap-1">
          <NavLink to="/cooks" className={link}>Home Cooks</NavLink>
          {user?.role === 'CUSTOMER' && (
            <>
              <NavLink to="/orders" className={link}>Orders</NavLink>
              <NavLink to="/cart" className={link} aria-label={`Cart, ${cart.data?.itemCount ?? 0} items`}>
                🛒 <span className="ml-0.5 rounded-full bg-brand-500 px-1.5 text-xs text-white">{cart.data?.itemCount ?? 0}</span>
              </NavLink>
            </>
          )}
          {user && user.role !== 'CUSTOMER' && <NavLink to={homeFor(user.role)} className={link}>{user.role === 'COOK' ? 'My Kitchen' : 'Admin'}</NavLink>}
          {user ? (
            <div className="relative">
              <button onClick={() => setMenu((m) => !m)} aria-haspopup="menu" aria-expanded={menu} className="flex items-center gap-1 rounded-lg px-3 py-1.5 text-sm font-bold hover:bg-cream-200">
                {user.name.split(' ')[0]}
                {!!notes.data?.unread && <span className="rounded-full bg-brand-500 px-1.5 text-xs text-white" aria-label={`${notes.data.unread} unread notifications`}>{notes.data.unread}</span>}
                ▾
              </button>
              {menu && (
                <div role="menu" className="card absolute right-0 mt-1 w-48 p-1" onClick={() => setMenu(false)}>
                  {user.role === 'CUSTOMER' && <>
                    <Link role="menuitem" className="block rounded-lg px-3 py-2 text-sm hover:bg-cream-100" to="/profile">Profile</Link>
                    <Link role="menuitem" className="block rounded-lg px-3 py-2 text-sm hover:bg-cream-100" to="/favorites">Favourite cooks</Link>
                  </>}
                  <Link role="menuitem" className="flex items-center justify-between rounded-lg px-3 py-2 text-sm hover:bg-cream-100" to="/notifications">
                    Notifications
                    {!!notes.data?.unread && <span className="rounded-full bg-brand-500 px-1.5 text-xs text-white">{notes.data.unread}</span>}
                  </Link>
                  <button role="menuitem" className="block w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-cream-100" onClick={() => { logout(); navigate('/') }}>Log out</button>
                </div>
              )}
            </div>
          ) : (
            <>
              <Link to="/login" className="rounded-lg px-3 py-1.5 text-sm font-bold text-ink-700 hover:bg-cream-200">Log in</Link>
              <Link to="/register" className="rounded-xl bg-brand-500 px-3 py-1.5 text-sm font-bold text-white hover:bg-brand-600">Sign up</Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}

export function Footer() {
  return (
    <footer className="mt-16 border-t border-cream-300 bg-cream-200/60">
      <div className="container-page flex flex-col gap-4 py-10 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <Logo />
          <p className="mt-2 text-sm text-ink-500">Homemade food. Real people. Local flavours.</p>
        </div>
        <div className="flex gap-6 text-sm font-semibold text-ink-700">
          <Link to="/cooks">Browse cooks</Link>
          <Link to="/register?role=COOK">Become a Home Cook</Link>
        </div>
      </div>
      <p className="pb-6 text-center text-xs text-ink-500">© {new Date().getFullYear()} GharSe · MVP</p>
    </footer>
  )
}

export function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:bg-white focus:p-2">Skip to content</a>
      <Navbar />
      <main id="main" className="flex-1"><Outlet /></main>
      <Footer />
    </div>
  )
}
