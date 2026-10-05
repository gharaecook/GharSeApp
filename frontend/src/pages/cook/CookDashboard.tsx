import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../../lib/api'
import { rupees } from '../../lib/format'
import type { CookDashboard as Dash, CookProfile } from '../../lib/types'
import { CookShell } from '../../components/SectionNav'
import { Badge, Card, ErrorState, LoadingState, PageTitle } from '../../components/ui'

function Stat({ label, value, to }: { label: string; value: string | number; to?: string }) {
  const body = <Card className="p-4"><p className="text-xs font-bold uppercase tracking-wide text-ink-500">{label}</p><p className="mt-1 text-2xl font-extrabold">{value}</p></Card>
  return to ? <Link to={to}>{body}</Link> : body
}

export default function CookDashboard() {
  const dash = useQuery({ queryKey: ['cook', 'dashboard'], queryFn: () => api.get<Dash>('/cook/dashboard') })
  const profile = useQuery({ queryKey: ['cook', 'profile'], queryFn: () => api.get<CookProfile>('/cook/profile') })
  if (dash.isLoading || profile.isLoading) return <CookShell><LoadingState /></CookShell>
  if (dash.error || profile.error) return <CookShell><ErrorState error={dash.error ?? profile.error} onRetry={() => { dash.refetch(); profile.refetch() }} /></CookShell>
  const d = dash.data!
  const p = profile.data!
  return (
    <CookShell>
      <PageTitle title={`Namaste, ${p.name.split(' ')[0]}`} subtitle="Here's how your kitchen is doing" />
      {d.approvalStatus !== 'APPROVED' && (
        <div role="status" className={`mb-6 rounded-2xl p-4 ${d.approvalStatus === 'REJECTED' ? 'bg-red-50 text-red-800' : 'bg-amber-50 text-amber-900'}`}>
          <p className="font-extrabold">{d.approvalStatus === 'PENDING' ? 'Your profile is waiting for approval' : 'Your profile needs changes'}</p>
          <p className="text-sm">{d.approvalStatus === 'PENDING' ? 'Complete your story, cuisines and kitchen details. You can add dishes and batches once GharSe approves you.' : p.rejectionReason}</p>
          <Link to="/cook/profile" className="mt-2 inline-block text-sm font-bold underline">Update my profile</Link>
        </div>
      )}
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">
        <Stat label="Today's orders" value={d.todaysOrders} to="/cook/orders" />
        <Stat label="Upcoming orders" value={d.upcomingOrders} to="/cook/orders" />
        <Stat label="Pending actions" value={d.pendingActions} to="/cook/orders" />
        <Stat label="Active batches" value={d.activeBatches} to="/cook/batches" />
        <Stat label="This month" value={rupees(d.earningsThisMonth)} to="/cook/earnings" />
        <Stat label="Total earnings" value={rupees(d.totalEarnings)} to="/cook/earnings" />
      </div>
      <div className="mt-6 flex flex-wrap items-center gap-3 text-sm">
        <Badge tone={d.approvalStatus === 'APPROVED' ? 'green' : d.approvalStatus === 'REJECTED' ? 'red' : 'amber'}>{d.approvalStatus}</Badge>
        {d.approvalStatus === 'APPROVED' && <Link to={`/cooks/${p.id}`} className="font-bold text-brand-600 hover:underline">View my public page →</Link>}
        <Link to="/cook/dishes/new" className="font-bold text-brand-600 hover:underline">+ Add a dish</Link>
        <Link to="/cook/batches/new" className="font-bold text-brand-600 hover:underline">+ Create a batch</Link>
      </div>
    </CookShell>
  )
}
