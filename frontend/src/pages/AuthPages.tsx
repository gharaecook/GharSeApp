import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { homeFor, useAuth } from '../lib/auth'
import { Button, Card, FormError, Input, Select } from '../components/ui'
import { api } from '../lib/api'

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
  const passwordReset = (location.state as { passwordReset?: boolean } | null)?.passwordReset
  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) })
  const m = useMutation({ mutationFn: (v: LoginForm) => login(v.email, v.password), onSuccess: (u) => navigate(from ?? homeFor(u.role), { replace: true }) })
  if (user) return <Navigate to={homeFor(user.role)} replace />
  return (
    <Shell title="Welcome back" subtitle="Log in to order from your favourite Home Cooks.">
      {passwordReset && <p role="status" className="mb-4 rounded-xl bg-leaf-100 p-3 text-sm font-bold text-leaf-700">Your password has been reset. Log in with your new password.</p>}
      <form onSubmit={handleSubmit((v) => m.mutate(v))} className="space-y-4" noValidate>
        <Input label="Email" type="email" autoComplete="email" {...register('email')} error={errors.email?.message} />
        <Input label="Password" type="password" autoComplete="current-password" {...register('password')} error={errors.password?.message} />
        <FormError error={m.error} />
        <Button type="submit" className="w-full" loading={m.isPending}>Log in</Button>
      </form>
      <p className="mt-3 text-right text-sm"><Link to="/forgot-password" className="font-bold text-brand-600">Forgot password?</Link></p>
      <p className="mt-4 text-center text-sm text-ink-500">New to CUOQO? <Link to="/register" className="font-bold text-brand-600">Create an account</Link></p>
      {import.meta.env.DEV && <p className="mt-4 rounded-xl bg-cream-100 p-3 text-xs text-ink-500">Demo (local only): priya@gharse.demo · meena@gharse.demo · admin@gharse.demo — password <b>Demo@1234</b></p>}
    </Shell>
  )
}

const forgotPasswordSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email'),
})
type ForgotPasswordForm = z.infer<typeof forgotPasswordSchema>

interface ForgotPasswordResponse { message: string; resetToken?: string }

export function ForgotPassword() {
  const { register, handleSubmit, formState: { errors } } = useForm<ForgotPasswordForm>({ resolver: zodResolver(forgotPasswordSchema) })
  const mutation = useMutation({
    mutationFn: (v: ForgotPasswordForm) => api.post<ForgotPasswordResponse>('/auth/forgot-password', v),
  })
  return (
    <Shell title="Forgot password?" subtitle="Enter the email address on your account to request a reset.">
      {mutation.isSuccess ? (
        <div role="status">
          <p className="text-sm text-ink-700">{mutation.data.message}</p>
          {mutation.data.resetToken && (
            <div className="mt-4 rounded-xl bg-cream-100 p-4">
              <p className="text-sm font-bold">Local manual reset token</p>
              <code className="mt-2 block break-all rounded-lg bg-white p-3 text-xs">{mutation.data.resetToken}</code>
              <Link to={`/reset-password?token=${encodeURIComponent(mutation.data.resetToken)}`} className="mt-3 inline-block font-bold text-brand-600">Continue to reset password →</Link>
            </div>
          )}
          <p className="mt-5 text-center text-sm"><Link to="/login" className="font-bold text-brand-600">Back to log in</Link></p>
        </div>
      ) : (
        <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="space-y-4" noValidate>
          <Input label="Email" type="email" autoComplete="email" {...register('email')} error={errors.email?.message} />
          <FormError error={mutation.error} />
          <Button type="submit" className="w-full" loading={mutation.isPending}>Request reset</Button>
          <p className="text-center text-sm text-ink-500"><Link to="/login" className="font-bold text-brand-600">Back to log in</Link></p>
        </form>
      )}
    </Shell>
  )
}

const resetPasswordSchema = z.object({
  password: z.string().min(8, 'At least 8 characters').max(72),
  confirmPassword: z.string().min(1, 'Confirm your new password'),
}).refine((v) => v.password === v.confirmPassword, {
  path: ['confirmPassword'],
  message: 'Passwords do not match',
})
type ResetPasswordForm = z.infer<typeof resetPasswordSchema>

export function ResetPassword() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const navigate = useNavigate()
  const { register, handleSubmit, formState: { errors } } = useForm<ResetPasswordForm>({ resolver: zodResolver(resetPasswordSchema) })
  const mutation = useMutation({
    mutationFn: (v: ResetPasswordForm) => api.post<void>('/auth/reset-password', { token, newPassword: v.password }),
    onSuccess: () => navigate('/login', { replace: true, state: { passwordReset: true } }),
  })
  return (
    <Shell title="Set a new password" subtitle="Choose a new password with at least 8 characters.">
      {!token ? <p role="alert" className="text-sm text-red-700">This reset link is missing its token. Request a new one from the forgot-password page.</p> : (
        <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="space-y-4" noValidate>
          <Input label="New password" type="password" autoComplete="new-password" {...register('password')} error={errors.password?.message} />
          <Input label="Confirm new password" type="password" autoComplete="new-password" {...register('confirmPassword')} error={errors.confirmPassword?.message} />
          <FormError error={mutation.error} />
          <Button type="submit" className="w-full" loading={mutation.isPending}>Reset password</Button>
        </form>
      )}
      <p className="mt-4 text-center text-sm text-ink-500"><Link to="/login" className="font-bold text-brand-600">Back to log in</Link></p>
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
    <Shell title="Join CUOQO" subtitle={role === 'COOK' ? 'Share your kitchen with your neighbourhood.' : 'Discover amazing Home Cooks around you.'}>
      <form onSubmit={handleSubmit((v) => m.mutate(v))} className="space-y-4" noValidate>
        <Select label="I’m signing up as" {...register('role')}>
          <option value="CUSTOMER">Foodie — order homemade food</option>
          <option value="COOK">Home Cook — sell homemade food</option>
        </Select>
        <Input label="Full name" autoComplete="name" {...register('name')} error={errors.name?.message} />
        <Input label="Email" type="email" autoComplete="email" {...register('email')} error={errors.email?.message} />
        <Input label="Mobile" type="tel" autoComplete="tel" {...register('phone')} error={errors.phone?.message} />
        {role === 'COOK' && <Input label="Locality" placeholder="e.g. Kothrud" {...register('locality')} error={errors.locality?.message} />}
        <Input label="Password" type="password" autoComplete="new-password" hint="At least 8 characters" {...register('password')} error={errors.password?.message} />
        <FormError error={m.error} />
        <Button type="submit" className="w-full" loading={m.isPending}>Create account</Button>
      </form>
      {role === 'COOK' && <p className="mt-3 text-xs text-ink-500">New cooks are reviewed by CUOQO before they can publish food.</p>}
      <p className="mt-4 text-center text-sm text-ink-500">Already have an account? <Link to="/login" className="font-bold text-brand-600">Log in</Link></p>
    </Shell>
  )
}
