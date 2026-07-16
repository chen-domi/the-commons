import React, { useState } from 'react';
import { X, Send, Package, MapPin } from 'lucide-react';
import { InventoryItem } from '../types';
import { useAuth } from '../context/AuthContext';

interface BorrowRequestModalProps {
  item: InventoryItem;
  onClose: () => void;
}

export default function BorrowRequestModal({ item, onClose }: BorrowRequestModalProps) {
  const { user } = useAuth();
  const [eventName, setEventName] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [quantity, setQuantity] = useState('1');
  const [notes, setNotes] = useState('');

  function handleSubmit(e: React.SyntheticEvent) {
    e.preventDefault();

    const body = [
      `Borrow Request from ${user?.name ?? 'Unknown'} (${user?.currentOrg ?? 'Unknown Org'})`,
      '',
      `Item: ${item.name}`,
      `QR Code: ${item.qrCode}`,
      `Quantity Requested: ${quantity} of ${item.quantity} available`,
      '',
      `Event: ${eventName}`,
      `Date Range: ${startDate} to ${endDate}`,
      notes ? `Notes: ${notes}` : '',
      '',
      `Requester Email: ${user?.email ?? ''}`,
    ].filter((l) => l !== undefined).join('\n');

    const ownerEmail = `eboard@bc.edu`; // placeholder — real app would look up contact
    const subject = encodeURIComponent(`[The Commons] Borrow Request: ${item.name}`);
    const bodyEncoded = encodeURIComponent(body);

    window.open(`mailto:${ownerEmail}?subject=${subject}&body=${bodyEncoded}`);
    onClose();
  }

  const inputClass =
    'w-full px-3.5 py-2.5 text-sm rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:border-transparent bg-white';
  const ring = { '--tw-ring-color': '#8B0000' } as React.CSSProperties;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl shadow-2xl w-full max-w-md"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <div>
            <h2 className="font-bold text-gray-800">Request to Borrow</h2>
            <p className="text-xs text-gray-400 mt-0.5">This will draft an email to the owning org</p>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors">
            <X size={18} />
          </button>
        </div>

        {/* Item summary */}
        <div className="px-6 py-4 border-b border-gray-50 bg-gray-50/60">
          <div className="flex items-start gap-3">
            <div className="w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0"
              style={{ backgroundColor: '#fff1f2', color: '#8B0000' }}>
              <Package size={16} />
            </div>
            <div>
              <p className="font-semibold text-gray-800 text-sm">{item.name}</p>
              <p className="text-xs text-gray-500">{item.org}</p>
              <p className="flex items-center gap-1 text-xs text-gray-400 mt-0.5">
                <MapPin size={10} />{item.location}
              </p>
            </div>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1.5">Event Name *</label>
            <input
              type="text"
              placeholder="e.g. Spring Cookout, Org Fair"
              value={eventName}
              onChange={(e) => setEventName(e.target.value)}
              required
              className={inputClass}
              style={ring}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-gray-600 mb-1.5">Start Date *</label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                required
                className={inputClass}
                style={ring}
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-600 mb-1.5">Return Date *</label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                required
                min={startDate}
                className={inputClass}
                style={ring}
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1.5">
              Quantity Needed * <span className="font-normal text-gray-400">({item.quantity} available)</span>
            </label>
            <input
              type="number"
              min={1}
              max={item.quantity}
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              required
              className={inputClass}
              style={ring}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1.5">Additional Notes</label>
            <textarea
              placeholder="Any special requirements or context…"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={3}
              className={`${inputClass} resize-none`}
              style={ring}
            />
          </div>

          <div className="flex gap-3 pt-1">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2.5 rounded-xl text-sm font-semibold border border-gray-200 text-gray-600 hover:bg-gray-50 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex-1 py-2.5 rounded-xl text-sm font-semibold text-white flex items-center justify-center gap-2 transition-all hover:opacity-90 active:scale-95"
              style={{ backgroundColor: '#8B0000' }}
            >
              <Send size={14} /> Send Request
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
