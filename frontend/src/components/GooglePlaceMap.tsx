import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { loadGoogleMaps } from '../lib/googleMaps'
import { Button } from './ui'

export interface SelectedPlace {
  placeId: string
  name: string
  formattedAddress: string
  locality: string
  city: string
  latitude: number
  longitude: number
}

export interface MapPoint {
  latitude: number
  longitude: number
}

export interface MapRegion {
  id: string
  name: string
  boundary: MapPoint[]
  status: string
}

interface GooglePlaceMapProps {
  value?: SelectedPlace | null
  coordinates?: { latitude: number; longitude: number } | null
  places?: Array<{ place: SelectedPlace; status?: string }>
  regions?: MapRegion[]
  drawingEnabled?: boolean
  onBoundaryComplete?: (boundary: MapPoint[]) => void
  onSelect: (place: SelectedPlace) => void
  title?: string
  instruction?: string
  mapHeight?: string
}

const indiaCenter = { lat: 22.5937, lng: 78.9629 }
const indiaBounds = { north: 37.2, south: 6.4, east: 97.5, west: 68.0 }
const emptyPlaces: Array<{ place: SelectedPlace; status?: string }> = []
const emptyRegions: MapRegion[] = []

function componentName(component: { types: string[]; long_name?: string | null; longText?: string | null } | undefined) {
  return component?.longText ?? component?.long_name ?? ''
}

function addressPart(components: Array<{ types: string[]; long_name?: string | null; longText?: string | null }>, type: string) {
  return componentName(components.find((component) => component.types.includes(type)))
}

function createRegionPolygon(map: google.maps.Map, region: MapRegion) {
  const supported = region.status === 'SUPPORTED'
  const polygon = new google.maps.Polygon({
    map,
    paths: region.boundary.map((point) => ({ lat: point.latitude, lng: point.longitude })),
    strokeColor: supported ? '#15803d' : '#b45309',
    strokeOpacity: 0.9,
    strokeWeight: 2,
    fillColor: supported ? '#22c55e' : '#f59e0b',
    fillOpacity: 0.22,
  })
  polygon.addListener('click', (event: google.maps.PolyMouseEvent) => {
    const content = document.createElement('div')
    const name = document.createElement('strong')
    name.textContent = region.name
    const status = document.createElement('div')
    status.textContent = supported ? 'Supported' : 'Not supported'
    content.append(name, status)
    new google.maps.InfoWindow({ content, position: event.latLng }).open({ map })
  })
  return polygon
}

function selectedPlace(
  fields: {
    placeId: string
    name: string
    formattedAddress: string
    location: google.maps.LatLngLiteral
    components: Array<{ types: string[]; long_name?: string | null; longText?: string | null }>
  },
): SelectedPlace {
  const locality = addressPart(fields.components, 'sublocality_level_1')
    || addressPart(fields.components, 'neighborhood')
    || addressPart(fields.components, 'locality')
    || addressPart(fields.components, 'administrative_area_level_3')
  const city = addressPart(fields.components, 'locality')
    || addressPart(fields.components, 'administrative_area_level_2')
    || addressPart(fields.components, 'administrative_area_level_3')
  const readableName = fields.name || locality || city || fields.formattedAddress
  return {
    placeId: fields.placeId,
    name: readableName,
    formattedAddress: fields.formattedAddress || readableName,
    locality: locality || readableName,
    city: city || locality || readableName,
    latitude: fields.location.lat,
    longitude: fields.location.lng,
  }
}

export function GooglePlaceMap({
  value,
  coordinates,
  places = emptyPlaces,
  regions = emptyRegions,
  drawingEnabled = false,
  onBoundaryComplete,
  onSelect,
  title = 'Search an area in India',
  instruction = 'Search a locality or city, or click the map to select a nearby place.',
  mapHeight = 'h-64',
}: GooglePlaceMapProps) {
  const mapElement = useRef<HTMLDivElement>(null)
  const mapRef = useRef<google.maps.Map | null>(null)
  const markerRef = useRef<google.maps.Marker | null>(null)
  const savedMarkersRef = useRef<google.maps.Marker[]>([])
  const placesRefForMarkers = useRef(places)
  const regionsRef = useRef(regions)
  const drawingEnabledRef = useRef(drawingEnabled)
  const onBoundaryCompleteRef = useRef(onBoundaryComplete)
  const savedPolygonsRef = useRef<google.maps.Polygon[]>([])
  const draftPolygonRef = useRef<google.maps.Polygon | null>(null)
  const geocoderRef = useRef<google.maps.Geocoder | null>(null)
  const placesRef = useRef<google.maps.PlacesLibrary | null>(null)
  const tokenRef = useRef<google.maps.places.AutocompleteSessionToken | null>(null)
  const onSelectRef = useRef(onSelect)
  const initialLocationRef = useRef({ value, coordinates })
  const [query, setQuery] = useState(value?.name ?? '')
  const [suggestions, setSuggestions] = useState<google.maps.places.PlacePrediction[]>([])
  const [loading, setLoading] = useState(false)
  const [selecting, setSelecting] = useState(false)
  const [error, setError] = useState('')
  const [draftBoundary, setDraftBoundary] = useState<MapPoint[]>([])

  useEffect(() => { onSelectRef.current = onSelect }, [onSelect])
  useEffect(() => { placesRefForMarkers.current = places }, [places])
  useEffect(() => { regionsRef.current = regions }, [regions])
  useEffect(() => { drawingEnabledRef.current = drawingEnabled }, [drawingEnabled])
  useEffect(() => { onBoundaryCompleteRef.current = onBoundaryComplete }, [onBoundaryComplete])

  useEffect(() => {
    let cancelled = false
    let mapClickListener: google.maps.MapsEventListener | undefined
    setLoading(true)
    loadGoogleMaps().then(({ maps, places, geocoding }) => {
      if (cancelled || !mapElement.current) return
      const initialValue = initialLocationRef.current.value
      const initialCoordinates = initialLocationRef.current.coordinates
      placesRef.current = places
      tokenRef.current = new places.AutocompleteSessionToken()
      const initialCenter = initialValue
        ? { lat: initialValue.latitude, lng: initialValue.longitude }
        : initialCoordinates
          ? { lat: initialCoordinates.latitude, lng: initialCoordinates.longitude }
          : indiaCenter
      const map = new maps.Map(mapElement.current, {
        center: initialCenter,
        zoom: initialValue || initialCoordinates ? 13 : 5,
        restriction: { latLngBounds: indiaBounds, strictBounds: true },
        mapTypeControl: false,
        streetViewControl: false,
        fullscreenControl: false,
      })
      mapRef.current = map
      geocoderRef.current = new geocoding.Geocoder()
      markerRef.current = new google.maps.Marker({ map, draggable: false })
      if (initialValue || initialCoordinates) markerRef.current?.setPosition(initialCenter)
      savedMarkersRef.current = placesRefForMarkers.current.map(({ place, status }) => new google.maps.Marker({
        map,
        position: { lat: place.latitude, lng: place.longitude },
        title: `${place.name}${status ? ` — ${status === 'SUPPORTED' ? 'Supported' : 'Not supported'}` : ''}`,
        icon: {
          path: google.maps.SymbolPath.CIRCLE,
          scale: 7,
          fillColor: status === 'SUPPORTED' ? '#16a34a' : '#f59e0b',
          fillOpacity: 1,
          strokeColor: '#ffffff',
          strokeWeight: 2,
        },
      }))
      savedPolygonsRef.current = regionsRef.current.map((region) => createRegionPolygon(map, region))
      mapClickListener = map.addListener('click', async (event: google.maps.MapMouseEvent) => {
        const clickedLocation = event.latLng
        if (!clickedLocation) return
        if (drawingEnabledRef.current) {
          setDraftBoundary((current) => [...current, { latitude: clickedLocation.lat(), longitude: clickedLocation.lng() }])
          return
        }
        if (!geocoderRef.current) return
        setError('')
        try {
          const { results } = await geocoderRef.current.geocode({ location: clickedLocation })
          const result = results.find((entry) => entry.place_id && entry.address_components.length > 0)
          if (!result) {
            setError('Could not identify that place. Search for a locality or city instead.')
            return
          }
          const place = selectedPlace({
            placeId: result.place_id,
            name: result.address_components.find((part) =>
              part.types.some((type) => ['sublocality_level_1', 'neighborhood', 'locality'].includes(type)),
            )?.long_name ?? '',
            formattedAddress: result.formatted_address,
            location: { lat: clickedLocation.lat(), lng: clickedLocation.lng() },
            components: result.address_components,
          })
          markerRef.current?.setPosition(clickedLocation)
          onSelectRef.current(place)
          setQuery(place.name)
          setSuggestions([])
        } catch {
          setError('Google Maps could not identify that place. Try searching by locality or city.')
        }
      })
      setLoading(false)
    }).catch((loadError: unknown) => {
      if (!cancelled) {
        setError(loadError instanceof Error ? loadError.message : 'Google Maps could not be loaded.')
        setLoading(false)
      }
    })
    return () => {
      cancelled = true
      mapClickListener?.remove()
      savedMarkersRef.current.forEach((marker) => marker.setMap(null))
      savedMarkersRef.current = []
      savedPolygonsRef.current.forEach((polygon) => polygon.setMap(null))
      savedPolygonsRef.current = []
      draftPolygonRef.current?.setMap(null)
      draftPolygonRef.current = null
      mapRef.current = null
      markerRef.current = null
      geocoderRef.current = null
      placesRef.current = null
    }
  }, [])

  useEffect(() => {
    if (!mapRef.current) return
    savedMarkersRef.current.forEach((marker) => marker.setMap(null))
    savedMarkersRef.current = places.map(({ place, status }) => new google.maps.Marker({
      map: mapRef.current,
      position: { lat: place.latitude, lng: place.longitude },
      title: `${place.name}${status ? ` — ${status === 'SUPPORTED' ? 'Supported' : 'Not supported'}` : ''}`,
      icon: {
        path: google.maps.SymbolPath.CIRCLE,
        scale: 7,
        fillColor: status === 'SUPPORTED' ? '#16a34a' : '#f59e0b',
        fillOpacity: 1,
        strokeColor: '#ffffff',
        strokeWeight: 2,
      },
    }))
  }, [places])

  useEffect(() => {
    const map = mapRef.current
    if (!map) return
    savedPolygonsRef.current.forEach((polygon) => polygon.setMap(null))
    savedPolygonsRef.current = regions.map((region) => createRegionPolygon(map, region))
  }, [regions])

  useEffect(() => {
    if (!mapRef.current) return
    draftPolygonRef.current?.setMap(null)
    draftPolygonRef.current = draftBoundary.length
      ? new google.maps.Polygon({
        map: mapRef.current,
        paths: draftBoundary.map((point) => ({ lat: point.latitude, lng: point.longitude })),
        strokeColor: '#2563eb',
        strokeOpacity: 1,
        strokeWeight: 3,
        fillColor: '#3b82f6',
        fillOpacity: 0.2,
        clickable: false,
      })
      : null
  }, [draftBoundary])

  useEffect(() => {
    if (drawingEnabled) {
      setDraftBoundary([])
      setError('')
    }
  }, [drawingEnabled])

  useEffect(() => {
    const position = value
      ? { lat: value.latitude, lng: value.longitude }
      : coordinates
        ? { lat: coordinates.latitude, lng: coordinates.longitude }
        : null
    if (!position || !mapRef.current || !markerRef.current) return
    mapRef.current.panTo(position)
    mapRef.current.setZoom(Math.max(mapRef.current.getZoom() ?? 5, 13))
    markerRef.current.setPosition(position)
    if (value) setQuery(value.name)
  }, [value, coordinates])

  useEffect(() => {
    const input = query.trim()
    const places = placesRef.current
    if (!input || !places || input === value?.name) {
      setSuggestions([])
      return
    }
    let cancelled = false
    const timer = window.setTimeout(() => {
      places.AutocompleteSuggestion.fetchAutocompleteSuggestions({
        input,
        includedRegionCodes: ['in'],
        sessionToken: tokenRef.current ?? undefined,
      }).then(({ suggestions: results }) => {
        if (!cancelled) setSuggestions(results.flatMap((suggestion) => suggestion.placePrediction ?? []))
      }).catch(() => {
        if (!cancelled) setError('Could not search Google Maps. Check your connection and try again.')
      })
    }, 250)
    return () => {
      cancelled = true
      window.clearTimeout(timer)
    }
  }, [query, value?.name])

  const selectSuggestion = async (prediction: google.maps.places.PlacePrediction) => {
    setSelecting(true)
    setError('')
    try {
      const place = prediction.toPlace()
      await place.fetchFields({ fields: ['id', 'displayName', 'formattedAddress', 'location', 'addressComponents'] })
      if (!place.location || !place.id) throw new Error('This result does not have a selectable map location.')
      const selection = selectedPlace({
        placeId: place.id,
        name: place.displayName ?? prediction.mainText?.text ?? '',
        formattedAddress: place.formattedAddress ?? prediction.text.toString(),
        location: { lat: place.location.lat(), lng: place.location.lng() },
        components: place.addressComponents ?? [],
      })
      mapRef.current?.panTo({ lat: selection.latitude, lng: selection.longitude })
      mapRef.current?.setZoom(13)
      markerRef.current?.setPosition({ lat: selection.latitude, lng: selection.longitude })
      onSelectRef.current(selection)
      setQuery(selection.name)
      setSuggestions([])
      tokenRef.current = placesRef.current ? new placesRef.current.AutocompleteSessionToken() : null
    } catch (selectionError) {
      setError(selectionError instanceof Error ? selectionError.message : 'Could not select that place.')
    } finally {
      setSelecting(false)
    }
  }

  const submitSearch = (event: FormEvent) => {
    event.preventDefault()
    if (suggestions[0]) void selectSuggestion(suggestions[0])
  }

  return (
    <section aria-label={title} className="space-y-2">
      <div>
        <label className="label" htmlFor="google-place-search">{title}</label>
        <p className="mb-2 text-sm text-ink-500">{instruction}</p>
      </div>
      <form className="relative" onSubmit={submitSearch}>
        <input
          id="google-place-search"
          className="input"
          autoComplete="off"
          placeholder="e.g. Bavdhan, Pune or Wright Town, Jabalpur"
          value={query}
          onChange={(event) => { setQuery(event.target.value); setError('') }}
          aria-label={title}
          aria-expanded={suggestions.length > 0}
          aria-controls="google-place-suggestions"
        />
        {suggestions.length > 0 && (
          <ul id="google-place-suggestions" role="listbox" className="absolute z-[1000] mt-1 max-h-56 w-full overflow-y-auto rounded-xl border border-cream-300 bg-white shadow-lg">
            {suggestions.map((prediction) => (
              <li key={prediction.placeId} role="option" aria-selected={false}>
                <button type="button" className="w-full px-3 py-2 text-left text-sm hover:bg-cream-100 disabled:opacity-50"
                  disabled={selecting} onClick={() => void selectSuggestion(prediction)}>
                  <span className="block font-bold">{prediction.mainText?.text ?? prediction.text.toString()}</span>
                  {prediction.secondaryText?.text && <span className="block text-xs text-ink-500">{prediction.secondaryText.text}</span>}
                </button>
              </li>
            ))}
          </ul>
        )}
        <Button type="submit" variant="secondary" className="mt-2 w-full" disabled={selecting || !suggestions.length}>
          {selecting ? 'Selecting…' : 'Search this area'}
        </Button>
      </form>
      <div ref={mapElement} className={`${mapHeight} overflow-hidden rounded-xl border border-cream-300 bg-cream-100`} aria-label="Google map" />
      {drawingEnabled && (
        <div className="flex flex-wrap items-center gap-2">
          <span className="mr-auto text-xs text-ink-500">
            {draftBoundary.length} boundary points; add at least 3
            {draftBoundary.length >= 3 && ' (the final edge closes automatically)'}
          </span>
          <Button type="button" variant="ghost" disabled={!draftBoundary.length}
            onClick={() => setDraftBoundary((current) => current.slice(0, -1))}>Undo point</Button>
          <Button type="button" variant="secondary" disabled={draftBoundary.length < 3}
            onClick={() => {
              onBoundaryCompleteRef.current?.(draftBoundary)
              setDraftBoundary([])
            }}>Finish boundary</Button>
          <Button type="button" variant="ghost" onClick={() => setDraftBoundary([])}>Clear drawing</Button>
        </div>
      )}
      {loading && <p role="status" className="text-xs text-ink-500">Loading Google Maps…</p>}
      {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
    </section>
  )
}
