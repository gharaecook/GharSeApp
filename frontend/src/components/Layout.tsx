import { useState, type FormEvent } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { homeFor, useAuth } from '../lib/auth'
import { useLocation2 } from '../lib/location'
import { PUNE_LOCALITIES } from '../lib/format'
import type { Cart, NotificationItem } from '../lib/types'

export function Logo({ className = '' }: { className?: string }) {
  return (
    <Link to="/" className={`flex flex-col items-center ${className}`} aria-label="CUOQO home">
      <img src="/cuoqo-logo.png" alt="" aria-hidden="true" className="h-14 w-auto object-contain sm:h-16" />
      <span className="text-[10px] font-extrabold tracking-[0.2em] text-brand-800">CUOQO</span>
    </Link>
  )
}

type NavIconName = 'home' | 'search' | 'orders' | 'cart' | 'kitchen' | 'dishes' | 'batches' | 'admin' | 'users' | 'account'

function NavIcon({ name }: { name: NavIconName }) {
  const paths: Record<NavIconName, string> = {
    home: 'M3 10.5 12 3l9 7.5M5.5 9v11h13V9M9 20v-6h6v6',
    search: 'm20 20-4.5-4.5M18 10.5a7.5 7.5 0 1 1-15 0 7.5 7.5 0 0 1 15 0Z',
    orders: 'M7 3h10l3 3v15H4V3h3Zm0 0v5h10V3m-8 9h8m-8 4h8',
    cart: 'M3 4h2l2.2 11.2a2 2 0 0 0 2 1.6h8.6a2 2 0 0 0 1.9-1.4L22 8H6m4 13h.01M18 21h.01',
    kitchen: 'M3 10h18M5 10l1.4 10h11.2L19 10M8 10V6a4 4 0 0 1 8 0v4m-6 4h4',
    dishes: 'M4 12h16M6 12a6 6 0 0 0 12 0M12 6V3m-3 0h6',
    batches: 'M4 5h16v15H4zM8 3v4m8-4v4M4 9h16m-11 4h.01M12 13h.01M15 13h.01M8 16h.01M12 16h.01',
    admin: 'M12 3 4 6v5c0 5 3.4 8.5 8 10 4.6-1.5 8-5 8-10V6l-8-3Zm-3 9 2 2 4-4',
    users: 'M16 20v-1.5a4.5 4.5 0 0 0-4.5-4.5h-3A4.5 4.5 0 0 0 4 18.5V20m6-10a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Zm6-6a3.5 3.5 0 0 1 0 6.8m2 3.2a4.5 4.5 0 0 1 4 4.5V20',
    account: 'M20 21a8 8 0 0 0-16 0m8-10a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z',
  }
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5"><path d={paths[name]} /></svg>
}

function AccountMenu({ mobile = false }: { mobile?: boolean }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const notes = useQuery({
    queryKey: ['notifications'], queryFn: () => api.get<{ unread: number; items: NotificationItem[] }>('/notifications'),
    enabled: !!user, refetchInterval: 30_000,
  })
  const [menu, setMenu] = useState(false)

  if (!user) return null
  return (
    <div className="relative">
      <button onClick={() => setMenu((m) => !m)} aria-haspopup="menu" aria-expanded={menu}
        className={mobile
          ? 'relative flex h-10 w-10 items-center justify-center rounded-full bg-brand-100 text-sm font-extrabold text-brand-800 active:scale-95'
          : 'flex items-center gap-1 rounded-lg px-3 py-1.5 text-sm font-bold hover:bg-cream-200'}
        aria-label={mobile ? 'Open account menu' : undefined}>
        {mobile ? user.name.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase() : user.name.split(' ')[0]}
        {!!notes.data?.unread && <span className={mobile ? 'absolute right-0 top-0 h-3 w-3 rounded-full border-2 border-cream-50 bg-brand-500' : 'ml-1 rounded-full bg-brand-500 px-1.5 text-xs text-white'} aria-label={`${notes.data.unread} unread notifications`}>{mobile ? '' : notes.data.unread}</span>}
        {!mobile && ' ▾'}
      </button>
      {menu && (
        <div role="menu" className="card absolute right-0 mt-2 w-52 p-1 shadow-xl" onClick={() => setMenu(false)}>
          {user.role === 'CUSTOMER' && <>
            <Link role="menuitem" className="block rounded-lg px-3 py-2.5 text-sm hover:bg-cream-100" to="/profile">Profile</Link>
            <Link role="menuitem" className="block rounded-lg px-3 py-2.5 text-sm hover:bg-cream-100" to="/favorites">Favourite cooks</Link>
          </>}
          <Link role="menuitem" className="flex items-center justify-between rounded-lg px-3 py-2.5 text-sm hover:bg-cream-100" to="/notifications">
            Notifications
            {!!notes.data?.unread && <span className="rounded-full bg-brand-500 px-1.5 text-xs text-white">{notes.data.unread}</span>}
          </Link>
          <button role="menuitem" className="block w-full rounded-lg px-3 py-2.5 text-left text-sm hover:bg-cream-100" onClick={() => { logout(); navigate('/') }}>Log out</button>
        </div>
      )}
    </div>
  )
}

function MobileTabBar() {
  const { user } = useAuth()
  const { pathname } = useLocation()
  const cart = useQuery({ queryKey: ['cart'], queryFn: () => api.get<Cart>('/cart'), enabled: user?.role === 'CUSTOMER' })
  const items: { to: string; label: string; icon: NavIconName; end?: boolean; badge?: number }[] =
    user?.role === 'COOK'
      ? [
          { to: '/cook', label: 'Home', icon: 'home', end: true },
          { to: '/cook/dishes', label: 'Dishes', icon: 'dishes' },
          { to: '/cook/batches', label: 'Batches', icon: 'batches' },
          { to: '/cook/orders', label: 'Orders', icon: 'orders' },
        ]
      : user?.role === 'ADMIN'
        ? [
            { to: '/admin', label: 'Home', icon: 'admin', end: true },
            { to: '/admin/cooks', label: 'Cooks', icon: 'kitchen' },
            { to: '/admin/orders', label: 'Orders', icon: 'orders' },
            { to: '/admin/users', label: 'Users', icon: 'users' },
          ]
        : user?.role === 'CUSTOMER'
          ? [
              { to: '/home', label: 'Home', icon: 'home', end: true },
              { to: '/cooks', label: 'Explore', icon: 'search' },
              { to: '/orders', label: 'Orders', icon: 'orders' },
              { to: '/cart', label: 'Cart', icon: 'cart', badge: cart.data?.itemCount },
            ]
          : [
              { to: '/home', label: 'Home', icon: 'home', end: true },
              { to: '/cooks', label: 'Explore', icon: 'search' },
              { to: '/login', label: 'Log in', icon: 'account' },
            ]

  return (
    <nav aria-label="Mobile navigation" className="mobile-tab-bar md:hidden">
      {items.map((item) => (
        <NavLink key={item.to} to={item.to} end={item.end}
          className={({ isActive }) => `mobile-tab${isActive || (item.label === 'Home' && pathname === '/') ? ' mobile-tab-active' : ''}`}
          aria-label={item.badge ? `${item.label}, ${item.badge} items` : item.label}>
          <span className="relative"><NavIcon name={item.icon} />{!!item.badge && <span className="absolute -right-2 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-brand-500 px-1 text-[10px] font-extrabold text-white">{item.badge}</span>}</span>
          <span>{item.label}</span>
        </NavLink>
      ))}
    </nav>
  )
}

function Navbar() {
  const { user } = useAuth()
  const { locality, setLocality } = useLocation2()
  const navigate = useNavigate()
  const location = useLocation()
  const [q, setQ] = useState('')
  const isHome = location.pathname === '/' || location.pathname === '/home'

  const cart = useQuery({ queryKey: ['cart'], queryFn: () => api.get<Cart>('/cart'), enabled: user?.role === 'CUSTOMER' })

  const onSearch = (e: FormEvent) => {
    e.preventDefault()
    navigate(`/cooks${q.trim() ? `?q=${encodeURIComponent(q.trim())}` : ''}`)
  }
  const link = ({ isActive }: { isActive: boolean }) => `rounded-lg px-3 py-1.5 text-sm font-bold ${isActive ? 'bg-brand-100 text-brand-700' : 'text-ink-700 hover:bg-cream-200'}`

  return (
    <header className="app-header sticky top-0 z-40 border-b border-cream-300/80 bg-cream-50/95 backdrop-blur-xl">
      <div className="container-page flex flex-wrap items-center gap-x-4 gap-y-2 py-2.5 md:py-3">
        <Logo className="hidden md:flex" />
        <label className="hidden items-center gap-1 text-sm font-bold text-ink-700 md:flex">
          <span aria-hidden>📍</span>
          <span className="sr-only">Location</span>
          <select aria-label="Choose your locality" value={locality} onChange={(e) => setLocality(e.target.value)} className="rounded-lg border border-cream-300 bg-white px-2 py-1 text-sm">
            <option value="">All of Pune</option>
            {PUNE_LOCALITIES.map((l) => <option key={l}>{l}</option>)}
          </select>
        </label>
        <form onSubmit={onSearch} role="search" className="hidden w-auto flex-1 md:block">
          <input value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search cooks and dishes" placeholder="What are you craving?" className="input !rounded-full" />
        </form>
        <nav aria-label="Primary navigation" className="ml-auto hidden items-center gap-1 md:flex">
          <NavLink to="/cooks" className={link}>Home Cooks</NavLink>
          {user?.role === 'CUSTOMER' && <>
            <NavLink to="/orders" className={link}>Orders</NavLink>
            <NavLink to="/cart" className={link} aria-label={`Cart, ${cart.data?.itemCount ?? 0} items`}>
              🛒 <span className="ml-0.5 rounded-full bg-brand-500 px-1.5 text-xs text-white">{cart.data?.itemCount ?? 0}</span>
            </NavLink>
          </>}
          {user && user.role !== 'CUSTOMER' && <NavLink to={homeFor(user.role)} className={link}>{user.role === 'COOK' ? 'My Kitchen' : 'Admin'}</NavLink>}
          {user ? <AccountMenu /> : <>
            <Link to="/login" className="rounded-lg px-3 py-1.5 text-sm font-bold text-ink-700 hover:bg-cream-200">Log in</Link>
            <Link to="/register" className="rounded-xl bg-brand-500 px-3 py-1.5 text-sm font-bold text-white hover:bg-brand-600">Sign up</Link>
          </>}
        </nav>
        <div className="flex w-full items-center justify-between md:hidden">
          <Logo className="min-w-0" />
          <div className="flex items-center gap-2">
            {user?.role !== 'ADMIN' && <label className="flex max-w-[145px] items-center gap-1 rounded-full border border-cream-300 bg-white px-2.5 py-2 text-xs font-bold text-ink-700">
              <span aria-hidden>📍</span><span className="sr-only">Location</span>
              <select aria-label="Choose your locality" value={locality} onChange={(e) => setLocality(e.target.value)} className="min-w-0 truncate bg-transparent text-xs outline-none">
                <option value="">All of Pune</option>
                {PUNE_LOCALITIES.map((l) => <option key={l}>{l}</option>)}
              </select>
            </label>}
            {user ? <AccountMenu mobile /> : <Link to="/register" className="rounded-full bg-brand-500 px-4 py-2 text-xs font-extrabold text-white active:scale-95">Join CUOQO</Link>}
          </div>
        </div>
        {!isHome && <form onSubmit={onSearch} role="search" className="w-full md:hidden">
          <input value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search cooks and dishes" placeholder="Search cooks, dishes, cuisines…" className="input !rounded-full !bg-white !py-2.5" />
        </form>}
      </div>
    </header>
  )
}

export function Footer() {
  return (
    <footer className="mt-16 hidden border-t border-cream-300 bg-cream-200/60 sm:block">
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
      <p className="pb-6 text-center text-xs text-ink-500">© {new Date().getFullYear()} CUOQO · MVP</p>
    </footer>
  )
}

export function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:bg-white focus:p-2">Skip to content</a>
      <Navbar />
      <main id="main" className="app-main flex-1"><Outlet /></main>
      <Footer />
      <MobileTabBar />
    </div>
  )
}
