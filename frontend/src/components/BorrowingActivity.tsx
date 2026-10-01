import React, { useEffect, useState } from 'react';
import { CalendarDays, Inbox, Package } from 'lucide-react';
import {
  approveBorrowingRequest,
  cancelBorrowingRequest,
  denyBorrowingRequest,
  getBorrowingRequests,
  returnBorrowingRequest,
} from '../api/borrowingApi';
import { useAuth } from '../context/AuthContext';
import { BorrowingRequest, BorrowingRequestStatus } from '../types';

interface BorrowingActivityProps {
  onSummaryChanged: () => void;
}

const statusStyles: Record<BorrowingRequestStatus, string> = {
  PENDING: 'bg-amber-50 text-amber-700',
  APPROVED: 'bg-green-50 text-green-700',
  DENIED: 'bg-red-50 text-red-700',
  CANCELLED: 'bg-gray-100 text-gray-600',
  RETURNED: 'bg-blue-50 text-blue-700',
};

export default function BorrowingActivity({ onSummaryChanged }: BorrowingActivityProps) {
  const { user } = useAuth();
  const [requests, setRequests] = useState<BorrowingRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [updatingId, setUpdatingId] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    getBorrowingRequests()
      .then((data) => { if (!cancelled) setRequests(data); })
      .catch((requestError) => {
        if (!cancelled) {
          setError(requestError instanceof Error ? requestError.message : 'Could not load borrowing requests');
        }
      })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  const canManage = (organization: string) =>
    !!user?.isOSIAdmin ||
    !!user?.organizations.some((membership) => membership.org === organization);

  async function transition(
    request: BorrowingRequest,
    action: 'approve' | 'deny' | 'cancel' | 'return'
  ) {
    setUpdatingId(request.id);
    setError('');
    try {
      const updated = action === 'approve'
        ? await approveBorrowingRequest(request.id)
        : action === 'deny'
          ? await denyBorrowingRequest(request.id)
          : action === 'cancel'
            ? await cancelBorrowingRequest(request.id)
            : await returnBorrowingRequest(request.id);
      setRequests((current) => current.map((item) => item.id === updated.id ? updated : item));
      onSummaryChanged();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Could not update borrowing request');
    } finally {
      setUpdatingId(null);
    }
  }

  if (loading) {
    return <p className="text-sm text-gray-400 py-12 text-center">Loading borrowing activity…</p>;
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-5">
        <div>
          <h2 className="font-bold text-gray-800">Borrowing Activity</h2>
          <p className="text-sm text-gray-500">Incoming and outgoing inventory requests</p>
        </div>
        <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-gray-100 text-gray-600">
          {requests.length}
        </span>
      </div>

      {error && <p className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}

      {requests.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Inbox size={40} className="mx-auto mb-3 opacity-40" />
          <p className="font-medium">No borrowing activity yet.</p>
          <p className="text-sm mt-1">Requests submitted through the marketplace will appear here.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {requests.map((request) => {
            const managesLender = canManage(request.lendingOrganization);
            const managesBorrower = canManage(request.borrowingOrganization);
            const busy = updatingId === request.id;
            return (
              <div key={request.id} className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="flex gap-3 min-w-0">
                    <div className="w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 bg-red-50 text-red-800">
                      <Package size={16} />
                    </div>
                    <div className="min-w-0">
                      <h3 className="font-semibold text-gray-800 truncate">{request.itemName}</h3>
                      <p className="text-sm text-gray-500">
                        {request.borrowingOrganization} requesting {request.quantity} from {request.lendingOrganization}
                      </p>
                    </div>
                  </div>
                  <span className={`text-xs font-semibold px-2.5 py-1 rounded-full ${statusStyles[request.status]}`}>
                    {request.status.toLowerCase()}
                  </span>
                </div>

                <div className="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-2 text-sm text-gray-600">
                  <p>{request.purpose}</p>
                  <p className="flex items-center gap-1.5 sm:justify-end">
                    <CalendarDays size={13} className="text-gray-400" />
                    {request.startDate} through {request.dueDate}
                  </p>
                </div>

                <div className="mt-4 flex flex-wrap justify-end gap-2">
                  {request.status === 'PENDING' && managesBorrower && (
                    <button disabled={busy} onClick={() => transition(request, 'cancel')}
                      className="px-3 py-2 rounded-lg text-xs font-semibold border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-50">
                      Cancel
                    </button>
                  )}
                  {request.status === 'PENDING' && managesLender && (
                    <>
                      <button disabled={busy} onClick={() => transition(request, 'deny')}
                        className="px-3 py-2 rounded-lg text-xs font-semibold border border-red-200 text-red-600 hover:bg-red-50 disabled:opacity-50">
                        Deny
                      </button>
                      <button disabled={busy} onClick={() => transition(request, 'approve')}
                        className="px-3 py-2 rounded-lg text-xs font-semibold text-white disabled:opacity-50"
                        style={{ backgroundColor: '#8B0000' }}>
                        Approve
                      </button>
                    </>
                  )}
                  {request.status === 'APPROVED' && managesLender && (
                    <button disabled={busy} onClick={() => transition(request, 'return')}
                      className="px-3 py-2 rounded-lg text-xs font-semibold text-white disabled:opacity-50"
                      style={{ backgroundColor: '#8B0000' }}>
                      Mark Returned
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
