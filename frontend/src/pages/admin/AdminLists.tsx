import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../lib/api'
import { formatDateTime, rupees } from '../../lib/format'
import type { Order, Page, User } from '../../lib/types'
import { AdminShell } from '../../components/SectionNav'
import { OrderStatusBadge } from '../../components/cards'
import { Badge, Button, Card, EmptyState, ErrorState, FormError, LoadingState, Modal, PageTitle } from '../../components/ui'
import { useAuth } from '../../lib/auth'

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
  const qc = useQueryClient()
  const { user: currentUser } = useAuth()
  const [userToSuspend, setUserToSuspend] = useState<User | null>(null)
  const q = useQuery({ queryKey: ['admin', 'users'], queryFn: () => api.get<Page<User>>('/admin/users?size=100') })
  const statusMutation = useMutation({
    mutationFn: ({ id, action }: { id: string; action: 'suspend' | 'reactivate' }) =>
      api.post<User>(`/admin/users/${id}/${action}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'users'] })
      setUserToSuspend(null)
    },
  })
  return (
    <AdminShell>
      <PageTitle title="Users" subtitle={q.data ? `${q.data.totalElements} total` : undefined} />
      {!userToSuspend && <FormError error={statusMutation.error} />}
      {q.isLoading ? <LoadingState /> : q.error ? <ErrorState error={q.error} onRetry={q.refetch} /> : (
        <Card className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-cream-200 text-xs uppercase text-ink-500"><tr><th className="p-3">Name</th><th className="p-3">Email</th><th className="p-3">Role</th><th className="p-3">Status</th><th className="p-3">Joined</th><th className="p-3">Actions</th></tr></thead>
            <tbody>
              {q.data?.content.map((u) => (
                <tr key={u.id} className="border-b border-cream-100 last:border-0">
                  <td className="p-3 font-bold">{u.name}</td><td className="p-3">{u.email}</td>
                  <td className="p-3"><Badge tone={u.role === 'ADMIN' ? 'red' : u.role === 'COOK' ? 'brand' : 'neutral'}>{u.role}</Badge></td>
                  <td className="p-3">{u.status}</td><td className="p-3 text-xs">{formatDateTime(u.createdAt)}</td>
                  <td className="p-3">
                    {u.id !== currentUser?.id && (u.status === 'ACTIVE'
                      ? <Button variant="danger" className="px-3 py-1.5" onClick={() => setUserToSuspend(u)}>Delete</Button>
                      : <Button variant="secondary" className="px-3 py-1.5" loading={statusMutation.isPending} onClick={() => statusMutation.mutate({ id: u.id, action: 'reactivate' })}>Reactivate</Button>)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
      <Modal open={!!userToSuspend} title="Delete this user?" onClose={() => setUserToSuspend(null)}>
        <p className="text-sm text-ink-700">This suspends {userToSuspend?.name}'s account and preserves their order history. You can reactivate the account later.</p>
        <div className="mt-3"><FormError error={statusMutation.error} /></div>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="ghost" onClick={() => setUserToSuspend(null)}>Cancel</Button>
          <Button variant="danger" loading={statusMutation.isPending} onClick={() => userToSuspend && statusMutation.mutate({ id: userToSuspend.id, action: 'suspend' })}>Delete user</Button>
        </div>
      </Modal>
    </AdminShell>
  )
}
