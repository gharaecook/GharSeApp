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
    <nav aria-label="Section" className="border-b border-cream-300 bg-white">
      <div className="container-page flex gap-1 overflow-x-auto py-2">
        {items.map((i) => (
          <NavLink key={i.to} to={i.to} end={i.end}
            className={({ isActive }) => `whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-bold ${isActive ? 'bg-brand-100 text-brand-700' : 'text-ink-700 hover:bg-cream-200'}`}>
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
