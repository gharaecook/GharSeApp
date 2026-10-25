import { useEffect } from 'react'
import L from 'leaflet'
import { MapContainer, Marker, TileLayer, useMap, useMapEvents } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'

const defaultCenter: [number, number] = [18.5204, 73.8567]
const pinIcon = L.divIcon({
  className: '',
  html: '<span style="font-size:32px;line-height:36px">📍</span>',
  iconSize: [32, 36],
  iconAnchor: [16, 34],
})

function MapInteractions({ onSelect }: { onSelect: (latitude: number, longitude: number) => void }) {
  useMapEvents({
    click: ({ latlng }) => onSelect(latlng.lat, latlng.lng),
  })
  return null
}

function Recenter({ center }: { center: [number, number] }) {
  const map = useMap()
  useEffect(() => {
    map.setView(center, map.getZoom())
  }, [center, map])
  return null
}

interface KitchenLocationPickerProps {
  latitude?: number | ''
  longitude?: number | ''
  onSelect: (latitude: number, longitude: number) => void
  title?: string
  instruction?: string
}

export function KitchenLocationPicker({
  latitude,
  longitude,
  onSelect,
  title = 'Select kitchen location on map',
  instruction = 'Click the map or drag the pin to set your location.',
}: KitchenLocationPickerProps) {
  const hasLocation = typeof latitude === 'number' && Number.isFinite(latitude)
    && typeof longitude === 'number' && Number.isFinite(longitude)
  const center: [number, number] = hasLocation ? [latitude, longitude] : defaultCenter

  return (
    <div>
      <p className="label">{title}</p>
      <p className="mb-2 text-sm text-ink-500">{instruction}</p>
      <div className="h-72 overflow-hidden rounded-xl border border-cream-300" aria-label="Location selection map">
        <MapContainer center={center} zoom={hasLocation ? 15 : 11} scrollWheelZoom className="h-full w-full">
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <MapInteractions onSelect={onSelect} />
          <Recenter center={center} />
          {hasLocation && <Marker
            position={[latitude, longitude]}
            icon={pinIcon}
            draggable
            eventHandlers={{
              dragend: (event) => {
                const point = event.target.getLatLng()
                onSelect(point.lat, point.lng)
              },
            }}
          />}
        </MapContainer>
      </div>
    </div>
  )
}
