import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { homeFor, useAuth } from '../lib/auth'
import type { Role } from '../lib/types'

/** Redirects anonymous visitors to /login and wrong-role users to their own home. */
export function RequireRole({ role }: { role: Role }) {
  const { user } = useAuth()
  const location = useLocation()
  if (!user) return <Navigate to="/login" state={{ from: location.pathname + location.search }} replace />
  if (user.role !== role) return <Navigate to={homeFor(user.role)} replace />
  return <Outlet />
}
