import { useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api, qs } from '../lib/api'
import { useLocation2 } from '../lib/location'
import type { Batch, CookSummary, Cuisine, Page } from '../lib/types'
import { BatchCard, CookCard } from '../components/cards'
import { EmptyState, ErrorState, LoadingState } from '../components/ui'

const EXAMPLES = ['Maharashtrian', 'Biryani', 'Puran Poli', 'Snacks', 'Cakes', 'Thali']
const cuisineEmoji: Record<string, string> = {
  Maharashtrian: '🥘', Kolhapuri: '🌶️', 'North Indian': '🫓', 'South Indian': '🥞', Bengali: '🐟', Gujarati: '🍲',
  Bakery: '🎂', Snacks: '🥟', Biryani: '🍚', Sweets: '🍮',
}

function Section({ title, subtitle, to, children }: { title: string; subtitle?: string; to?: string; children: ReactNode }) {
  return (
    <section className="container-page mt-12">
      <div className="mb-4 flex items-end justify-between gap-3">
        <div><h2 className="text-2xl">{title}</h2>{subtitle && <p className="text-sm text-ink-500">{subtitle}</p>}</div>
        {to && <Link to={to} className="text-sm font-bold text-brand-600 hover:underline">See all →</Link>}
      </div>
      {children}
    </section>
  )
}

function CookRow({ params, empty }: { params: Record<string, string | number | boolean | undefined>; empty: string }) {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['cooks', params],
    queryFn: () => api.get<Page<CookSummary>>(`/cooks${qs({ size: 3, ...params })}`),
  })
  if (isLoading) return <LoadingState />
  if (error) return <ErrorState error={error} onRetry={refetch} />
  if (!data?.content.length) return <EmptyState title={empty} />
  return <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{data.content.map((c) => <CookCard key={c.id} cook={c} />)}</div>
}

function TopCookRow() {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['cooks', 'top'],
    queryFn: () => api.get<CookSummary[]>('/cooks/top'),
  })
  if (isLoading) return <LoadingState />
  if (error) return <ErrorState error={error} onRetry={refetch} />
  if (!data?.length) return <EmptyState title="No top cooks yet" />
  return <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{data.map((cook, index) => <CookCard key={cook.id} cook={cook} awardRank={index + 1} />)}</div>
}

export default function Home() {
  const [q, setQ] = useState('')
  const navigate = useNavigate()
  const { locality } = useLocation2()
  const cuisines = useQuery({ queryKey: ['cuisines'], queryFn: () => api.get<Cuisine[]>('/cuisines') })
  const limited = useQuery({ queryKey: ['batches', 'limited'], queryFn: () => api.get<Batch[]>('/batches?limited=true&limit=3') })

  const go = (term: string) => navigate(`/cooks${qs({ q: term })}`)
  const onSubmit = (e: FormEvent) => { e.preventDefault(); go(q.trim()) }

  return (
    <>
      <div className="bg-gradient-to-b from-brand-100 via-cream-100 to-cream-100">
        <div className="container-page py-12 text-center sm:py-20">
          <p className="mb-3 inline-block rounded-full bg-white px-3 py-1 text-xs font-bold uppercase tracking-wider text-brand-600">घर जैसा खाना</p>
          <h1 className="mx-auto max-w-3xl text-4xl leading-tight sm:text-5xl">Discover amazing <span className="text-brand-600">Home Cooks</span> around you.</h1>
          <p className="mx-auto mt-3 max-w-xl text-lg text-ink-700">Homemade food. Real people. Local flavours.</p>
          <form onSubmit={onSubmit} role="search" className="mx-auto mt-8 flex max-w-xl gap-2">
            <input value={q} onChange={(e) => setQ(e.target.value)} aria-label="What are you craving?" placeholder="What are you craving?" className="input !rounded-full !py-3.5 !text-base shadow-card" />
            <button className="rounded-full bg-brand-500 px-6 font-bold text-white hover:bg-brand-600">Search</button>
          </form>
          <div className="mt-4 flex flex-wrap justify-center gap-2">
            {EXAMPLES.map((e) => <button key={e} onClick={() => go(e)} className="rounded-full border border-brand-200 bg-white px-3 py-1 text-sm font-semibold text-brand-700 hover:bg-brand-50">{e}</button>)}
          </div>
        </div>
      </div>

      <Section title="Top Home Cooks" subtitle="Celebrated for completed orders and customer reviews" to="/cooks">
        <TopCookRow />
      </Section>
      <Section title={locality ? `Home Cooks near ${locality}` : 'Home Cooks Near You'} subtitle="Meet the people behind the food" to="/cooks">
        <CookRow params={{ locality: locality || undefined, sort: 'rating' }} empty={`No cooks in ${locality || 'your area'} yet`} />
      </Section>
      <Section title="Popular This Week" subtitle="Loved by the most customers" to="/cooks?sort=popular">
        <CookRow params={{ sort: 'popular' }} empty="Nothing popular yet" />
      </Section>
      <Section title="Regional Specialities" subtitle="Every region has its own flavour">
        {cuisines.isLoading ? <LoadingState /> : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
            {cuisines.data?.map((c) => (
              <Link key={c.id} to={`/cooks${qs({ cuisine: c.name })}`} className="card flex items-center gap-3 p-4 transition hover:-translate-y-0.5 hover:shadow-lg">
                <span className="text-3xl" aria-hidden>{cuisineEmoji[c.name] ?? '🍽️'}</span>
                <span className="font-extrabold">{c.name}</span>
              </Link>
            ))}
          </div>
        )}
      </Section>
      <Section title="Available Today" subtitle="Fresh batches you can still pre-order today" to="/cooks?availableToday=true">
        <CookRow params={{ availableToday: true }} empty="No batches open today — check back soon" />
      </Section>
      <Section title="Limited Batch" subtitle="Almost gone. Grab yours.">
        {limited.isLoading ? <LoadingState /> : limited.error ? <ErrorState error={limited.error} onRetry={limited.refetch} /> : !limited.data?.length ? <EmptyState title="No limited batches right now" /> : (
          <div className="grid gap-4 md:grid-cols-3">
            {limited.data.map((b) => <BatchCard key={b.id} batch={b} showCook><Link to={`/cooks/${b.cookId}`} className="mt-3 inline-block text-sm font-bold text-brand-600 hover:underline">View cook →</Link></BatchCard>)}
          </div>
        )}
      </Section>
      <Section title="New Home Cooks" subtitle="Say hello to the newest members" to="/cooks?sort=newest">
        <CookRow params={{ sort: 'newest' }} empty="No new cooks yet" />
      </Section>

      <section className="container-page mt-14">
        <div className="rounded-3xl bg-leaf-600 p-8 text-center text-white sm:p-12">
          <h2 className="text-3xl">Love to cook? Share your kitchen.</h2>
          <p className="mx-auto mt-2 max-w-lg text-leaf-100">Tell your story, list your dishes and take pre-orders from neighbours who want real homemade food.</p>
          <Link to="/register?role=COOK" className="mt-5 inline-block rounded-xl bg-white px-6 py-3 font-bold text-leaf-700 hover:bg-leaf-50">Become a Home Cook</Link>
        </div>
      </section>
    </>
  )
}
