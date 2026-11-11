import { Capacitor } from '@capacitor/core'
import { Geolocation } from '@capacitor/geolocation'

interface LocationOptions {
  enableHighAccuracy?: boolean
  timeout?: number
  maximumAge?: number
}

export class LocationPermissionDeniedError extends Error {}

export async function getCurrentLocation(options: LocationOptions = {}): Promise<{ latitude: number; longitude: number }> {
  if (Capacitor.isNativePlatform()) {
    const permission = await Geolocation.checkPermissions()
    if (permission.location !== 'granted') {
      const requested = await Geolocation.requestPermissions({ permissions: ['location'] })
      if (requested.location !== 'granted') throw new LocationPermissionDeniedError()
    }
    const position = await Geolocation.getCurrentPosition(options)
    return { latitude: position.coords.latitude, longitude: position.coords.longitude }
  }

  if (!navigator.geolocation) throw new Error('Location is not supported in this browser.')
  try {
    const position = await new Promise<GeolocationPosition>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, options)
    })
    return { latitude: position.coords.latitude, longitude: position.coords.longitude }
  } catch (error) {
    if (typeof GeolocationPositionError !== 'undefined'
      && error instanceof GeolocationPositionError
      && error.code === error.PERMISSION_DENIED) {
      throw new LocationPermissionDeniedError()
    }
    throw error
  }
}
