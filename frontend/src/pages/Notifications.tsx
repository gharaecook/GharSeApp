import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { formatDateTime } from '../lib/format'
import type { NotificationItem } from '../lib/types'
import { Badge, Button, Card, EmptyState, ErrorState, LoadingState, PageTitle } from '../components/ui'

interface NotificationList {
  unread: number
  items: NotificationItem[]
}

export default function Notifications() {
  const queryClient = useQueryClient()
  const notifications = useQuery({
    queryKey: ['notifications'],
    queryFn: () => api.get<NotificationList>('/notifications'),
  })
  const markRead = useMutation({
    mutationFn: (id: string) => api.post<void>(`/notifications/${id}/read`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  if (notifications.isLoading) return <LoadingState />
  if (notifications.error) return <ErrorState error={notifications.error} onRetry={notifications.refetch} />

  const list = notifications.data!
  return (
    <div className="container-page max-w-3xl py-8">
      <PageTitle
        title="Notifications"
        subtitle={`${list.unread} unread notification${list.unread === 1 ? '' : 's'}`}
      />
      {markRead.error && <p role="alert" className="mb-4 text-sm font-semibold text-red-600">{markRead.error.message}</p>}
      {!list.items.length ? (
        <EmptyState emoji="🔔" title="You're all caught up" message="Order updates and other account activity will appear here." />
      ) : (
        <ul className="space-y-3">
          {list.items.map((item) => (
            <li key={item.id}>
              <Card className={`flex flex-wrap items-start justify-between gap-4 p-4 ${item.read ? '' : 'border-brand-300 bg-brand-50/40'}`}>
                <div className="min-w-0 flex-1">
                  <div className="mb-1 flex flex-wrap items-center gap-2">
                    <h2 className="font-extrabold">{item.title}</h2>
                    {!item.read && <Badge tone="brand">New</Badge>}
                  </div>
                  <p className="text-sm text-ink-700">{item.message}</p>
                  <p className="mt-2 text-xs text-ink-500">{formatDateTime(item.createdAt)}</p>
                </div>
                {!item.read && (
                  <Button
                    variant="secondary"
                    loading={markRead.isPending && markRead.variables === item.id}
                    disabled={markRead.isPending}
                    onClick={() => markRead.mutate(item.id)}
                  >
                    Mark as read
                  </Button>
                )}
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
