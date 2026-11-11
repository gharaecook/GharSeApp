import { importLibrary, setOptions } from '@googlemaps/js-api-loader'

let configured = false

export async function loadGoogleMaps() {
  const key = import.meta.env.VITE_GOOGLE_MAPS_API_KEY
  if (!key) {
    throw new Error('Google Maps is not configured. Set VITE_GOOGLE_MAPS_API_KEY and enable Maps JavaScript API and Places API.')
  }
  if (!configured) {
    setOptions({ key, v: 'weekly' })
    configured = true
  }
  const [maps, places, geocoding] = await Promise.all([
    importLibrary('maps'),
    importLibrary('places'),
    importLibrary('geocoding'),
  ])
  return { maps, places, geocoding }
}
