export type Role = 'CUSTOMER' | 'COOK' | 'ADMIN'
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type OrderType = 'PICKUP' | 'COOK_DELIVERY' | 'THIRD_PARTY_DELIVERY'
export type OrderStatusValue =
  | 'CREATED' | 'CONFIRMED' | 'PREPARING' | 'READY' | 'OUT_FOR_DELIVERY'
  | 'READY_FOR_PICKUP' | 'DELIVERED' | 'PICKED_UP' | 'COMPLETED' | 'CANCELLED'
export type DishCategory = 'MEAL' | 'THALI' | 'SNACK' | 'SWEET' | 'BAKERY' | 'BEVERAGE' | 'OTHER'

export interface User {
  id: string; name: string; email: string; phone: string; role: Role; status: string
  address?: string; cookId?: string; createdAt: string
}
export interface AuthResponse { accessToken: string; refreshToken: string; expiresIn: number; user: User }

export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }

export interface CookSummary {
  id: string; name: string; headline?: string; locality?: string; city?: string; profilePhotoUrl?: string
  yearsOfCooking?: number; rating: number; reviewCount: number; cuisines: string[]; signatureDishes: string[]
  storySnippet?: string; availableToday: boolean; distanceKm?: number; verified: boolean; joinedAt: string
}
export interface CookDetail {
  summary: CookSummary; bio?: string; kitchenDescription?: string; kitchenPhotos: string[]
  fssaiVerified: boolean; kitchenVerified: boolean; favorite?: boolean
}
export interface Cuisine { id: string; name: string }

export interface Dish {
  id: string; cookId: string; cookName: string; name: string; description?: string; category: DishCategory
  cuisineId?: string; cuisine?: string; imageUrl?: string; price: number; portionDescription?: string
  preparationTimeMinutes?: number; ingredients?: string; allergens?: string; vegetarian: boolean; active: boolean
}
export interface BatchDish { dishId: string; name: string; imageUrl?: string; price: number; vegetarian: boolean; portionDescription?: string }
export interface Batch {
  id: string; cookId: string; cookName: string; cookLocality?: string; name: string; description?: string
  availableDate: string; orderCutoff: string; capacity: number; reservedQuantity: number; remainingQuantity: number
  soldOut: boolean; orderingOpen: boolean; pickupStart?: string; pickupEnd?: string; deliveryStart?: string
  deliveryEnd?: string; status: 'OPEN' | 'CLOSED' | 'CANCELLED'; dishes: BatchDish[]
}

export interface CartItem {
  id: string; dishId: string; dishName: string; imageUrl?: string; batchId: string; batchName: string
  availableDate: string; unitPrice: number; quantity: number; lineTotal: number; remaining: number; orderingOpen: boolean
}
export interface Cart { id?: string; cookId?: string; cookName?: string; items: CartItem[]; subtotal: number; itemCount: number }

export interface OrderItem {
  dishId: string; dishName: string; batchId: string; batchName: string; availableDate: string
  pickupStart?: string; pickupEnd?: string; deliveryStart?: string; deliveryEnd?: string
  unitPrice: number; quantity: number; totalPrice: number
}
export interface Order {
  id: string; orderNumber: string; cookId: string; cookName: string; cookLocality?: string
  customerId: string; customerName: string; customerPhone?: string; items: OrderItem[]
  subtotal: number; deliveryFee: number; totalAmount: number; orderType: OrderType; paymentStatus: string
  orderStatus: OrderStatusValue; deliveryAddress?: string; pickupAddress?: string; deliveryProvider?: string
  trackingUrl?: string; canCancel: boolean; reviewed: boolean; createdAt: string; updatedAt: string
  paymentCheckout?: { provider: string; keyId: string; orderId: string; amount: number; currency: string }
}

export interface Review {
  id: string; orderId: string; customerName: string; rating: number; tasteRating?: number; hygieneRating?: number
  packagingRating?: number; reliabilityRating?: number; comment?: string; createdAt: string
}

export interface Kitchen {
  address?: string; locality?: string; latitude?: number; longitude?: number; description?: string
  verificationStatus: ApprovalStatus; photos: string[]
}
export interface CookDocument {
  id: string; documentType: string; documentNumber?: string; documentUrl?: string; status: ApprovalStatus; expiryDate?: string
}
export interface CookProfile {
  id: string; userId: string; name: string; email: string; phone: string; address?: string; headline?: string
  bio?: string; yearsOfCooking?: number; locality?: string; city?: string; profilePhotoUrl?: string
  signatureDishes?: string; approvalStatus: ApprovalStatus; rejectionReason?: string; rating: number
  reviewCount: number; cuisines: Cuisine[]; kitchen?: Kitchen; documents: CookDocument[]; createdAt: string
}
export interface CookDashboard {
  approvalStatus: ApprovalStatus; todaysOrders: number; upcomingOrders: number; pendingActions: number
  activeBatches: number; totalEarnings: number; earningsThisMonth: number
}
export interface Earnings {
  totalEarnings: number; pendingEarnings: number; earningsToday: number; earningsThisMonth: number
  completedOrders: number; recent: { orderId: string; orderNumber: string; amount: number; completedAt: string }[]
}

export interface AdminDashboard {
  totalUsers: number; totalCustomers: number; totalCooks: number; pendingCooks: number; approvedCooks: number
  rejectedCooks: number; totalOrders: number; completedOrders: number; revenue: number
}
export interface AdminCookRow {
  id: string; userId: string; name: string; email: string; phone: string; locality?: string
  approvalStatus: ApprovalStatus; rating: number; reviewCount: number; createdAt: string
}
export interface AdminCookDetail { profile: CookProfile; dishes: Dish[] }

export interface NotificationItem { id: string; type: string; title: string; message: string; read: boolean; createdAt: string }
