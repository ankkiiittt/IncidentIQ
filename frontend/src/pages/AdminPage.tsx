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
  Sparkles,
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
import { getAdminSettings, updateAdminSetting } from '../api/ai';
import { useAuth } from '../context/AuthContext';
import { UserRole } from '../types';

export const AdminPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { user: currentUser } = useAuth();

  const [activeTab, setActiveTab] = useState<'users' | 'policy' | 'ai'>('users');
  const [isCreateUserOpen, setIsCreateUserOpen] = useState(false);
  const [resetSuccessModal, setResetSuccessModal] = useState<{ email: string; tempPass: string } | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [saveSuccessMsg, setSaveSuccessMsg] = useState<string | null>(null);

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

  // AI Settings Query & Mutation
  const { data: aiSettings = [], isLoading: aiSettingsLoading } = useQuery({
    queryKey: ['admin-ai-settings'],
    queryFn: getAdminSettings,
    enabled: activeTab === 'ai',
  });

  const updateSettingMutation = useMutation({
    mutationFn: ({ key, value, description }: { key: string; value: unknown; description?: string }) =>
      updateAdminSetting(key, { value, description }),
    onSuccess: (updated) => {
      setSaveSuccessMsg(`Updated setting "${updated.key}" successfully.`);
      queryClient.invalidateQueries({ queryKey: ['admin-ai-settings'] });
      setTimeout(() => setSaveSuccessMsg(null), 4000);
    },
    onError: (err: Error) => setActionError(err.message),
  });

  const getSettingVal = <T,>(key: string, defaultVal: T): T => {
    const s = aiSettings.find((x) => x.key === key);
    return s && s.value !== undefined ? (s.value as T) : defaultVal;
  };

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
        <button
          onClick={() => setActiveTab('ai')}
          className={`pb-2.5 flex items-center gap-2 border-b-2 transition-colors ${
            activeTab === 'ai'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-muted hover:text-text'
          }`}
        >
          <Sparkles className="w-4 h-4" />
          AI Intelligence Configuration
        </button>
      </div>

      {saveSuccessMsg && (
        <div className="card-surface p-3 bg-emerald-50 border-emerald-200 text-xs text-emerald-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{saveSuccessMsg}</span>
          </div>
          <button onClick={() => setSaveSuccessMsg(null)} className="text-emerald-700 font-bold ml-2">
            ×
          </button>
        </div>
      )}

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

      {/* Tab 3: AI Intelligence Configuration */}
      {activeTab === 'ai' && (
        <div className="card-surface p-6 space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-border pb-4">
            <div>
              <h3 className="text-sm font-bold text-text">AI Diagnostic & RCA Engine Configuration</h3>
              <p className="text-xs text-muted mt-0.5">
                Tune retrieval-augmented generation (RAG) thresholds, automatic triage triggers, and rate quotas.
              </p>
            </div>
            <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-indigo-50 text-indigo-700 border border-indigo-200">
              Runtime Settings
            </span>
          </div>

          {aiSettingsLoading ? (
            <div className="py-12 flex flex-col items-center justify-center text-muted">
              <Loader2 className="w-6 h-6 animate-spin text-primary mb-2" />
              <span className="text-xs">Loading AI system settings...</span>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5 text-xs">
              {/* 1. Master Switch: ai.enabled */}
              <div className="p-4 rounded-lg border border-border bg-slate-50 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="space-y-0.5">
                    <span className="font-bold text-text text-xs block">AI Pipeline Master Switch</span>
                    <span className="text-[11px] text-muted">Key: ai.enabled</span>
                  </div>
                  <button
                    onClick={() =>
                      updateSettingMutation.mutate({
                        key: 'ai.enabled',
                        value: !getSettingVal('ai.enabled', true),
                      })
                    }
                    disabled={updateSettingMutation.isPending}
                    className={`px-3 py-1 rounded text-xs font-semibold transition-colors ${
                      getSettingVal('ai.enabled', true)
                        ? 'bg-emerald-600 text-white hover:bg-emerald-700'
                        : 'bg-slate-300 text-slate-700 hover:bg-slate-400'
                    }`}
                  >
                    {getSettingVal('ai.enabled', true) ? 'Enabled' : 'Disabled'}
                  </button>
                </div>
                <p className="text-[11px] text-muted leading-relaxed">
                  When disabled, all automated triage invocations and manual AI diagnostic requests are blocked with a conflict status.
                </p>
              </div>

              {/* 2. Automated Trigger: ai.auto_analyze */}
              <div className="p-4 rounded-lg border border-border bg-slate-50 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="space-y-0.5">
                    <span className="font-bold text-text text-xs block">Automated Incident Triage</span>
                    <span className="text-[11px] text-muted">Key: ai.auto_analyze</span>
                  </div>
                  <button
                    onClick={() =>
                      updateSettingMutation.mutate({
                        key: 'ai.auto_analyze',
                        value: !getSettingVal('ai.auto_analyze', true),
                      })
                    }
                    disabled={updateSettingMutation.isPending}
                    className={`px-3 py-1 rounded text-xs font-semibold transition-colors ${
                      getSettingVal('ai.auto_analyze', true)
                        ? 'bg-indigo-600 text-white hover:bg-indigo-700'
                        : 'bg-slate-300 text-slate-700 hover:bg-slate-400'
                    }`}
                  >
                    {getSettingVal('ai.auto_analyze', true) ? 'Active' : 'Off'}
                  </button>
                </div>
                <p className="text-[11px] text-muted leading-relaxed">
                  Automatically launches grounded AI diagnostic pipeline asynchronously whenever an incident is created.
                </p>
              </div>

              {/* 3. Top-K Knowledge Chunks: ai.rag.top_k */}
              <div className="p-4 rounded-lg border border-border bg-slate-50 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="space-y-0.5">
                    <span className="font-bold text-text text-xs block">RAG Retrieved Chunks (Top-K)</span>
                    <span className="text-[11px] text-muted">Key: ai.rag.top_k</span>
                  </div>
                  <span className="font-mono font-bold text-sm text-text bg-white px-2 py-0.5 rounded border border-border">
                    {getSettingVal('ai.rag.top_k', 5)}
                  </span>
                </div>
                <p className="text-[11px] text-muted leading-relaxed">
                  Maximum number of relevant runbook/postmortem vector chunks supplied in the evidence prompt context.
                </p>
                <div className="flex items-center gap-3">
                  <input
                    type="range"
                    min="1"
                    max="15"
                    step="1"
                    value={getSettingVal('ai.rag.top_k', 5)}
                    onChange={(e) =>
                      updateSettingMutation.mutate({
                        key: 'ai.rag.top_k',
                        value: parseInt(e.target.value, 10),
                      })
                    }
                    className="w-full accent-indigo-600"
                  />
                </div>
              </div>

              {/* 4. Min Similarity Threshold: ai.rag.min_similarity */}
              <div className="p-4 rounded-lg border border-border bg-slate-50 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="space-y-0.5">
                    <span className="font-bold text-text text-xs block">Minimum Similarity Threshold</span>
                    <span className="text-[11px] text-muted">Key: ai.rag.min_similarity</span>
                  </div>
                  <span className="font-mono font-bold text-sm text-text bg-white px-2 py-0.5 rounded border border-border">
                    {Math.round(getSettingVal('ai.rag.min_similarity', 0.40) * 100)}%
                  </span>
                </div>
                <p className="text-[11px] text-muted leading-relaxed">
                  Cosine similarity cutoff. If no chunks pass this threshold, analysis terminates early with INSUFFICIENT_EVIDENCE.
                </p>
                <div className="flex items-center gap-3">
                  <input
                    type="range"
                    min="0.10"
                    max="0.90"
                    step="0.05"
                    value={getSettingVal('ai.rag.min_similarity', 0.40)}
                    onChange={(e) =>
                      updateSettingMutation.mutate({
                        key: 'ai.rag.min_similarity',
                        value: parseFloat(e.target.value),
                      })
                    }
                    className="w-full accent-indigo-600"
                  />
                </div>
              </div>

              {/* 5. User Hourly Rate Limit: ai.rate_limit_per_user_per_hour */}
              <div className="p-4 rounded-lg border border-border bg-slate-50 space-y-3 md:col-span-2">
                <div className="flex items-center justify-between">
                  <div className="space-y-0.5">
                    <span className="font-bold text-text text-xs block">Manual Analysis Rate Limit</span>
                    <span className="text-[11px] text-muted">Key: ai.rate_limit_per_user_per_hour</span>
                  </div>
                  <span className="font-mono font-bold text-sm text-text bg-white px-2.5 py-0.5 rounded border border-border">
                    {getSettingVal('ai.rate_limit_per_user_per_hour', 10)} runs / hour
                  </span>
                </div>
                <p className="text-[11px] text-muted leading-relaxed">
                  Controls budget and rate limiting across engineers to prevent accidental LLM provider cost overrun.
                </p>
                <div className="flex items-center gap-3 max-w-sm">
                  <input
                    type="range"
                    min="1"
                    max="50"
                    step="1"
                    value={getSettingVal('ai.rate_limit_per_user_per_hour', 10)}
                    onChange={(e) =>
                      updateSettingMutation.mutate({
                        key: 'ai.rate_limit_per_user_per_hour',
                        value: parseInt(e.target.value, 10),
                      })
                    }
                    className="w-full accent-indigo-600"
                  />
                </div>
              </div>
            </div>
          )}
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
