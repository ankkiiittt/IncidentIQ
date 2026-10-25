// src/pages/LoginPage.tsx
import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Loader2, Lock, Mail, ShieldAlert } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Return to previous route or dashboard
  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!email.trim() || !password) {
      setErrorMessage('Please enter both email and password.');
      return;
    }

    setIsLoading(true);
    try {
      const user = await login(email.trim(), password);
      if (user.mustChangePassword) {
        navigate('/account', { replace: true });
      } else {
        navigate(from, { replace: true });
      }
    } catch (err: unknown) {
      if (err instanceof Error) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage('Failed to sign in. Please verify your credentials.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickLogin = (presetEmail: string, presetPass: string) => {
    setEmail(presetEmail);
    setPassword(presetPass);
  };

  return (
    <div className="min-h-screen bg-bg flex flex-col justify-center py-12 sm:px-6 lg:px-8 font-sans">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-primary text-white font-bold text-xl shadow mb-3">
          IQ
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-text">Sign in to IncidentIQ</h1>
        <p className="mt-1 text-sm text-muted">
          AI-Powered Enterprise Incident Intelligence Platform
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="card-surface py-8 px-6 shadow-sm sm:px-10">
          {errorMessage && (
            <div className="mb-6 rounded-md bg-red-50 border border-red-200 p-4 text-xs text-red-800 flex items-start gap-2.5">
              <ShieldAlert className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
              <div className="leading-relaxed">{errorMessage}</div>
            </div>
          )}

          <form className="space-y-5" onSubmit={handleSubmit}>
            <div>
              <label htmlFor="email" className="block text-xs font-semibold text-text uppercase tracking-wider mb-1.5">
                Work Email
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-muted absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  id="email"
                  type="email"
                  autoComplete="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@company.com"
                  className="w-full bg-slate-50 border border-border rounded-md pl-9 pr-3 py-2 text-sm text-text placeholder-muted focus:bg-white focus:border-primary focus:outline-none transition-colors"
                />
              </div>
            </div>

            <div>
              <label htmlFor="password" className="block text-xs font-semibold text-text uppercase tracking-wider mb-1.5">
                Password
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-muted absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className="w-full bg-slate-50 border border-border rounded-md pl-9 pr-10 py-2 text-sm text-text placeholder-muted focus:bg-white focus:border-primary focus:outline-none transition-colors"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted hover:text-text"
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full flex items-center justify-center py-2.5 px-4 rounded-md bg-primary hover:bg-primary-hover text-white text-sm font-semibold shadow-sm focus:outline-none transition-colors disabled:opacity-50"
            >
              {isLoading ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  Signing in...
                </>
              ) : (
                'Sign in'
              )}
            </button>
          </form>

          {/* Demo Credentials Helper */}
          <div className="mt-8 pt-6 border-t border-border">
            <span className="text-xs text-muted font-medium block mb-2">Demo Credentials:</span>
            <div className="space-y-1.5">
              <button
                type="button"
                onClick={() => handleQuickLogin('admin@incidentiq.local', 'AdminInitialPassword123!')}
                className="w-full text-left px-2.5 py-1.5 rounded bg-slate-50 hover:bg-slate-100 border border-slate-200 text-xs flex justify-between items-center transition-colors"
              >
                <span className="font-medium text-slate-700">admin@incidentiq.local</span>
                <span className="text-[10px] uppercase font-bold text-purple-700 bg-purple-100 px-1.5 py-0.5 rounded">ADMIN</span>
              </button>
            </div>
          </div>
        </div>

        <p className="mt-4 text-center text-xs text-muted">
          Enterprise Security: Lockout after 5 failed attempts for 15 minutes.
        </p>
      </div>
    </div>
  );
};
