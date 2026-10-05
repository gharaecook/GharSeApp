import type { DishCategory, OrderStatusValue, OrderType } from './types'

export const rupees = (n: number | undefined | null) =>
  `₹${Number(n ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`

export function formatDate(iso: string, opts: Intl.DateTimeFormatOptions = { weekday: 'long', day: 'numeric', month: 'short' }) {
  // Date-only strings (yyyy-mm-dd) must be parsed as local dates, not UTC.
  const d = /^\d{4}-\d{2}-\d{2}$/.test(iso) ? new Date(`${iso}T00:00:00`) : new Date(iso)
  return d.toLocaleDateString('en-IN', opts)
}

export function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' })
}

/** "12:00:00" -> "12 PM" / "12:30 PM" */
export function formatTime(t?: string) {
  if (!t) return ''
  const [h, m] = t.split(':').map(Number)
  const suffix = h >= 12 ? 'PM' : 'AM'
  const hr = h % 12 === 0 ? 12 : h % 12
  return m ? `${hr}:${String(m).padStart(2, '0')} ${suffix}` : `${hr} ${suffix}`
}
export const timeWindow = (a?: string, b?: string) => (a && b ? `${formatTime(a)} – ${formatTime(b)}` : '')

export const orderTypeLabel: Record<OrderType, string> = {
  PICKUP: 'Pickup',
  COOK_DELIVERY: 'Delivery by cook',
  THIRD_PARTY_DELIVERY: 'Courier delivery',
}

export const statusLabel: Record<OrderStatusValue, string> = {
  CREATED: 'Order placed', CONFIRMED: 'Confirmed', PREPARING: 'Preparing', READY: 'Ready',
  OUT_FOR_DELIVERY: 'Out for delivery', READY_FOR_PICKUP: 'Ready for pickup', DELIVERED: 'Delivered',
  PICKED_UP: 'Picked up', COMPLETED: 'Completed', CANCELLED: 'Cancelled',
}

export const categoryEmoji: Record<DishCategory, string> = {
  MEAL: '🍛', THALI: '🍱', SNACK: '🥟', SWEET: '🍮', BAKERY: '🎂', BEVERAGE: '🥛', OTHER: '🍽️',
}
export const categoryLabel: Record<DishCategory, string> = {
  MEAL: 'Meals', THALI: 'Thali', SNACK: 'Snacks', SWEET: 'Sweets', BAKERY: 'Bakery', BEVERAGE: 'Beverages', OTHER: 'Other',
}

/** Steps shown on the order tracker for a given fulfilment type. */
export function trackerSteps(type: OrderType): OrderStatusValue[] {
  return type === 'PICKUP'
    ? ['CREATED', 'CONFIRMED', 'PREPARING', 'READY', 'READY_FOR_PICKUP', 'PICKED_UP', 'COMPLETED']
    : ['CREATED', 'CONFIRMED', 'PREPARING', 'READY', 'OUT_FOR_DELIVERY', 'DELIVERED', 'COMPLETED']
}

export const PUNE_LOCALITIES = ['Kothrud', 'Baner', 'Aundh', 'Viman Nagar', 'Koregaon Park', 'Hadapsar', 'Wakad', 'Shivajinagar']
