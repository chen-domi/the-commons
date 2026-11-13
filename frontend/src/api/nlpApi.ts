import { getCsrfToken } from './authApi';

export interface ItemDraft {
  name: string | null;
  category: string | null;
  location: string | null;
  quantity: number | null;
  lastUsed: string | null;
  shared: boolean | null;
  missingRequiredFields: string[];
}

export async function getNlpAvailability(signal: AbortSignal): Promise<boolean> {
  const response = await fetch('/api/nlp/status', { credentials: 'include', signal });
  if (!response.ok) return false;
  return (await response.json()).available === true;
}

export async function parseItemDescription(description: string, organization: string,
  signal: AbortSignal): Promise<ItemDraft> {
  const csrf = await getCsrfToken();
  if (signal.aborted) throw new DOMException('Request cancelled', 'AbortError');
  const response = await fetch('/api/inventory/parse-description', {
    method: 'POST', credentials: 'include', signal,
    headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
    body: JSON.stringify({ description, organization }),
  });
  if (!response.ok) {
    let message = 'AI form filling failed. Please try again or enter the item manually.';
    try {
      const body = await response.json();
      if (typeof body.message === 'string') message = body.message;
    } catch { /* Keep a safe fallback for non-JSON error pages. */ }
    throw new Error(message);
  }
  return response.json();
}
