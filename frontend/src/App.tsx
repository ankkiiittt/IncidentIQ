// src/App.tsx
import React from 'react';
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { AppLayout } from './components/AppLayout';
import { AuthProvider, useAuth } from './context/AuthContext';
import { AccountPage } from './pages/AccountPage';
import { AdminPage } from './pages/AdminPage';
import { CreateIncidentPage } from './pages/CreateIncidentPage';
import { DashboardPage } from './pages/DashboardPage';
import { IncidentDetailPage } from './pages/IncidentDetailPage';
import { IncidentListPage } from './pages/IncidentListPage';
import { AnalyticsPage } from './pages/AnalyticsPage';
import { KnowledgePage } from './pages/KnowledgePage';
import { LoginPage } from './pages/LoginPage';
import { ServicesPage } from './pages/ServicesPage';

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-bg flex flex-col items-center justify-center text-muted">
        <Loader2 className="w-8 h-8 animate-spin text-primary mb-3" />
        <span className="text-xs font-medium">Validating IncidentIQ session...</span>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
};

const PublicOnlyRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-bg flex flex-col items-center justify-center text-muted">
        <Loader2 className="w-8 h-8 animate-spin text-primary mb-3" />
      </div>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
};

export default function App(): React.JSX.Element {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Authentication Route */}
          <Route
            path="/login"
            element={
              <PublicOnlyRoute>
                <LoginPage />
              </PublicOnlyRoute>
            }
          />

          {/* Protected Application Routes */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<DashboardPage />} />
            <Route path="incidents" element={<IncidentListPage />} />
            <Route path="incidents/new" element={<CreateIncidentPage />} />
            <Route path="incidents/:id" element={<IncidentDetailPage />} />
            <Route path="services" element={<ServicesPage />} />
            <Route path="knowledge" element={<KnowledgePage />} />
            <Route path="analytics" element={<AnalyticsPage />} />
            <Route path="admin" element={<AdminPage />} />
            <Route path="account" element={<AccountPage />} />
          </Route>

          {/* 404 Fallback */}
          <Route
            path="*"
            element={
              <div className="min-h-screen bg-bg flex flex-col items-center justify-center p-6 text-center">
                <h1 className="text-4xl font-extrabold text-text font-mono mb-2">404</h1>
                <p className="text-sm text-muted mb-4">The requested page does not exist.</p>
                <a
                  href="/"
                  className="px-4 py-2 rounded-md bg-primary hover:bg-primary-hover text-white text-xs font-semibold transition-colors"
                >
                  Return to Dashboard
                </a>
              </div>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
