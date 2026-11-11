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
    <section className="container-page mt-8 sm:mt-12">
      <div className="mb-4 flex items-end justify-between gap-3">
        <div><h2 className="text-xl sm:text-2xl">{title}</h2>{subtitle && <p className="text-sm text-ink-500">{subtitle}</p>}</div>
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
  return <div className="mobile-carousel">{data.content.map((c) => <CookCard key={c.id} cook={c} />)}</div>
}

function TopCookRow() {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['cooks', 'top'],
    queryFn: () => api.get<CookSummary[]>('/cooks/top'),
  })
  if (isLoading) return <LoadingState />
  if (error) return <ErrorState error={error} onRetry={refetch} />
  if (!data?.length) return <EmptyState title="No top cooks yet" />
  return <div className="mobile-carousel">{data.map((cook, index) => <CookCard key={cook.id} cook={cook} awardRank={index + 1} />)}</div>
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
      <div className="mx-auto w-full max-w-7xl px-4 pt-5 sm:px-6 sm:pt-8 lg:px-8">
        <section className="hero-panel relative isolate overflow-hidden rounded-[1.75rem] shadow-[0_24px_70px_rgba(74,49,10,0.2)] sm:rounded-[2rem]" aria-labelledby="home-hero-title">
          <div className="grid min-h-[570px] lg:min-h-[560px] lg:grid-cols-[1.08fr_0.92fr]">
            <div className="relative z-10 flex min-w-0 flex-col justify-center bg-gradient-to-br from-[#76500d] via-[#8c600e] to-[#a87517] px-5 py-8 text-white sm:px-10 sm:py-12 lg:px-14">
              <div className="mb-7 flex w-fit items-center gap-3 rounded-2xl border border-white/30 bg-white/95 p-2.5 pr-5 shadow-lg">
                <img src="/zaykesh-logo.png" alt="Zaykesh" className="h-14 w-14 rounded-xl object-cover sm:h-16 sm:w-16" />
                <div>
                  <p className="text-sm font-extrabold tracking-wide text-ink-900">Zaykesh</p>
                  <p className="text-xs font-semibold text-ink-500">A little taste of home</p>
                </div>
              </div>
              <p className="mb-3 text-xs font-extrabold uppercase tracking-[0.2em] text-amber-100">घर जैसा खाना <span className="mx-1 text-amber-300">·</span> MADE NEAR YOU</p>
              <h1 id="home-hero-title" className="max-w-xl text-[2.45rem] font-extrabold leading-[1.08] tracking-tight sm:text-5xl lg:text-[3.5rem]">
                Find your taste of <span className="text-[#f6d78e]">home.</span>
              </h1>
              <p className="mt-4 max-w-lg text-base leading-relaxed text-amber-50/90 sm:text-lg">Meet talented home cooks nearby and discover the dishes made with a little extra love.</p>
              <form onSubmit={onSubmit} role="search" className="mt-6 flex max-w-xl gap-2 rounded-2xl bg-white p-1.5 shadow-xl sm:mt-8">
                <input value={q} onChange={(e) => setQ(e.target.value)} aria-label="What are you craving?" placeholder="What are you craving?" className="min-w-0 flex-1 rounded-xl border-0 bg-transparent px-3 py-3 text-base text-ink-900 outline-none placeholder:text-ink-300 focus:ring-2 focus:ring-brand-300" />
                <button className="shrink-0 rounded-xl bg-brand-500 px-5 py-3 font-extrabold text-white transition hover:bg-brand-600 active:scale-95 sm:px-6">Explore</button>
              </form>
              <div className="mobile-category-row mt-4 sm:mt-5">
                {EXAMPLES.map((e) => <button key={e} onClick={() => go(e)} className="shrink-0 rounded-full border border-white/35 bg-white/10 px-3.5 py-2 text-sm font-bold text-white transition hover:bg-white/20 active:scale-95">{e}</button>)}
              </div>
            </div>
            <div className="hero-food-visual relative min-h-[250px] min-w-0 overflow-hidden sm:min-h-[320px] lg:min-h-full">
              <img src="/zaykesh-food-hero.jpg" alt="A freshly prepared Indian meal, ready to share" className="absolute inset-0 h-full w-full object-cover" />
              <div className="absolute inset-0 bg-gradient-to-t from-[#291c08]/75 via-transparent to-[#291c08]/10 md:bg-gradient-to-r md:from-[#76500d]/35 md:via-transparent md:to-transparent" />
              <div className="absolute bottom-5 left-5 right-5 rounded-2xl border border-white/25 bg-[#3b2a12]/65 p-4 text-white shadow-xl backdrop-blur-md sm:bottom-7 sm:left-7 sm:right-7 sm:p-5">
                <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-amber-200">Made fresh, close to home</p>
                <p className="mt-1 text-lg font-extrabold sm:text-xl">Good food brings us together.</p>
              </div>
            </div>
          </div>
        </section>
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
          <div className="mobile-category-row sm:grid sm:grid-cols-3 sm:overflow-visible sm:mx-0 sm:flex-none sm:flex-wrap sm:p-0 lg:grid-cols-5">
            {cuisines.data?.map((c) => (
              <Link key={c.id} to={`/cooks${qs({ cuisine: c.name })}`} className="card flex min-w-[150px] shrink-0 items-center gap-3 p-3 transition hover:-translate-y-0.5 hover:shadow-lg active:scale-[0.98] sm:min-w-0">
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
          <div className="mobile-carousel md:grid-cols-3">
            {limited.data.map((b) => <BatchCard key={b.id} batch={b} showCook><Link to={`/cooks/${b.cookId}`} className="mt-3 inline-block text-sm font-bold text-brand-600 hover:underline">View cook →</Link></BatchCard>)}
          </div>
        )}
      </Section>
      <Section title="New Home Cooks" subtitle="Say hello to the newest members" to="/cooks?sort=newest">
        <CookRow params={{ sort: 'newest' }} empty="No new cooks yet" />
      </Section>

      <section className="container-page mt-14">
        <div className="rounded-3xl bg-gradient-to-br from-brand-700 via-brand-600 to-[#a87517] p-8 text-center text-white shadow-[0_16px_40px_rgba(74,49,10,0.16)] sm:p-12">
          <h2 className="text-3xl">Love to cook? Share your kitchen.</h2>
          <p className="mx-auto mt-2 max-w-lg text-amber-100">Tell your story, list your dishes and take pre-orders from neighbours who want real homemade food.</p>
          <Link to="/register?role=COOK" className="mt-5 inline-block rounded-xl bg-cream-50 px-6 py-3 font-extrabold text-brand-700 shadow-sm hover:bg-cream-100">Become a Home Cook</Link>
        </div>
      </section>
    </>
  )
}
