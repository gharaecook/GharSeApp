import { useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../lib/api'
import { formatDateTime } from '../../lib/format'
import type { AdminCookDetail as Detail, AdminCookRow, ApprovalStatus, Page } from '../../lib/types'
import { AdminShell } from '../../components/SectionNav'
import { Badge, Button, Card, EmptyState, ErrorState, FormError, LoadingState, Modal, PageTitle, Rating, Textarea } from '../../components/ui'

const tone = (s: ApprovalStatus) => (s === 'APPROVED' ? 'green' : s === 'REJECTED' ? 'red' : 'amber') as 'green' | 'red' | 'amber'

export function AdminCooks() {
  const [params, setParams] = useSearchParams()
  const status = params.get('status') ?? ''
  const cooks = useQuery({ queryKey: ['admin', 'cooks', status], queryFn: () => api.get<Page<AdminCookRow>>(`/admin/cooks?size=100${status ? `&status=${status}` : ''}`) })
  return (
    <AdminShell>
      <PageTitle title="Cooks" />
      <div className="mb-4 flex gap-2">
        {['', 'PENDING', 'APPROVED', 'REJECTED'].map((s) => <button key={s} onClick={() => setParams(s ? { status: s } : {})} className={`rounded-full px-4 py-1.5 text-sm font-bold ${status === s ? 'bg-brand-500 text-white' : 'bg-white text-ink-700'}`}>{s || 'All'}</button>)}
      </div>
      {cooks.isLoading ? <LoadingState /> : cooks.error ? <ErrorState error={cooks.error} onRetry={cooks.refetch} /> : !cooks.data?.content.length ? <EmptyState title="No cooks here" /> : (
        <Card className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-cream-200 text-xs uppercase text-ink-500"><tr><th className="p-3">Cook</th><th className="p-3">Locality</th><th className="p-3">Status</th><th className="p-3">Rating</th><th className="p-3">Joined</th><th className="p-3" /></tr></thead>
            <tbody>
              {cooks.data.content.map((c) => (
                <tr key={c.id} className="border-b border-cream-100 last:border-0">
                  <td className="p-3"><b>{c.name}</b><br /><span className="text-xs text-ink-500">{c.email}</span></td>
                  <td className="p-3">{c.locality}</td>
                  <td className="p-3"><Badge tone={tone(c.approvalStatus)}>{c.approvalStatus}</Badge></td>
                  <td className="p-3"><Rating value={c.rating} count={c.reviewCount} /></td>
                  <td className="p-3 text-xs">{formatDateTime(c.createdAt)}</td>
                  <td className="p-3"><Link to={`/admin/cooks/${c.id}`} className="font-bold text-brand-600">Review →</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </AdminShell>
  )
}

export function AdminCookDetail() {
  const { id = '' } = useParams()
  const qc = useQueryClient()
  const [rejecting, setRejecting] = useState(false)
  const [reason, setReason] = useState('')
  const [documentUrls, setDocumentUrls] = useState<Record<string, string>>({})
  const q = useQuery({ queryKey: ['admin', 'cook', id], queryFn: () => api.get<Detail>(`/admin/cooks/${id}`) })
  const done = () => { qc.invalidateQueries({ queryKey: ['admin'] }); setRejecting(false) }
  const approve = useMutation({ mutationFn: () => api.post(`/admin/cooks/${id}/approve`), onSuccess: done })
  const reject = useMutation({ mutationFn: () => api.post(`/admin/cooks/${id}/reject`, { reason }), onSuccess: done })
  const documentUrl = useMutation({
    mutationFn: (documentId: string) => api.get<{ url: string }>(`/admin/cooks/${id}/documents/${documentId}/url`),
    onSuccess: (result, documentId) => setDocumentUrls((current) => ({ ...current, [documentId]: result.url })),
  })

  if (q.isLoading) return <AdminShell><LoadingState /></AdminShell>
  if (q.error) return <AdminShell><ErrorState error={q.error} onRetry={q.refetch} /></AdminShell>
  const { profile: p, dishes } = q.data!
  return (
    <AdminShell>
      <PageTitle title={p.name} subtitle={`${p.email} · ${p.phone}`} action={<Badge tone={tone(p.approvalStatus)}>{p.approvalStatus}</Badge>} />
      <div className="grid gap-4 lg:grid-cols-2">
        <Card className="space-y-2 p-5">
          <h2 className="text-lg">Profile</h2>
          <p className="font-bold text-brand-700">{p.headline}</p>
          <p className="text-sm italic text-ink-700">{p.bio ? `“${p.bio}”` : 'No story yet'}</p>
          <p className="text-sm"><b>Locality:</b> {p.locality} · <b>Experience:</b> {p.yearsOfCooking ?? '—'} years</p>
          <p className="text-sm"><b>Cuisines:</b> {p.cuisines.map((c) => c.name).join(', ') || '—'}</p>
          <p className="text-sm"><b>Signature:</b> {p.signatureDishes || '—'}</p>
          {p.rejectionReason && <p className="text-sm text-red-700"><b>Last rejection:</b> {p.rejectionReason}</p>}
        </Card>
        <Card className="space-y-2 p-5">
          <h2 className="text-lg">Kitchen</h2>
          <p className="text-sm"><b>Address:</b> {p.kitchen?.address || '—'}</p>
          <p className="text-sm">{p.kitchen?.description}</p>
          <p className="text-sm"><b>Coordinates:</b> {p.kitchen?.latitude ?? '—'}, {p.kitchen?.longitude ?? '—'}</p>
          <div className="flex flex-wrap gap-2">{p.kitchen?.photos.map((u) => <img key={u} src={u} alt="Kitchen" className="h-20 rounded-lg" />)}</div>
        </Card>
        <Card className="space-y-2 p-5">
          <h2 className="text-lg">Documents</h2>
          {!p.documents.length ? <p className="text-sm text-ink-500">No documents uploaded (FSSAI optional).</p> : p.documents.map((d) => (
            <p key={d.id} className="text-sm"><b>{d.documentType}</b> {d.documentNumber} <Badge tone={tone(d.status)}>{d.status}</Badge> {d.hasFile && (documentUrls[d.id]
              ? <a className="font-bold text-brand-600" href={documentUrls[d.id]} target="_blank" rel="noreferrer">Open file</a>
              : <Button variant="ghost" className="px-2 py-1" loading={documentUrl.isPending} onClick={() => documentUrl.mutate(d.id)}>Get secure file link</Button>)}</p>
          ))}
          <FormError error={documentUrl.error} />
        </Card>
        <Card className="space-y-2 p-5">
          <h2 className="text-lg">Dishes ({dishes.length})</h2>
          {!dishes.length ? <p className="text-sm text-ink-500">No dishes yet.</p> : dishes.map((d) => <p key={d.id} className="text-sm">{d.name} · ₹{d.price} {!d.active && <Badge>inactive</Badge>}</p>)}
        </Card>
      </div>
      <FormError error={approve.error} />
      <div className="mt-6 flex gap-3">
        <Button variant="green" onClick={() => approve.mutate()} loading={approve.isPending} disabled={p.approvalStatus === 'APPROVED'}>Approve</Button>
        <Button variant="danger" onClick={() => setRejecting(true)} disabled={p.approvalStatus === 'REJECTED'}>Reject</Button>
      </div>
      <Modal open={rejecting} title="Reject this cook" onClose={() => setRejecting(false)}>
        <Textarea label="Reason (shown to the cook)" rows={3} value={reason} onChange={(e) => setReason(e.target.value)} />
        <FormError error={reject.error} />
        <div className="mt-3 flex justify-end gap-2"><Button variant="ghost" onClick={() => setRejecting(false)}>Cancel</Button><Button variant="danger" disabled={!reason.trim()} loading={reject.isPending} onClick={() => reject.mutate()}>Reject</Button></div>
      </Modal>
    </AdminShell>
  )
}
