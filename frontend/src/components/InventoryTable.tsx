import { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { Package, QrCode, MapPin, Pencil, Trash2, Lock, Printer } from 'lucide-react';
import { InventoryItem } from '../types';
import { categoryColors } from '../data/inventory';
import { useAuth } from '../context/AuthContext';

function timeAgo(dateStr?: string): string | null {
  if (!dateStr) return null;
  const months = (Date.now() - new Date(dateStr).getTime()) / (1000 * 60 * 60 * 24 * 30.5);
  if (months < 1) return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  if (months < 12) return `${Math.floor(months)}mo ago`;
  const yrs = Math.floor(months / 12);
  return `${yrs}yr${yrs > 1 ? 's' : ''} ago`;
}

interface InventoryTableProps {
  items: InventoryItem[];
  checkedOutItems: string[];
  viewMode: 'global' | 'club';          // global = all orgs; club = own org only
  onScanClick: (qrCode: string) => void;
  onEdit: (item: InventoryItem) => void;
  onDelete: (id: number) => void;
  onToggleShare: (id: number, shared: boolean) => void;
}

export default function InventoryTable({
  items, checkedOutItems, viewMode, onScanClick, onEdit, onDelete, onToggleShare,
}: InventoryTableProps) {
  const { user } = useAuth();
  const currentRole = localStorage.getItem('currentRole') as 'eboard' | null;

  if (items.length === 0) {
    return (
      <div className="text-center py-16 text-gray-400">
        <Package size={40} className="mx-auto mb-3 opacity-40" />
        <p className="font-medium">
          {viewMode === 'club' ? 'No items in your inventory yet.' : 'No items match your search.'}
        </p>
      </div>
    );
  }

  const globalCols = ['QR Code', 'Item', 'Organization', 'Location', 'Status', 'Qty', 'Last Used', 'Added', ''];
  const clubCols   = ['QR Code', 'Item', 'Location', 'Status', 'Qty', 'Marketplace', 'Last Used', 'Added', ''];

  return (
    <div className="overflow-x-auto rounded-2xl border border-gray-100 shadow-sm">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-gray-50 text-left">
            {(viewMode === 'club' ? clubCols : globalCols).map((h) => (
              <th key={h} className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">{h}</th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-50">
          {items.map((item) => {
            const canEdit =
              user?.isOSIAdmin ||
              (item.org === user?.currentOrg && currentRole === 'eboard');
            const canToggle = canEdit;
            return (
              <InventoryRow
                key={item.id}
                item={item}
                isCheckedOut={checkedOutItems.includes(item.qrCode)}
                viewMode={viewMode}
                canEdit={!!canEdit}
                canToggle={!!canToggle}
                onScanClick={onScanClick}
                onEdit={onEdit}
                onDelete={onDelete}
                onToggleShare={onToggleShare}
              />
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

// ── Row ───────────────────────────────────────────────────────────────────────

interface RowProps {
  item: InventoryItem;
  isCheckedOut: boolean;
  viewMode: 'global' | 'club';
  canEdit: boolean;
  canToggle: boolean;
  onScanClick: (qrCode: string) => void;
  onEdit: (item: InventoryItem) => void;
  onDelete: (id: number) => void;
  onToggleShare: (id: number, shared: boolean) => void;
}

function QRModal({ qrCode, onClose }: { qrCode: string; onClose: () => void }) {
  function handlePrint() {
    const win = window.open('', '_blank', 'width=300,height=350');
    if (!win) return;
    const sc = String.fromCharCode(60) + '/script>';
    win.document.write(`
      <html><body style="display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;font-family:monospace;gap:12px">
        <div id="qr"></div>
        <p style="font-size:12px;color:#555">${qrCode}</p>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js">${sc}
        <script>new QRCode(document.getElementById('qr'),{text:'${qrCode}',width:180,height:180});</script>
      </body></html>
    `);
    win.document.close();
    setTimeout(() => win.print(), 800);
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ backgroundColor: 'rgba(0,0,0,0.55)' }}
      onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-2xl p-8 flex flex-col items-center gap-4"
        onClick={(e) => e.stopPropagation()}>
        <QRCodeSVG value={qrCode} size={260} bgColor="#ffffff" fgColor="#6B0000" />
        <p className="text-sm font-mono text-gray-500 text-center">{qrCode}</p>
        <div className="flex gap-3 w-full">
          <button onClick={onClose}
            className="flex-1 py-2.5 rounded-xl text-sm font-semibold border border-gray-200 text-gray-600 hover:bg-gray-50 transition-colors">
            Close
          </button>
          <button onClick={handlePrint}
            className="flex-1 flex items-center justify-center gap-1.5 py-2.5 rounded-xl text-sm font-semibold text-white transition-colors hover:opacity-90"
            style={{ backgroundColor: '#8B0000' }}>
            <Printer size={14} /> Print
          </button>
        </div>
      </div>
    </div>
  );
}

function QRPopover({ qrCode }: { qrCode: string }) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <button onClick={() => setOpen(true)} title="View QR code"
        className="flex items-center gap-1.5 font-mono text-xs font-medium px-2.5 py-1.5 rounded-lg border transition-all hover:shadow-sm active:scale-95"
        style={{ borderColor: '#CFB87C', color: '#8B0000', backgroundColor: '#fffbeb' }}>
        <QrCode size={12} />{qrCode}
      </button>
      {open && <QRModal qrCode={qrCode} onClose={() => setOpen(false)} />}
    </>
  );
}

function InventoryRow({ item, isCheckedOut, viewMode, canEdit, canToggle, onScanClick: _onScanClick, onEdit, onDelete, onToggleShare }: RowProps) {
  return (
    <tr className={`transition-colors ${isCheckedOut ? 'bg-gray-50 hover:bg-gray-100' : 'bg-white hover:bg-amber-50'}`}>
      {/* QR Code */}
      <td className="px-4 py-3">
        <QRPopover qrCode={item.qrCode} />
      </td>

      {/* Name + category */}
      <td className="px-4 py-3">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full flex-shrink-0"
            style={{ backgroundColor: isCheckedOut ? '#dc2626' : '#16a34a' }} />
          <p className={`font-medium ${isCheckedOut ? 'text-gray-400' : 'text-gray-800'}`}>{item.name}</p>
        </div>
        <span className={`mt-1 inline-block text-xs font-medium px-2 py-0.5 rounded-full ml-4 ${isCheckedOut ? 'bg-gray-100 text-gray-400' : (categoryColors[item.category] ?? 'bg-gray-100 text-gray-600')}`}>
          {item.category}
        </span>
      </td>

      {/* Org (global only) */}
      {viewMode === 'global' && <td className="px-4 py-3 text-gray-600">{item.org}</td>}

      {/* Location */}
      <td className="px-4 py-3">
        <span className="flex items-center gap-1 text-gray-500 text-xs">
          <MapPin size={11} />{item.location}
        </span>
      </td>

      {/* Status */}
      <td className="px-4 py-3">
        <span className="inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1 rounded-full"
          style={isCheckedOut ? { backgroundColor: '#fee2e2', color: '#dc2626' } : { backgroundColor: '#dcfce7', color: '#16a34a' }}>
          <span className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: isCheckedOut ? '#dc2626' : '#16a34a' }} />
          {isCheckedOut ? 'Checked Out' : 'Available'}
        </span>
        {isCheckedOut && item.checkoutDueDate && (
          <p className="text-xs text-gray-400 mt-0.5 ml-1">
            Due {new Date(item.checkoutDueDate + 'T00:00:00').toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
          </p>
        )}
        {isCheckedOut && item.checkoutPurpose && (
          <p className="text-xs text-gray-400 mt-0.5 ml-1 truncate max-w-[140px]" title={item.checkoutPurpose}>
            {item.checkoutPurpose}
          </p>
        )}
      </td>

      {/* Qty */}
      <td className="px-4 py-3 text-center font-semibold text-gray-700">
        {isCheckedOut ? <span className="text-red-400 line-through">{item.quantity}</span> : item.quantity}
      </td>

      {/* Marketplace toggle (club view only) */}
      {viewMode === 'club' && (
        <td className="px-4 py-3">
          {canToggle ? (
            <button onClick={() => onToggleShare(item.id, !item.shared)}
              className="flex items-center gap-2 text-xs font-semibold transition-all"
              title={item.shared ? 'Remove from marketplace' : 'List on marketplace'}>
              <span className="relative inline-flex h-5 w-9 flex-shrink-0 rounded-full border-2 border-transparent transition-colors"
                style={{ backgroundColor: item.shared ? '#8B0000' : '#d1d5db' }}>
                <span className="pointer-events-none inline-block h-4 w-4 rounded-full bg-white shadow transition-transform"
                  style={{ transform: item.shared ? 'translateX(16px)' : 'translateX(0)' }} />
              </span>
              <span style={{ color: item.shared ? '#8B0000' : '#9ca3af' }}>
                {item.shared ? 'Listed' : 'Not listed'}
              </span>
            </button>
          ) : (
            <span className="text-xs font-semibold px-2 py-0.5 rounded-full"
              style={item.shared ? { backgroundColor: '#fff1f2', color: '#8B0000' } : { backgroundColor: '#f3f4f6', color: '#9ca3af' }}>
              {item.shared ? 'Listed' : 'Not listed'}
            </span>
          )}
        </td>
      )}

      {/* Last Used */}
      <td className="px-4 py-3 text-xs text-gray-400">{item.lastUsed}</td>

      {/* Added */}
      <td className="px-4 py-3 text-xs text-gray-400">{timeAgo(item.createdAt) ?? '—'}</td>

      {/* Actions */}
      <td className="px-4 py-3">
        {canEdit ? (
          <div className="flex items-center gap-1">
            <button title="Edit" onClick={() => onEdit(item)}
              className="p-1.5 rounded-lg text-gray-400 hover:text-blue-600 hover:bg-blue-50 transition-colors">
              <Pencil size={14} />
            </button>
            <button title="Delete" onClick={() => { if (window.confirm(`Delete "${item.name}"?`)) onDelete(item.id); }}
              className="p-1.5 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-50 transition-colors">
              <Trash2 size={14} />
            </button>
          </div>
        ) : (
          <span title="View only" className="text-gray-300"><Lock size={13} /></span>
        )}
      </td>
    </tr>
  );
}
