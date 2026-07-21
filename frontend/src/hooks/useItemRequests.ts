import { useCallback, useEffect, useState } from 'react';
import { localData } from '../lib/localData';
import { ItemRequest } from '../types';
import { useCurrentUser } from './useCurrentUser';

interface PostRequestInput { itemName: string; category?: string; notes?: string }

export function useItemRequests() {
  const { user, currentOrg } = useCurrentUser();
  const [requests, setRequests] = useState<ItemRequest[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => { setRequests(localData.getRequests()); setLoading(false); }, []);

  const postRequest = useCallback(async ({ itemName, category, notes }: PostRequestInput) => {
    const request: ItemRequest = { id: Date.now(), org: currentOrg, itemName: itemName.trim(),
      category: category || null, notes: notes?.trim() || null, status: 'pending',
      createdBy: user?.id, createdAt: new Date().toISOString() };
    setRequests((previous) => localData.saveRequests([request, ...previous]));
  }, [currentOrg, user]);

  const deleteRequest = useCallback(async (id: number) => {
    setRequests((previous) => localData.saveRequests(previous.filter((request) => request.id !== id)));
  }, []);

  const fulfillRequest = useCallback(async (id: number) => {
    setRequests((previous) => localData.saveRequests(previous.map((request) => request.id === id
      ? { ...request, status: 'fulfilled' as const } : request)));
  }, []);

  return { requests, loading, count: requests.length, postRequest, deleteRequest, fulfillRequest };
}
