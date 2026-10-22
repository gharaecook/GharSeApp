import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { formatDate, formatDateTime, orderTypeLabel, rupees, timeWindow } from '../lib/format'
import type { Cart, Order, Page } from '../lib/types'
import { OrderStatusBadge, OrderTracker } from '../components/cards'
import { Badge, Button, Card, EmptyState, ErrorState, FormError, LoadingState, Modal, PageTitle, StarInput, Textarea } from '../components/ui'

type RazorpayCheckoutResponse = {
  razorpay_order_id: string
  razorpay_payment_id: string
  razorpay_signature: string
}

type RazorpayOptions = {
  key: string
  amount: number
  currency: string
  name: string
  description: string
  order_id: string
  handler: (response: RazorpayCheckoutResponse) => void
  modal: { ondismiss: () => void }
}

declare global {
  interface Window {
    Razorpay?: new (options: RazorpayOptions) => { open: () => void }
  }
}

function loadRazorpay(): Promise<void> {
  if (window.Razorpay) return Promise.resolve()
  return new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>('script[src="https://checkout.razorpay.com/v1/checkout.js"]')
    const script = existing ?? document.createElement('script')
    script.src = 'https://checkout.razorpay.com/v1/checkout.js'
    const onLoad = () => {
      script.dataset.loaded = 'true'
      if (window.Razorpay) resolve()
      else reject(new Error('Razorpay checkout could not be initialized'))
    }
    const onError = () => {
      script.remove()
      reject(new Error('Could not load Razorpay checkout. Check your connection and try again.'))
    }
    if (existing?.dataset.loaded === 'true') {
      reject(new Error('Razorpay checkout could not be initialized'))
    } else {
      script.addEventListener('load', onLoad, { once: true })
      script.addEventListener('error', onError, { once: true })
      if (!existing) document.body.appendChild(script)
    }
  })
}

export function Orders() {
  const orders = useQuery({ queryKey: ['orders'], queryFn: () => api.get<Page<Order>>('/orders?size=50') })
  if (orders.isLoading) return <LoadingState />
  if (orders.error) return <ErrorState error={orders.error} onRetry={orders.refetch} />
  const list = orders.data!.content
  return (
    <div className="container-page py-8">
      <PageTitle title="Your orders" />
      {!list.length ? <EmptyState emoji="🧾" title="No orders yet" message="Your homemade food orders will show up here." action={<Link to="/cooks" className="font-bold text-brand-600">Find a Home Cook</Link>} /> : (
        <ul className="space-y-3">
          {list.map((o) => (
            <li key={o.id}>
              <Link to={`/orders/${o.id}`} className="card block p-4 transition hover:shadow-lg">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div><p className="font-extrabold">#{o.orderNumber} · {o.cookName}</p><p className="text-sm text-ink-500">{o.items.map((i) => `${i.quantity} × ${i.dishName}`).join(', ')}</p></div>
                  <div className="text-right"><OrderStatusBadge status={o.orderStatus} /><p className="mt-1 font-extrabold">{rupees(o.totalAmount)}</p></div>
                </div>
                <p className="mt-1 text-xs text-ink-500">{formatDateTime(o.createdAt)}</p>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export function OrderDetail() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const placed = (useLocation().state as { placed?: boolean } | null)?.placed
  const qc = useQueryClient()
  const [reviewOpen, setReviewOpen] = useState(false)
  const [openingPayment, setOpeningPayment] = useState(false)
  const [paymentError, setPaymentError] = useState('')
  const order = useQuery({ queryKey: ['order', id], queryFn: () => api.get<Order>(`/orders/${id}`), refetchInterval: 15_000 })
  const cancel = useMutation({
    mutationFn: () => api.post<Order>(`/orders/${id}/cancel`),
    onSuccess: (o) => { qc.setQueryData(['order', id], o); qc.invalidateQueries({ queryKey: ['orders'] }) },
  })
  const reorder = useMutation({
    mutationFn: async (o: Order) => {
      await api.del('/cart')
      let last: Cart | undefined
      for (const i of o.items) last = await api.post<Cart>('/cart/items', { dishId: i.dishId, batchId: i.batchId, quantity: i.quantity })
      return last
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cart'] }); navigate('/cart') },
  })
  const verifyPayment = useMutation({
    mutationFn: (response: RazorpayCheckoutResponse) => api.post<Order>(`/orders/${id}/payment/verify`, {
      razorpayOrderId: response.razorpay_order_id,
      razorpayPaymentId: response.razorpay_payment_id,
      razorpaySignature: response.razorpay_signature,
    }),
    onSuccess: (paidOrder) => {
      qc.setQueryData(['order', id], paidOrder)
      qc.invalidateQueries({ queryKey: ['orders'] })
      setPaymentError('')
      setOpeningPayment(false)
    },
    onError: (error) => {
      setPaymentError(error instanceof Error ? error.message : 'Payment verification failed. Please try again.')
      setOpeningPayment(false)
    },
  })

  if (order.isLoading) return <LoadingState />
  if (order.error) return <ErrorState error={order.error} onRetry={order.refetch} />
  const o = order.data!

  const startPayment = async () => {
    const checkout = o.paymentCheckout
    if (!checkout) return
    setPaymentError('')
    setOpeningPayment(true)
    try {
      await loadRazorpay()
      if (!window.Razorpay) throw new Error('Razorpay checkout could not be initialized')
      const payment = new window.Razorpay({
        key: checkout.keyId,
        amount: checkout.amount,
        currency: checkout.currency,
        name: 'GharSe',
        description: `Payment for order #${o.orderNumber}`,
        order_id: checkout.orderId,
        handler: (response) => { verifyPayment.mutate(response) },
        modal: { ondismiss: () => setOpeningPayment(false) },
      })
      payment.open()
    } catch (error) {
      setPaymentError(error instanceof Error ? error.message : 'Could not start payment. Please try again.')
      setOpeningPayment(false)
    }
  }

  return (
    <div className="container-page max-w-3xl py-8">
      {placed && <div role="status" className="mb-4 rounded-2xl bg-leaf-100 p-4 font-bold text-leaf-700">
        {o.paymentStatus === 'PAID' ? `🎉 Order placed! ${o.cookName} will confirm it shortly.` : 'Order created. Complete payment to send it to the cook.'}
      </div>}
      <PageTitle title={`Order #${o.orderNumber}`} subtitle={formatDateTime(o.createdAt)} action={<OrderStatusBadge status={o.orderStatus} />} />
      <Card className="p-5">
        <Link to={`/cooks/${o.cookId}`} className="text-lg font-extrabold text-brand-700 hover:underline">{o.cookName}'s Kitchen</Link>
        <p className="text-sm text-ink-500">{o.cookLocality}</p>
        <ul className="mt-4 divide-y divide-cream-200">
          {o.items.map((i, idx) => (
            <li key={idx} className="flex justify-between gap-3 py-2 text-sm">
              <div>
                <p className="font-bold">{i.quantity} × {i.dishName}</p>
                <p className="text-xs text-ink-500">{formatDate(i.availableDate)} · {o.orderType === 'PICKUP' ? `Pickup ${timeWindow(i.pickupStart, i.pickupEnd)}` : `Delivery ${timeWindow(i.deliveryStart, i.deliveryEnd)}`}</p>
              </div>
              <b>{rupees(i.totalPrice)}</b>
            </li>
          ))}
        </ul>
        <div className="mt-2 space-y-1 border-t border-cream-200 pt-3 text-sm">
          <div className="flex justify-between"><span>Subtotal</span><span>{rupees(o.subtotal)}</span></div>
          <div className="flex justify-between"><span>Delivery</span><span>{o.deliveryFee ? rupees(o.deliveryFee) : 'Free'}</span></div>
          <div className="flex justify-between text-base font-extrabold"><span>Total</span><span>{rupees(o.totalAmount)}</span></div>
        </div>
        <div className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
          <div><p className="font-bold text-ink-500">{orderTypeLabel[o.orderType]}</p>{o.deliveryAddress && <p>{o.deliveryAddress}</p>}{o.pickupAddress && <p>Pickup from: {o.pickupAddress}</p>}{o.trackingUrl && <p className="break-all text-xs text-ink-500">Courier: {o.deliveryProvider}</p>}</div>
          <div>
            <p className="font-bold text-ink-500">Payment</p>
            <Badge tone={o.paymentStatus === 'PAID' ? 'green' : 'neutral'}>{o.paymentStatus.replace('_', ' ')}</Badge>
            {o.paymentStatus === 'PENDING' && o.paymentCheckout && (
              <Button className="mt-2" loading={openingPayment || verifyPayment.isPending} onClick={startPayment}>Pay securely</Button>
            )}
            {paymentError && <p role="alert" className="mt-2 text-sm text-red-700">{paymentError}</p>}
          </div>
        </div>
      </Card>

      <Card className="mt-4 p-5"><h2 className="mb-3 text-lg">Status</h2><OrderTracker order={o} /></Card>

      <div className="mt-4 flex flex-wrap gap-3">
        {o.canCancel && <Button variant="danger" loading={cancel.isPending} onClick={() => { if (window.confirm('Cancel this order?')) cancel.mutate() }}>Cancel order</Button>}
        {o.orderStatus === 'COMPLETED' && !o.reviewed && <Button onClick={() => setReviewOpen(true)}>⭐ Review {o.cookName}</Button>}
        {o.reviewed && <Badge tone="green">✓ Reviewed</Badge>}
        {['COMPLETED', 'CANCELLED'].includes(o.orderStatus) && <Button variant="secondary" loading={reorder.isPending} onClick={() => reorder.mutate(o)}>↻ Reorder</Button>}
      </div>
      <div className="mt-3 space-y-2"><FormError error={cancel.error} /><FormError error={reorder.error} /></div>
      <ReviewModal orderId={o.id} open={reviewOpen} onClose={() => setReviewOpen(false)} cookId={o.cookId} />
    </div>
  )
}

function ReviewModal({ orderId, cookId, open, onClose }: { orderId: string; cookId: string; open: boolean; onClose: () => void }) {
  const qc = useQueryClient()
  const [r, setR] = useState({ rating: 5, tasteRating: 5, hygieneRating: 5, packagingRating: 5, reliabilityRating: 5 })
  const [comment, setComment] = useState('')
  const submit = useMutation({
    mutationFn: () => api.post(`/orders/${orderId}/review`, { ...r, comment }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['order', orderId] })
      qc.invalidateQueries({ queryKey: ['cook', cookId] })
      onClose()
    },
  })
  return (
    <Modal open={open} title="How was it?" onClose={onClose}>
      <div className="space-y-2">
        <StarInput label="Overall" value={r.rating} onChange={(v) => setR({ ...r, rating: v })} />
        <StarInput label="Taste" value={r.tasteRating} onChange={(v) => setR({ ...r, tasteRating: v })} />
        <StarInput label="Hygiene" value={r.hygieneRating} onChange={(v) => setR({ ...r, hygieneRating: v })} />
        <StarInput label="Packaging" value={r.packagingRating} onChange={(v) => setR({ ...r, packagingRating: v })} />
        <StarInput label="Reliability" value={r.reliabilityRating} onChange={(v) => setR({ ...r, reliabilityRating: v })} />
        <Textarea label="Tell others about it (optional)" rows={3} maxLength={2000} value={comment} onChange={(e) => setComment(e.target.value)} />
        <FormError error={submit.error} />
        <div className="flex justify-end gap-2 pt-2"><Button variant="ghost" onClick={onClose}>Not now</Button><Button loading={submit.isPending} onClick={() => submit.mutate()}>Submit review</Button></div>
      </div>
    </Modal>
  )
}
