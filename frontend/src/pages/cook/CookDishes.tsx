import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../lib/api'
import { categoryLabel } from '../../lib/format'
import type { Cuisine, Dish, DishCategory } from '../../lib/types'
import { CookShell } from '../../components/SectionNav'
import { DishCard, DishVisual } from '../../components/cards'
import { Badge, Button, Card, EmptyState, ErrorState, FormError, Input, LoadingState, PageTitle, Select, Textarea } from '../../components/ui'

export function CookDishes() {
  const qc = useQueryClient()
  const dishes = useQuery({ queryKey: ['cook', 'dishes'], queryFn: () => api.get<Dish[]>('/cook/dishes') })
  const toggle = useMutation({
    mutationFn: (d: Dish) => d.active ? api.del(`/cook/dishes/${d.id}`) : api.put(`/cook/dishes/${d.id}`, { ...d, cuisineId: d.cuisineId, active: true }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['cook', 'dishes'] }),
  })
  return (
    <CookShell>
      <PageTitle title="My dishes" action={<Link to="/cook/dishes/new" className="rounded-xl bg-brand-500 px-4 py-2.5 text-sm font-bold text-white">+ New dish</Link>} />
      {dishes.isLoading ? <LoadingState /> : dishes.error ? <ErrorState error={dishes.error} onRetry={dishes.refetch} /> : !dishes.data?.length ? (
        <EmptyState emoji="👩‍🍳" title="No dishes yet" message="Add your first dish. Only approved cooks can publish food." action={<Link to="/cook/dishes/new" className="font-bold text-brand-600">Add a dish</Link>} />
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {dishes.data.map((d) => (
            <div key={d.id} className={d.active ? '' : 'opacity-60'}>
              <DishCard dish={d} action={
                <div className="flex items-center gap-2">
                  {!d.active && <Badge>Inactive</Badge>}
                  <Link to={`/cook/dishes/${d.id}`} className="text-sm font-bold text-brand-600">Edit</Link>
                  <button className="text-sm font-bold text-ink-500 hover:text-red-600" onClick={() => toggle.mutate(d)}>{d.active ? 'Deactivate' : 'Activate'}</button>
                </div>} />
            </div>
          ))}
        </div>
      )}
      <div className="mt-3"><FormError error={toggle.error} /></div>
    </CookShell>
  )
}

const schema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(150),
  description: z.string().max(2000).optional(),
  category: z.enum(['MEAL', 'THALI', 'SNACK', 'SWEET', 'BAKERY', 'BEVERAGE', 'OTHER']),
  cuisineId: z.string().optional(),
  price: z.coerce.number().min(0, 'Price cannot be negative'),
  portionDescription: z.string().max(120).optional(),
  preparationTimeMinutes: z.union([z.literal(''), z.coerce.number().int().min(0)]).optional(),
  ingredients: z.string().max(2000).optional(),
  allergens: z.string().max(300).optional(),
  vegetarian: z.boolean(),
  active: z.boolean(),
})
type Form = z.infer<typeof schema>

export function DishForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [image, setImage] = useState<string | null>(null)
  const [uploadError, setUploadError] = useState<unknown>(null)
  const cuisines = useQuery({ queryKey: ['cuisines'], queryFn: () => api.get<Cuisine[]>('/cuisines') })
  const dishes = useQuery({ queryKey: ['cook', 'dishes'], queryFn: () => api.get<Dish[]>('/cook/dishes'), enabled: !!id })
  const existing = id ? dishes.data?.find((d) => d.id === id) : undefined

  const { register, handleSubmit, formState: { errors } } = useForm<Form>({
    resolver: zodResolver(schema) as never,
    defaultValues: { category: 'MEAL', vegetarian: true, active: true, price: 0 },
    values: existing && { name: existing.name, description: existing.description ?? '', category: existing.category as DishCategory, cuisineId: existing.cuisineId ?? '',
      price: existing.price, portionDescription: existing.portionDescription ?? '', preparationTimeMinutes: existing.preparationTimeMinutes ?? '',
      ingredients: existing.ingredients ?? '', allergens: existing.allergens ?? '', vegetarian: existing.vegetarian, active: existing.active },
  })
  const save = useMutation({
    mutationFn: (v: Form) => {
      const body = { ...v, cuisineId: v.cuisineId || undefined, preparationTimeMinutes: v.preparationTimeMinutes === '' ? undefined : v.preparationTimeMinutes, imageUrl: image ?? existing?.imageUrl }
      return id ? api.put(`/cook/dishes/${id}`, body) : api.post('/cook/dishes', body)
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cook', 'dishes'] }); navigate('/cook/dishes') },
  })
  if (id && dishes.isLoading) return <CookShell><LoadingState /></CookShell>
  if (id && dishes.data && !existing) return <CookShell><ErrorState error={new Error('Dish not found')} /></CookShell>

  return (
    <CookShell>
      <PageTitle title={id ? 'Edit dish' : 'New dish'} />
      <Card className="max-w-2xl p-5">
        <form onSubmit={handleSubmit((v) => save.mutate(v))} className="space-y-4" noValidate>
          <div className="flex items-center gap-4">
            <DishVisual dish={{ imageUrl: image ?? existing?.imageUrl, category: existing?.category ?? 'MEAL', name: 'Dish' }} className="h-20 w-20 rounded-xl" />
            <label className="text-sm font-bold text-brand-600">Upload photo<input type="file" accept="image/jpeg,image/png,image/webp" className="sr-only"
              onChange={async (e) => { const f = e.target.files?.[0]; if (!f) return; setUploadError(null); try { setImage(await api.upload(f, 'dishes')) } catch (err) { setUploadError(err) } }} /></label>
          </div>
          <Input label="Dish name" {...register('name')} error={errors.name?.message} />
          <Textarea label="Description" rows={3} {...register('description')} />
          <div className="grid gap-4 sm:grid-cols-2">
            <Select label="Category" {...register('category')}>{(Object.keys(categoryLabel) as DishCategory[]).map((c) => <option key={c} value={c}>{categoryLabel[c]}</option>)}</Select>
            <Select label="Cuisine" {...register('cuisineId')}><option value="">—</option>{cuisines.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</Select>
            <Input label="Price (₹)" type="number" step="0.5" min={0} {...register('price')} error={errors.price?.message} />
            <Input label="Portion" placeholder="4 pieces" {...register('portionDescription')} />
            <Input label="Preparation time (minutes)" type="number" min={0} {...register('preparationTimeMinutes')} error={errors.preparationTimeMinutes?.message} />
            <Input label="Allergens" placeholder="gluten, nuts" {...register('allergens')} />
          </div>
          <Textarea label="Ingredients" rows={2} {...register('ingredients')} />
          <div className="flex gap-6 text-sm font-semibold">
            <label className="flex items-center gap-2"><input type="checkbox" {...register('vegetarian')} /> Vegetarian</label>
            <label className="flex items-center gap-2"><input type="checkbox" {...register('active')} /> Active</label>
          </div>
          <FormError error={uploadError} />
          <FormError error={save.error} />
          <div className="flex gap-2"><Button type="submit" loading={save.isPending}>Save dish</Button><Button type="button" variant="ghost" onClick={() => navigate('/cook/dishes')}>Cancel</Button></div>
        </form>
      </Card>
    </CookShell>
  )
}
