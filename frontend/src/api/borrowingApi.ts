import {
  BorrowingNotificationSummary,
  BorrowingRequest,
} from '../types';
import { getCsrfToken } from './authApi';

interface CreateBorrowingRequestBody {
  inventoryItemId: number;
  borrowingOrganization: string;
  quantity: number;
  purpose: string;
  startDate: string;
  dueDate: string;
}

async function responseError(response: Response): Promise<Error> {
  try {
    const data: { message?: string } = await response.json();
    if (data.message) return new Error(data.message);
  } catch {
    // The response did not contain a JSON error body.
  }
  return new Error(`Borrowing request failed (${response.status})`);
}

async function mutationHeaders(includeJson = false): Promise<HeadersInit> {
  const csrf = await getCsrfToken();
  return {
    ...(includeJson ? { 'Content-Type': 'application/json' } : {}),
    [csrf.headerName]: csrf.token,
  };
}

export async function getBorrowingRequests(): Promise<BorrowingRequest[]> {
  const response = await fetch('/api/borrowing-requests', {
    credentials: 'include',
  });
  if (!response.ok) throw await responseError(response);
  return response.json();
}

export async function getBorrowingNotificationSummary(): Promise<BorrowingNotificationSummary> {
  const response = await fetch('/api/borrowing-requests/notifications', {
    credentials: 'include',
  });
  if (!response.ok) throw await responseError(response);
  return response.json();
}

export async function createBorrowingRequest(
  body: CreateBorrowingRequestBody
): Promise<BorrowingRequest> {
  const response = await fetch('/api/borrowing-requests', {
    method: 'POST',
    credentials: 'include',
    headers: await mutationHeaders(true),
    body: JSON.stringify(body),
  });
  if (!response.ok) throw await responseError(response);
  return response.json();
}

async function transitionRequest(
  id: number,
  action: 'approve' | 'deny' | 'cancel' | 'return'
): Promise<BorrowingRequest> {
  const response = await fetch(`/api/borrowing-requests/${id}/${action}`, {
    method: 'PATCH',
    credentials: 'include',
    headers: await mutationHeaders(),
  });
  if (!response.ok) throw await responseError(response);
  return response.json();
}

export const approveBorrowingRequest = (id: number) =>
  transitionRequest(id, 'approve');
export const denyBorrowingRequest = (id: number) =>
  transitionRequest(id, 'deny');
export const cancelBorrowingRequest = (id: number) =>
  transitionRequest(id, 'cancel');
export const returnBorrowingRequest = (id: number) =>
  transitionRequest(id, 'return');
