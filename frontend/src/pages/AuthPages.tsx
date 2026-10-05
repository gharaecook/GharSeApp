import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { homeFor, useAuth } from '../lib/auth'
import { Button, Card, FormError, Input, Select } from '../components/ui'

const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
})
type LoginForm = z.infer<typeof loginSchema>

export const registerSchema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(120),
  email: z.string().min(1, 'Email is required').email('Enter a valid email'),
  phone: z.string().regex(/^[0-9+\-\s]{10,20}$/, 'Enter a valid phone number'),
  password: z.string().min(8, 'At least 8 characters').max(72),
  role: z.enum(['CUSTOMER', 'COOK']),
  locality: z.string().max(120).optional(),
})
type RegisterForm = z.infer<typeof registerSchema>

function Shell({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
  return (
    <div className="container-page flex justify-center py-10">
      <Card className="w-full max-w-md p-6 sm:p-8">
        <h1 className="text-3xl">{title}</h1>
        <p className="mb-5 mt-1 text-sm text-ink-500">{subtitle}</p>
        {children}
      </Card>
    </div>
  )
}

export function Login() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from
  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) })
  const m = useMutation({ mutationFn: (v: LoginForm) => login(v.email, v.password), onSuccess: (u) => navigate(from ?? homeFor(u.role), { replace: true }) })
  if (user) return <Navigate to={homeFor(user.role)} replace />
  return (
    <Shell title="Welcome back" subtitle="Log in to order from your favourite Home Cooks.">
      <form onSubmit={handleSubmit((v) => m.mutate(v))} className="space-y-4" noValidate>
        <Input label="Email" type="email" autoComplete="email" {...register('email')} error={errors.email?.message} />
        <Input label="Password" type="password" autoComplete="current-password" {...register('password')} error={errors.password?.message} />
        <FormError error={m.error} />
        <Button type="submit" className="w-full" loading={m.isPending}>Log in</Button>
      </form>
      <p className="mt-4 text-center text-sm text-ink-500">New to GharSe? <Link to="/register" className="font-bold text-brand-600">Create an account</Link></p>
      {import.meta.env.DEV && <p className="mt-4 rounded-xl bg-cream-100 p-3 text-xs text-ink-500">Demo (local only): priya@gharse.demo · meena@gharse.demo · admin@gharse.demo — password <b>Demo@1234</b></p>}
    </Shell>
  )
}

export function Register() {
  const { user, register: signUp } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { register, handleSubmit, watch, formState: { errors } } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    defaultValues: { role: params.get('role') === 'COOK' ? 'COOK' : 'CUSTOMER' },
  })
  const role = watch('role')
  const m = useMutation({
    mutationFn: (v: RegisterForm) => signUp(v),
    onSuccess: (u) => navigate(u.role === 'COOK' ? '/cook/profile' : '/home', { replace: true }),
  })
  if (user) return <Navigate to={homeFor(user.role)} replace />
  return (
    <Shell title="Join GharSe" subtitle={role === 'COOK' ? 'Share your kitchen with your neighbourhood.' : 'Discover amazing Home Cooks around you.'}>
      <form onSubmit={handleSubmit((v) => m.mutate(v))} className="space-y-4" noValidate>
        <Select label="I want to…" {...register('role')}>
          <option value="CUSTOMER">Order homemade food</option>
          <option value="COOK">Cook and sell food</option>
        </Select>
        <Input label="Full name" autoComplete="name" {...register('name')} error={errors.name?.message} />
        <Input label="Email" type="email" autoComplete="email" {...register('email')} error={errors.email?.message} />
        <Input label="Mobile" type="tel" autoComplete="tel" {...register('phone')} error={errors.phone?.message} />
        {role === 'COOK' && <Input label="Locality" placeholder="e.g. Kothrud" {...register('locality')} error={errors.locality?.message} />}
        <Input label="Password" type="password" autoComplete="new-password" hint="At least 8 characters" {...register('password')} error={errors.password?.message} />
        <FormError error={m.error} />
        <Button type="submit" className="w-full" loading={m.isPending}>Create account</Button>
      </form>
      {role === 'COOK' && <p className="mt-3 text-xs text-ink-500">New cooks are reviewed by GharSe before they can publish food.</p>}
      <p className="mt-4 text-center text-sm text-ink-500">Already have an account? <Link to="/login" className="font-bold text-brand-600">Log in</Link></p>
    </Shell>
  )
}
