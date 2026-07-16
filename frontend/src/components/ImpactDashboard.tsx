import React, { useState } from 'react';
import { Package, Bell, Zap, Lightbulb, Plus, ArrowLeftRight, CheckCircle2, Clock, QrCode, Settings, X, AlertCircle } from 'lucide-react';
import { InventoryItem } from '../types';
import { useAuth } from '../context/AuthContext';
import { BC_CLUBS } from '../data/clubs';
import Combobox from './Combobox';

interface ImpactDashboardProps {
  items: InventoryItem[];
  onAddItem: () => void;
  onGoToMarketplace: () => void;
  onScanClick: () => void;
}

function OrgManagerModal({ onClose }: { onClose: () => void }) {
  const { user, joinOrg, selectOrg, leaveOrg, switchOrg } = useAuth();
  const [orgName, setOrgName] = useState('');
  const [pin, setPin] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const availableOrgs = BC_CLUBS;

  const inputClass = 'w-full px-3.5 py-2.5 text-sm rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:border-transparent bg-white';
  const ring = { '--tw-ring-color': '#8B0000' } as React.CSSProperties;

  async function handleJoin(e: React.FormEvent) {
    e.preventDefault();
    if (pin.length !== 4) { setError('PIN must be 4 digits.'); return; }
    setError('');
    setLoading(true);
    try {
      await joinOrg(orgName.trim(), pin);
      selectOrg(orgName.trim(), 'eboard');
      setOrgName('');
      setPin('');
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ backgroundColor: 'rgba(0,0,0,0.45)' }}
      onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <h2 className="font-bold text-gray-800">Manage Organizations</h2>
          <button onClick={onClose} className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors">
            <X size={18} />
          </button>
        </div>

        <div className="p-6 space-y-5">
          {/* Current orgs */}
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Your Organizations</p>
            {(user?.organizations.length ?? 0) === 0 ? (
              <p className="text-sm text-gray-400 italic">You haven't joined any organizations yet.</p>
            ) : (
              <div className="space-y-2">
                {user?.organizations.map(({ org, role }) => (
                  <div key={org} className="flex items-center justify-between px-3 py-2.5 rounded-xl border border-gray-100 bg-gray-50">
                    <div className="flex items-center gap-2.5 min-w-0">
                      <span className="w-2 h-2 rounded-full flex-shrink-0"
                        style={{ backgroundColor: org === user.currentOrg ? '#8B0000' : '#d1d5db' }} />
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-gray-800 truncate">{org}</p>
                        <p className="text-xs text-gray-400 capitalize">{role}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-1 flex-shrink-0 ml-2">
                      {org !== user.currentOrg && (
                        <button onClick={() => switchOrg(org)}
                          className="text-xs px-2 py-1 rounded-lg border border-gray-200 text-gray-600 hover:bg-white transition-colors">
                          Switch
                        </button>
                      )}
                      {org === user.currentOrg && (
                        <span className="text-xs px-2 py-1 rounded-lg font-semibold"
                          style={{ backgroundColor: '#fff1f2', color: '#8B0000' }}>Active</span>
                      )}
                      <button onClick={() => leaveOrg(org)}
                        className="p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 transition-colors"
                        title="Leave org">
                        <X size={13} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Join org */}
          <div className="pt-1 border-t border-gray-100">
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Join an Organization</p>
            <form onSubmit={handleJoin} className="space-y-2">
              <Combobox
                options={availableOrgs}
                value={orgName}
                onChange={setOrgName}
                placeholder="Search organizations…"
                style={ring}
              />
              <input
                type="password"
                inputMode="numeric"
                maxLength={4}
                placeholder="• • • •"
                value={pin}
                onChange={(e) => { setPin(e.target.value.replace(/\D/g, '')); setError(''); }}
                className={`${inputClass} tracking-widest text-center font-bold text-base placeholder-gray-300`}
                style={ring}
              />
              {error && (
                <div className="flex items-center gap-1.5 text-xs text-red-600">
                  <AlertCircle size={12} /> {error}
                </div>
              )}
              <button type="submit" disabled={loading || !orgName.trim() || pin.length !== 4}
                className="w-full py-2.5 rounded-xl text-sm font-semibold text-white transition-all hover:opacity-90 active:scale-95 disabled:opacity-40"
                style={{ backgroundColor: '#8B0000' }}>
                {loading ? 'Joining…' : 'Join'}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function ImpactDashboard({ items, onAddItem, onGoToMarketplace, onScanClick }: ImpactDashboardProps) {
  const { user } = useAuth();
  const [showOrgManager, setShowOrgManager] = useState(false);
  const isAdmin = !!user?.isOSIAdmin;
  const canAdd = isAdmin || user?.organizations.find((o) => o.org === user.currentOrg)?.role === 'eboard';

  const myItems = isAdmin ? items : items.filter((i) => i.org === user?.currentOrg);
  const myShared = myItems.filter((i) => i.shared);
  const threeMonthsAgo = new Date();
  threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3);
  const tipItem = myItems.find((i) => !i.shared && (!i.createdAt || new Date(i.createdAt) <= threeMonthsAgo)) ?? null;

  // Demo notification counts
  const pendingRequests = 2;
  const approvedBorrows = 1;

  return (
    <div className="mb-6 space-y-3">
      {/* 3-column row */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">

        {/* Your Org */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
          <div className="flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0"
              style={{ backgroundColor: '#fff1f2', color: '#8B0000' }}>
              <Package size={16} />
            </div>
            <span className="text-sm font-bold text-gray-700 truncate flex-1 min-w-0">
              {isAdmin ? 'All Organizations' : (user?.currentOrg || 'No Org')}
            </span>
            {!isAdmin && (
              <button onClick={() => setShowOrgManager(true)}
                className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors flex-shrink-0"
                title="Manage organizations">
                <Settings size={14} />
              </button>
            )}
          </div>
          <p className="text-2xl font-bold text-gray-800">{myItems.length}</p>
          <p className="text-xs text-gray-500 mt-0.5">{myItems.length === 1 ? 'item' : 'items'} in inventory</p>
          <div className="mt-3 pt-3 border-t border-gray-100">
            <span className="text-xs text-gray-400">{myShared.length} on marketplace</span>
          </div>
        </div>

        {/* Notifications */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
          <div className="flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0"
              style={{ backgroundColor: '#fffbeb', color: '#b45309' }}>
              <Bell size={16} />
            </div>
            <span className="text-sm font-bold text-gray-700">Notifications</span>
          </div>
          <div className="space-y-2.5">
            <div className="flex items-center gap-2">
              <Clock size={13} className="text-amber-500 flex-shrink-0" />
              <span className="text-sm text-gray-700">
                <span className="font-semibold">{pendingRequests}</span> pending requests
              </span>
            </div>
            <div className="flex items-center gap-2">
              <CheckCircle2 size={13} className="text-green-500 flex-shrink-0" />
              <span className="text-sm text-gray-700">
                <span className="font-semibold">{approvedBorrows}</span> approved borrow
              </span>
            </div>
          </div>
          <button onClick={onGoToMarketplace}
            className="mt-3 pt-3 border-t border-gray-100 w-full text-left text-xs font-semibold transition-colors hover:opacity-70"
            style={{ color: '#8B0000' }}>
            View all activity →
          </button>
        </div>

        {/* Quick Actions */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
          <div className="flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0"
              style={{ backgroundColor: '#f5f3ff', color: '#7c3aed' }}>
              <Zap size={16} />
            </div>
            <span className="text-sm font-bold text-gray-700">Quick Actions</span>
          </div>
          <div className="space-y-2">
            {canAdd && (
              <button onClick={onAddItem}
                className="flex items-center gap-2 w-full px-3 py-2 rounded-xl text-sm font-semibold text-white transition-all hover:opacity-90 active:scale-95"
                style={{ backgroundColor: '#8B0000' }}>
                <Plus size={14} /> Add item
              </button>
            )}
            <button onClick={onGoToMarketplace}
              className="flex items-center gap-2 w-full px-3 py-2 rounded-xl text-sm font-semibold border border-gray-200 text-gray-700 hover:bg-gray-50 transition-colors">
              <ArrowLeftRight size={14} /> View requests
            </button>
            <button onClick={onScanClick}
              className="flex items-center gap-2 w-full px-3 py-2 rounded-xl text-sm font-semibold border border-gray-200 text-gray-700 hover:bg-gray-50 transition-colors">
              <QrCode size={14} /> Scan QR
            </button>
          </div>
        </div>
      </div>

      {/* Tip row */}
      {tipItem && canAdd && (
        <div className="flex items-start gap-3 px-5 py-3.5 rounded-2xl border"
          style={{ backgroundColor: '#fffbeb', borderColor: '#fde68a' }}>
          <Lightbulb size={15} className="text-amber-500 flex-shrink-0 mt-0.5" />
          <span className="text-sm text-amber-800 flex-1 min-w-0">
            <span className="font-semibold">Tip:</span> You have items added 3+ months ago that aren't shared.
            Consider listing on the marketplace:{' '}
            <span className="font-semibold">{tipItem.name}</span>
          </span>
          <button onClick={onGoToMarketplace}
            className="text-xs font-semibold px-3 py-1.5 rounded-lg flex-shrink-0 transition-colors hover:opacity-90"
            style={{ backgroundColor: '#8B0000', color: 'white' }}>
            List it
          </button>
        </div>
      )}

      {showOrgManager && <OrgManagerModal onClose={() => setShowOrgManager(false)} />}
    </div>
  );
}
