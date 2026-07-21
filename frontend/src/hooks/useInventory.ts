import { useCallback, useEffect, useState } from 'react';
import { localData } from '../lib/localData';
import { InventoryItem } from '../types';

export function useInventory(orgFilter?: string) {
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [checkedOutItems, setCheckedOutItems] = useState<string[]>([]);

  useEffect(() => {
    const all = localData.getInventory();
    const visible = orgFilter ? all.filter((item) => item.org === orgFilter) : all;
    setItems(visible);
    setCheckedOutItems(visible.filter((item) => item.checkedOut).map((item) => item.qrCode));
    setLoading(false);
  }, [orgFilter]);

  const persistVisible = useCallback((next: InventoryItem[]) => {
    if (!orgFilter) return localData.saveInventory(next);
    const otherOrgs = localData.getInventory().filter((item) => item.org !== orgFilter);
    localData.saveInventory([...otherOrgs, ...next]);
    return next;
  }, [orgFilter]);

  const saveItem = useCallback(async (saved: InventoryItem) => {
    setItems((previous) => persistVisible(previous.some((item) => item.id === saved.id)
      ? previous.map((item) => item.id === saved.id ? saved : item)
      : [...previous, saved]));
  }, [persistVisible]);

  const deleteItem = useCallback(async (id: number) => {
    setItems((previous) => persistVisible(previous.filter((item) => item.id !== id)));
  }, [persistVisible]);

  const toggleShare = useCallback(async (id: number, shared: boolean) => {
    setItems((previous) => persistVisible(previous.map((item) => item.id === id ? { ...item, shared } : item)));
  }, [persistVisible]);

  const toggleCheckout = useCallback(async (qrCode: string) => {
    const item = items.find((candidate) => candidate.qrCode === qrCode);
    if (!item) return null;
    const isOut = checkedOutItems.includes(qrCode);
    setItems((previous) => persistVisible(previous.map((candidate) => candidate.qrCode === qrCode
      ? { ...candidate, checkedOut: !isOut } : candidate)));
    setCheckedOutItems((previous) => isOut ? previous.filter((code) => code !== qrCode) : [...previous, qrCode]);
    return { item, action: (isOut ? 'Checked In' : 'Checked Out') as 'Checked In' | 'Checked Out' };
  }, [items, checkedOutItems, persistVisible]);

  return { items, loading, checkedOutItems, saveItem, deleteItem, toggleShare, toggleCheckout };
}
