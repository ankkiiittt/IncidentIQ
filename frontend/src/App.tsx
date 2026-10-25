import React, { useEffect, useState } from 'react';
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  Cpu,
  Layers,
  Search,
  Server,
  Sparkles,
} from 'lucide-react';

export default function App(): React.JSX.Element {
  const [backendStatus, setBackendStatus] = useState<'checking' | 'connected' | 'offline'>('checking');

  useEffect(() => {
    fetch('/actuator/health')
      .then((res) => {
        if (res.ok) return res.json();
        throw new Error('Not OK');
      })
      .then(() => setBackendStatus('connected'))
      .catch(() => setBackendStatus('offline'));
  }, []);

  return (
    <div className="min-h-screen bg-bg text-text flex flex-col font-sans">
      {/* Top Advisory Banner */}
      <div className="bg-amber-50 border-b border-amber-200 px-4 py-2 text-xs text-amber-800 text-center font-medium">
        Disclaimer: IncidentIQ AI output is advisory. Recommendations are suggestions that the system never executes. Humans make every decision.
      </div>

      {/* Main Layout Container */}
      <div className="flex-1 flex max-w-layout w-full mx-auto">
        {/* Left Sidebar Skeleton (240px) */}
        <aside className="w-60 border-r border-border bg-surface p-4 flex flex-col justify-between hidden md:flex">
          <div className="space-y-6">
            <div className="flex items-center gap-2 px-2 py-1">
              <div className="w-7 h-7 rounded bg-primary text-white flex items-center justify-center font-semibold text-sm">
                IQ
              </div>
              <span className="font-semibold text-base tracking-tight text-text">IncidentIQ</span>
            </div>

            <nav className="space-y-1 text-sm font-medium">
              <a
                href="#dashboard"
                className="flex items-center gap-3 px-3 py-2 rounded-md bg-slate-100 text-primary font-medium"
              >
                <Activity className="w-4 h-4 text-primary" />
                Dashboard
              </a>
              <a
                href="#incidents"
                className="flex items-center gap-3 px-3 py-2 rounded-md text-muted hover:text-text hover:bg-slate-50 transition-colors"
              >
                <AlertTriangle className="w-4 h-4 text-muted" />
                Incidents
              </a>
              <a
                href="#services"
                className="flex items-center gap-3 px-3 py-2 rounded-md text-muted hover:text-text hover:bg-slate-50 transition-colors"
              >
                <Server className="w-4 h-4 text-muted" />
                Services
              </a>
              <a
                href="#knowledge"
                className="flex items-center gap-3 px-3 py-2 rounded-md text-muted hover:text-text hover:bg-slate-50 transition-colors"
              >
                <Layers className="w-4 h-4 text-muted" />
                Knowledge Base
              </a>
            </nav>
          </div>

          {/* System Environment Status */}
          <div className="card-surface p-3 text-xs space-y-2">
            <div className="text-muted font-medium flex items-center justify-between">
              <span>Backend API</span>
              <span
                className={`inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold ${
                  backendStatus === 'connected'
                    ? 'bg-success-bg text-success-text'
                    : backendStatus === 'checking'
                    ? 'bg-slate-100 text-muted'
                    : 'bg-amber-100 text-amber-800'
                }`}
              >
                {backendStatus === 'connected' ? 'LIVE' : backendStatus === 'checking' ? 'PROBING' : 'READY (LOCAL)'}
              </span>
            </div>
            <p className="text-muted leading-relaxed">
              Target: Java 21 LTS + Spring Boot 3.3.5
            </p>
          </div>
        </aside>

        {/* Primary Content Area */}
        <main className="flex-1 flex flex-col min-w-0">
          {/* Top Bar */}
          <header className="h-14 border-b border-border bg-surface px-6 flex items-center justify-between">
            <div className="relative w-72">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
              <input
                type="text"
                placeholder="Search incidents, runbooks..."
                aria-label="Search"
                className="w-full pl-9 pr-3 py-1.5 text-xs bg-bg border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary text-text placeholder:text-muted"
                disabled
              />
            </div>

            <div className="flex items-center gap-3">
              <span className="badge-ai">
                <Sparkles className="w-3 h-3" />
                AI Advisory Active
              </span>
              <div className="w-8 h-8 rounded-full bg-slate-200 border border-border flex items-center justify-center text-xs font-semibold text-text">
                AD
              </div>
            </div>
          </header>

          {/* Body Content */}
          <div className="p-6 space-y-6 flex-1 overflow-y-auto">
            {/* Header info */}
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-xl font-semibold text-text tracking-tight">Phase P0: Foundation Active</h1>
                <p className="text-xs text-muted mt-1">
                  Single-tenant modular monolith baseline initialized with strict quality gates.
                </p>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-xs px-2.5 py-1 rounded-full border border-border bg-surface font-mono text-muted">
                  Milestone: P0 Initialized
                </span>
              </div>
            </div>

            {/* Design Tokens Verification Grid */}
            <div className="card-surface p-5 space-y-4">
              <div className="border-b border-border pb-3">
                <h2 className="text-sm font-semibold text-text">Design System Tokens (UI/UX Brief Section 3)</h2>
                <p className="text-xs text-muted mt-0.5">
                  Validating WCAG 2.1 AA compliant semantic tint tokens and typographical hierarchy.
                </p>
              </div>

              {/* Priority Chips */}
              <div className="space-y-2">
                <span className="text-xs font-medium text-muted">Priority Chips:</span>
                <div className="flex flex-wrap items-center gap-2 text-xs font-semibold">
                  <span className="px-2.5 py-1 rounded bg-p1-bg text-p1-text border border-red-200">
                    P1 Critical
                  </span>
                  <span className="px-2.5 py-1 rounded bg-p2-bg text-p2-text border border-orange-200">
                    P2 High
                  </span>
                  <span className="px-2.5 py-1 rounded bg-p3-bg text-p3-text border border-yellow-200">
                    P3 Medium
                  </span>
                  <span className="px-2.5 py-1 rounded bg-p4-bg text-p4-text border border-slate-200">
                    P4 Low
                  </span>
                  <span className="px-2.5 py-1 rounded bg-success-bg text-success-text border border-emerald-200 flex items-center gap-1">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    Resolved
                  </span>
                  <span className="px-2.5 py-1 rounded bg-ai-bg text-ai-text border border-ai-border flex items-center gap-1">
                    <Sparkles className="w-3.5 h-3.5" />
                    AI-generated
                  </span>
                </div>
              </div>

              {/* Chart Series Swatches */}
              <div className="space-y-2 pt-2">
                <span className="text-xs font-medium text-muted">Chart Series Tokens (Series 1-6):</span>
                <div className="flex items-center gap-3">
                  <div className="flex items-center gap-1.5 text-xs text-muted">
                    <span className="w-4 h-4 rounded bg-series-1" /> Series 1
                  </div>
                  <div className="flex items-center gap-1.5 text-xs text-muted">
                    <span className="w-4 h-4 rounded bg-series-2" /> Series 2
                  </div>
                  <div className="flex items-center gap-1.5 text-xs text-muted">
                    <span className="w-4 h-4 rounded bg-series-3" /> Series 3
                  </div>
                  <div className="flex items-center gap-1.5 text-xs text-muted">
                    <span className="w-4 h-4 rounded bg-series-4" /> Series 4
                  </div>
                  <div className="flex items-center gap-1.5 text-xs text-muted">
                    <span className="w-4 h-4 rounded bg-series-5" /> Series 5
                  </div>
                  <div className="flex items-center gap-1.5 text-xs text-muted">
                    <span className="w-4 h-4 rounded bg-series-6" /> Series 6
                  </div>
                </div>
              </div>
            </div>

            {/* Architecture Invariants Card */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="card-surface p-4 space-y-2">
                <div className="flex items-center gap-2 text-primary font-semibold text-sm">
                  <Cpu className="w-4 h-4" />
                  <span>Backend Core</span>
                </div>
                <p className="text-xs text-muted leading-relaxed">
                  Java 21 LTS with Spring Boot 3.3.5. Package-by-feature architecture with virtual threads enabled and constructor injection.
                </p>
              </div>

              <div className="card-surface p-4 space-y-2">
                <div className="flex items-center gap-2 text-primary font-semibold text-sm">
                  <Layers className="w-4 h-4" />
                  <span>PostgreSQL + pgvector</span>
                </div>
                <p className="text-xs text-muted leading-relaxed">
                  Flyway reproducible migrations with HNSW cosine similarity index (384 dimensions) and strict SQL pre-filtering.
                </p>
              </div>

              <div className="card-surface p-4 space-y-2">
                <div className="flex items-center gap-2 text-primary font-semibold text-sm">
                  <Activity className="w-4 h-4" />
                  <span>RFC 7807 & Resilience</span>
                </div>
                <p className="text-xs text-muted leading-relaxed">
                  Structured ProblemDetail errors, MDC correlation IDs on every request, and graceful degradation on external failures.
                </p>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
