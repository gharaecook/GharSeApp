import { useMemo } from 'react'
import { GooglePlaceMap } from './GooglePlaceMap'

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
  instruction = 'Search for a locality, click the map, or drag the pin to set your location.',
}: KitchenLocationPickerProps) {
  const coordinates = useMemo(() => {
    if (typeof latitude !== 'number' || typeof longitude !== 'number') return null
    return { latitude, longitude }
  }, [latitude, longitude])

  return (
    <GooglePlaceMap
      coordinates={coordinates}
      onSelect={(place) => onSelect(place.latitude, place.longitude)}
      title={title}
      instruction={instruction}
      mapHeight="h-72"
    />
  )
}
