export interface AuthenticatedUser {
  name: string;
  email: string;
  pictureUrl: string | null;
  globalRole: 'USER' | 'ADMIN';
}

export interface CsrfToken {
  headerName: string;
  token: string;
}

export async function getCurrentUser(): Promise<AuthenticatedUser | null> {
  const response = await fetch('/api/auth/me', {
    credentials: 'include',
  });

  if (response.status === 401) return null;
  if (!response.ok) throw new Error('Could not check login status');

  return response.json();
}

export async function getCsrfToken(): Promise<CsrfToken> {
  const response = await fetch('/api/auth/csrf', {
    credentials: 'include',
  });

  if (!response.ok) throw new Error('Could not prepare secure request');

  return response.json();
}

export async function endCurrentSession(): Promise<void> {
  const response = await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'include',
  });

  if (!response.ok) throw new Error('Could not sign out');
}
