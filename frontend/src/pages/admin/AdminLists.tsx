import { useQuery } from '@tanstack/react-query'
import { api } from '../../lib/api'
import { formatDateTime, rupees } from '../../lib/format'
import type { Order, Page, User } from '../../lib/types'
import { AdminShell } from '../../components/SectionNav'
import { OrderStatusBadge } from '../../components/cards'
import { Badge, Card, EmptyState, ErrorState, LoadingState, PageTitle } from '../../components/ui'

export function AdminOrders() {
  const q = useQuery({ queryKey: ['admin', 'orders'], queryFn: () => api.get<Page<Order>>('/admin/orders?size=100') })
  return (
    <AdminShell>
      <PageTitle title="Orders" subtitle={q.data ? `${q.data.totalElements} total` : undefined} />
      {q.isLoading ? <LoadingState /> : q.error ? <ErrorState error={q.error} onRetry={q.refetch} /> : !q.data?.content.length ? <EmptyState title="No orders yet" /> : (
        <Card className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-cream-200 text-xs uppercase text-ink-500"><tr><th className="p-3">Order</th><th className="p-3">Customer</th><th className="p-3">Cook</th><th className="p-3">Amount</th><th className="p-3">Status</th><th className="p-3">Date</th></tr></thead>
            <tbody>
              {q.data.content.map((o) => (
                <tr key={o.id} className="border-b border-cream-100 last:border-0">
                  <td className="p-3 font-bold">#{o.orderNumber}</td><td className="p-3">{o.customerName}</td><td className="p-3">{o.cookName}</td>
                  <td className="p-3">{rupees(o.totalAmount)}</td><td className="p-3"><OrderStatusBadge status={o.orderStatus} /></td><td className="p-3 text-xs">{formatDateTime(o.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </AdminShell>
  )
}

export function AdminUsers() {
  const q = useQuery({ queryKey: ['admin', 'users'], queryFn: () => api.get<Page<User>>('/admin/users?size=100') })
  return (
    <AdminShell>
      <PageTitle title="Users" subtitle={q.data ? `${q.data.totalElements} total` : undefined} />
      {q.isLoading ? <LoadingState /> : q.error ? <ErrorState error={q.error} onRetry={q.refetch} /> : (
        <Card className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-cream-200 text-xs uppercase text-ink-500"><tr><th className="p-3">Name</th><th className="p-3">Email</th><th className="p-3">Role</th><th className="p-3">Status</th><th className="p-3">Joined</th></tr></thead>
            <tbody>
              {q.data?.content.map((u) => (
                <tr key={u.id} className="border-b border-cream-100 last:border-0">
                  <td className="p-3 font-bold">{u.name}</td><td className="p-3">{u.email}</td>
                  <td className="p-3"><Badge tone={u.role === 'ADMIN' ? 'red' : u.role === 'COOK' ? 'brand' : 'neutral'}>{u.role}</Badge></td>
                  <td className="p-3">{u.status}</td><td className="p-3 text-xs">{formatDateTime(u.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </AdminShell>
  )
}
