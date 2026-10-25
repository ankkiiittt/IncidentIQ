// src/pages/AdminPage.tsx
import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AlertTriangle,
  CheckCircle2,
  Copy,
  KeyRound,
  Loader2,
  Plus,
  Shield,
  UserCheck,
  UserX,
  Users,
} from 'lucide-react';
import {
  createUserApi,
  getUsersApi,
  resetPasswordApi,
  updateUserApi,
} from '../api/users';
import { useAuth } from '../context/AuthContext';
import { UserRole } from '../types';

export const AdminPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { user: currentUser } = useAuth();

  const [activeTab, setActiveTab] = useState<'users' | 'policy'>('users');
  const [isCreateUserOpen, setIsCreateUserOpen] = useState(false);
  const [resetSuccessModal, setResetSuccessModal] = useState<{ email: string; tempPass: string } | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // New user form state
  const [newName, setNewName] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [newRole, setNewRole] = useState<UserRole>('ENGINEER');
  const [newTempPassword, setNewTempPassword] = useState('');

  // Fetch users
  const { data: usersData, isLoading: usersLoading } = useQuery({
    queryKey: ['admin-users'],
    queryFn: () => getUsersApi(0, 100),
  });

  const users = usersData?.content || [];

  // Mutations
  const createUserMutation = useMutation({
    mutationFn: createUserApi,
    onSuccess: () => {
      setIsCreateUserOpen(false);
      setNewName('');
      setNewEmail('');
      setNewTempPassword('');
      setActionError(null);
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
    onError: (err: Error) => setActionError(err.message),
  });

  const updateUserMutation = useMutation({
    mutationFn: ({ id, role, enabled }: { id: string; role?: UserRole; enabled?: boolean }) =>
      updateUserApi(id, { role, enabled }),
    onSuccess: () => {
      setActionError(null);
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
    onError: (err: Error) => setActionError(err.message),
  });

  const resetPasswordMutation = useMutation({
    mutationFn: resetPasswordApi,
    onSuccess: (data, userId) => {
      const u = users.find((x) => x.id === userId);
      setResetSuccessModal({
        email: u?.email || 'User',
        tempPass: data.temporaryPassword,
      });
      setActionError(null);
    },
    onError: (err: Error) => setActionError(err.message),
  });

  if (currentUser?.role !== 'ADMIN') {
    return (
      <div className="card-surface p-12 text-center max-w-md mx-auto my-12">
        <Shield className="w-12 h-12 text-red-500 mx-auto mb-3" />
        <h2 className="text-lg font-bold text-text mb-1">Access Restricted</h2>
        <p className="text-xs text-muted">
          Only users with the ADMIN role can access user and system administration controls.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-text">Administration</h1>
          <p className="text-xs text-muted mt-0.5">
            Role-based user management, permissions, and security policies
          </p>
        </div>

        <button
          onClick={() => setIsCreateUserOpen(true)}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-primary hover:bg-primary-hover text-white text-xs font-semibold shadow-sm transition-colors"
        >
          <Plus className="w-4 h-4" />
          Create User
        </button>
      </div>

      {actionError && (
        <div className="card-surface p-3 bg-red-50 border-red-200 text-xs text-red-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-red-600 shrink-0" />
            <span>{actionError}</span>
          </div>
          <button onClick={() => setActionError(null)} className="text-red-700 font-bold ml-2">
            ×
          </button>
        </div>
      )}

      {/* Tabs */}
      <div className="border-b border-border flex gap-4 text-xs font-semibold">
        <button
          onClick={() => setActiveTab('users')}
          className={`pb-2.5 flex items-center gap-2 border-b-2 transition-colors ${
            activeTab === 'users'
              ? 'border-primary text-primary'
              : 'border-transparent text-muted hover:text-text'
          }`}
        >
          <Users className="w-4 h-4" />
          User Management ({users.length})
        </button>
        <button
          onClick={() => setActiveTab('policy')}
          className={`pb-2.5 flex items-center gap-2 border-b-2 transition-colors ${
            activeTab === 'policy'
              ? 'border-primary text-primary'
              : 'border-transparent text-muted hover:text-text'
          }`}
        >
          <Shield className="w-4 h-4" />
          Access & Lockout Policies
        </button>
      </div>

      {/* Tab 1: Users Table */}
      {activeTab === 'users' && (
        <div className="card-surface overflow-hidden shadow-sm">
          {usersLoading ? (
            <div className="py-20 flex flex-col items-center justify-center text-muted">
              <Loader2 className="w-8 h-8 animate-spin text-primary mb-3" />
              <p className="text-xs">Loading user registry...</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 border-b border-border text-muted font-semibold uppercase tracking-wider text-[11px]">
                  <tr>
                    <th className="py-3 px-4">User</th>
                    <th className="py-3 px-3">Role</th>
                    <th className="py-3 px-3">Status</th>
                    <th className="py-3 px-3">Security Flag</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {users.map((u) => (
                    <tr key={u.id} className="hover:bg-slate-50/80 transition-colors">
                      <td className="py-3 px-4">
                        <div className="font-semibold text-text">{u.name}</div>
                        <div className="text-[11px] text-muted">{u.email}</div>
                      </td>
                      <td className="py-3 px-3">
                        <select
                          value={u.role}
                          onChange={(e) =>
                            updateUserMutation.mutate({
                              id: u.id,
                              role: e.target.value as UserRole,
                            })
                          }
                          className="bg-slate-50 border border-border rounded px-2 py-1 text-xs text-text focus:bg-white focus:outline-none"
                        >
                          <option value="ADMIN">ADMIN</option>
                          <option value="ENGINEER">ENGINEER</option>
                          <option value="SUPPORT">SUPPORT</option>
                          <option value="VIEWER">VIEWER</option>
                        </select>
                      </td>
                      <td className="py-3 px-3">
                        <button
                          onClick={() =>
                            updateUserMutation.mutate({
                              id: u.id,
                              enabled: !u.enabled,
                            })
                          }
                          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold transition-colors ${
                            u.enabled
                              ? 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                              : 'bg-red-50 text-red-700 hover:bg-red-100'
                          }`}
                        >
                          {u.enabled ? <UserCheck className="w-3 h-3" /> : <UserX className="w-3 h-3" />}
                          {u.enabled ? 'Active' : 'Disabled'}
                        </button>
                      </td>
                      <td className="py-3 px-3">
                        {u.mustChangePassword ? (
                          <span className="inline-flex items-center gap-1 text-[10px] font-medium text-amber-700 bg-amber-50 px-1.5 py-0.5 rounded border border-amber-200">
                            <KeyRound className="w-3 h-3" />
                            Must Change Password
                          </span>
                        ) : (
                          <span className="text-[11px] text-muted">Standard</span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <button
                          onClick={() => {
                            if (confirm(`Reset temporary password for ${u.email}?`)) {
                              resetPasswordMutation.mutate(u.id);
                            }
                          }}
                          className="text-xs font-semibold text-primary hover:text-primary-hover hover:underline"
                        >
                          Reset Password
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Tab 2: Security Policies info */}
      {activeTab === 'policy' && (
        <div className="card-surface p-6 space-y-6">
          <div>
            <h3 className="text-sm font-bold text-text mb-1">Security & Access Policy Controls</h3>
            <p className="text-xs text-muted leading-relaxed">
              Platform authentication security configurations active in the backend runtime.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
            <div className="p-4 rounded-md border border-border bg-slate-50 space-y-1.5">
              <span className="font-bold text-text block">Account Lockout Policy</span>
              <p className="text-muted leading-relaxed">
                Accounts are automatically locked for <span className="font-semibold text-text">15 minutes</span> after{' '}
                <span className="font-semibold text-text">5 consecutive failed login attempts</span>.
              </p>
            </div>

            <div className="p-4 rounded-md border border-border bg-slate-50 space-y-1.5">
              <span className="font-bold text-text block">Access Token Expiry & Rotation</span>
              <p className="text-muted leading-relaxed">
                JWT Access Tokens expire in <span className="font-semibold text-text">15 minutes</span>. Refresh tokens rotate on every use, with full family revocation upon reuse detection.
              </p>
            </div>

            <div className="p-4 rounded-md border border-border bg-slate-50 space-y-1.5">
              <span className="font-bold text-text block">Safeguard Rule: Admin Protection</span>
              <p className="text-muted leading-relaxed">
                The last active administrator account cannot be demoted or disabled to prevent lockout.
              </p>
            </div>

            <div className="p-4 rounded-md border border-border bg-slate-50 space-y-1.5">
              <span className="font-bold text-text block">Audit Immutability</span>
              <p className="text-muted leading-relaxed">
                All login attempts, user modifications, and role updates are written to an append-only audit log protected by database triggers.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Create User Modal */}
      {isCreateUserOpen && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              createUserMutation.mutate({
                name: newName.trim(),
                email: newEmail.trim(),
                role: newRole,
                temporaryPassword: newTempPassword.trim() || undefined,
              });
            }}
            className="card-surface p-5 max-w-md w-full shadow-lg space-y-4"
          >
            <h3 className="text-sm font-bold text-text">Create User Account</h3>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-text mb-1">Full Name *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g., Alex Engineer"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-text mb-1">Email Address *</label>
                <input
                  type="email"
                  required
                  placeholder="name@company.com"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                  className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-text mb-1">Role *</label>
                <select
                  value={newRole}
                  onChange={(e) => setNewRole(e.target.value as UserRole)}
                  className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
                >
                  <option value="ADMIN">ADMIN</option>
                  <option value="ENGINEER">ENGINEER</option>
                  <option value="SUPPORT">SUPPORT</option>
                  <option value="VIEWER">VIEWER</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-text mb-1">
                  Temporary Password (Optional)
                </label>
                <input
                  type="text"
                  placeholder="Leave blank to auto-generate"
                  value={newTempPassword}
                  onChange={(e) => setNewTempPassword(e.target.value)}
                  className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text font-mono focus:bg-white focus:outline-none"
                />
                <span className="text-[10px] text-muted block mt-1">
                  User will be prompted to change this on their first login.
                </span>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-border">
              <button
                type="button"
                onClick={() => setIsCreateUserOpen(false)}
                className="px-3 py-1.5 rounded border border-border text-xs text-muted hover:text-text"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={createUserMutation.isPending}
                className="px-3 py-1.5 rounded bg-primary hover:bg-primary-hover text-white text-xs font-semibold disabled:opacity-50"
              >
                Create Account
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Password Reset Modal */}
      {resetSuccessModal && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="card-surface p-5 max-w-md w-full shadow-lg space-y-4">
            <div className="flex items-center gap-2 text-emerald-700 font-bold text-sm">
              <CheckCircle2 className="w-5 h-5" />
              Password Reset Successfully
            </div>

            <p className="text-xs text-muted">
              Temporary password generated for <span className="font-semibold text-text">{resetSuccessModal.email}</span>. Share this securely with the user:
            </p>

            <div className="flex items-center justify-between p-3 rounded bg-slate-100 border border-slate-300 font-mono text-sm text-text font-bold">
              <span>{resetSuccessModal.tempPass}</span>
              <button
                onClick={() => {
                  navigator.clipboard.writeText(resetSuccessModal.tempPass);
                  alert('Temporary password copied to clipboard!');
                }}
                className="p-1 rounded hover:bg-slate-200 text-muted hover:text-text"
                title="Copy to Clipboard"
              >
                <Copy className="w-4 h-4" />
              </button>
            </div>

            <div className="flex justify-end">
              <button
                type="button"
                onClick={() => setResetSuccessModal(null)}
                className="px-3 py-1.5 rounded bg-primary text-white text-xs font-semibold"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
