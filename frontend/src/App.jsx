import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';
import DashboardLayout from './components/DashboardLayout';
import LandingPage from './pages/LandingPage';
import Login from './pages/Login';
import Register from './pages/Register';
import ProfilePage from './pages/ProfilePage';
import AdminHome from './pages/admin/AdminHome';
import AdminVendors from './pages/admin/AdminVendors';
import AdminShopkeepers from './pages/admin/AdminShopkeepers';
import AdminQueries from './pages/admin/AdminQueries';
import InventoryList from './pages/shared/InventoryList';
import AddItems from './pages/shared/AddItems';
import VendorsPage from './pages/shared/VendorsPage';
import SoldItems from './pages/shared/SoldItems';
import AlertsPage from './pages/shared/AlertsPage';
import ContactUs from './pages/shared/ContactUs';
import ChatHistory from './pages/shared/ChatHistory';
import OrdersPage from './pages/shared/OrdersPage';
import { useAuth } from './context/AuthContext';

function RoleRedirect() {
  const { user, isAuthenticated, loading } = useAuth();
  if (loading) return <div className="container page">Loading…</div>;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (user?.role === 'ADMIN') return <Navigate to="/admin" replace />;
  if (user?.role === 'VENDOR') return <Navigate to="/vendor/inventory" replace />;
  return <Navigate to="/shopkeeper/inventory" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/home" element={<RoleRedirect />} />

      <Route element={<ProtectedRoute roles={['ADMIN']} />}>
        <Route path="/admin" element={<DashboardLayout basePath="/admin" />}>
          <Route index element={<AdminHome />} />
          <Route path="vendors" element={<AdminVendors />} />
          <Route path="shopkeepers" element={<AdminShopkeepers />} />
          <Route path="queries" element={<AdminQueries />} />
          <Route path="profile" element={<ProfilePage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['SHOPKEEPER']} />}>
        <Route path="/shopkeeper" element={<DashboardLayout basePath="/shopkeeper" />}>
          <Route index element={<Navigate to="inventory" replace />} />
          <Route path="inventory" element={<InventoryList />} />
          <Route path="add" element={<AddItems />} />
          <Route path="vendors" element={<VendorsPage />} />
          <Route path="sold" element={<SoldItems />} />
          <Route path="alerts" element={<AlertsPage />} />
          <Route path="contact" element={<ContactUs />} />
          <Route path="chat" element={<ChatHistory />} />
          <Route path="orders" element={<OrdersPage />} />
          <Route path="profile" element={<ProfilePage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['VENDOR']} />}>
        <Route path="/vendor" element={<DashboardLayout basePath="/vendor" />}>
          <Route index element={<Navigate to="inventory" replace />} />
          <Route path="inventory" element={<InventoryList />} />
          <Route path="add" element={<AddItems />} />
          <Route path="vendors" element={<VendorsPage />} />
          <Route path="orders" element={<OrdersPage />} />
          <Route path="contact" element={<ContactUs />} />
          <Route path="alerts" element={<AlertsPage />} />
          <Route path="chat" element={<ChatHistory />} />
          <Route path="profile" element={<ProfilePage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
