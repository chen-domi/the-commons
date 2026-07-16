import { useState, useEffect, useCallback } from 'react';
import { supabase } from '../lib/supabase';
import { InventoryItem } from '../types';
import { useCurrentUser } from './useCurrentUser';

function rowToItem(row: any): InventoryItem {
  return {
    id:         row.id,
    qrCode:     row.qr_code,
    name:       row.name,
    category:   row.category,
    org:        row.org,
    location:   row.location,
    quantity:   row.quantity,
    lastUsed:   row.last_used,
    shared:     row.shared,
    checkedOut: row.checked_out,
    createdAt:  row.created_at,
  };
}

interface UseInventoryResult {
  items: InventoryItem[];
  loading: boolean;
  checkedOutItems: string[];
  saveItem: (item: InventoryItem) => Promise<void>;
  deleteItem: (id: number) => Promise<void>;
  toggleShare: (id: number, shared: boolean) => Promise<void>;
  toggleCheckout: (qrCode: string) => Promise<{ action: 'Checked In' | 'Checked Out'; item: InventoryItem } | null>;
}

/**
 * Manages inventory CRUD for the current session org.
 * Pass orgFilter to restrict to a specific org, or omit for all items.
 */
export function useInventory(orgFilter?: string): UseInventoryResult {
  const { user } = useCurrentUser();
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [checkedOutItems, setCheckedOutItems] = useState<string[]>([]);

  useEffect(() => {
    let query = supabase.from('inventory').select('*').order('id');
    if (orgFilter) query = query.eq('org', orgFilter);

    query.then(({ data }) => {
      if (data) {
        setItems(data.map(rowToItem));
        setCheckedOutItems(data.filter((r) => r.checked_out).map((r) => r.qr_code));
      }
      setLoading(false);
    });
  }, [orgFilter]);

  const saveItem = useCallback(async (saved: InventoryItem) => {
    const isNew = !items.some((i) => i.id === saved.id);
    const row = {
      qr_code:   saved.qrCode,
      name:      saved.name,
      category:  saved.category,
      org:       saved.org,
      location:  saved.location,
      quantity:  saved.quantity,
      last_used: saved.lastUsed,
      shared:    saved.shared,
    };

    if (isNew) {
      const { data, error } = await supabase
        .from('inventory')
        .insert({ ...row, created_by: user?.id ?? null })
        .select()
        .single();
      if (data) {
        setItems((prev) => [...prev, rowToItem(data)]);
      } else {
        if (error) console.error('Inventory insert failed:', error.message);
        setItems((prev) => [...prev, saved]);
      }
    } else {
      const { error } = await supabase.from('inventory').update(row).eq('id', saved.id);
      if (error) console.error('Inventory update failed:', error.message);
      setItems((prev) => prev.map((i) => (i.id === saved.id ? saved : i)));
    }
  }, [items, user]);

  const deleteItem = useCallback(async (id: number) => {
    await supabase.from('inventory').delete().eq('id', id);
    setItems((prev) => prev.filter((i) => i.id !== id));
  }, []);

  const toggleShare = useCallback(async (id: number, shared: boolean) => {
    await supabase.from('inventory').update({ shared }).eq('id', id);
    setItems((prev) => prev.map((i) => (i.id === id ? { ...i, shared } : i)));
  }, []);

  const toggleCheckout = useCallback(async (qrCode: string) => {
    const item = items.find((i) => i.qrCode === qrCode);
    if (!item) return null;
    const isOut = checkedOutItems.includes(qrCode);
    await supabase.from('inventory').update({ checked_out: !isOut }).eq('qr_code', qrCode);
    setCheckedOutItems((prev) => isOut ? prev.filter((q) => q !== qrCode) : [...prev, qrCode]);
    return { item, action: (isOut ? 'Checked In' : 'Checked Out') as 'Checked In' | 'Checked Out' };
  }, [items, checkedOutItems]);

  return { items, loading, checkedOutItems, saveItem, deleteItem, toggleShare, toggleCheckout };
}
