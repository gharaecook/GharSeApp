import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, api } from '../lib/api'
import { useAuth } from '../lib/auth'
import type { Cart } from '../lib/types'
import { Button, FormError, Modal } from './ui'

/** Quantity picker + Add to Cart. Enforces the one-cook-per-cart rule with a Clear Cart / Cancel choice. */
export function AddToCart({ dishId, batchId, max, disabled }: { dishId: string; batchId: string; max: number; disabled?: boolean }) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [qty, setQty] = useState(1)
  const [conflict, setConflict] = useState(false)
  const [added, setAdded] = useState(false)

  const add = useMutation({
    mutationFn: () => api.post<Cart>('/cart/items', { dishId, batchId, quantity: qty }),
    onSuccess: (cart) => {
      qc.setQueryData(['cart'], cart)
      setAdded(true)
      setTimeout(() => setAdded(false), 2000)
    },
    onError: (e) => {
      if (e instanceof ApiError && e.code === 'CART_OTHER_COOK') setConflict(true)
    },
  })
  const clearAndAdd = useMutation({
    mutationFn: async () => {
      await api.del('/cart')
      return api.post<Cart>('/cart/items', { dishId, batchId, quantity: qty })
    },
    onSuccess: (cart) => {
      qc.setQueryData(['cart'], cart)
      setConflict(false)
    },
  })

  const onAdd = () => {
    if (!user) return navigate('/login', { state: { from: window.location.pathname } })
    if (user.role !== 'CUSTOMER') return
    add.mutate()
  }
  const limit = Math.max(1, Math.min(max, 20))

  return (
    <div className="flex flex-col items-end gap-1">
      <div className="flex items-center gap-2">
        <div className="flex items-center rounded-xl border border-cream-300 bg-white">
          <button type="button" aria-label="Decrease quantity" className="px-3 py-1.5 font-bold" onClick={() => setQty((q) => Math.max(1, q - 1))} disabled={disabled}>−</button>
          <span className="w-6 text-center text-sm font-bold" aria-live="polite">{qty}</span>
          <button type="button" aria-label="Increase quantity" className="px-3 py-1.5 font-bold" onClick={() => setQty((q) => Math.min(limit, q + 1))} disabled={disabled}>+</button>
        </div>
        <Button onClick={onAdd} loading={add.isPending} disabled={disabled || (!!user && user.role !== 'CUSTOMER')}>{added ? '✓ Added' : 'Add to Cart'}</Button>
      </div>
      {add.isError && !conflict && <FormError error={add.error} />}
      <Modal open={conflict} title="Start a new cart?" onClose={() => setConflict(false)}>
        <p className="mb-4 text-sm text-ink-700">Your cart contains items from another Home Cook.</p>
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={() => setConflict(false)}>Cancel</Button>
          <Button onClick={() => clearAndAdd.mutate()} loading={clearAndAdd.isPending}>Clear Cart</Button>
        </div>
        <FormError error={clearAndAdd.error} />
      </Modal>
    </div>
  )
}
