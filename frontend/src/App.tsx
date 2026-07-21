import React, { useState, useEffect } from 'react';
import { Search, Package, Recycle, ArrowLeftRight, Plus, Globe, Inbox, ShieldCheck, Trophy } from 'lucide-react';

import { AuthProvider, useAuth } from './context/AuthContext';
import { localData } from './lib/localData';
import Header from './components/Header';
import ImpactDashboard from './components/ImpactDashboard';
import QRScannerModal from './components/QRScannerModal';
import InventoryTable from './components/InventoryTable';
import SharingMarketplace from './components/SharingMarketplace';
import RequestsBoard from './components/RequestsBoard';
import Combobox from './components/Combobox';
import LoginPage from './components/LoginPage';
import AddItemModal from './components/AddItemModal';
import CheckoutDetailsModal from './components/CheckoutDetailsModal';
import Leaderboard from './components/Leaderboard';
import { InventoryItem, ScanResult } from './types';

type Tab = 'club-inventory' | 'global-inventory' | 'marketplace' | 'wanted' | 'leaderboard';

export default function App() {
  return (
    <AuthProvider>
      <AppInner />
    </AuthProvider>
  );
}

function AppInner() {
  const { user, loading, needsOrgSelection } = useAuth();

  if (loading) return (
    <div className="min-h-screen flex items-center justify-center" style={{ backgroundColor: '#6B0000' }}>
      <div className="w-10 h-10 rounded-full border-4 border-white/20 border-t-white animate-spin" />
    </div>
  );

  // Not logged in, or needs to pick org + enter PIN
  if (!user || needsOrgSelection) return <LoginPage />;

  return <MainApp />;
}

function MainApp() {
  const { user } = useAuth();
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [loadingItems, setLoadingItems] = useState(true);
  const [activeTab, setActiveTab] = useState<Tab>('club-inventory');
  const [searchTerm, setSearchTerm] = useState('');
  const [showScanner, setShowScanner] = useState(false);
  const [checkedOutItems, setCheckedOutItems] = useState<string[]>([]);
  const [scanResult, setScanResult] = useState<ScanResult | null>(null);
  const [showAddItem, setShowAddItem] = useState(false);
  const [editingItem, setEditingItem] = useState<InventoryItem | null>(null);
  const [pendingCheckoutQR, setPendingCheckoutQR] = useState<string | null>(null);
  const [filterCategory, setFilterCategory] = useState('');
  const [filterOrg, setFilterOrg] = useState('');
  const [requestCount, setRequestCount] = useState(0);

  const isAdmin = !!user?.isOSIAdmin;
  // Role is stored in localStorage after PIN entry — scoped to the current session's org
  const currentRole = localStorage.getItem('currentRole') as 'eboard' | null;
  const canAdd = isAdmin || currentRole === 'eboard';

  // Auto-close scanner 2s after scan
  useEffect(() => {
    if (!scanResult) return;
    const t = setTimeout(() => { setScanResult(null); setShowScanner(false); }, 2000);
    return () => clearTimeout(t);
  }, [scanResult]);

  // Reset search and filters on tab change
  useEffect(() => { setSearchTerm(''); setFilterCategory(''); setFilterOrg(''); }, [activeTab]);

  // Local browser data keeps the frontend usable until the Spring API is connected.
  useEffect(() => {
    const data = localData.getInventory();
    setItems(data);
    setCheckedOutItems(data.filter((item) => item.checkedOut).map((item) => item.qrCode));
    setLoadingItems(false);
  }, []);

  // Load request count for tab badge
  useEffect(() => {
    setRequestCount(localData.getRequests().length);
  }, []);

  const handleSaveItem = async (saved: InventoryItem) => {
    const isNew = !items.some((i) => i.id === saved.id);
    setItems((prev) => {
      const next = isNew ? [...prev, saved] : prev.map((i) => (i.id === saved.id ? saved : i));
      return localData.saveInventory(next);
    });
    setShowAddItem(false);
    setEditingItem(null);
  };

  const handleDeleteItem = async (id: number) => {
    setItems((prev) => localData.saveInventory(prev.filter((i) => i.id !== id)));
  };

  const handleToggleShare = async (id: number, shared: boolean) => {
    setItems((prev) => localData.saveInventory(prev.map((i) => (i.id === id ? { ...i, shared } : i))));
  };

  const handleScan = (qrCode: string) => {
    const item = items.find((i) => i.qrCode === qrCode);
    if (!item) return;
    const isOut = checkedOutItems.includes(qrCode);
    if (isOut) {
      // Checking IN — execute immediately, no prompt needed
      executeCheckout(qrCode, null, null);
    } else {
      // Checking OUT — show details prompt
      setShowScanner(false);
      setPendingCheckoutQR(qrCode);
    }
  };

  const executeCheckout = async (qrCode: string, purpose: string | null, dueDate: string | null) => {
    const item = items.find((i) => i.qrCode === qrCode);
    if (!item) return;
    const isOut = checkedOutItems.includes(qrCode);
    if (!isOut) {
      setItems((prev) => prev.map((i) =>
        i.qrCode === qrCode
          ? { ...i, checkedOut: true, borrowCount: (i.borrowCount ?? 0) + 1, checkoutPurpose: purpose ?? undefined, checkoutDueDate: dueDate ?? undefined }
          : i
      ));
    } else {
      setItems((prev) => prev.map((i) =>
        i.qrCode === qrCode
          ? { ...i, checkedOut: false, checkoutPurpose: undefined, checkoutDueDate: undefined }
          : i
      ));
    }
    setItems((prev) => localData.saveInventory(prev));
    setCheckedOutItems((prev) => isOut ? prev.filter((q) => q !== qrCode) : [...prev, qrCode]);
    setScanResult({ item, action: isOut ? 'Checked In' : 'Checked Out' });
    setShowScanner(true);
    setPendingCheckoutQR(null);
  };

  const handleTableQRClick = (qrCode: string) => {
    setScanResult(null);
    handleScan(qrCode);
  };

  const applySearch = (list: InventoryItem[]) => {
    const q = searchTerm.toLowerCase();
    return list.filter(({ name, org, category, qrCode }) => {
      if (filterCategory && category !== filterCategory) return false;
      if (filterOrg && org !== filterOrg) return false;
      if (!q) return true;
      return name.toLowerCase().includes(q) || org.toLowerCase().includes(q) ||
        category.toLowerCase().includes(q) || qrCode.toLowerCase().includes(q);
    });
  };

  const allCategories = Array.from(new Set(items.map((i) => i.category))).sort();
  const allOrgs = Array.from(new Set(items.map((i) => i.org))).sort();

  // Club inventory = own org (or all for admin)
  const clubItems = isAdmin ? items : items.filter((i) => i.org === user?.currentOrg);
  const globalItems = items;

  type TabDef = { key: Tab; label: string; icon: React.ReactNode; count?: number; eboardOnly?: boolean };
  const tabs: TabDef[] = [
    { key: 'club-inventory',   label: isAdmin ? 'All Organizations' : 'Your Inventory', icon: <Package size={15} />, count: clubItems.length },
    { key: 'global-inventory', label: 'Global Inventory', icon: <Globe size={15} />,            count: globalItems.length },
    { key: 'marketplace',      label: 'Marketplace',      icon: <ArrowLeftRight size={15} />,   count: items.filter((i) => i.shared).length },
    { key: 'wanted',           label: 'Wanted',           icon: <Inbox size={15} />,            count: requestCount },
    { key: 'leaderboard',      label: 'Leaderboard',      icon: <Trophy size={15} /> },
  ];

  // Only show Settings tab for eboard / OSI admin
  const visibleTabs = tabs.filter((t) => !t.eboardOnly || canAdd);

  return (
    <div className="min-h-screen" style={{ backgroundColor: '#f8f4ee' }}>
      <Header />

      {isAdmin && (
        <div className="flex items-center justify-center gap-2 px-4 py-2 text-xs font-semibold"
          style={{ backgroundColor: '#CFB87C', color: '#1a2744' }}>
          <ShieldCheck size={13} />
          OSI Admin — viewing all organizations
        </div>
      )}

      <main className="max-w-6xl mx-auto px-4 py-8">
        <ImpactDashboard
          items={items}
          onAddItem={() => { setActiveTab('club-inventory'); setEditingItem(null); setShowAddItem(true); }}
          onGoToMarketplace={() => setActiveTab('marketplace')}
          onScanClick={() => { setScanResult(null); setShowScanner(true); }}
        />

        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
          {/* Tab bar */}
          <div className="flex border-b border-gray-100 overflow-x-auto">
            {visibleTabs.map((tab) => (
              <button key={tab.key} onClick={() => setActiveTab(tab.key)}
                className="flex items-center gap-2 px-6 py-4 text-sm font-semibold transition-colors border-b-2 -mb-px whitespace-nowrap flex-shrink-0"
                style={activeTab === tab.key
                  ? { borderColor: '#8B0000', color: '#8B0000' }
                  : { borderColor: 'transparent', color: '#6b7280' }}>
                {tab.icon}
                {tab.label}
                {tab.count !== undefined && (
                  <span className="text-xs font-bold px-2 py-0.5 rounded-full ml-0.5"
                    style={activeTab === tab.key
                      ? { backgroundColor: '#8B0000', color: 'white' }
                      : { backgroundColor: '#e5e7eb', color: '#6b7280' }}>
                    {tab.count}
                  </span>
                )}
              </button>
            ))}
          </div>

          {/* Toolbar */}
          {activeTab !== 'wanted' && activeTab !== 'leaderboard' && (
          <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50 flex flex-wrap items-center gap-3">
            {activeTab !== 'marketplace' && (
              <div className="relative max-w-sm flex-1 min-w-[160px]">
                <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                <input type="text" placeholder="Search items, orgs, categories…"
                  value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-9 pr-4 py-2.5 text-sm rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:border-transparent bg-white"
                  style={{ '--tw-ring-color': '#CFB87C' } as React.CSSProperties} />
              </div>
            )}

            {activeTab !== 'club-inventory' && (
              <>
                <Combobox
                  options={allCategories}
                  value={filterCategory}
                  onChange={setFilterCategory}
                  placeholder="All Categories"
                  allOptionLabel="All Categories"
                  className="min-w-[150px]"
                  style={{ '--tw-ring-color': '#CFB87C' } as React.CSSProperties}
                />
                <Combobox
                  options={allOrgs}
                  value={filterOrg}
                  onChange={setFilterOrg}
                  placeholder="All Organizations"
                  allOptionLabel="All Organizations"
                  className="min-w-[180px]"
                  style={{ '--tw-ring-color': '#CFB87C' } as React.CSSProperties}
                />
              </>
            )}

            {canAdd && activeTab === 'club-inventory' && (
              <button onClick={() => { setEditingItem(null); setShowAddItem(true); }}
                className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl text-sm font-semibold text-white transition-all hover:opacity-90 active:scale-95 flex-shrink-0"
                style={{ backgroundColor: '#8B0000' }}>
                <Plus size={15} />
                Add Item
              </button>
            )}
          </div>
          )}

          {/* Content */}
          <div className="p-5">
            {loadingItems ? (
              <div className="text-center py-16 text-gray-400">
                <div className="w-8 h-8 rounded-full border-4 border-gray-200 border-t-gray-400 animate-spin mx-auto mb-3" />
                <p className="text-sm">Loading inventory…</p>
              </div>
            ) : (
              <>
                {activeTab === 'club-inventory' && (
                  <InventoryTable
                    items={applySearch(clubItems)}
                    checkedOutItems={checkedOutItems}
                    viewMode="club"
                    onScanClick={handleTableQRClick}
                    onEdit={(item) => { setEditingItem(item); setShowAddItem(true); }}
                    onDelete={handleDeleteItem}
                    onToggleShare={handleToggleShare}
                  />
                )}
                {activeTab === 'global-inventory' && (
                  <InventoryTable
                    items={applySearch(globalItems)}
                    checkedOutItems={checkedOutItems}
                    viewMode="global"
                    onScanClick={handleTableQRClick}
                    onEdit={(item) => { setEditingItem(item); setShowAddItem(true); }}
                    onDelete={handleDeleteItem}
                    onToggleShare={handleToggleShare}
                  />
                )}
                {activeTab === 'marketplace' && (
                  <SharingMarketplace items={items} checkedOutItems={checkedOutItems} filterCategory={filterCategory} filterOrg={filterOrg} />
                )}
                {activeTab === 'wanted' && (
                  <RequestsBoard
                    onGoToInventory={() => setActiveTab('club-inventory')}
                    onCountChange={setRequestCount}
                  />
                )}
                {activeTab === 'leaderboard' && (
                  <Leaderboard items={items} />
                )}
              </>
            )}
          </div>
        </div>

        <div className="mt-8 flex flex-wrap items-center justify-center gap-4 text-xs text-gray-400">
          <span className="flex items-center gap-1.5"><Recycle size={13} />Reducing waste across Boston College</span>
          <span className="hidden sm:block">&bull;</span>
          <span>300+ Student Organizations</span>
          <span className="hidden sm:block">&bull;</span>
          <span>Spring 2026</span>
        </div>
      </main>

      {pendingCheckoutQR && (() => {
        const pendingItem = items.find((i) => i.qrCode === pendingCheckoutQR);
        return pendingItem ? (
          <CheckoutDetailsModal
            item={pendingItem}
            onConfirm={(purpose, dueDate) => executeCheckout(pendingCheckoutQR, purpose, dueDate)}
            onCancel={() => setPendingCheckoutQR(null)}
          />
        ) : null;
      })()}

      {showAddItem && (
        <AddItemModal
          item={editingItem ?? undefined}
          nextId={items.length > 0 ? Math.max(...items.map((i) => i.id)) + 1 : 1}
          onSave={handleSaveItem}
          onClose={() => { setShowAddItem(false); setEditingItem(null); }}
        />
      )}

      {showScanner && (
        <QRScannerModal
          onClose={() => setShowScanner(false)}
          onScan={handleScan}
          scanResult={scanResult}
          checkedOutItems={checkedOutItems}
        />
      )}
    </div>
  );
}
