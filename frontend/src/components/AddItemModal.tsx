import React, { useState } from 'react';
import { X, Wand2 } from 'lucide-react';
import { InventoryItem } from '../types';
import { categoryColors } from '../data/inventory';
import { useAuth } from '../context/AuthContext';
import Combobox from './Combobox';

const CATEGORIES = Object.keys(categoryColors);

const PRESET_LOCATIONS = [
  'Carney Hall, Suite 147',
  'Carney Storage',
  'Mod Lot Storage',
  'McElroy Commons',
  'Stokes Hall',
  'Gasson Hall',
  'Higgins Hall',
  'Fulton Hall',
];

interface AddItemModalProps {
  item?: InventoryItem;
  nextId: number;
  onSave: (item: InventoryItem) => void;
  onClose: () => void;
}

function generateQR(org: string, name: string, id: number): string {
  const orgAbbr = org.replace(/[^a-zA-Z ]/g, '').split(' ').map((w) => w[0] ?? '').join('').toUpperCase().slice(0, 4);
  const nameAbbr = name.replace(/[^a-zA-Z ]/g, '').split(' ').filter(Boolean).map((w) => w[0]).join('').toUpperCase().slice(0, 6);
  return `BC-${orgAbbr}-${nameAbbr}-${String(id).padStart(3, '0')}`;
}

export default function AddItemModal({ item, nextId, onSave, onClose }: AddItemModalProps) {
  const { user } = useAuth();
  const editing = !!item;

  const [name, setName] = useState(item?.name ?? '');
  const [category, setCategory] = useState(item?.category ?? CATEGORIES[0]);
  const [location, setLocation] = useState(item?.location ?? '');
  const [quantity, setQuantity] = useState(String(item?.quantity ?? 1));
  const [lastUsed, setLastUsed] = useState(item?.lastUsed === '—' ? '' : (item?.lastUsed ?? ''));
  const [dateAdded, setDateAdded] = useState(
    item?.createdAt ? item.createdAt.split('T')[0] : new Date().toISOString().split('T')[0]
  );
  const [shared, setShared] = useState(item?.shared ?? false);
  const [qrCode, setQrCode] = useState(item?.qrCode ?? '');
  const [qrTouched, setQrTouched] = useState(editing);

  const org = user?.currentOrg ?? '';

  const inputClass = 'w-full px-3.5 py-2.5 text-sm rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:border-transparent bg-white';
  const ring = { '--tw-ring-color': '#8B0000' } as React.CSSProperties;

  function handleAutoQR() {
    setQrCode(generateQR(org, name, item?.id ?? nextId));
    setQrTouched(true);
  }

  function handleSubmit(e: React.SyntheticEvent) {
    e.preventDefault();
    const qty = parseInt(quantity, 10);
    if (!name.trim() || !location.trim() || isNaN(qty) || qty < 1) return;
    const finalQR = qrTouched && qrCode.trim() ? qrCode.trim() : generateQR(org, name, item?.id ?? nextId);
    onSave({
      id: item?.id ?? nextId,
      name: name.trim(),
      category,
      org,
      location: location.trim(),
      quantity: qty,
      lastUsed: lastUsed.trim() || '—',
      createdAt: dateAdded ? new Date(dateAdded).toISOString() : undefined,
      shared,
      qrCode: finalQR,
    });
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ backgroundColor: 'rgba(0,0,0,0.45)' }}
      onClick={onClose}
    >
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <h2 className="font-bold text-gray-800">{editing ? 'Edit Item' : `Add Item to ${org}`}</h2>
          <button onClick={onClose} className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors">
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* Name */}
          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1.5">Item Name *</label>
            <input type="text" value={name} onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Folding Tables (set of 6)" required className={inputClass} style={ring} />
          </div>

          {/* Category + Quantity */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-gray-600 mb-1.5">Category *</label>
              <Combobox
                options={CATEGORIES}
                value={category}
                onChange={setCategory}
                placeholder="Type or select…"
                style={ring}
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-600 mb-1.5">Quantity *</label>
              <input type="number" min={1} value={quantity} onChange={(e) => setQuantity(e.target.value)}
                required className={inputClass} style={ring} />
            </div>
          </div>

          {/* Location */}
          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1.5">Storage Location *</label>
            <Combobox
              options={PRESET_LOCATIONS}
              value={location}
              onChange={setLocation}
              placeholder="Type or select a location…"
              required
              style={ring}
            />
          </div>

          {/* Last Used + Date Added */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-gray-600 mb-1.5">Last Used At <span className="font-normal text-gray-400">(event)</span></label>
              <input type="text" value={lastUsed} onChange={(e) => setLastUsed(e.target.value)}
                placeholder="e.g. Student Involvement Fair" className={inputClass} style={ring} />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-600 mb-1.5">Date Added</label>
              <input type="date" value={dateAdded} onChange={(e) => setDateAdded(e.target.value)}
                className={inputClass} style={ring} />
            </div>
          </div>

          {/* QR Code */}
          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1.5">QR Code</label>
            <div className="flex gap-2">
              <input type="text" value={qrCode}
                onChange={(e) => { setQrCode(e.target.value); setQrTouched(true); }}
                placeholder="Auto-generated if blank"
                className={`${inputClass} font-mono text-xs flex-1`} style={ring} />
              <button type="button" onClick={handleAutoQR}
                className="flex items-center gap-1.5 px-3 py-2.5 rounded-xl text-xs font-semibold border border-gray-200 text-gray-600 hover:bg-gray-50 transition-colors flex-shrink-0">
                <Wand2 size={13} /> Generate
              </button>
            </div>
          </div>

          {/* Share on Marketplace */}
          <div className="flex items-center justify-between px-4 py-3 rounded-xl border border-gray-200 bg-gray-50">
            <div>
              <p className="text-sm font-semibold text-gray-700">List on Marketplace</p>
              <p className="text-xs text-gray-400 mt-0.5">Other orgs can see and request this item</p>
            </div>
            <button type="button" onClick={() => setShared((s) => !s)}
              className="relative inline-flex h-6 w-11 flex-shrink-0 rounded-full border-2 border-transparent transition-colors"
              style={{ backgroundColor: shared ? '#8B0000' : '#d1d5db' }}>
              <span className="pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow transition-transform"
                style={{ transform: shared ? 'translateX(20px)' : 'translateX(0)' }} />
            </button>
          </div>

          {/* Actions */}
          <div className="flex gap-3 pt-1">
            <button type="button" onClick={onClose}
              className="flex-1 py-2.5 rounded-xl text-sm font-semibold border border-gray-200 text-gray-600 hover:bg-gray-50 transition-colors">
              Cancel
            </button>
            <button type="submit"
              className="flex-1 py-2.5 rounded-xl text-sm font-semibold text-white transition-all hover:opacity-90 active:scale-95"
              style={{ backgroundColor: '#8B0000' }}>
              {editing ? 'Save Changes' : 'Add Item'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
