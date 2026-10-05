import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { CookSummary, User } from '../lib/types'
import { CookCard } from '../components/cards'
import { Button, Card, EmptyState, ErrorState, FormError, Input, LoadingState, PageTitle, Textarea } from '../components/ui'

export function Profile() {
  const qc = useQueryClient()
  const me = useQuery({ queryKey: ['me'], queryFn: () => api.get<User>('/users/me') })
  const { register, handleSubmit } = useForm<{ name: string; phone: string; address: string }>({ values: me.data && { name: me.data.name, phone: me.data.phone, address: me.data.address ?? '' } })
  const save = useMutation({ mutationFn: (v: { name: string; phone: string; address: string }) => api.put<User>('/users/me', v), onSuccess: (u) => qc.setQueryData(['me'], u) })
  if (me.isLoading) return <LoadingState />
  if (me.error) return <ErrorState error={me.error} onRetry={me.refetch} />
  return (
    <div className="container-page max-w-xl py-8">
      <PageTitle title="Your profile" subtitle={me.data!.email} />
      <Card className="p-6">
        <form onSubmit={handleSubmit((v) => save.mutate(v))} className="space-y-4">
          <Input label="Name" {...register('name', { required: true })} />
          <Input label="Mobile" type="tel" {...register('phone', { required: true })} />
          <Textarea label="Default delivery address" rows={3} {...register('address')} />
          <FormError error={save.error} />
          {save.isSuccess && <p role="status" className="text-sm font-bold text-leaf-600">Saved ✓</p>}
          <Button type="submit" loading={save.isPending}>Save changes</Button>
        </form>
      </Card>
    </div>
  )
}

export function Favorites() {
  const favs = useQuery({ queryKey: ['favorites'], queryFn: () => api.get<CookSummary[]>('/favorites') })
  if (favs.isLoading) return <LoadingState />
  if (favs.error) return <ErrorState error={favs.error} onRetry={favs.refetch} />
  return (
    <div className="container-page py-8">
      <PageTitle title="Favourite cooks" />
      {!favs.data?.length ? <EmptyState emoji="♡" title="No favourites yet" message="Tap the heart on a cook's page to save them here." action={<Link to="/cooks" className="font-bold text-brand-600">Browse cooks</Link>} /> : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{favs.data.map((c) => <CookCard key={c.id} cook={c} />)}</div>
      )}
    </div>
  )
}
