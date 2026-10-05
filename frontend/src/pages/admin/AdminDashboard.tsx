import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../../lib/api'
import { rupees } from '../../lib/format'
import type { AdminDashboard as Dash } from '../../lib/types'
import { AdminShell } from '../../components/SectionNav'
import { Card, ErrorState, LoadingState, PageTitle } from '../../components/ui'

export default function AdminDashboard() {
  const d = useQuery({ queryKey: ['admin', 'dashboard'], queryFn: () => api.get<Dash>('/admin/dashboard') })
  if (d.isLoading) return <AdminShell><LoadingState /></AdminShell>
  if (d.error) return <AdminShell><ErrorState error={d.error} onRetry={d.refetch} /></AdminShell>
  const s = d.data!
  const stats: [string, string | number, string?][] = [
    ['Total users', s.totalUsers, '/admin/users'], ['Total cooks', s.totalCooks, '/admin/cooks'], ['Pending cooks', s.pendingCooks, '/admin/cooks?status=PENDING'],
    ['Approved cooks', s.approvedCooks, '/admin/cooks?status=APPROVED'], ['Orders', s.totalOrders, '/admin/orders'], ['Completed orders', s.completedOrders, '/admin/orders'],
    ['Revenue', rupees(s.revenue)],
  ]
  return (
    <AdminShell>
      <PageTitle title="Admin dashboard" />
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {stats.map(([label, value, to]) => {
          const card = <Card className={`p-4 ${label === 'Pending cooks' && s.pendingCooks > 0 ? 'ring-2 ring-amber-300' : ''}`}><p className="text-xs font-bold uppercase tracking-wide text-ink-500">{label}</p><p className="mt-1 text-2xl font-extrabold">{value}</p></Card>
          return to ? <Link key={label} to={to}>{card}</Link> : <div key={label}>{card}</div>
        })}
      </div>
    </AdminShell>
  )
}
