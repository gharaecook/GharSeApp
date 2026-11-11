const apiUrl = process.env.VITE_API_URL

if (!apiUrl) {
  throw new Error('Set VITE_API_URL to the HTTPS URL of your deployed backend before building the iOS app.')
}

let parsedUrl
try {
  parsedUrl = new URL(apiUrl)
} catch {
  throw new Error('VITE_API_URL must be a valid HTTPS URL.')
}

if (parsedUrl.protocol !== 'https:' || parsedUrl.hostname === 'localhost') {
  throw new Error('The iOS app requires an HTTPS backend URL reachable from the device.')
}
