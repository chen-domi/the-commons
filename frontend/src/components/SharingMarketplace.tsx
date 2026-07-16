import React, { useState } from 'react';
import { Package, MapPin, ChevronRight, ArrowLeftRight } from 'lucide-react';
import { InventoryItem } from '../types';
import { categoryColors } from '../data/inventory';
import { useAuth } from '../context/AuthContext';
import BorrowRequestModal from './BorrowRequestModal';

interface SharingMarketplaceProps {
  items: InventoryItem[];
  checkedOutItems: string[];
  filterCategory: string;
  filterOrg: string;
}

export default function SharingMarketplace({ items, checkedOutItems, filterCategory, filterOrg }: SharingMarketplaceProps) {
  const { user } = useAuth();
  const [requestItem, setRequestItem] = useState<InventoryItem | null>(null);

  // Only show items opted into sharing, not checked out, matching active filters
  const available = items.filter((i) =>
    i.shared &&
    !checkedOutItems.includes(i.qrCode) &&
    (!filterCategory || i.category === filterCategory) &&
    (!filterOrg || i.org === filterOrg)
  );

  return (
    <div>
      <div className="flex items-center gap-2 mb-5">
        <ArrowLeftRight size={17} style={{ color: '#8B0000' }} />
        <p className="text-sm font-semibold text-gray-600">
          {available.length} item{available.length !== 1 ? 's' : ''} available to borrow
        </p>
      </div>

      {available.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Package size={40} className="mx-auto mb-3 opacity-40" />
          <p className="font-medium">No items are currently listed for sharing.</p>
          <p className="text-sm mt-1">E-Board members can list items from their Club Inventory tab.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {available.map((item) => {
            const isOwn = !user?.isOSIAdmin && item.org === user?.currentOrg;
            return (
              <MarketplaceCard
                key={item.id}
                item={item}
                isOwn={isOwn}
                onRequest={() => setRequestItem(item)}
              />
            );
          })}
        </div>
      )}

      {requestItem && (
        <BorrowRequestModal item={requestItem} onClose={() => setRequestItem(null)} />
      )}
    </div>
  );
}

// ── Card ──────────────────────────────────────────────────────────────────────

function MarketplaceCard({
  item, isOwn, onRequest,
}: { item: InventoryItem; isOwn: boolean; onRequest: () => void }) {
  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm hover:shadow-lg hover:-translate-y-0.5 transition-all p-5 flex flex-col">
      <div className="flex items-start justify-between mb-3">
        <span className={`text-xs font-semibold px-2.5 py-1 rounded-full ${categoryColors[item.category] ?? 'bg-gray-100 text-gray-600'}`}>
          {item.category}
        </span>
        <span className="flex items-center gap-1 text-xs font-semibold text-green-600 bg-green-50 px-2 py-0.5 rounded-full">
          <span className="w-1.5 h-1.5 rounded-full bg-green-500" />
          Available
        </span>
      </div>

      <h3 className="font-semibold text-gray-800 text-base mb-1">{item.name}</h3>
      <p className="text-sm text-gray-500 mb-1">{item.org}</p>
      <div className="flex items-center gap-1 text-xs text-gray-400 mb-3">
        <MapPin size={11} />{item.location}
      </div>
      <div className="flex items-center gap-2 mb-4">
        <Package size={13} className="text-gray-400" />
        <span className="text-sm text-gray-600">
          <span className="font-semibold text-gray-800">{item.quantity}</span> available
        </span>
      </div>

      {isOwn ? (
        <div className="mt-auto w-full py-2.5 rounded-xl text-sm font-semibold text-center"
          style={{ backgroundColor: '#f3f4f6', color: '#6b7280' }}>
          Your listing
        </div>
      ) : (
        <button
          onClick={onRequest}
          className="mt-auto w-full py-2.5 rounded-xl text-sm font-semibold text-white flex items-center justify-center gap-2 transition-all hover:opacity-90 active:scale-95"
          style={{ backgroundColor: '#8B0000' }}
        >
          Request to Borrow <ChevronRight size={15} />
        </button>
      )}
    </div>
  );
}
