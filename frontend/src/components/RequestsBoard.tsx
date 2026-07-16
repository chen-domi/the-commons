import React, { useState, useEffect } from 'react';
import { Plus, X, AlertCircle, Inbox, ChevronRight, Trash2 } from 'lucide-react';
import { supabase } from '../lib/supabase';
import { categoryColors } from '../data/inventory';
import { useAuth } from '../context/AuthContext';
import { ItemRequest } from '../types';
import Combobox from './Combobox';

const CATEGORIES = Object.keys(categoryColors);

function rowToRequest(row: any): ItemRequest {
  return {
    id:        row.id,
    org:       row.org,
    itemName:  row.item_name,
    category:  row.category,
    notes:     row.notes,
    status:    row.status ?? 'pending',
    createdBy: row.created_by,
    createdAt: row.created_at,
  };
}

// ── Post request modal ─────────────────────────────────────────────────────────

function PostRequestModal({ onClose, onPosted }: { onClose: () => void; onPosted: (r: ItemRequest) => void }) {
  const { user } = useAuth();
  const [itemName, setItemName] = useState('');
  const [category, setCategory] = useState('');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const inputClass = 'w-full px-3.5 py-2.5 text-sm rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:border-transparent bg-white';
  const ring = { '--tw-ring-color': '#8B0000' } as React.CSSProperties;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!itemName.trim()) return;
    setLoading(true);

    const row = {
      org:       user?.currentOrg ?? '',
      item_name: itemName.trim(),
      category:  category || null,
      notes:     notes.trim() || null,
      created_by: user?.id ?? null,
    };

    const { data, error: err } = await supabase
      .from('item_requests')
      .insert(row)
      .select()
      .single();

    if (data) {
      onPosted(rowToRequest(data));
    } else {
      // Dev login fallback — no real session
      if (err) { console.error('Request post failed:', err.message); setError('Failed to post — saved locally.'); }
      onPosted({
        id: Date.now(),
        org: user?.currentOrg ?? '',
        itemName: itemName.trim(),
        category: category || null,
        notes: notes.trim() || null,
        createdAt: new Date().toISOString(),
      });
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ backgroundColor: 'rgba(0,0,0,0.45)' }} onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <h2 className="font-bold text-gray-800">Post a Request</h2>
          <button onClick={onClose} className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors">
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1.5">What are you looking for? *</label>
            <input type="text" value={itemName} onChange={(e) => setItemName(e.target.value)}
              placeholder="e.g. Folding tables, PA system, pop-up tent…"
              required autoFocus className={inputClass} style={ring} />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1.5">Category <span className="font-normal text-gray-400">(optional)</span></label>
            <Combobox
              options={CATEGORIES}
              value={category}
              onChange={setCategory}
              placeholder="Any category"
              allOptionLabel="Any category"
              style={ring}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1.5">Details <span className="font-normal text-gray-400">(optional)</span></label>
            <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={3}
              placeholder="Dates needed, quantity, any other details…"
              className={`${inputClass} resize-none`} style={ring} />
          </div>

          {error && (
            <p className="flex items-center gap-1.5 text-xs text-red-600">
              <AlertCircle size={12} />{error}
            </p>
          )}

          <div className="flex gap-3 pt-1">
            <button type="button" onClick={onClose}
              className="flex-1 py-2.5 rounded-xl text-sm font-semibold border border-gray-200 text-gray-600 hover:bg-gray-50 transition-colors">
              Cancel
            </button>
            <button type="submit" disabled={loading}
              className="flex-1 py-2.5 rounded-xl text-sm font-semibold text-white transition-all hover:opacity-90 active:scale-95 disabled:opacity-60"
              style={{ backgroundColor: '#8B0000' }}>
              {loading ? 'Posting…' : 'Post Request'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ── Request card ───────────────────────────────────────────────────────────────

function RequestCard({ request, canDelete, isOwn, onDelete, onWeHaveThis }: {
  request: ItemRequest;
  canDelete: boolean;
  isOwn: boolean;
  onDelete: () => void;
  onWeHaveThis: () => void;
}) {
  const date = new Date(request.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });

  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 flex flex-col gap-3">
      <div className="flex items-start justify-between gap-2">
        <div className="flex-1 min-w-0">
          <p className="font-semibold text-gray-800 text-base leading-snug">{request.itemName}</p>
          <p className="text-sm text-gray-500 mt-0.5">{request.org}</p>
        </div>
        <div className="flex items-center gap-1 flex-shrink-0">
          {request.category && (
            <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${(categoryColors as any)[request.category] ?? 'bg-gray-100 text-gray-600'}`}>
              {request.category}
            </span>
          )}
          {canDelete && (
            <button onClick={onDelete} title="Delete request"
              className="p-1.5 rounded-lg text-gray-300 hover:text-red-500 hover:bg-red-50 transition-colors">
              <Trash2 size={13} />
            </button>
          )}
        </div>
      </div>

      {request.notes && (
        <p className="text-sm text-gray-500 leading-relaxed">{request.notes}</p>
      )}

      <div className="flex items-center justify-between mt-auto pt-1">
        <span className="text-xs text-gray-400">{date}</span>
        {isOwn ? (
          <span className="text-xs font-semibold px-3 py-1.5 rounded-lg"
            style={{ backgroundColor: '#f3f4f6', color: '#9ca3af' }}>
            Your request
          </span>
        ) : (
          <button onClick={onWeHaveThis}
            className="flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-lg transition-all hover:opacity-90 active:scale-95 text-white"
            style={{ backgroundColor: '#8B0000' }}>
            We have this! <ChevronRight size={12} />
          </button>
        )}
      </div>
    </div>
  );
}

// ── Main board ─────────────────────────────────────────────────────────────────

export default function RequestsBoard({ onGoToInventory, onCountChange }: { onGoToInventory: () => void; onCountChange?: (n: number) => void }) {
  const { user } = useAuth();
  const [requests, setRequests] = useState<ItemRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [showPost, setShowPost] = useState(false);

  const canPost = !!(user?.isOSIAdmin || user?.organizations.some((o) => o.role === 'eboard'));

  useEffect(() => {
    supabase.from('item_requests').select('*').order('created_at', { ascending: false }).then(({ data }) => {
      if (data) {
        setRequests(data.map(rowToRequest));
        onCountChange?.(data.length);
      }
      setLoading(false);
    });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  function handlePosted(r: ItemRequest) {
    setRequests((prev) => { const next = [r, ...prev]; onCountChange?.(next.length); return next; });
    setShowPost(false);
  }

  async function handleDelete(id: number) {
    await supabase.from('item_requests').delete().eq('id', id);
    setRequests((prev) => { const next = prev.filter((r) => r.id !== id); onCountChange?.(next.length); return next; });
  }

  return (
    <div>
      {/* Header */}
      <div className="flex items-center justify-between mb-5">
        <p className="text-sm font-semibold text-gray-600">
          {requests.length} request{requests.length !== 1 ? 's' : ''} from orgs looking to borrow
        </p>
        {canPost && (
          <button onClick={() => setShowPost(true)}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-semibold text-white transition-all hover:opacity-90 active:scale-95"
            style={{ backgroundColor: '#8B0000' }}>
            <Plus size={14} /> Post a Request
          </button>
        )}
      </div>

      {loading ? (
        <div className="text-center py-16 text-gray-400">
          <div className="w-8 h-8 rounded-full border-4 border-gray-200 border-t-gray-400 animate-spin mx-auto mb-3" />
          <p className="text-sm">Loading requests…</p>
        </div>
      ) : requests.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Inbox size={40} className="mx-auto mb-3 opacity-40" />
          <p className="font-medium">No requests yet.</p>
          <p className="text-sm mt-1">Post a request to let other orgs know what you're looking for.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {requests.map((r) => {
            const isOwn = r.org === user?.currentOrg;
            return (
              <RequestCard
                key={r.id}
                request={r}
                isOwn={isOwn}
                canDelete={isOwn || !!user?.isOSIAdmin}
                onDelete={() => handleDelete(r.id)}
                onWeHaveThis={onGoToInventory}
              />
            );
          })}
        </div>
      )}

      {showPost && <PostRequestModal onClose={() => setShowPost(false)} onPosted={handlePosted} />}
    </div>
  );
}
