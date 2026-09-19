import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ roles }) {
  const { isAuthenticated, user, loading } = useAuth();

  if (loading) {
    return (
      <div className="container page" style={{ padding: '3rem 0' }}>
        Loading…
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (roles && !roles.includes(user?.role)) {
    const fallback =
      user?.role === 'ADMIN'
        ? '/admin'
        : user?.role === 'VENDOR'
          ? '/vendor/inventory'
          : '/shopkeeper/inventory';
    return <Navigate to={fallback} replace />;
  }

  return <Outlet />;
}
