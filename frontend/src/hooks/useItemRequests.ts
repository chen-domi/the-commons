import { useState, useEffect, useCallback } from 'react';
import { supabase } from '../lib/supabase';
import { ItemRequest } from '../types';
import { useCurrentUser } from './useCurrentUser';

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

interface PostRequestInput {
  itemName: string;
  category?: string;
  notes?: string;
}

interface UseItemRequestsResult {
  requests: ItemRequest[];
  loading: boolean;
  count: number;
  postRequest: (input: PostRequestInput) => Promise<void>;
  deleteRequest: (id: number) => Promise<void>;
  fulfillRequest: (id: number) => Promise<void>;
}

/**
 * Manages item requests (wanted board) for the current org.
 */
export function useItemRequests(): UseItemRequestsResult {
  const { user, currentOrg } = useCurrentUser();
  const [requests, setRequests] = useState<ItemRequest[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    supabase
      .from('item_requests')
      .select('*')
      .order('created_at', { ascending: false })
      .then(({ data }) => {
        if (data) setRequests(data.map(rowToRequest));
        setLoading(false);
      });
  }, []);

  const postRequest = useCallback(async ({ itemName, category, notes }: PostRequestInput) => {
    const row = {
      org:        currentOrg,
      item_name:  itemName.trim(),
      category:   category || null,
      notes:      notes?.trim() || null,
      created_by: user?.id ?? null,
    };
    const { data, error } = await supabase
      .from('item_requests')
      .insert(row)
      .select()
      .single();

    if (data) {
      setRequests((prev) => [rowToRequest(data), ...prev]);
    } else {
      if (error) console.error('Request post failed:', error.message);
      // Fallback: local state
      setRequests((prev) => [{
        id: Date.now(),
        org: currentOrg,
        itemName: itemName.trim(),
        category: category || null,
        notes: notes?.trim() || null,
        status: 'pending',
        createdAt: new Date().toISOString(),
      }, ...prev]);
    }
  }, [currentOrg, user]);

  const deleteRequest = useCallback(async (id: number) => {
    await supabase.from('item_requests').delete().eq('id', id);
    setRequests((prev) => prev.filter((r) => r.id !== id));
  }, []);

  const fulfillRequest = useCallback(async (id: number) => {
    await supabase.from('item_requests').update({ status: 'fulfilled' }).eq('id', id);
    setRequests((prev) => prev.map((r) => r.id === id ? { ...r, status: 'fulfilled' as const } : r));
  }, []);

  return {
    requests,
    loading,
    count: requests.length,
    postRequest,
    deleteRequest,
    fulfillRequest,
  };
}
