import { useState } from 'react';
import { Package, X, Calendar, FileText } from 'lucide-react';
import { InventoryItem } from '../types';

interface CheckoutDetailsModalProps {
  item: InventoryItem;
  onConfirm: (purpose: string, dueDate: string) => void;
  onCancel: () => void;
}

export default function CheckoutDetailsModal({ item, onConfirm, onCancel }: CheckoutDetailsModalProps) {
  const [purpose, setPurpose] = useState('');
  const [dueDate, setDueDate] = useState('');

  const today = new Date().toISOString().split('T')[0];

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!purpose.trim() || !dueDate) return;
    onConfirm(purpose.trim(), dueDate);
  }

  const inputClass = 'w-full px-3.5 py-2.5 text-sm rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:border-transparent bg-white';
  const ring = { '--tw-ring-color': '#8B0000' } as React.CSSProperties;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ backgroundColor: 'rgba(0,0,0,0.6)' }}
      onClick={onCancel}>
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden"
        onClick={(e) => e.stopPropagation()}>

        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <div className="flex items-center gap-2">
            <Package size={18} style={{ color: '#8B0000' }} />
            <span className="font-semibold text-gray-800">Check Out Item</span>
          </div>
          <button onClick={onCancel} className="text-gray-400 hover:text-gray-600 transition-colors">
            <X size={20} />
          </button>
        </div>

        {/* Item preview */}
        <div className="px-5 pt-4 pb-0">
          <div className="flex items-center gap-3 px-3.5 py-3 rounded-xl border border-gray-100 bg-gray-50">
            <span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ backgroundColor: '#16a34a' }} />
            <div className="min-w-0">
              <p className="text-sm font-semibold text-gray-800 truncate">{item.name}</p>
              <p className="text-xs text-gray-400 truncate">{item.org} · {item.category}</p>
            </div>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="px-5 py-4 space-y-3">
          <div>
            <label className="flex items-center gap-1.5 text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">
              <FileText size={11} /> Purpose
            </label>
            <input
              type="text"
              value={purpose}
              onChange={(e) => setPurpose(e.target.value)}
              placeholder="e.g. Club fair, Spring Gala, Fundraiser…"
              required
              autoFocus
              className={inputClass}
              style={ring}
            />
          </div>

          <div>
            <label className="flex items-center gap-1.5 text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">
              <Calendar size={11} /> Return by
            </label>
            <input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              min={today}
              required
              className={inputClass}
              style={ring}
            />
          </div>

          <div className="flex gap-2 pt-1">
            <button type="button" onClick={onCancel}
              className="flex-1 py-2.5 rounded-xl text-sm font-semibold border border-gray-200 text-gray-600 hover:bg-gray-50 transition-colors">
              Cancel
            </button>
            <button type="submit" disabled={!purpose.trim() || !dueDate}
              className="flex-1 py-2.5 rounded-xl text-sm font-semibold text-white transition-all hover:opacity-90 active:scale-95 disabled:opacity-40"
              style={{ backgroundColor: '#8B0000' }}>
              Confirm Checkout
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
