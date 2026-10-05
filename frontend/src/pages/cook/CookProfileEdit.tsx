import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../lib/api'
import type { CookProfile, Cuisine } from '../../lib/types'
import { CookShell } from '../../components/SectionNav'
import { Avatar, Badge, Button, Card, ErrorState, FormError, Input, LoadingState, PageTitle, Textarea } from '../../components/ui'

const schema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(120),
  phone: z.string().regex(/^[0-9+\-\s]{10,20}$/, 'Enter a valid phone number'),
  address: z.string().max(500).optional(),
  locality: z.string().trim().min(1, 'Locality is required').max(120),
  yearsOfCooking: z.coerce.number().int().min(0).max(80),
  headline: z.string().max(200).optional(),
  bio: z.string().max(4000).optional(),
  signatureDishes: z.string().max(500).optional(),
  kitchenAddress: z.string().max(500).optional(),
  kitchenDescription: z.string().max(4000).optional(),
  latitude: z.union([z.literal(''), z.coerce.number().min(-90).max(90)]).optional(),
  longitude: z.union([z.literal(''), z.coerce.number().min(-180).max(180)]).optional(),
})
type Form = z.infer<typeof schema>

export default function CookProfileEdit() {
  const qc = useQueryClient()
  const profile = useQuery({ queryKey: ['cook', 'profile'], queryFn: () => api.get<CookProfile>('/cook/profile') })
  const cuisines = useQuery({ queryKey: ['cuisines'], queryFn: () => api.get<Cuisine[]>('/cuisines') })
  const [picked, setPicked] = useState<string[] | null>(null)
  const [photo, setPhoto] = useState<string | null>(null)
  const [kitchenPhotos, setKitchenPhotos] = useState<string[] | null>(null)
  const [uploadError, setUploadError] = useState<unknown>(null)
  const [fssai, setFssai] = useState({ number: '', url: '' })

  const p = profile.data
  const { register, handleSubmit, formState: { errors } } = useForm<Form>({
    resolver: zodResolver(schema) as never,
    values: p && {
      name: p.name, phone: p.phone, address: p.address ?? '', locality: p.locality ?? '', yearsOfCooking: p.yearsOfCooking ?? 0,
      headline: p.headline ?? '', bio: p.bio ?? '', signatureDishes: p.signatureDishes ?? '',
      kitchenAddress: p.kitchen?.address ?? '', kitchenDescription: p.kitchen?.description ?? '',
      latitude: p.kitchen?.latitude ?? '', longitude: p.kitchen?.longitude ?? '',
    },
  })

  const save = useMutation({
    mutationFn: (v: Form) => api.put<CookProfile>('/cook/profile', {
      ...v, latitude: v.latitude === '' ? undefined : v.latitude, longitude: v.longitude === '' ? undefined : v.longitude,
      cuisineIds: picked ?? p?.cuisines.map((c) => c.id), profilePhotoUrl: photo ?? p?.profilePhotoUrl,
      kitchenPhotos: kitchenPhotos ?? p?.kitchen?.photos,
    }),
    onSuccess: (r) => { qc.setQueryData(['cook', 'profile'], r); qc.invalidateQueries({ queryKey: ['cook', 'dashboard'] }) },
  })
  const addDoc = useMutation({
    mutationFn: () => api.post('/cook/documents', { documentType: 'FSSAI', documentNumber: fssai.number || undefined, documentUrl: fssai.url || undefined }),
    onSuccess: () => { setFssai({ number: '', url: '' }); qc.invalidateQueries({ queryKey: ['cook', 'profile'] }) },
  })

  const upload = async (file: File | undefined, folder: 'profiles' | 'kitchens' | 'documents', done: (url: string) => void) => {
    if (!file) return
    setUploadError(null)
    try { done(await api.upload(file, folder)) } catch (e) { setUploadError(e) }
  }

  if (profile.isLoading) return <CookShell><LoadingState /></CookShell>
  if (profile.error || !p) return <CookShell><ErrorState error={profile.error} onRetry={profile.refetch} /></CookShell>
  const chosen = new Set(picked ?? p.cuisines.map((c) => c.id))
  const photos = kitchenPhotos ?? p.kitchen?.photos ?? []
  const fssaiDoc = p.documents.find((d) => d.documentType === 'FSSAI')

  return (
    <CookShell>
      <PageTitle title="My profile & kitchen" subtitle="Your story is what makes customers choose you." action={<Badge tone={p.approvalStatus === 'APPROVED' ? 'green' : p.approvalStatus === 'REJECTED' ? 'red' : 'amber'}>{p.approvalStatus}</Badge>} />
      {p.approvalStatus === 'REJECTED' && <div role="alert" className="mb-4 rounded-2xl bg-red-50 p-4 text-sm text-red-800"><b>Changes requested:</b> {p.rejectionReason}. Update your profile and save to resubmit.</div>}
      <form onSubmit={handleSubmit((v) => save.mutate(v))} className="space-y-6" noValidate>
        <Card className="space-y-4 p-5">
          <h2 className="text-lg">Personal information</h2>
          <div className="flex items-center gap-4">
            <Avatar name={p.name} src={photo ?? p.profilePhotoUrl} size={72} />
            <label className="text-sm font-bold text-brand-600">Change photo<input type="file" accept="image/jpeg,image/png,image/webp" className="sr-only" onChange={(e) => upload(e.target.files?.[0], 'profiles', setPhoto)} /></label>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Name" {...register('name')} error={errors.name?.message} />
            <Input label="Mobile" type="tel" {...register('phone')} error={errors.phone?.message} />
            <Input label="Locality" {...register('locality')} error={errors.locality?.message} />
            <Input label="Years of cooking" type="number" min={0} {...register('yearsOfCooking')} error={errors.yearsOfCooking?.message} />
          </div>
          <Textarea label="Home address (private)" rows={2} {...register('address')} />
        </Card>

        <Card className="space-y-4 p-5">
          <h2 className="text-lg">Cooking profile</h2>
          <Input label="Speciality headline" placeholder="Authentic Kolhapuri Food" {...register('headline')} />
          <Textarea label="Your story" rows={5} placeholder="Cooking has been part of my family for over 25 years…" {...register('bio')} />
          <Input label="Signature dishes" placeholder="Puran Poli, Misal, Thali" hint="Comma separated" {...register('signatureDishes')} />
          <fieldset>
            <legend className="label">Cuisines</legend>
            <div className="flex flex-wrap gap-2">
              {cuisines.data?.map((c) => {
                const on = chosen.has(c.id)
                return <button type="button" key={c.id} aria-pressed={on} onClick={() => { const n = new Set(chosen); if (on) n.delete(c.id); else n.add(c.id); setPicked([...n]) }}
                  className={`rounded-full border px-3 py-1 text-sm font-bold ${on ? 'border-brand-500 bg-brand-500 text-white' : 'border-cream-300 bg-white text-ink-700'}`}>{c.name}</button>
              })}
            </div>
          </fieldset>
        </Card>

        <Card className="space-y-4 p-5">
          <h2 className="text-lg">My kitchen</h2>
          <Textarea label="Kitchen address" rows={2} hint="Only shared with customers who choose pickup, after you confirm." {...register('kitchenAddress')} />
          <Textarea label="About the kitchen" rows={3} {...register('kitchenDescription')} />
          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Latitude (optional)" inputMode="decimal" hint="Enables distance sorting" {...register('latitude')} error={errors.latitude?.message} />
            <Input label="Longitude (optional)" inputMode="decimal" {...register('longitude')} error={errors.longitude?.message} />
          </div>
          <div>
            <p className="label">Kitchen photos</p>
            <div className="flex flex-wrap gap-3">
              {photos.map((u) => <div key={u} className="relative"><img src={u} alt="Kitchen" className="h-20 w-28 rounded-xl object-cover" /><button type="button" aria-label="Remove photo" className="absolute -right-2 -top-2 rounded-full bg-white px-1.5 shadow" onClick={() => setKitchenPhotos(photos.filter((x) => x !== u))}>✕</button></div>)}
              {photos.length < 10 && <label className="flex h-20 w-28 cursor-pointer items-center justify-center rounded-xl border-2 border-dashed border-cream-300 text-sm font-bold text-brand-600">+ Add<input type="file" accept="image/jpeg,image/png,image/webp" className="sr-only" onChange={(e) => upload(e.target.files?.[0], 'kitchens', (u) => setKitchenPhotos([...photos, u]))} /></label>}
            </div>
          </div>
        </Card>
        <FormError error={uploadError} />
        <FormError error={save.error} />
        {save.isSuccess && <p role="status" className="font-bold text-leaf-600">Profile saved ✓</p>}
        <Button type="submit" loading={save.isPending}>Save profile</Button>
      </form>

      <Card className="mt-6 space-y-3 p-5">
        <h2 className="text-lg">FSSAI & documents</h2>
        <p className="text-sm text-ink-500">FSSAI is optional for now, but verified cooks earn a trust badge.</p>
        {fssaiDoc ? <p className="text-sm">FSSAI {fssaiDoc.documentNumber} · <Badge tone={fssaiDoc.status === 'APPROVED' ? 'green' : 'amber'}>{fssaiDoc.status}</Badge></p> : (
          <div className="grid gap-3 sm:grid-cols-2">
            <Input label="FSSAI number" value={fssai.number} onChange={(e) => setFssai({ ...fssai, number: e.target.value })} />
            <label className="text-sm font-bold text-brand-600">Upload certificate (PDF/image)<input type="file" accept="application/pdf,image/jpeg,image/png" className="mt-2 block text-ink-700" onChange={(e) => upload(e.target.files?.[0], 'documents', (u) => setFssai((f) => ({ ...f, url: u })))} /></label>
            <Button type="button" variant="secondary" disabled={!fssai.number && !fssai.url} loading={addDoc.isPending} onClick={() => addDoc.mutate()}>Submit document</Button>
          </div>
        )}
        <FormError error={addDoc.error} />
      </Card>
    </CookShell>
  )
}
