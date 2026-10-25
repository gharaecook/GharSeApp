import { NavLink } from 'react-router-dom'

export interface NavItem { to: string; label: string; end?: boolean }

export const COOK_NAV: NavItem[] = [
  { to: '/cook', label: 'Overview', end: true },
  { to: '/cook/profile', label: 'My Profile & Kitchen' },
  { to: '/cook/dishes', label: 'My Dishes' },
  { to: '/cook/batches', label: 'Batches' },
  { to: '/cook/orders', label: 'Orders' },
  { to: '/cook/earnings', label: 'Earnings' },
]

export const ADMIN_NAV: NavItem[] = [
  { to: '/admin', label: 'Dashboard', end: true },
  { to: '/admin/cooks', label: 'Cooks' },
  { to: '/admin/orders', label: 'Orders' },
  { to: '/admin/users', label: 'Users' },
]

/** Horizontal tab bar used across the cook and admin areas (scrolls on small screens). */
export function SectionNav({ items }: { items: NavItem[] }) {
  return (
    <nav aria-label="Section" className="border-b border-cream-300/80 bg-white/90 backdrop-blur">
      <div className="container-page flex gap-1 overflow-x-auto py-2 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {items.map((i) => (
          <NavLink key={i.to} to={i.to} end={i.end}
            className={({ isActive }) => `whitespace-nowrap rounded-full px-3.5 py-2 text-sm font-bold transition active:scale-95 ${isActive ? 'bg-brand-100 text-brand-700' : 'text-ink-700 hover:bg-cream-200'}`}>
            {i.label}
          </NavLink>
        ))}
      </div>
    </nav>
  )
}

export function CookShell({ children }: { children: React.ReactNode }) {
  return <><SectionNav items={COOK_NAV} /><div className="container-page py-8">{children}</div></>
}

export function AdminShell({ children }: { children: React.ReactNode }) {
  return <><SectionNav items={ADMIN_NAV} /><div className="container-page py-8">{children}</div></>
}
