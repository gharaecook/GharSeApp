import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { GooglePlaceMap } from '../components/GooglePlaceMap'

afterEach(() => { vi.unstubAllEnvs() })

describe('GooglePlaceMap', () => {
  it('shows setup instructions when the Google Maps key is missing', async () => {
    vi.stubEnv('VITE_GOOGLE_MAPS_API_KEY', '')
    render(<GooglePlaceMap onSelect={() => {}} />)

    expect(await screen.findByRole('alert')).toHaveTextContent(/VITE_GOOGLE_MAPS_API_KEY/)
  })
})
