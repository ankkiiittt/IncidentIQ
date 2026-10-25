// src/components/AppLayout.tsx
import React, { useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  Activity,
  AlertTriangle,
  BookOpen,
  KeyRound,
  LogOut,
  Plus,
  Search,
  Server,
  Shield,
  User as UserIcon,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { RoleBadge } from './Badges';

export const AppLayout: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [globalSearch, setGlobalSearch] = useState('');

  const handleGlobalSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (globalSearch.trim()) {
      navigate(`/incidents?q=${encodeURIComponent(globalSearch.trim())}`);
    }
  };

  const canCreateIncident = user && (user.role === 'ADMIN' || user.role === 'ENGINEER' || user.role === 'SUPPORT');

  return (
    <div className="min-h-screen bg-bg text-text flex flex-col font-sans">
      {/* Top Advisory Banner */}
      <div className="bg-amber-50 border-b border-amber-200 px-4 py-1.5 text-xs text-amber-800 text-center font-medium">
        Disclaimer: IncidentIQ AI output is advisory. Recommendations are suggestions that the system never executes. Humans make every decision.
      </div>

      {/* Force Password Change Warning Banner */}
      {user?.mustChangePassword && (
        <div className="bg-red-50 border-b border-red-200 px-4 py-2 text-xs text-red-800 flex items-center justify-center gap-2">
          <KeyRound className="w-4 h-4 text-red-600" />
          <span>
            You are signed in with a temporary password. You must change your password to continue securely.{' '}
            <Link to="/account" className="font-semibold underline hover:text-red-950">
              Change password now
            </Link>
          </span>
        </div>
      )}

      {/* Main Container */}
      <div className="flex-1 flex max-w-layout w-full mx-auto">
        {/* Left Sidebar (240px) */}
        <aside className="w-60 border-r border-border bg-surface flex flex-col justify-between shrink-0">
          <div>
            {/* Brand Logo & Name */}
            <div className="p-4 border-b border-border flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-lg bg-primary text-white flex items-center justify-center font-bold text-sm shadow-sm">
                IQ
              </div>
              <div>
                <span className="font-bold text-base tracking-tight text-text block leading-none">IncidentIQ</span>
                <span className="text-[10px] text-muted font-medium tracking-wide">ENTERPRISE SRE</span>
              </div>
            </div>

            {/* Navigation Links */}
            <nav className="p-3 space-y-1 text-sm font-medium">
              <NavLink
                to="/"
                end
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${
                    isActive
                      ? 'bg-primary-light text-primary font-semibold'
                      : 'text-muted hover:text-text hover:bg-slate-50'
                  }`
                }
              >
                <Activity className="w-4 h-4" />
                Dashboard
              </NavLink>

              <NavLink
                to="/incidents"
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${
                    isActive
                      ? 'bg-primary-light text-primary font-semibold'
                      : 'text-muted hover:text-text hover:bg-slate-50'
                  }`
                }
              >
                <AlertTriangle className="w-4 h-4" />
                Incidents
              </NavLink>

              <NavLink
                to="/services"
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${
                    isActive
                      ? 'bg-primary-light text-primary font-semibold'
                      : 'text-muted hover:text-text hover:bg-slate-50'
                  }`
                }
              >
                <Server className="w-4 h-4" />
                Services
              </NavLink>

              <NavLink
                to="/knowledge"
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${
                    isActive
                      ? 'bg-primary-light text-primary font-semibold'
                      : 'text-muted hover:text-text hover:bg-slate-50'
                  }`
                }
              >
                <BookOpen className="w-4 h-4" />
                Knowledge Base
              </NavLink>

              {user?.role === 'ADMIN' && (
                <NavLink
                  to="/admin"
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${
                      isActive
                        ? 'bg-primary-light text-primary font-semibold'
                        : 'text-muted hover:text-text hover:bg-slate-50'
                    }`
                  }
                >
                  <Shield className="w-4 h-4" />
                  Admin
                </NavLink>
              )}
            </nav>
          </div>

          {/* User Profile Section at bottom */}
          <div className="p-3 border-t border-border bg-slate-50/50 space-y-3">
            <div className="px-1">
              <div className="flex items-center justify-between mb-1">
                <span className="text-xs font-semibold text-text truncate max-w-[120px]">{user?.name}</span>
                {user && <RoleBadge role={user.role} />}
              </div>
              <span className="text-[11px] text-muted truncate block">{user?.email}</span>
            </div>

            <div className="pt-1 border-t border-slate-200/60 flex items-center justify-between text-xs font-medium text-muted">
              <Link
                to="/account"
                className="flex items-center gap-1.5 hover:text-text py-1 px-1.5 rounded hover:bg-slate-100 transition-colors"
                title="Account Settings"
              >
                <UserIcon className="w-3.5 h-3.5" />
                Account
              </Link>
              <button
                onClick={() => logout()}
                className="flex items-center gap-1.5 text-red-600 hover:text-red-700 py-1 px-1.5 rounded hover:bg-red-50 transition-colors"
                title="Sign Out"
              >
                <LogOut className="w-3.5 h-3.5" />
                Sign Out
              </button>
            </div>
          </div>
        </aside>

        {/* Primary Content Container */}
        <div className="flex-1 flex flex-col min-w-0 overflow-y-auto">
          {/* Top Bar with Global Search and New Incident Button */}
          <header className="h-14 border-b border-border bg-surface px-6 flex items-center justify-between shrink-0 gap-4">
            <form onSubmit={handleGlobalSearch} className="flex-1 max-w-md relative">
              <Search className="w-4 h-4 text-muted absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search incidents by number, title, or description..."
                value={globalSearch}
                onChange={(e) => setGlobalSearch(e.target.value)}
                className="w-full bg-slate-50 border border-border rounded-md pl-9 pr-4 py-1.5 text-sm text-text placeholder-muted focus:bg-white focus:border-primary focus:outline-none transition-colors"
              />
            </form>

            <div className="flex items-center gap-3">
              {canCreateIncident && (
                <Link
                  to="/incidents/new"
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-primary hover:bg-primary-hover text-white text-sm font-medium shadow-sm transition-colors"
                >
                  <Plus className="w-4 h-4" />
                  New Incident
                </Link>
              )}
            </div>
          </header>

          {/* Page Outlet */}
          <main className="flex-1 p-6">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
};
