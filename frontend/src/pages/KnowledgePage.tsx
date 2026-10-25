// src/pages/KnowledgePage.tsx
// Screen S7: Knowledge Base & Vector Search

import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AlertCircle,
  BookOpen,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  ExternalLink,
  FileText,
  Filter,
  Layers,
  Loader2,
  Plus,
  Search,
  Shield,
  UploadCloud,
  X,
} from 'lucide-react';
import {
  createKnowledgeDocumentApi,
  getKnowledgeDocumentChunksApi,
  listKnowledgeDocumentsApi,
  semanticSearchApi,
} from '../api/knowledge';
import { getServicesApi } from '../api/services';
import { useAuth } from '../context/AuthContext';
import {
  AccessLevel,
  DocumentSourceType,
  KnowledgeChunk,
  KnowledgeDocument,
  SemanticSearchResult,
  Service,
} from '../types';

export const KnowledgePage: React.FC = () => {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<'search' | 'documents'>('search');

  // Search States
  const [searchQuery, setSearchQuery] = useState('');
  const [sourceTypeFilter, setSourceTypeFilter] = useState<DocumentSourceType | ''>('');
  const [serviceFilter, setServiceFilter] = useState('');
  const [minSimilarity, setMinSimilarity] = useState(0.4);
  const [searchLimit, setSearchLimit] = useState(5);
  const [expandedChunkId, setExpandedChunkId] = useState<string | null>(null);

  // Documents Library States
  const [docSourceFilter, setDocSourceFilter] = useState<DocumentSourceType | ''>('');
  const [docServiceFilter, setDocServiceFilter] = useState('');
  const [selectedDocForChunks, setSelectedDocForChunks] = useState<KnowledgeDocument | null>(null);

  // Upload Modal State
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [uploadTitle, setUploadTitle] = useState('');
  const [uploadSourceType, setUploadSourceType] = useState<DocumentSourceType>('RUNBOOK');
  const [uploadAccessLevel, setUploadAccessLevel] = useState<AccessLevel>('PUBLIC');
  const [uploadServiceId, setUploadServiceId] = useState('');
  const [uploadContent, setUploadContent] = useState('');
  const [uploadError, setUploadError] = useState<string | null>(null);

  // Fetch Services for dropdowns
  const { data: servicesData } = useQuery({
    queryKey: ['services'],
    queryFn: () => getServicesApi(false, 0, 100),
  });
  const services: Service[] = servicesData?.content || [];

  // Semantic Search Mutation
  const searchMutation = useMutation({
    mutationFn: () =>
      semanticSearchApi({
        query: searchQuery.trim(),
        sourceType: sourceTypeFilter ? sourceTypeFilter : undefined,
        serviceId: serviceFilter ? serviceFilter : undefined,
        minSimilarity,
        limit: searchLimit,
      }),
  });

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      searchMutation.mutate();
    }
  };

  // Documents Query
  const {
    data: documentsData,
    isLoading: documentsLoading,
    refetch: refetchDocuments,
  } = useQuery({
    queryKey: ['knowledge-documents', docSourceFilter, docServiceFilter],
    queryFn: () =>
      listKnowledgeDocumentsApi({
        sourceType: docSourceFilter ? docSourceFilter : undefined,
        serviceId: docServiceFilter ? docServiceFilter : undefined,
        size: 50,
      }),
    enabled: activeTab === 'documents',
  });

  // Chunks Query for Selected Document
  const {
    data: docChunks = [],
    isLoading: chunksLoading,
  } = useQuery({
    queryKey: ['knowledge-chunks', selectedDocForChunks?.id],
    queryFn: () => getKnowledgeDocumentChunksApi(selectedDocForChunks!.id),
    enabled: !!selectedDocForChunks,
  });

  // Upload Document Mutation
  const uploadMutation = useMutation({
    mutationFn: () =>
      createKnowledgeDocumentApi({
        title: uploadTitle.trim(),
        sourceType: uploadSourceType,
        accessLevel: uploadAccessLevel,
        serviceId: uploadServiceId ? uploadServiceId : undefined,
        content: uploadContent,
      }),
    onSuccess: () => {
      setIsUploadOpen(false);
      setUploadTitle('');
      setUploadContent('');
      setUploadError(null);
      queryClient.invalidateQueries({ queryKey: ['knowledge-documents'] });
      refetchDocuments();
    },
    onError: (err: Error) => {
      setUploadError(err.message || 'Failed to upload document');
    },
  });

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 200 * 1024) {
      setUploadError('Document size exceeds 200 KB limit.');
      return;
    }

    if (!uploadTitle) {
      const fileNameWithoutExt = file.name.replace(/\.[^/.]+$/, '');
      setUploadTitle(fileNameWithoutExt);
    }

    const reader = new FileReader();
    reader.onload = (evt) => {
      setUploadContent(evt.target?.result as string || '');
      setUploadError(null);
    };
    reader.onerror = () => setUploadError('Failed to read document file.');
    reader.readAsText(file);
  };

  const handleUploadSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadTitle.trim()) {
      setUploadError('Document title is required.');
      return;
    }
    if (!uploadContent.trim()) {
      setUploadError('Document content cannot be empty.');
      return;
    }
    if (new Blob([uploadContent]).size > 200 * 1024) {
      setUploadError('Document content exceeds 200 KB limit.');
      return;
    }
    setUploadError(null);
    uploadMutation.mutate();
  };

  const canUpload = user && (user.role === 'ADMIN' || user.role === 'ENGINEER');

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <BookOpen className="w-5 h-5 text-primary" />
            <h1 className="text-xl font-bold text-text tracking-tight">Knowledge Base & Intelligence</h1>
          </div>
          <p className="text-xs text-muted mt-1">
            Semantic vector search across operational runbooks, postmortems, FAQs, and resolved incident resolutions.
          </p>
        </div>

        {/* Tab Controls & Upload Button */}
        <div className="flex items-center gap-3">
          <div className="flex bg-slate-100 p-0.5 rounded-lg border border-border text-xs font-semibold">
            <button
              onClick={() => setActiveTab('search')}
              className={`px-3 py-1.5 rounded-md transition-colors flex items-center gap-1.5 ${
                activeTab === 'search'
                  ? 'bg-white text-text shadow-sm'
                  : 'text-muted hover:text-text'
              }`}
            >
              <Search className="w-3.5 h-3.5" />
              Semantic Search
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`px-3 py-1.5 rounded-md transition-colors flex items-center gap-1.5 ${
                activeTab === 'documents'
                  ? 'bg-white text-text shadow-sm'
                  : 'text-muted hover:text-text'
              }`}
            >
              <FileText className="w-3.5 h-3.5" />
              Document Library
            </button>
          </div>

          {canUpload && (
            <button
              onClick={() => {
                setUploadError(null);
                setIsUploadOpen(true);
              }}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-primary hover:bg-primary-hover text-white text-xs font-semibold transition-colors shadow-sm"
            >
              <Plus className="w-3.5 h-3.5" />
              Upload Document
            </button>
          )}
        </div>
      </div>

      {/* TAB 1: SEMANTIC SEARCH */}
      {activeTab === 'search' && (
        <div className="space-y-6">
          {/* Search Box Card */}
          <div className="card-surface p-5 space-y-4">
            <form onSubmit={handleSearchSubmit} className="space-y-3">
              <div className="relative">
                <Search className="w-5 h-5 text-muted absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Describe an incident, symptom, error message or architectural query (e.g. 'HikariCP connection pool timeout')..."
                  className="w-full bg-slate-50 border border-border rounded-lg pl-11 pr-28 py-2.5 text-sm text-text placeholder-muted focus:bg-white focus:border-primary focus:outline-none transition-colors"
                />
                <button
                  type="submit"
                  disabled={searchMutation.isPending || !searchQuery.trim()}
                  className="absolute right-2 top-1/2 -translate-y-1/2 px-4 py-1.5 rounded-md bg-primary hover:bg-primary-hover disabled:opacity-50 text-white text-xs font-semibold transition-colors flex items-center gap-1.5"
                >
                  {searchMutation.isPending ? (
                    <>
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      Searching...
                    </>
                  ) : (
                    'Search'
                  )}
                </button>
              </div>

              {/* Filters Row */}
              <div className="flex flex-wrap items-center gap-3 pt-1 text-xs">
                <div className="flex items-center gap-1.5 text-muted">
                  <Filter className="w-3.5 h-3.5" />
                  <span className="font-medium">Filters:</span>
                </div>

                {/* Source Type Filter */}
                <select
                  value={sourceTypeFilter}
                  onChange={(e) => setSourceTypeFilter(e.target.value as DocumentSourceType | '')}
                  className="bg-slate-50 border border-border rounded px-2.5 py-1 text-text focus:outline-none focus:border-primary"
                >
                  <option value="">All Document Types</option>
                  <option value="RUNBOOK">Runbooks</option>
                  <option value="POSTMORTEM">Postmortems</option>
                  <option value="FAQ">FAQs</option>
                  <option value="INCIDENT">Resolved Incidents</option>
                </select>

                {/* Service Filter */}
                <select
                  value={serviceFilter}
                  onChange={(e) => setServiceFilter(e.target.value)}
                  className="bg-slate-50 border border-border rounded px-2.5 py-1 text-text focus:outline-none focus:border-primary"
                >
                  <option value="">All Services</option>
                  {services.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name} ({s.environment})
                    </option>
                  ))}
                </select>

                {/* Min Similarity Slider / Dropdown */}
                <div className="flex items-center gap-1.5 ml-auto">
                  <span className="text-muted">Min Match:</span>
                  <select
                    value={minSimilarity}
                    onChange={(e) => setMinSimilarity(parseFloat(e.target.value))}
                    className="bg-slate-50 border border-border rounded px-2 py-1 text-text focus:outline-none focus:border-primary tabular-nums"
                  >
                    <option value="0.30">30% (Broad)</option>
                    <option value="0.40">40% (Standard)</option>
                    <option value="0.50">50% (Strict)</option>
                    <option value="0.60">60% (High Precision)</option>
                  </select>

                  <span className="text-muted ml-2">Top:</span>
                  <select
                    value={searchLimit}
                    onChange={(e) => setSearchLimit(parseInt(e.target.value, 10))}
                    className="bg-slate-50 border border-border rounded px-2 py-1 text-text focus:outline-none focus:border-primary tabular-nums"
                  >
                    <option value="3">3</option>
                    <option value="5">5</option>
                    <option value="10">10</option>
                  </select>
                </div>
              </div>
            </form>
          </div>

          {/* Search Results Display */}
          <div className="space-y-4">
            {searchMutation.isSuccess && (
              <div className="flex items-center justify-between text-xs text-muted px-1">
                <span>
                  Found <strong className="text-text font-bold">{searchMutation.data.length}</strong> matching chunk(s) across knowledge documents
                </span>
                <span>Sorted by pgvector cosine similarity</span>
              </div>
            )}

            {searchMutation.isPending && (
              <div className="card-surface p-12 text-center text-muted">
                <Loader2 className="w-6 h-6 animate-spin mx-auto text-primary mb-2" />
                <p className="text-xs">Computing deterministic query embedding & running pgvector search...</p>
              </div>
            )}

            {searchMutation.isSuccess && searchMutation.data.length === 0 && (
              <div className="card-surface p-8 text-center text-muted space-y-2">
                <AlertCircle className="w-8 h-8 mx-auto text-amber-500" />
                <h3 className="text-sm font-semibold text-text">No relevant knowledge chunks found</h3>
                <p className="text-xs max-w-md mx-auto">
                  No chunks reached the {Math.round(minSimilarity * 100)}% similarity threshold. Try lowering the minimum match percentage or adjusting your search keywords.
                </p>
              </div>
            )}

            {searchMutation.data && searchMutation.data.map((res: SemanticSearchResult) => {
              const isExpanded = expandedChunkId === res.chunkId;
              const matchPercent = Math.round(res.similarity * 100);

              let matchColor = 'bg-slate-100 text-slate-700 border-slate-200';
              if (matchPercent >= 60) {
                matchColor = 'bg-emerald-50 text-emerald-700 border-emerald-200';
              } else if (matchPercent >= 45) {
                matchColor = 'bg-blue-50 text-blue-700 border-blue-200';
              }

              return (
                <div
                  key={res.chunkId}
                  className="card-surface p-5 border-l-4 border-l-primary hover:border-slate-300 transition-all space-y-3"
                >
                  {/* Result Header */}
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span
                          className={`px-2 py-0.5 rounded text-xs font-bold border tabular-nums ${matchColor}`}
                        >
                          {matchPercent}% Match
                        </span>
                        <h3 className="text-sm font-bold text-text hover:text-primary transition-colors">
                          {res.documentTitle}
                        </h3>
                      </div>

                      {/* Heading Hierarchy Breadcrumb */}
                      <p className="text-xs text-muted font-medium flex items-center gap-1.5">
                        <Layers className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                        <span className="truncate">{res.headingPath}</span>
                      </p>
                    </div>

                    {/* Metadata Badges */}
                    <div className="flex items-center gap-1.5 text-[11px]">
                      <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700 font-semibold uppercase tracking-wider">
                        {res.sourceType}
                      </span>
                      {res.serviceName && (
                        <span className="px-2 py-0.5 rounded bg-indigo-50 text-indigo-700 font-medium">
                          {res.serviceName}
                        </span>
                      )}
                      {res.accessLevel === 'ENGINEERING' && (
                        <span className="px-2 py-0.5 rounded bg-amber-50 text-amber-700 font-medium flex items-center gap-1">
                          <Shield className="w-3 h-3" />
                          ENG ONLY
                        </span>
                      )}
                      <span className="text-muted tabular-nums">~{res.tokenCount} tokens</span>
                    </div>
                  </div>

                  {/* Content / Snippet */}
                  <div className="bg-slate-50 rounded-md p-3 text-xs text-text border border-border/80 font-mono leading-relaxed whitespace-pre-wrap">
                    {isExpanded ? res.content : res.snippet}
                  </div>

                  {/* Expand / Collapse Action */}
                  <div className="flex items-center justify-between pt-1 text-xs">
                    <button
                      onClick={() => setExpandedChunkId(isExpanded ? null : res.chunkId)}
                      className="inline-flex items-center gap-1 text-primary hover:text-primary-hover font-semibold transition-colors"
                    >
                      {isExpanded ? (
                        <>
                          <ChevronUp className="w-3.5 h-3.5" />
                          Show Less
                        </>
                      ) : (
                        <>
                          <ChevronDown className="w-3.5 h-3.5" />
                          Show Full Chunk Content
                        </>
                      )}
                    </button>

                    <button
                      onClick={() => {
                        setSelectedDocForChunks({
                          id: res.documentId,
                          title: res.documentTitle,
                          sourceType: res.sourceType,
                          accessLevel: res.accessLevel,
                          status: 'INGESTED',
                          chunkCount: 0,
                          version: 1,
                          contentHash: '',
                          createdAt: '',
                          updatedAt: '',
                        });
                      }}
                      className="inline-flex items-center gap-1 text-muted hover:text-text font-medium transition-colors"
                    >
                      <ExternalLink className="w-3.5 h-3.5" />
                      View All Document Chunks
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* TAB 2: DOCUMENT LIBRARY */}
      {activeTab === 'documents' && (
        <div className="space-y-4">
          {/* Filter Bar */}
          <div className="card-surface p-4 flex flex-wrap items-center justify-between gap-4">
            <div className="flex flex-wrap items-center gap-3 text-xs">
              <span className="font-semibold text-text">Filter Documents:</span>
              <select
                value={docSourceFilter}
                onChange={(e) => setDocSourceFilter(e.target.value as DocumentSourceType | '')}
                className="bg-slate-50 border border-border rounded px-2.5 py-1 text-text focus:outline-none focus:border-primary"
              >
                <option value="">All Source Types</option>
                <option value="RUNBOOK">Runbook</option>
                <option value="POSTMORTEM">Postmortem</option>
                <option value="FAQ">FAQ</option>
                <option value="INCIDENT">Incident Resolution</option>
              </select>

              <select
                value={docServiceFilter}
                onChange={(e) => setDocServiceFilter(e.target.value)}
                className="bg-slate-50 border border-border rounded px-2.5 py-1 text-text focus:outline-none focus:border-primary"
              >
                <option value="">All Services</option>
                {services.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>

            <span className="text-xs text-muted tabular-nums">
              Total Documents: {documentsData?.totalElements ?? documentsData?.content?.length ?? 0}
            </span>
          </div>

          {/* Documents Table */}
          <div className="card-surface overflow-hidden">
            {documentsLoading ? (
              <div className="p-12 text-center text-muted">
                <Loader2 className="w-6 h-6 animate-spin mx-auto text-primary mb-2" />
                <p className="text-xs">Loading knowledge documents...</p>
              </div>
            ) : !documentsData?.content || documentsData.content.length === 0 ? (
              <div className="p-12 text-center text-muted">
                <FileText className="w-8 h-8 mx-auto text-slate-300 mb-2" />
                <p className="text-xs font-medium">No documents found matching the filter.</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="border-b border-border bg-slate-50/75 text-muted uppercase font-bold tracking-wider text-[11px]">
                      <th className="py-3 px-4">Title</th>
                      <th className="py-3 px-3">Type</th>
                      <th className="py-3 px-3">Access</th>
                      <th className="py-3 px-3">Service</th>
                      <th className="py-3 px-3">Chunks</th>
                      <th className="py-3 px-3">Status</th>
                      <th className="py-3 px-3">Created</th>
                      <th className="py-3 px-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {documentsData.content.map((doc: KnowledgeDocument) => (
                      <tr key={doc.id} className="hover:bg-slate-50/80 transition-colors">
                        <td className="py-3 px-4">
                          <span className="font-semibold text-text block">{doc.title}</span>
                          {doc.errorMessage && (
                            <span className="text-[11px] text-red-600 block mt-0.5 truncate max-w-sm">
                              {doc.errorMessage}
                            </span>
                          )}
                        </td>
                        <td className="py-3 px-3">
                          <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700 font-semibold uppercase text-[10px] tracking-wider">
                            {doc.sourceType}
                          </span>
                        </td>
                        <td className="py-3 px-3">
                          {doc.accessLevel === 'ENGINEERING' ? (
                            <span className="px-2 py-0.5 rounded bg-amber-50 text-amber-800 font-semibold text-[10px] border border-amber-200">
                              ENGINEERING
                            </span>
                          ) : (
                            <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-600 font-medium text-[10px]">
                              PUBLIC
                            </span>
                          )}
                        </td>
                        <td className="py-3 px-3 text-muted">
                          {doc.serviceName || '—'}
                        </td>
                        <td className="py-3 px-3 font-mono font-medium text-text">
                          {doc.chunkCount}
                        </td>
                        <td className="py-3 px-3">
                          {doc.status === 'INGESTED' ? (
                            <span className="inline-flex items-center gap-1 text-emerald-700 font-semibold">
                              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                              Ingested
                            </span>
                          ) : doc.status === 'PROCESSING' ? (
                            <span className="inline-flex items-center gap-1 text-blue-700 font-semibold">
                              <Loader2 className="w-3.5 h-3.5 animate-spin text-blue-600" />
                              Processing
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-red-700 font-semibold">
                              <AlertCircle className="w-3.5 h-3.5 text-red-600" />
                              Failed
                            </span>
                          )}
                        </td>
                        <td className="py-3 px-3 text-muted tabular-nums">
                          {new Date(doc.createdAt).toLocaleDateString()}
                        </td>
                        <td className="py-3 px-4 text-right">
                          <button
                            onClick={() => setSelectedDocForChunks(doc)}
                            className="text-primary hover:text-primary-hover font-semibold transition-colors"
                          >
                            View Chunks
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* DOCUMENT CHUNKS MODAL / DRAWER */}
      {selectedDocForChunks && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-surface rounded-xl border border-border shadow-2xl max-w-3xl w-full max-h-[85vh] flex flex-col overflow-hidden animate-in fade-in duration-150">
            {/* Modal Header */}
            <div className="p-4 border-b border-border flex items-center justify-between bg-slate-50/50 shrink-0">
              <div className="space-y-0.5">
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded bg-slate-200 text-slate-800 font-mono text-[10px] uppercase font-bold">
                    {selectedDocForChunks.sourceType}
                  </span>
                  <h3 className="font-bold text-sm text-text truncate max-w-lg">
                    {selectedDocForChunks.title}
                  </h3>
                </div>
                <p className="text-xs text-muted">
                  Ingested Vector Chunks (cosine search units with 384-dimensional embeddings)
                </p>
              </div>

              <button
                onClick={() => setSelectedDocForChunks(null)}
                className="p-1 rounded-md text-muted hover:text-text hover:bg-slate-100 transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Chunks List */}
            <div className="p-5 overflow-y-auto space-y-4 flex-1">
              {chunksLoading ? (
                <div className="p-12 text-center text-muted">
                  <Loader2 className="w-6 h-6 animate-spin mx-auto text-primary mb-2" />
                  <p className="text-xs">Loading chunks...</p>
                </div>
              ) : docChunks.length === 0 ? (
                <p className="text-xs text-muted text-center py-8">No chunks found for this document.</p>
              ) : (
                docChunks.map((chunk: KnowledgeChunk) => (
                  <div
                    key={chunk.id}
                    className="p-4 rounded-lg border border-border bg-slate-50/50 space-y-2 text-xs"
                  >
                    <div className="flex items-center justify-between text-[11px] text-muted border-b border-border/60 pb-1.5">
                      <span className="font-bold text-text">Chunk #{chunk.chunkIndex + 1}</span>
                      <div className="flex items-center gap-3">
                        <span>~{chunk.tokenCount} tokens</span>
                        <span>{chunk.accessLevel}</span>
                      </div>
                    </div>
                    {chunk.metadata && chunk.metadata.headingPath ? (
                      <p className="text-[11px] text-slate-500 font-medium">
                        Path: {String(chunk.metadata.headingPath)}
                      </p>
                    ) : null}
                    <div className="p-3 bg-white rounded border border-border text-xs text-text font-mono leading-relaxed whitespace-pre-wrap">
                      {chunk.content}
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Modal Footer */}
            <div className="p-3 border-t border-border bg-slate-50 flex justify-end shrink-0">
              <button
                onClick={() => setSelectedDocForChunks(null)}
                className="px-4 py-1.5 rounded-md bg-white border border-border hover:bg-slate-100 text-xs font-semibold text-text transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* UPLOAD DOCUMENT MODAL */}
      {isUploadOpen && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-surface rounded-xl border border-border shadow-2xl max-w-2xl w-full max-h-[90vh] flex flex-col overflow-hidden animate-in fade-in duration-150">
            {/* Modal Header */}
            <div className="p-4 border-b border-border flex items-center justify-between bg-slate-50/50 shrink-0">
              <div className="flex items-center gap-2">
                <UploadCloud className="w-4 h-4 text-primary" />
                <h3 className="font-bold text-sm text-text">Upload Knowledge Document</h3>
              </div>
              <button
                onClick={() => setIsUploadOpen(false)}
                className="p-1 rounded-md text-muted hover:text-text hover:bg-slate-100 transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Modal Form Body */}
            <form onSubmit={handleUploadSubmit} className="p-5 overflow-y-auto space-y-4 flex-1">
              {uploadError && (
                <div className="p-3 rounded-md bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{uploadError}</span>
                </div>
              )}

              {/* Title Input */}
              <div className="space-y-1">
                <label className="block text-xs font-bold text-text">
                  Document Title <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={uploadTitle}
                  onChange={(e) => setUploadTitle(e.target.value)}
                  placeholder="e.g. Redis Cluster Sentinel Failover Procedure"
                  className="w-full bg-slate-50 border border-border rounded px-3 py-2 text-xs text-text focus:bg-white focus:border-primary focus:outline-none"
                  required
                />
              </div>

              {/* Grid: Source Type & Access Level */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="block text-xs font-bold text-text">Source Type</label>
                  <select
                    value={uploadSourceType}
                    onChange={(e) => setUploadSourceType(e.target.value as DocumentSourceType)}
                    className="w-full bg-slate-50 border border-border rounded px-3 py-2 text-xs text-text focus:bg-white focus:border-primary focus:outline-none"
                  >
                    <option value="RUNBOOK">Operational Runbook</option>
                    <option value="POSTMORTEM">Incident Postmortem</option>
                    <option value="FAQ">SRE / SLA FAQ</option>
                  </select>
                </div>

                <div className="space-y-1">
                  <label className="block text-xs font-bold text-text">Access Level</label>
                  <select
                    value={uploadAccessLevel}
                    onChange={(e) => setUploadAccessLevel(e.target.value as AccessLevel)}
                    className="w-full bg-slate-50 border border-border rounded px-3 py-2 text-xs text-text focus:bg-white focus:border-primary focus:outline-none"
                  >
                    <option value="PUBLIC">Public (All roles)</option>
                    <option value="ENGINEERING">Engineering Only (Admin & Engineers)</option>
                  </select>
                </div>
              </div>

              {/* Associated Service */}
              <div className="space-y-1">
                <label className="block text-xs font-bold text-text">Associated Service (Optional)</label>
                <select
                  value={uploadServiceId}
                  onChange={(e) => setUploadServiceId(e.target.value)}
                  className="w-full bg-slate-50 border border-border rounded px-3 py-2 text-xs text-text focus:bg-white focus:border-primary focus:outline-none"
                >
                  <option value="">None (Platform-wide)</option>
                  {services.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name} ({s.environment})
                    </option>
                  ))}
                </select>
              </div>

              {/* File Attachment / Content Input */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="block text-xs font-bold text-text">
                    Markdown Content <span className="text-red-500">*</span>
                  </label>
                  <label className="cursor-pointer text-xs text-primary hover:text-primary-hover font-semibold transition-colors flex items-center gap-1">
                    <UploadCloud className="w-3.5 h-3.5" />
                    Load from .md file
                    <input
                      type="file"
                      accept=".md,.txt"
                      onChange={handleFileUpload}
                      className="hidden"
                    />
                  </label>
                </div>

                <textarea
                  value={uploadContent}
                  onChange={(e) => setUploadContent(e.target.value)}
                  placeholder="# Runbook Title&#10;&#10;## Overview&#10;Describe diagnosis and step-by-step remediation..."
                  rows={10}
                  className="w-full bg-slate-50 border border-border rounded px-3 py-2 text-xs text-text font-mono focus:bg-white focus:border-primary focus:outline-none leading-relaxed"
                  required
                />
                <p className="text-[11px] text-muted">
                  Max size: 200 KB. Document will be automatically chunked at markdown heading boundaries and embedded into vector storage.
                </p>
              </div>

              {/* Modal Buttons */}
              <div className="pt-2 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setIsUploadOpen(false)}
                  className="px-4 py-2 rounded-md bg-white border border-border hover:bg-slate-100 text-xs font-semibold text-text transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={uploadMutation.isPending}
                  className="px-4 py-2 rounded-md bg-primary hover:bg-primary-hover disabled:opacity-50 text-white text-xs font-semibold transition-colors flex items-center gap-1.5"
                >
                  {uploadMutation.isPending ? (
                    <>
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      Chunking & Ingesting...
                    </>
                  ) : (
                    'Ingest Document'
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
