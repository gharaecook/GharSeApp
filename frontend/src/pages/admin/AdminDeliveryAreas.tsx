import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { GooglePlaceMap, type MapPoint, type SelectedPlace } from '../../components/GooglePlaceMap'
import { AdminShell } from '../../components/SectionNav'
import { Badge, Button, Card, EmptyState, ErrorState, FormError, Input, LoadingState, PageTitle, Select } from '../../components/ui'
import { api } from '../../lib/api'
import type { DeliveryArea, DeliveryAreaStatus } from '../../lib/types'

type DeliveryAreaRequest =
  | (SelectedPlace & { areaType: 'PLACE'; status: DeliveryAreaStatus })
  | { areaType: 'REGION'; name: string; boundary: MapPoint[]; status: DeliveryAreaStatus }

function mapPlace(area: DeliveryArea): SelectedPlace {
  return {
    placeId: area.placeId ?? '',
    name: area.name,
    formattedAddress: area.formattedAddress ?? '',
    locality: area.locality ?? '',
    city: area.city ?? '',
    latitude: area.latitude ?? 0,
    longitude: area.longitude ?? 0,
  }
}

const statusLabel: Record<DeliveryAreaStatus, string> = {
  SUPPORTED: 'Supported',
  NOT_SUPPORTED: 'Not supported',
}

export default function AdminDeliveryAreas() {
  const queryClient = useQueryClient()
  const [selected, setSelected] = useState<SelectedPlace | null>(null)
  const [drawEnabled, setDrawEnabled] = useState(false)
  const [draftRegion, setDraftRegion] = useState<MapPoint[] | null>(null)
  const [regionName, setRegionName] = useState('')
  const [newStatus, setNewStatus] = useState<DeliveryAreaStatus>('NOT_SUPPORTED')
  const areas = useQuery({
    queryKey: ['admin', 'delivery-areas'],
    queryFn: () => api.get<DeliveryArea[]>('/admin/delivery-areas'),
  })
  const mapPlaces = useMemo(() => (areas.data ?? [])
    .filter((area) => area.areaType === 'PLACE' && area.latitude != null && area.longitude != null)
    .map((area) => ({ place: mapPlace(area), status: area.status })), [areas.data])
  const mapRegions = useMemo(() => (areas.data ?? [])
    .filter((area) => area.areaType === 'REGION' && area.boundary)
    .flatMap((area) => area.boundary
      ? [{ id: area.id, name: area.name, boundary: area.boundary, status: area.status }]
      : []), [areas.data])
  const create = useMutation({
    mutationFn: (body: DeliveryAreaRequest) => api.post<DeliveryArea>('/admin/delivery-areas', body),
    onSuccess: async () => {
      setSelected(null)
      setDraftRegion(null)
      setRegionName('')
      await queryClient.invalidateQueries({ queryKey: ['admin', 'delivery-areas'] })
    },
  })
  const updateStatus = useMutation({
    mutationFn: ({ id, status }: { id: string; status: DeliveryAreaStatus }) =>
      api.put<DeliveryArea>(`/admin/delivery-areas/${id}/status`, { status }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'delivery-areas'] }),
  })
  const remove = useMutation({
    mutationFn: (id: string) => api.del(`/admin/delivery-areas/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'delivery-areas'] }),
  })

  return (
    <AdminShell>
      <PageTitle title="Delivery areas" subtitle="Add place pins or draw regions on Google Maps and mark support status." />
      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(320px,0.9fr)]">
        <Card className="space-y-4 p-5">
          <div className="flex flex-wrap gap-2">
            <Button type="button" variant={!drawEnabled ? 'primary' : 'secondary'} onClick={() => {
              setDrawEnabled(false)
              setDraftRegion(null)
            }}>Add a place pin</Button>
            <Button type="button" variant={drawEnabled ? 'primary' : 'secondary'} onClick={() => {
              setSelected(null)
              setDraftRegion(null)
              setDrawEnabled(true)
            }}>Draw a region</Button>
          </div>
          <GooglePlaceMap
            value={selected}
            places={mapPlaces}
            regions={mapRegions}
            drawingEnabled={drawEnabled}
            onBoundaryComplete={(boundary) => {
              setDraftRegion(boundary)
              setDrawEnabled(false)
              setRegionName('')
              create.reset()
            }}
            onSelect={(place) => { setSelected(place); create.reset() }}
            title="Find an area in India"
            instruction={drawEnabled
              ? 'Click around the region you want to cover. Add at least three points, then finish the boundary.'
              : 'Search for a locality like Bavdhan, Pune or Wright Town, Jabalpur, or click the map to add a place pin.'}
            mapHeight="h-[420px]"
          />
          {selected && (
            <div className="rounded-xl border border-cream-300 bg-cream-100 p-3">
              <p className="font-bold">{selected.name}</p>
              <p className="text-sm text-ink-500">{selected.formattedAddress}</p>
              <div className="mt-3 flex flex-col gap-2 sm:flex-row">
                <Select label="Area support" value={newStatus}
                  onChange={(event) => setNewStatus(event.target.value as DeliveryAreaStatus)}>
                  <option value="SUPPORTED">Supported</option>
                  <option value="NOT_SUPPORTED">Not supported</option>
                </Select>
                <Button className="self-end" loading={create.isPending} onClick={() => create.mutate({
                  areaType: 'PLACE',
                  ...selected,
                  status: newStatus,
                })}>
                  Save place
                </Button>
              </div>
            </div>
          )}
          {draftRegion && (
            <div className="space-y-3 rounded-xl border border-cream-300 bg-cream-100 p-3">
              <p className="font-bold">New drawn region</p>
              <p className="text-sm text-ink-500">{draftRegion.length} boundary points</p>
              <Input label="Region name" maxLength={160} value={regionName} placeholder="e.g. West Pune"
                onChange={(event) => setRegionName(event.target.value)} />
              <Select label="Area support" value={newStatus}
                onChange={(event) => setNewStatus(event.target.value as DeliveryAreaStatus)}>
                <option value="SUPPORTED">Supported</option>
                <option value="NOT_SUPPORTED">Not supported</option>
              </Select>
              <div className="flex gap-2">
                <Button type="button" loading={create.isPending} disabled={!regionName.trim()} onClick={() => create.mutate({
                  areaType: 'REGION',
                  name: regionName.trim(),
                  boundary: draftRegion,
                  status: newStatus,
                })}>Save region</Button>
                <Button type="button" variant="ghost" onClick={() => setDraftRegion(null)}>Discard</Button>
              </div>
            </div>
          )}
          <FormError error={create.error} />
          <div className="flex flex-wrap gap-3 text-xs text-ink-500">
            <span><span className="mr-1 inline-block h-3 w-3 rounded-full bg-green-600" />Supported</span>
            <span><span className="mr-1 inline-block h-3 w-3 rounded-full bg-amber-500" />Not supported</span>
          </div>
        </Card>

        <section aria-labelledby="saved-areas-heading">
          <div className="mb-3 flex items-center justify-between">
            <h2 id="saved-areas-heading" className="text-xl font-extrabold">Saved areas</h2>
            {areas.data && <span className="text-sm text-ink-500">{areas.data.length} total</span>}
          </div>
          {areas.isLoading ? <LoadingState /> : areas.error ? <ErrorState error={areas.error} onRetry={areas.refetch} /> : !areas.data?.length ? (
            <EmptyState title="No delivery areas yet" message="Search for your first locality on the map." />
          ) : (
            <div className="space-y-3">
              {areas.data.map((area) => (
                <Card key={area.id} className="space-y-3 p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="font-extrabold">{area.name}</p>
                      <p className="text-sm text-ink-500">{area.areaType === 'REGION'
                        ? `Drawn region · ${area.boundary?.length ?? 0} points`
                        : area.formattedAddress}</p>
                    </div>
                    <Badge tone={area.status === 'SUPPORTED' ? 'green' : 'amber'}>{statusLabel[area.status]}</Badge>
                  </div>
                  <div className="flex flex-col gap-2 sm:flex-row">
                    <Select label={`Area support for ${area.name}`} value={area.status}
                      disabled={updateStatus.isPending}
                      onChange={(event) => updateStatus.mutate({ id: area.id, status: event.target.value as DeliveryAreaStatus })}>
                      <option value="SUPPORTED">Supported</option>
                      <option value="NOT_SUPPORTED">Not supported</option>
                    </Select>
                    <Button type="button" variant="danger" className="self-end" loading={remove.isPending}
                      onClick={() => remove.mutate(area.id)}>Remove</Button>
                  </div>
                </Card>
              ))}
            </div>
          )}
          <FormError error={updateStatus.error ?? remove.error} />
        </section>
      </div>
    </AdminShell>
  )
}
