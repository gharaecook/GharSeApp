import { Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { RequireAuth, RequireRole } from './components/Guards'
import { EmptyState } from './components/ui'
import Home from './pages/Home'
import Cooks from './pages/Cooks'
import CookProfilePage from './pages/CookProfilePage'
import DishPage from './pages/DishPage'
import { ForgotPassword, Login, Register, ResetPassword } from './pages/AuthPages'
import CartPage from './pages/CartPage'
import Checkout from './pages/Checkout'
import { OrderDetail, Orders } from './pages/Orders'
import { Favorites, Profile } from './pages/Account'
import CookDashboard from './pages/cook/CookDashboard'
import CookProfileEdit from './pages/cook/CookProfileEdit'
import { CookDishes, DishForm } from './pages/cook/CookDishes'
import { BatchForm, CookBatches } from './pages/cook/CookBatches'
import { CookEarnings, CookOrders } from './pages/cook/CookOrders'
import AdminDashboard from './pages/admin/AdminDashboard'
import { AdminCookDetail, AdminCooks } from './pages/admin/AdminCooks'
import { AdminOrders, AdminUsers } from './pages/admin/AdminLists'
import Notifications from './pages/Notifications'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="/home" element={<Home />} />
        <Route path="/cooks" element={<Cooks />} />
        <Route path="/cooks/:id" element={<CookProfilePage />} />
        <Route path="/dishes/:id" element={<DishPage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />

        <Route element={<RequireAuth />}>
          <Route path="/notifications" element={<Notifications />} />
        </Route>

        <Route element={<RequireRole role="CUSTOMER" />}>
          <Route path="/cart" element={<CartPage />} />
          <Route path="/checkout" element={<Checkout />} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/orders/:id" element={<OrderDetail />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/favorites" element={<Favorites />} />
        </Route>

        <Route element={<RequireRole role="COOK" />}>
          <Route path="/cook" element={<CookDashboard />} />
          <Route path="/cook/profile" element={<CookProfileEdit />} />
          <Route path="/cook/dishes" element={<CookDishes />} />
          <Route path="/cook/dishes/new" element={<DishForm />} />
          <Route path="/cook/dishes/:id" element={<DishForm />} />
          <Route path="/cook/batches" element={<CookBatches />} />
          <Route path="/cook/batches/new" element={<BatchForm />} />
          <Route path="/cook/batches/:id" element={<BatchForm />} />
          <Route path="/cook/orders" element={<CookOrders />} />
          <Route path="/cook/earnings" element={<CookEarnings />} />
        </Route>

        <Route element={<RequireRole role="ADMIN" />}>
          <Route path="/admin" element={<AdminDashboard />} />
          <Route path="/admin/cooks" element={<AdminCooks />} />
          <Route path="/admin/cooks/:id" element={<AdminCookDetail />} />
          <Route path="/admin/orders" element={<AdminOrders />} />
          <Route path="/admin/users" element={<AdminUsers />} />
        </Route>

        <Route path="*" element={<div className="container-page py-10"><EmptyState emoji="🧭" title="Page not found" message="That page doesn't exist." /></div>} />
      </Route>
    </Routes>
  )
}
