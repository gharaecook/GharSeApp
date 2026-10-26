import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { GooglePlaceMap, type SelectedPlace } from '../../components/GooglePlaceMap'
import { AdminShell } from '../../components/SectionNav'
import { Badge, Button, Card, EmptyState, ErrorState, FormError, LoadingState, PageTitle, Select } from '../../components/ui'
import { api } from '../../lib/api'
import type { DeliveryArea, DeliveryAreaStatus } from '../../lib/types'

function mapPlace(area: DeliveryArea): SelectedPlace {
  return {
    placeId: area.placeId,
    name: area.name,
    formattedAddress: area.formattedAddress,
    locality: area.locality,
    city: area.city,
    latitude: area.latitude,
    longitude: area.longitude,
  }
}

const statusLabel: Record<DeliveryAreaStatus, string> = {
  DELIVERING: 'Delivering',
  NOT_STARTED: 'Not started',
}

export default function AdminDeliveryAreas() {
  const queryClient = useQueryClient()
  const [selected, setSelected] = useState<SelectedPlace | null>(null)
  const [newStatus, setNewStatus] = useState<DeliveryAreaStatus>('NOT_STARTED')
  const areas = useQuery({
    queryKey: ['admin', 'delivery-areas'],
    queryFn: () => api.get<DeliveryArea[]>('/admin/delivery-areas'),
  })
  const mapPlaces = useMemo(() => (areas.data ?? []).map((area) => ({ place: mapPlace(area), status: area.status })), [areas.data])
  const create = useMutation({
    mutationFn: (place: SelectedPlace) => api.post<DeliveryArea>('/admin/delivery-areas', {
      ...place,
      status: newStatus,
    }),
    onSuccess: async () => {
      setSelected(null)
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
      <PageTitle title="Delivery areas" subtitle="Choose Indian localities on Google Maps and mark whether delivery has started." />
      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(320px,0.9fr)]">
        <Card className="space-y-4 p-5">
          <GooglePlaceMap
            value={selected}
            places={mapPlaces}
            onSelect={(place) => { setSelected(place); create.reset() }}
            title="Find an area in India"
            instruction="Search for a locality like Bavdhan, Pune or Wright Town, Jabalpur, then add it to the map."
            mapHeight="h-[420px]"
          />
          {selected && (
            <div className="rounded-xl border border-cream-300 bg-cream-100 p-3">
              <p className="font-bold">{selected.name}</p>
              <p className="text-sm text-ink-500">{selected.formattedAddress}</p>
              <div className="mt-3 flex flex-col gap-2 sm:flex-row">
                <Select label="Delivery status" value={newStatus}
                  onChange={(event) => setNewStatus(event.target.value as DeliveryAreaStatus)}>
                  <option value="DELIVERING">Delivering</option>
                  <option value="NOT_STARTED">Not started yet</option>
                </Select>
                <Button className="self-end" loading={create.isPending} onClick={() => create.mutate(selected)}>
                  Add area
                </Button>
              </div>
            </div>
          )}
          <FormError error={create.error} />
          <p className="text-xs text-ink-500">Areas are saved as Google place locations (markers), not drawn boundary polygons.</p>
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
                      <p className="text-sm text-ink-500">{area.formattedAddress}</p>
                    </div>
                    <Badge tone={area.status === 'DELIVERING' ? 'green' : 'amber'}>{statusLabel[area.status]}</Badge>
                  </div>
                  <div className="flex flex-col gap-2 sm:flex-row">
                    <Select label={`Delivery status for ${area.name}`} value={area.status}
                      disabled={updateStatus.isPending}
                      onChange={(event) => updateStatus.mutate({ id: area.id, status: event.target.value as DeliveryAreaStatus })}>
                      <option value="DELIVERING">Delivering</option>
                      <option value="NOT_STARTED">Not started yet</option>
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
