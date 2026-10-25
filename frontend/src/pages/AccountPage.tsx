// src/pages/AccountPage.tsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  CheckCircle2,
  KeyRound,
  Loader2,
  Lock,
  ShieldCheck,
  User as UserIcon,
} from 'lucide-react';
import { RoleBadge } from '../components/Badges';
import { useAuth } from '../context/AuthContext';

export const AccountPage: React.FC = () => {
  const { user, changePassword } = useAuth();
  const navigate = useNavigate();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setSuccessMsg(null);

    if (newPassword.length < 8) {
      setErrorMsg('New password must be at least 8 characters long.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setErrorMsg('New password and confirmation do not match.');
      return;
    }

    setIsLoading(true);
    try {
      await changePassword(currentPassword, newPassword);
      setSuccessMsg('Your password was updated successfully. Redirecting to incidents...');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setTimeout(() => {
        navigate('/incidents');
      }, 1000);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setErrorMsg(err.message);
      } else {
        setErrorMsg('Failed to update password. Please check your current password.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h1 className="text-xl font-bold tracking-tight text-text">Account Settings</h1>
        <p className="text-xs text-muted mt-0.5">
          Manage your personal credentials and security settings
        </p>
      </div>

      {/* Must Change Password Alert */}
      {user?.mustChangePassword && (
        <div className="card-surface p-4 bg-amber-50 border-amber-200 text-xs text-amber-800 flex items-start gap-2.5">
          <KeyRound className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" />
          <div className="space-y-1">
            <span className="font-bold block">Mandatory Password Update</span>
            <p className="leading-relaxed">
              Your account is currently using a temporary password. You must update your password to a strong personal password.
            </p>
          </div>
        </div>
      )}

      {/* User Profile Card */}
      <div className="card-surface p-5 space-y-3">
        <h3 className="text-xs font-bold text-text uppercase tracking-wider border-b border-border pb-2 flex items-center gap-1.5">
          <UserIcon className="w-3.5 h-3.5 text-muted" />
          Profile Details
        </h3>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div>
            <span className="text-muted block mb-0.5">Full Name</span>
            <span className="font-semibold text-text">{user?.name}</span>
          </div>
          <div>
            <span className="text-muted block mb-0.5">Email Address</span>
            <span className="font-semibold text-text">{user?.email}</span>
          </div>
          <div>
            <span className="text-muted block mb-0.5">Platform Role</span>
            {user && <RoleBadge role={user.role} />}
          </div>
          <div>
            <span className="text-muted block mb-0.5">Session Status</span>
            <span className="inline-flex items-center gap-1 text-emerald-700 font-semibold">
              <ShieldCheck className="w-3.5 h-3.5" />
              Authenticated (Active)
            </span>
          </div>
        </div>
      </div>

      {/* Change Password Card */}
      <div className="card-surface p-5 space-y-4 shadow-sm">
        <div className="border-b border-border pb-2">
          <h3 className="text-sm font-bold text-text flex items-center gap-2">
            <Lock className="w-4 h-4 text-muted" />
            Change Password
          </h3>
          <p className="text-xs text-muted mt-0.5">
            Passwords must contain at least 8 characters.
          </p>
        </div>

        {errorMsg && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-md text-xs text-red-800 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-red-600 shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        {successMsg && (
          <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-md text-xs text-emerald-800 flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{successMsg}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block font-semibold text-text mb-1">Current Password *</label>
            <input
              type="password"
              required
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              placeholder="••••••••••••"
              className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
            />
          </div>

          <div>
            <label className="block font-semibold text-text mb-1">New Password *</label>
            <input
              type="password"
              required
              minLength={8}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="••••••••••••"
              className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
            />
          </div>

          <div>
            <label className="block font-semibold text-text mb-1">Confirm New Password *</label>
            <input
              type="password"
              required
              minLength={8}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="••••••••••••"
              className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
            />
          </div>

          <div className="pt-2 flex justify-end">
            <button
              type="submit"
              disabled={isLoading || !currentPassword || !newPassword || !confirmPassword}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-md bg-primary hover:bg-primary-hover text-white text-xs font-semibold shadow-sm transition-colors disabled:opacity-50"
            >
              {isLoading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
              Update Password
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
